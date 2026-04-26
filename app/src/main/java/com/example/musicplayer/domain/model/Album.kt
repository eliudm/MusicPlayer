package com.example.musicplayer.domain.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val year: Int,
    val artUri: Uri?
)
