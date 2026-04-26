package com.example.musicplayer.service

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerManager @Inject constructor(
    player: ExoPlayer,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("eq_settings", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    val bandCount: Int get() = equalizer?.numberOfBands?.toInt() ?: 0
    val presets: List<String> get() = (0 until (equalizer?.numberOfPresets?.toInt() ?: 0))
        .map { equalizer?.getPresetName(it.toShort()) ?: "" }

    val reverbPresets: List<String> = listOf(
        "None", "Small Room", "Medium Room", "Large Room",
        "Medium Hall", "Large Hall", "Plate"
    )

    init {
        val sessionId = player.audioSessionId
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        runCatching { equalizer       = Equalizer(0, sessionId).apply { this.enabled = enabled } }
        runCatching { bassBoost       = BassBoost(0, sessionId).apply { this.enabled = enabled } }
        runCatching { virtualizer     = Virtualizer(0, sessionId).apply { this.enabled = enabled } }
        runCatching { presetReverb    = PresetReverb(0, sessionId).apply { this.enabled = enabled } }
        runCatching { loudnessEnhancer = LoudnessEnhancer(sessionId).apply { this.enabled = enabled } }
        if (equalizer != null) { loadSavedBands(); loadSavedExtras() }
        loadSavedReverb()
        loadSavedLoudness()
    }

    fun getBandLevelRange(): ShortArray {
        val eq = equalizer ?: return shortArrayOf(-1500, 1500)
        return shortArrayOf(eq.bandLevelRange[0], eq.bandLevelRange[1])
    }

    fun getBandLevel(band: Int): Short = equalizer?.getBandLevel(band.toShort()) ?: 0
    fun getBandCenterFreq(band: Int): Int = equalizer?.getCenterFreq(band.toShort()) ?: 0

    fun setBandLevel(band: Int, level: Short) {
        equalizer?.setBandLevel(band.toShort(), level)
        prefs.edit().putInt("$KEY_BAND$band", level.toInt()).apply()
    }

    fun applyPreset(index: Int) {
        equalizer?.usePreset(index.toShort())
        (0 until bandCount).forEach { band ->
            prefs.edit().putInt("$KEY_BAND$band", getBandLevel(band).toInt()).apply()
        }
        prefs.edit().putInt(KEY_PRESET, index).apply()
    }

    fun getBassBoostStrength(): Short = bassBoost?.roundedStrength ?: 0
    fun getVirtualizerStrength(): Short = virtualizer?.roundedStrength ?: 0

    fun setBassBoostStrength(strength: Short) {
        bassBoost?.setStrength(strength)
        prefs.edit().putInt(KEY_BASS, strength.toInt()).apply()
    }

    fun setVirtualizerStrength(strength: Short) {
        virtualizer?.setStrength(strength)
        prefs.edit().putInt(KEY_VIRT, strength.toInt()).apply()
    }

    // ── Reverb ────────────────────────────────────────────────────────────────

    fun getReverbPreset(): Int = prefs.getInt(KEY_REVERB, PresetReverb.PRESET_NONE.toInt())

    fun setReverbPreset(preset: Int) {
        runCatching { presetReverb?.preset = preset.toShort() }
        prefs.edit().putInt(KEY_REVERB, preset).apply()
    }

    // ── Loudness Enhancer ─────────────────────────────────────────────────────

    /** Target gain in millibels (100 mB = 1 dB). Range 0–1500 mB (0–15 dB). */
    fun getLoudnessGainMb(): Int = prefs.getInt(KEY_LOUDNESS, 0)

    fun setLoudnessGainMb(gainMb: Int) {
        if (gainMb <= 0) {
            loudnessEnhancer?.enabled = false
        } else {
            runCatching {
                loudnessEnhancer?.enabled = true
                loudnessEnhancer?.setTargetGain(gainMb)
            }
        }
        prefs.edit().putInt(KEY_LOUDNESS, gainMb).apply()
    }

    /** Apply ReplayGain from track tags (call after each song transition). */
    fun applyReplayGain(gainDb: Float) {
        if (gainDb == 0f) return
        val gainMb = (gainDb * 100).toInt()
        runCatching {
            loudnessEnhancer?.enabled = true
            loudnessEnhancer?.setTargetGain(gainMb)
        }
    }

    // ── Global enable ─────────────────────────────────────────────────────────

    fun isEnabled(): Boolean = equalizer?.enabled ?: true

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        presetReverb?.enabled = enabled
        if (!enabled) loudnessEnhancer?.enabled = false
        else if (getLoudnessGainMb() > 0) loudnessEnhancer?.enabled = true
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getSavedPreset(): Int = prefs.getInt(KEY_PRESET, -1)

    private fun loadSavedBands() {
        (0 until bandCount).forEach { band ->
            val saved = prefs.getInt("$KEY_BAND$band", Int.MIN_VALUE)
            if (saved != Int.MIN_VALUE) equalizer?.setBandLevel(band.toShort(), saved.toShort())
        }
    }

    private fun loadSavedExtras() {
        val bass = prefs.getInt(KEY_BASS, 0)
        val virt = prefs.getInt(KEY_VIRT, 0)
        if (bass > 0) bassBoost?.setStrength(bass.toShort())
        if (virt > 0) virtualizer?.setStrength(virt.toShort())
    }

    private fun loadSavedReverb() {
        val preset = prefs.getInt(KEY_REVERB, PresetReverb.PRESET_NONE.toInt())
        runCatching { presetReverb?.preset = preset.toShort() }
    }

    private fun loadSavedLoudness() {
        val gain = prefs.getInt(KEY_LOUDNESS, 0)
        if (gain > 0) runCatching {
            loudnessEnhancer?.enabled = true
            loudnessEnhancer?.setTargetGain(gain)
        }
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BAND = "band_"
        private const val KEY_BASS = "bass_boost"
        private const val KEY_VIRT = "virtualizer"
        private const val KEY_PRESET = "preset"
        private const val KEY_REVERB = "reverb_preset"
        private const val KEY_LOUDNESS = "loudness_gain_mb"
    }
}
