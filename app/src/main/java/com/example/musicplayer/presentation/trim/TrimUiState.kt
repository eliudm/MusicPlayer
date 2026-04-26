package com.example.musicplayer.presentation.trim

import com.example.musicplayer.domain.model.Song

data class TrimUiState(
    val song: Song? = null,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val isProcessing: Boolean = false,
    val savedFileName: String? = null,
    val error: String? = null
)
