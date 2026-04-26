package com.example.musicplayer.domain.model

data class DriveBackupFile(
    val id: String,
    val name: String,
    val createdTime: Long,
    val sizeBytes: Long
)
