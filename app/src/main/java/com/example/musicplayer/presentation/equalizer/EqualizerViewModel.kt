package com.example.musicplayer.presentation.equalizer

import androidx.lifecycle.ViewModel
import com.example.musicplayer.service.EqualizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class EqualizerUiState(
    val bands: List<Float> = emptyList(),
    val bandFrequencies: List<String> = emptyList(),
    val bandLevelMin: Int = -1500,
    val bandLevelMax: Int = 1500,
    val presets: List<String> = emptyList(),
    val selectedPreset: Int = -1,
    val isEnabled: Boolean = true,
    val bassBoostStrength: Float = 0f,
    val virtualizerStrength: Float = 0f,
    val reverbPresets: List<String> = emptyList(),
    val selectedReverbPreset: Int = 0,
    val loudnessGainMb: Float = 0f
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val eq: EqualizerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        val range = eq.getBandLevelRange()
        val bands = (0 until eq.bandCount).map { eq.getBandLevel(it).toFloat() }
        val freqs = (0 until eq.bandCount).map { formatFreqMilliHz(eq.getBandCenterFreq(it)) }
        _uiState.update {
            it.copy(
                bands = bands,
                bandFrequencies = freqs,
                bandLevelMin = range[0].toInt(),
                bandLevelMax = range[1].toInt(),
                presets = eq.presets,
                selectedPreset = eq.getSavedPreset(),
                isEnabled = eq.isEnabled(),
                bassBoostStrength = eq.getBassBoostStrength().toFloat(),
                virtualizerStrength = eq.getVirtualizerStrength().toFloat(),
                reverbPresets = eq.reverbPresets,
                selectedReverbPreset = eq.getReverbPreset(),
                loudnessGainMb = eq.getLoudnessGainMb().toFloat()
            )
        }
    }

    fun setBandLevel(band: Int, level: Float) {
        eq.setBandLevel(band, level.toInt().toShort())
        _uiState.update {
            val updated = it.bands.toMutableList().also { list -> list[band] = level }
            it.copy(bands = updated, selectedPreset = -1)
        }
    }

    fun applyPreset(index: Int) {
        eq.applyPreset(index)
        val bands = (0 until eq.bandCount).map { eq.getBandLevel(it).toFloat() }
        _uiState.update { it.copy(selectedPreset = index, bands = bands) }
    }

    fun setBassBoost(strength: Float) {
        eq.setBassBoostStrength(strength.toInt().toShort())
        _uiState.update { it.copy(bassBoostStrength = strength) }
    }

    fun setVirtualizer(strength: Float) {
        eq.setVirtualizerStrength(strength.toInt().toShort())
        _uiState.update { it.copy(virtualizerStrength = strength) }
    }

    fun setReverbPreset(index: Int) {
        eq.setReverbPreset(index)
        _uiState.update { it.copy(selectedReverbPreset = index) }
    }

    fun setLoudnessGain(gainMb: Float) {
        eq.setLoudnessGainMb(gainMb.toInt())
        _uiState.update { it.copy(loudnessGainMb = gainMb) }
    }

    fun toggleEnabled(enabled: Boolean) {
        eq.setEnabled(enabled)
        _uiState.update { it.copy(isEnabled = enabled) }
    }
}

private fun formatFreqMilliHz(milliHz: Int): String {
    val hz = milliHz / 1000
    return when {
        hz >= 10_000 -> "${hz / 1000}kHz"
        hz >= 1_000  -> "${hz / 1000}.${(hz % 1000) / 100}kHz"
        else         -> "${hz}Hz"
    }
}
