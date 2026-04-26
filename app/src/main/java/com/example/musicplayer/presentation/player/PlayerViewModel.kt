package com.example.musicplayer.presentation.player

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.service.PlayerController
import com.example.musicplayer.domain.model.AudioFormat
import com.example.musicplayer.domain.model.GestureAction
import com.example.musicplayer.domain.model.GestureSettings
import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.domain.usecase.GetQueueUseCase
import com.example.musicplayer.domain.usecase.PlaySongUseCase
import com.example.musicplayer.domain.usecase.SeekUseCase
import com.example.musicplayer.service.SpectrumProvider
import com.example.musicplayer.util.autoArtworkDir
import com.example.musicplayer.util.artworkDir
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playSong: PlaySongUseCase,
    private val seek: SeekUseCase,
    private val getQueue: GetQueueUseCase,
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    val spectrumProvider: SpectrumProvider,
    private val prefs: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val player: Player get() = playerController.activePlayer

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val gestureSettings: StateFlow<GestureSettings> = combine(
        prefs.gestureSwipeEnabled,
        prefs.gesturePinchEnabled,
        prefs.swipeUpAction,
        prefs.swipeDownAction
    ) { swipe, pinch, up, down ->
        GestureSettings(swipe, pinch, up, down)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GestureSettings())

    private val crossfadeEnabled: StateFlow<Boolean> =
        prefs.crossfadeEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val crossfadeDuration: StateFlow<Float> =
        prefs.crossfadeDuration.stateIn(viewModelScope, SharingStarted.Eagerly, 3f)

    val isCastActive: StateFlow<Boolean> = playerController.isCastActive

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val triedAutoFetch = mutableSetOf<Long>()
    private var fadeInJob: Job? = null

    private val completionListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _uiState.update { it.copy(positionMs = 0L, progress = 0f) }
            }
        }
    }

    init {
        viewModelScope.launch {
            getQueue().collect { songs ->
                _uiState.update { it.copy(queue = songs) }
            }
        }
        viewModelScope.launch {
            repository.getCurrentSong().collect { song ->
                val prevId = _uiState.value.currentSong?.id
                _uiState.update { it.copy(currentSong = song) }
                if (song != null && song.id != prevId) {
                    autoFetchCoverArt(song)
                    if (crossfadeEnabled.value && prevId != null) startFadeIn()
                }
            }
        }
        viewModelScope.launch {
            repository.observeIsPlaying().collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(100)
                if (player.isPlaying) {
                    val pos = player.currentPosition
                    val song = _uiState.value.currentSong
                    val duration = song?.duration?.takeIf { it > 0 } ?: 1L
                    _uiState.update { it.copy(positionMs = pos, progress = pos.toFloat() / duration) }

                    if (crossfadeEnabled.value && song != null && fadeInJob?.isActive != true) {
                        val fadeMs = (crossfadeDuration.value * 1000f).toLong()
                        val remaining = duration - pos
                        if (remaining in 0L..fadeMs) {
                            player.volume = (remaining.toFloat() / fadeMs).coerceIn(0.05f, 1f)
                        }
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.Main) {
            player.addListener(completionListener)
        }
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.PlaySong -> viewModelScope.launch {
                val fmt = event.song.audioFormat
                if (!fmt.isPlayable) {
                    _uiState.update {
                        it.copy(
                            currentSong = event.song,
                            unsupportedFormatMessage = "${fmt.label} format is not supported for playback"
                        )
                    }
                    return@launch
                }
                _uiState.update { it.copy(unsupportedFormatMessage = null) }
                resetVolume()
                val allSongs = repository.getSongs().first()
                val idx = allSongs.indexOfFirst { it.id == event.song.id }
                if (idx >= 0) {
                    repository.playWithQueue(allSongs, idx)
                } else {
                    playSong(event.song)
                }
            }
            is PlayerEvent.Seek -> viewModelScope.launch {
                seek(event.position)
                val duration = _uiState.value.currentSong?.duration?.takeIf { it > 0 } ?: 1L
                _uiState.update { it.copy(positionMs = event.position, progress = event.position.toFloat() / duration) }
            }
            is PlayerEvent.TogglePlayPause -> {
                if (_uiState.value.isPlaying) viewModelScope.launch { repository.pause() }
                else player.play()
            }
            is PlayerEvent.ToggleFav -> viewModelScope.launch {
                val song = _uiState.value.currentSong ?: return@launch
                val nowFav = repository.toggleFavorite(song.id)
                _uiState.update { it.copy(isFavorite = nowFav) }
            }
            is PlayerEvent.SkipNext -> skipNext()
            is PlayerEvent.SkipPrevious -> skipPrevious()
            is PlayerEvent.ToggleShuffle -> {
                val newShuffle = !_uiState.value.shuffleEnabled
                _uiState.update { it.copy(shuffleEnabled = newShuffle) }
                player.shuffleModeEnabled = newShuffle
            }
            is PlayerEvent.CycleRepeat -> {
                val newMode = when (_uiState.value.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                _uiState.update { it.copy(repeatMode = newMode) }
                player.repeatMode = when (newMode) {
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                }
            }
        }
    }

    // ── Volume control ───────────────────────────────────────────────────────

    fun adjustVolume(raise: Boolean) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // ── Crossfade helpers ────────────────────────────────────────────────────

    private fun resetVolume() {
        fadeInJob?.cancel()
        player.volume = 1f
    }

    private fun startFadeIn() {
        fadeInJob?.cancel()
        fadeInJob = viewModelScope.launch {
            val fadeMs = (crossfadeDuration.value * 1000f).toLong().coerceAtLeast(500L)
            val steps = 30
            repeat(steps) { i ->
                delay(fadeMs / steps)
                player.volume = ((i + 1).toFloat() / steps).coerceIn(0f, 1f)
            }
            player.volume = 1f
        }
    }

    // ── Cover art auto-fetch ──────────────────────────────────────────────────

    private fun autoFetchCoverArt(song: Song) {
        val songId = song.id
        if (songId in triedAutoFetch) return

        val customFile = File(artworkDir(context), "$songId.jpg")
        if (customFile.exists()) return

        val autoFile = File(autoArtworkDir(context), "$songId.jpg")
        if (autoFile.exists()) {
            _uiState.update { it.copy(artworkVersion = it.artworkVersion + 1) }
            return
        }

        triedAutoFetch.add(songId)
        viewModelScope.launch(Dispatchers.IO) {
            val url = resolveArtworkUrl(song) ?: return@launch
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.connect()
                if (conn.responseCode == 200) {
                    conn.inputStream.use { input ->
                        autoFile.outputStream().use { input.copyTo(it) }
                    }
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(artworkVersion = it.artworkVersion + 1) }
                        Log.d("PlayerVM", "auto cover saved for '${song.title}'")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w("PlayerVM", "auto cover download failed: ${e.message}")
            }
        }
    }

    private suspend fun resolveArtworkUrl(song: Song): String? = withContext(Dispatchers.IO) {
        val artist = song.artist.trim().takeIf { it.isNotBlank() && it != "<unknown>" } ?: ""
        val album = song.album.trim().takeIf { it.isNotBlank() && it != "<unknown>" } ?: ""

        val queries = buildList {
            if (artist.isNotEmpty() && album.isNotEmpty()) add("$artist $album")
            if (artist.isNotEmpty()) add(artist)
            val match = Regex("""^(.+?)\s+["'](.+?)["']$""").matchEntire(song.title.trim())
            val extractedArtist = match?.groupValues?.get(1)
            val extractedTitle = match?.groupValues?.get(2)
            if (extractedArtist != null && extractedTitle != null)
                add("$extractedArtist $extractedTitle")
        }

        for (query in queries) {
            val url = fetchArtworkUrlFromItunes(query)
            if (url != null) return@withContext url
        }
        null
    }

    private fun fetchArtworkUrlFromItunes(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val json = URL(
                "https://itunes.apple.com/search?term=$encoded&media=music&entity=album&limit=3"
            ).readText()
            val results = JSONObject(json).getJSONArray("results")
            if (results.length() == 0) return null
            results.getJSONObject(0)
                .optString("artworkUrl100", null)
                ?.replace("100x100bb", "600x600bb")
        } catch (e: Exception) {
            Log.w("PlayerVM", "iTunes lookup failed for '$query': ${e.message}")
            null
        }
    }

    // ── Playback helpers ──────────────────────────────────────────────────────

    private fun skipNext() {
        if (_uiState.value.queue.isEmpty()) return
        resetVolume()
        player.seekToNextMediaItem()
    }

    private fun skipPrevious() {
        val state = _uiState.value
        if (state.positionMs > 3000L) {
            viewModelScope.launch { seek(0L) }
            _uiState.update { it.copy(progress = 0f, positionMs = 0L) }
            return
        }
        resetVolume()
        player.seekToPreviousMediaItem()
    }

    override fun onCleared() {
        super.onCleared()
        player.removeListener(completionListener)
    }
}
