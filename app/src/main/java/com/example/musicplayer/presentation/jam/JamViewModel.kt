package com.example.musicplayer.presentation.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.service.JamSession
import com.example.musicplayer.service.JamSessionManager
import com.example.musicplayer.service.JamState
import com.example.musicplayer.service.RemoteSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JamUiState(
    val jamState: JamState = JamState(),
    val sessionName: String = "My Jam",
    val hostSongs: List<RemoteSong> = emptyList(),
    val isLoadingHostSongs: Boolean = false,
    val addedSongIds: Set<Long> = emptySet()
)

@HiltViewModel
class JamViewModel @Inject constructor(
    private val jamSessionManager: JamSessionManager,
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            jamSessionManager.state.collect { jamState ->
                _uiState.update { it.copy(jamState = jamState) }
            }
        }
        // Keep host song list fresh
        viewModelScope.launch {
            repository.getSongs().collect { songs ->
                jamSessionManager.updateHostSongs(songs)
            }
        }
        // When remote device adds a song, enqueue it locally
        viewModelScope.launch {
            jamSessionManager.songAddEvents.collect { songId ->
                val song = repository.getSongs().first().find { it.id == songId }
                if (song != null) repository.enqueue(song)
            }
        }
    }

    fun setSessionName(name: String) = _uiState.update { it.copy(sessionName = name) }

    fun startHosting() = viewModelScope.launch {
        val songs = repository.getSongs().first()
        jamSessionManager.startHosting(_uiState.value.sessionName, songs)
    }

    fun stopHosting() = jamSessionManager.stopHosting()

    fun startDiscovery() = jamSessionManager.startDiscovery()

    fun stopDiscovery() = jamSessionManager.stopDiscovery()

    fun browseSession(session: JamSession) = viewModelScope.launch {
        _uiState.update { it.copy(isLoadingHostSongs = true, hostSongs = emptyList()) }
        val songs = jamSessionManager.fetchHostSongs(session)
        _uiState.update { it.copy(hostSongs = songs, isLoadingHostSongs = false) }
    }

    fun leaveSession() {
        jamSessionManager.leaveSession()
        _uiState.update { it.copy(hostSongs = emptyList(), addedSongIds = emptySet()) }
    }

    fun addSongToHost(session: JamSession, songId: Long) = viewModelScope.launch {
        val ok = jamSessionManager.addSongToHost(session, songId)
        if (ok) _uiState.update { it.copy(addedSongIds = it.addedSongIds + songId) }
    }

    override fun onCleared() {
        super.onCleared()
        jamSessionManager.stopDiscovery()
    }
}
