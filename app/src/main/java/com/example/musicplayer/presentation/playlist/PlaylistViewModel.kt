package com.example.musicplayer.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.domain.usecase.AddToPlaylistUseCase
import com.example.musicplayer.domain.usecase.CreatePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isCreating: Boolean = false
)

sealed interface PlaylistEvent {
    data class CreatePlaylist(val name: String) : PlaylistEvent
    data class DeletePlaylist(val id: Long) : PlaylistEvent
    data class AddSong(val playlistId: Long, val songId: Long, val position: Int) : PlaylistEvent
    data class RemoveSong(val playlistId: Long, val songId: Long) : PlaylistEvent
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val createPlaylist: CreatePlaylistUseCase,
    private val addToPlaylist: AddToPlaylistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun onEvent(event: PlaylistEvent) {
        when (event) {
            is PlaylistEvent.CreatePlaylist -> viewModelScope.launch {
                createPlaylist(event.name)
            }
            is PlaylistEvent.DeletePlaylist -> viewModelScope.launch {
                repository.deletePlaylist(event.id)
            }
            is PlaylistEvent.AddSong -> viewModelScope.launch {
                addToPlaylist(event.playlistId, event.songId, event.position)
            }
            is PlaylistEvent.RemoveSong -> viewModelScope.launch {
                repository.removeSongFromPlaylist(event.playlistId, event.songId)
            }
        }
    }
}
