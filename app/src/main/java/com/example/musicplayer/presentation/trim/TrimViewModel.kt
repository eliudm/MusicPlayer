package com.example.musicplayer.presentation.trim

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class TrimViewModel @Inject constructor(
    private val repository: MusicRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val songId: Long = savedStateHandle["songId"] ?: -1L

    private val _state = MutableStateFlow(TrimUiState())
    val state: StateFlow<TrimUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val song = repository.getSongs().first().find { it.id == songId } ?: return@launch
            _state.update { it.copy(song = song, endMs = song.duration) }
        }
    }

    fun setRange(startMs: Long, endMs: Long) {
        _state.update { it.copy(startMs = startMs, endMs = endMs) }
    }

    fun clearResult() {
        _state.update { it.copy(savedFileName = null, error = null) }
    }

    fun trim() {
        val song = _state.value.song ?: return
        val startMs = _state.value.startMs
        val endMs = _state.value.endMs
        if (endMs - startMs < 1000L) {
            _state.update { it.copy(error = "Selection must be at least 1 second") }
            return
        }
        _state.update { it.copy(isProcessing = true, error = null, savedFileName = null) }
        viewModelScope.launch {
            val result = performTrim(song, startMs, endMs)
            result.fold(
                onSuccess = { name -> _state.update { it.copy(isProcessing = false, savedFileName = name) } },
                onFailure = { e -> _state.update { it.copy(isProcessing = false, error = e.message ?: "Trim failed") } }
            )
        }
    }

    private suspend fun performTrim(song: Song, startMs: Long, endMs: Long): Result<String> =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, song.uri, null)

                var audioTrackIndex = -1
                var format: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        format = trackFormat
                        break
                    }
                }
                if (audioTrackIndex == -1 || format == null) {
                    return@withContext Result.failure(Exception("No audio track found"))
                }
                extractor.selectTrack(audioTrackIndex)

                val baseName = song.title.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
                val outputName = "${baseName}_trimmed.m4a"

                // Write to a temp file first (works on all API levels)
                val tempFile = File(context.cacheDir, outputName)
                val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrack = muxer.addTrack(format)
                muxer.start()

                val startUs = startMs * 1000L
                val endUs = endMs * 1000L
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
                val info = MediaCodec.BufferInfo()
                while (true) {
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    val sampleTime = extractor.sampleTime
                    if (sampleTime > endUs) break
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrack, buffer, info)
                    extractor.advance()
                }
                muxer.stop()
                muxer.release()

                // Move temp file to Music folder
                val savedName = saveToMusicFolder(tempFile, outputName)
                tempFile.delete()
                Result.success(savedName)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                extractor.release()
            }
        }

    private fun saveToMusicFolder(tempFile: File, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Could not create MediaStore entry")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                tempFile.inputStream().use { it.copyTo(out) }
            }
            val update = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, update, null, null)
            fileName
        } else {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            musicDir.mkdirs()
            val dest = File(musicDir, fileName)
            tempFile.copyTo(dest, overwrite = true)
            fileName
        }
    }
}
