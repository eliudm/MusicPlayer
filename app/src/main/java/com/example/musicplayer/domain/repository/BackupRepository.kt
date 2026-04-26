package com.example.musicplayer.domain.repository

import android.content.Intent
import com.example.musicplayer.domain.model.BackupData
import com.example.musicplayer.domain.model.DriveBackupFile

interface BackupRepository {
    fun isSignedIn(): Boolean
    fun getSignedInEmail(): String?
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Boolean
    suspend fun signOut()
    suspend fun uploadBackup(): Result<DriveBackupFile>
    suspend fun listBackups(): Result<List<DriveBackupFile>>
    suspend fun downloadAndRestore(fileId: String): Result<Unit>
    suspend fun deleteBackup(fileId: String): Result<Unit>
}
