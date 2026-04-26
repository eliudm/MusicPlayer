package com.example.musicplayer.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MiniPlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false
)

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val player: ExoPlayer
) : ViewModel() {

    val state: StateFlow<MiniPlayerState> = combine(
        repository.getCurrentSong(),
        repository.observeIsPlaying()
    ) { song, playing -> MiniPlayerState(song, playing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MiniPlayerState())

    fun togglePlayPause() {
        viewModelScope.launch {
            if (player.isPlaying) repository.pause() else player.play()
        }
    }
}
