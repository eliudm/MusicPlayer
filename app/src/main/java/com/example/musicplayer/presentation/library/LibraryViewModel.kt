package com.example.musicplayer.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.domain.usecase.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortCategory: SortCategory = SortCategory.SONGS,
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    // Derived display data
    val displaySongs: List<Song> = emptyList(),
    val groupedSongs: Map<String, List<Song>> = emptyMap()
)

@OptIn(FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playSong: PlaySongUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.getSongs().collect { songs ->
                _uiState.update { it.copy(songs = songs, isLoading = false) }
                recompute()
            }
        }
        viewModelScope.launch {
            repository.getAlbums().collect { albums ->
                _uiState.update { it.copy(albums = albums) }
            }
        }
        viewModelScope.launch {
            repository.getPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    if (q.isBlank()) repository.getSongs()
                    else repository.searchSongs(q)
                }
                .collect { songs ->
                    _uiState.update { it.copy(songs = songs) }
                    recompute()
                }
        }
    }

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.ScanMedia -> {}
            is LibraryEvent.PlaySong -> viewModelScope.launch { playSong(event.song) }
            is LibraryEvent.Search -> {
                searchQuery.value = event.query
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is LibraryEvent.SongMenuAction -> handleMenuAction(event.song, event.action)
            is LibraryEvent.SetSortCategory -> {
                _uiState.update { it.copy(sortCategory = event.category) }
                recompute()
            }
            is LibraryEvent.SetSortOrder -> {
                _uiState.update { it.copy(sortOrder = event.order) }
                recompute()
            }
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val songs = state.songs
        val category = state.sortCategory
        val order = state.sortOrder

        when (category) {
            SortCategory.SONGS -> {
                val sorted = sortSongs(songs, order)
                _uiState.update { it.copy(displaySongs = sorted, groupedSongs = emptyMap()) }
            }
            SortCategory.ARTISTS -> {
                val grouped = songs
                    .groupBy { it.artist.ifBlank { "Unknown Artist" } }
                    .toSortedMap(compareBy { it.lowercase() })
                    .mapValues { (_, v) -> v.sortedBy { it.title.lowercase() } }
                _uiState.update { it.copy(displaySongs = emptyList(), groupedSongs = grouped) }
            }
            SortCategory.ALBUMS -> {
                val grouped = songs
                    .groupBy { it.album.ifBlank { "Unknown Album" } }
                    .toSortedMap(compareBy { it.lowercase() })
                    .mapValues { (_, v) -> v.sortedBy { it.trackNumber.takeIf { n -> n > 0 }?.toLong() ?: Long.MAX_VALUE } }
                _uiState.update { it.copy(displaySongs = emptyList(), groupedSongs = grouped) }
            }
            SortCategory.GENRES -> {
                val grouped = songs
                    .groupBy { it.genre.ifBlank { "Unknown Genre" } }
                    .toSortedMap(compareBy { it.lowercase() })
                    .mapValues { (_, v) -> v.sortedBy { it.title.lowercase() } }
                _uiState.update { it.copy(displaySongs = emptyList(), groupedSongs = grouped) }
            }
            SortCategory.PLAYLISTS -> {
                // Playlists are shown directly from state.playlists — no song grouping needed
                _uiState.update { it.copy(displaySongs = emptyList(), groupedSongs = emptyMap()) }
            }
            SortCategory.FOLDERS -> {
                val grouped = songs
                    .groupBy { it.bucketName.ifBlank { "Unknown Folder" } }
                    .toSortedMap(compareBy { it.lowercase() })
                    .mapValues { (_, v) -> v.sortedBy { it.title.lowercase() } }
                _uiState.update { it.copy(displaySongs = emptyList(), groupedSongs = grouped) }
            }
        }
    }

    private fun sortSongs(songs: List<Song>, order: SortOrder): List<Song> = when (order) {
        SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
        SortOrder.ARTIST -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        SortOrder.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        SortOrder.DURATION -> songs.sortedByDescending { it.duration }
    }

    private fun handleMenuAction(song: Song, action: com.example.musicplayer.presentation.common.SongMenuAction) {
        when (action) {
            com.example.musicplayer.presentation.common.SongMenuAction.PlayNext ->
                viewModelScope.launch { repository.playNext(song) }
            com.example.musicplayer.presentation.common.SongMenuAction.Enqueue ->
                viewModelScope.launch { repository.enqueue(song) }
            com.example.musicplayer.presentation.common.SongMenuAction.Delete ->
                viewModelScope.launch { repository.deleteSong(song) }
            else -> {}
        }
    }
}
