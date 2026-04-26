package com.example.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey val songId: Long,
    val lrcContent: String?,
    val plainContent: String?,
    val isSynced: Boolean,
    val isManual: Boolean,
    val fetchedAt: Long = System.currentTimeMillis()
)
