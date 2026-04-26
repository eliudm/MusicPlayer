package com.example.musicplayer.presentation.settings.backup

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        refreshSignInState()
    }

    fun refreshSignInState() {
        val signedIn = repository.isSignedIn()
        _uiState.update {
            it.copy(isSignedIn = signedIn, accountEmail = repository.getSignedInEmail())
        }
        if (signedIn) loadBackups()
    }

    fun getSignInIntent(): Intent = repository.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            val success = repository.handleSignInResult(data)
            if (success) {
                refreshSignInState()
            } else {
                _uiState.update { it.copy(errorMessage = "Sign-in failed or was cancelled") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.signOut()
            _uiState.update {
                it.copy(isLoading = false, isSignedIn = false, accountEmail = null, backups = emptyList())
            }
        }
    }

    fun backup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.uploadBackup()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Backup created successfully") }
                    loadBackups()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Backup failed: ${e.message?.take(120)}") }
                }
        }
    }

    fun loadBackups() {
        viewModelScope.launch {
            repository.listBackups()
                .onSuccess { files ->
                    _uiState.update { it.copy(backups = files) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = "Could not load backups: ${e.message?.take(120)}") }
                }
        }
    }

    fun restore(fileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.downloadAndRestore(fileId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Restore completed — restart the app to see all changes") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Restore failed: ${e.message?.take(120)}") }
                }
        }
    }

    fun deleteBackup(fileId: String) {
        viewModelScope.launch {
            repository.deleteBackup(fileId)
                .onSuccess { loadBackups() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = "Delete failed: ${e.message?.take(120)}") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
