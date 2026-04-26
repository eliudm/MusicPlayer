package com.example.musicplayer.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val songs: List<Song>,
    val createdAt: Long,
    val modifiedAt: Long
)
