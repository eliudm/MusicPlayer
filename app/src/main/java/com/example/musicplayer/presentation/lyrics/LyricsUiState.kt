package com.example.musicplayer.presentation.lyrics

import com.example.musicplayer.domain.model.Lyrics

enum class LyricsStatus { IDLE, LOADING, LOADED, NOT_FOUND, ERROR }

data class LyricsUiState(
    val status: LyricsStatus = LyricsStatus.IDLE,
    val lyrics: Lyrics? = null,
    val errorMessage: String? = null
)
