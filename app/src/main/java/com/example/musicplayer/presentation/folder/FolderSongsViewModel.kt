package com.example.musicplayer.presentation.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.domain.usecase.PlaySongUseCase
import com.example.musicplayer.presentation.common.SongMenuAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderSongsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playSong: PlaySongUseCase
) : ViewModel() {

    private val bucketId: Long = checkNotNull(savedStateHandle["bucketId"])

    val songs: StateFlow<List<Song>> = repository.getSongsInFolder(bucketId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folderName: StateFlow<String> = songs
        .map { it.firstOrNull()?.bucketName ?: "Folder" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Folder")

    fun play(song: Song) {
        viewModelScope.launch { playSong(song) }
    }

    fun handleMenuAction(song: Song, action: SongMenuAction) {
        when (action) {
            SongMenuAction.PlayNext -> viewModelScope.launch { repository.playNext(song) }
            SongMenuAction.Enqueue -> viewModelScope.launch { repository.enqueue(song) }
            SongMenuAction.Delete -> viewModelScope.launch { repository.deleteSong(song) }
            else -> { /* Share, Ringtone, Trim, Artwork, AddTo handled in UI layer */ }
        }
    }
}
