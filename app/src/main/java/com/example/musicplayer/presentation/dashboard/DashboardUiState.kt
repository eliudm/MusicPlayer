package com.example.musicplayer.presentation.dashboard

import com.example.musicplayer.domain.model.Folder
import com.example.musicplayer.domain.model.Song

data class DashboardUiState(
    val allSongs: List<Song> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList()
)
