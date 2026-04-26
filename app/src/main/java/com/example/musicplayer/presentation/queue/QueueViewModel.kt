package com.example.musicplayer.presentation.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    private val prefs: AppPreferences
) : ViewModel() {

    data class State(
        val queue: List<Song> = emptyList(),
        val currentIndex: Int = 0,
        val autoDjEnabled: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getQueue().collect { queue ->
                _state.update { it.copy(queue = queue) }
            }
        }
        viewModelScope.launch {
            prefs.autoDjEnabled.collect { enabled ->
                _state.update { it.copy(autoDjEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            while (true) {
                _state.update {
                    it.copy(currentIndex = playerController.exoPlayer.currentMediaItemIndex)
                }
                delay(500)
            }
        }
    }

    fun moveItem(from: Int, to: Int) = viewModelScope.launch { repository.moveQueueItem(from, to) }
    fun removeItem(index: Int) = viewModelScope.launch { repository.removeFromQueue(index) }
    fun clearQueue() = viewModelScope.launch { repository.clearQueue() }
    fun toggleAutoDj() = viewModelScope.launch { prefs.setAutoDjEnabled(!_state.value.autoDjEnabled) }
}
