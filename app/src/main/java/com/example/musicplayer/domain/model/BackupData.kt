package com.example.musicplayer.domain.model

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val playlists: List<PlaylistBackup> = emptyList(),
    val favorites: List<Long> = emptyList(),
    val playHistory: List<PlayHistoryBackup> = emptyList(),
    val preferences: PreferencesBackup = PreferencesBackup()
)

data class PlaylistBackup(
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val songIds: List<Long>
)

data class PlayHistoryBackup(
    val songId: Long,
    val playCount: Int,
    val lastPlayedAt: Long
)

data class PreferencesBackup(
    val themeMode: String = "SYSTEM",
    val pauseOnDetach: Boolean = false,
    val colorThemeId: String = "purple_haze",
    val skinId: String = "modern",
    val minDurationSec: Int = 30,
    val minSizeKb: Int = 100
)
