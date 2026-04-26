package com.example.musicplayer.presentation.player

import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val isFavorite: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val artworkVersion: Int = 0,
    val unsupportedFormatMessage: String? = null
)
