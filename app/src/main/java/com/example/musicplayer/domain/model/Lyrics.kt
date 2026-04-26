package com.example.musicplayer.domain.model

data class Lyrics(
    val lines: List<LyricsLine>,
    val isSynced: Boolean,
    val isManual: Boolean
)
