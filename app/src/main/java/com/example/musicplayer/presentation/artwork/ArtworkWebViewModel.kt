package com.example.musicplayer.presentation.artwork

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.util.artworkDir
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

data class ArtworkWebState(
    val song: Song? = null,
    val initialUrl: String = "",
    val isDownloading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ArtworkWebViewModel @Inject constructor(
    private val repository: MusicRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val songId: Long = savedStateHandle["songId"] ?: -1L

    private val _state = MutableStateFlow(ArtworkWebState())
    val state: StateFlow<ArtworkWebState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val song = repository.getSongs().first().find { it.id == songId } ?: return@launch
            val query = URLEncoder.encode("${song.artist} ${song.album} album art", "UTF-8")
            _state.update { it.copy(song = song, initialUrl = "https://www.google.com/search?q=$query&tbm=isch") }
        }
    }

    fun downloadAndSave(url: String) {
        _state.update { it.copy(isDownloading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val dest = File(artworkDir(context), "$songId.jpg")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/91.0 Mobile Safari/537.36")
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.connect()
                if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
                conn.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
                conn.disconnect()
            }.onSuccess {
                _state.update { it.copy(isDownloading = false, saved = true) }
            }.onFailure { e ->
                _state.update { it.copy(isDownloading = false, error = "Download failed: ${e.message}") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
