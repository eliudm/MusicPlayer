package com.example.musicplayer.presentation.artwork

import com.example.musicplayer.domain.model.Song

data class ArtworkUiState(
    val song: Song? = null,
    val artworkVersion: Int = 0,
    val showSearchPanel: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<String> = emptyList(),
    val isDownloading: Boolean = false,
    val message: String? = null
)
