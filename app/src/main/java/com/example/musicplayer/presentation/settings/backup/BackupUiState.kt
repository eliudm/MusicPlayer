package com.example.musicplayer.presentation.settings.backup

import com.example.musicplayer.domain.model.DriveBackupFile

data class BackupUiState(
    val isSignedIn: Boolean = false,
    val accountEmail: String? = null,
    val backups: List<DriveBackupFile> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
