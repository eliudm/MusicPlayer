package com.example.musicplayer.presentation.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.LyricsRepository
import com.example.musicplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsRepo: LyricsRepository,
    private val musicRepo: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private var lastSongId = -1L

    init {
        viewModelScope.launch {
            musicRepo.getCurrentSong()
                .distinctUntilChanged { a, b -> a?.id == b?.id }
                .collect { song ->
                    if (song != null && song.id != lastSongId) {
                        lastSongId = song.id
                        fetchLyrics(song)
                    }
                }
        }
    }

    fun retry(song: Song?) {
        if (song == null) return
        viewModelScope.launch {
            lyricsRepo.clearLyrics(song.id)
            lastSongId = -1L
            fetchLyrics(song)
        }
    }

    fun saveManualLyrics(songId: Long, text: String, currentSong: Song?) {
        viewModelScope.launch {
            lyricsRepo.saveManualLyrics(songId, text)
            lastSongId = -1L
            if (currentSong != null) fetchLyrics(currentSong)
        }
    }

    private fun fetchLyrics(song: Song) {
        _uiState.update { it.copy(status = LyricsStatus.LOADING, lyrics = null) }
        viewModelScope.launch {
            lyricsRepo.getLyrics(song).fold(
                onSuccess = { lyrics ->
                    if (lyrics != null && lyrics.lines.isNotEmpty()) {
                        _uiState.update { it.copy(status = LyricsStatus.LOADED, lyrics = lyrics) }
                    } else {
                        _uiState.update { it.copy(status = LyricsStatus.NOT_FOUND, lyrics = null) }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(status = LyricsStatus.ERROR, errorMessage = e.message)
                    }
                }
            )
        }
    }
}
