package com.example.musicplayer.presentation.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.data.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, runBlocking { prefs.themeMode.first() })

    val pauseOnDetach: StateFlow<Boolean> = prefs.pauseOnDetach
        .stateIn(viewModelScope, SharingStarted.Eagerly, runBlocking { prefs.pauseOnDetach.first() })

    val colorThemeId: StateFlow<String> = prefs.colorThemeId
        .stateIn(viewModelScope, SharingStarted.Eagerly, runBlocking { prefs.colorThemeId.first() })

    val skinId: StateFlow<String> = prefs.skinId
        .stateIn(viewModelScope, SharingStarted.Eagerly, runBlocking { prefs.skinId.first() })

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        prefs.setThemeMode(mode)
    }

    fun togglePauseOnDetach() = viewModelScope.launch {
        prefs.setPauseOnDetach(!pauseOnDetach.value)
    }

    fun setColorThemeId(id: String) = viewModelScope.launch {
        prefs.setColorThemeId(id)
    }

    fun setSkinId(id: String) = viewModelScope.launch {
        prefs.setSkinId(id)
    }

    fun scanMusic() = viewModelScope.launch {
        _scanMessage.value = "Library scan complete"
    }

    fun clearScanMessage() { _scanMessage.value = null }
}
