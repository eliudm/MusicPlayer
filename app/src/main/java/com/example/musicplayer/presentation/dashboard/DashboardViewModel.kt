package com.example.musicplayer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.domain.usecase.PlaySongUseCase
import com.example.musicplayer.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playSong: PlaySongUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSongs().collect { songs ->
                _uiState.update { it.copy(allSongs = songs) }
            }
        }
        viewModelScope.launch {
            repository.getFolders().collect { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }
        viewModelScope.launch {
            repository.getFavorites().collect { songs ->
                _uiState.update { it.copy(favorites = songs) }
            }
        }
        viewModelScope.launch {
            repository.getRecentlyPlayed(5).collect { songs ->
                _uiState.update { it.copy(recentlyPlayed = songs) }
            }
        }
        viewModelScope.launch {
            repository.getRecentlyAdded(20).collect { songs ->
                _uiState.update { it.copy(recentlyAdded = songs) }
            }
        }
        viewModelScope.launch {
            repository.getMostPlayed(20).collect { songs ->
                _uiState.update { it.copy(mostPlayed = songs) }
            }
        }
    }

    fun play(song: Song) {
        viewModelScope.launch { playSong(song) }
    }
}
