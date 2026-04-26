package com.example.musicplayer.presentation.library

import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.presentation.common.SongMenuAction

enum class SortCategory(val label: String) {
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    GENRES("Genres"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders")
}

enum class SortOrder(val label: String) {
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
    ARTIST("Artist"),
    DATE_ADDED("Date Added"),
    DURATION("Duration")
}

sealed interface LibraryEvent {
    data object ScanMedia : LibraryEvent
    data class PlaySong(val song: Song) : LibraryEvent
    data class Search(val query: String) : LibraryEvent
    data class SongMenuAction(val song: Song, val action: com.example.musicplayer.presentation.common.SongMenuAction) : LibraryEvent
    data class SetSortCategory(val category: SortCategory) : LibraryEvent
    data class SetSortOrder(val order: SortOrder) : LibraryEvent
}
