package com.example.musicplayer.presentation.settings

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class State(
        val crossfadeEnabled: Boolean = false,
        val crossfadeDurationSec: Float = 3f,
        val outputSampleRateHz: Int = 0,
        val hasUsbDac: Boolean = false,
        val hasHighResPlatform: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefs.crossfadeEnabled, prefs.crossfadeDuration) { enabled, duration ->
                enabled to duration
            }.collect { (enabled, duration) ->
                _state.update { it.copy(crossfadeEnabled = enabled, crossfadeDurationSec = duration) }
            }
        }
        detectAudioCapabilities()
    }

    private fun detectAudioCapabilities() {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val sampleRate = audioManager
            .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 0

        val hasUsbDac = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type == android.media.AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
            }
        } else false

        val hasHighRes = context.packageManager
            .hasSystemFeature("android.hardware.audio.pro")

        _state.update {
            it.copy(
                outputSampleRateHz = sampleRate,
                hasUsbDac = hasUsbDac,
                hasHighResPlatform = hasHighRes
            )
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) =
        viewModelScope.launch { prefs.setCrossfadeEnabled(enabled) }

    fun setCrossfadeDuration(seconds: Float) =
        viewModelScope.launch { prefs.setCrossfadeDuration(seconds) }
}
