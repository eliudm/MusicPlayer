package com.example.musicplayer.service

import android.media.audiofx.Visualizer
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

@Singleton
class SpectrumProvider @Inject constructor(player: ExoPlayer) {

    companion object {
        const val BAR_COUNT = 28
        private const val CAPTURE_SIZE = 1024
        // Max possible magnitude per bin (re = 127, im = 0) ≈ 127
        private const val MAX_MAGNITUDE = 128f
    }

    private val _bands = MutableStateFlow(FloatArray(BAR_COUNT))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    // Per-bar smoothed values – mutated only from the Visualizer callback thread
    private val smoothed = FloatArray(BAR_COUNT)

    private val visualizer: Visualizer? = runCatching {
        Visualizer(player.audioSessionId).apply {
            captureSize = CAPTURE_SIZE
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer, bytes: ByteArray, rate: Int) = Unit
                    override fun onFftDataCapture(v: Visualizer, fft: ByteArray, rate: Int) {
                        processFft(fft)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,  // ~10 captures/sec
                false,
                true
            )
            enabled = true
        }
    }.getOrNull()

    val isAvailable: Boolean get() = visualizer != null

    private fun processFft(fft: ByteArray) {
        val halfN = fft.size / 2
        if (halfN < 2) return

        // Step 1 – compute magnitude per frequency bin
        val magnitudes = FloatArray(halfN)
        for (i in 0 until halfN) {
            val re = fft[2 * i].toFloat()
            val im = fft[2 * i + 1].toFloat()
            magnitudes[i] = sqrt(re * re + im * im)
        }

        // Step 2 – aggregate bins into BAR_COUNT logarithmic bands
        val logMin = ln(2.0)
        val logMax = ln(halfN.toDouble())
        val raw = FloatArray(BAR_COUNT)
        for (bar in 0 until BAR_COUNT) {
            val binStart = exp(logMin + bar * (logMax - logMin) / BAR_COUNT)
                .toInt().coerceIn(1, halfN - 1)
            val binEnd = exp(logMin + (bar + 1) * (logMax - logMin) / BAR_COUNT)
                .toInt().coerceIn(binStart, halfN - 1)
            var peak = 0f
            for (bin in binStart..binEnd) {
                if (magnitudes[bin] > peak) peak = magnitudes[bin]
            }
            raw[bar] = (peak / MAX_MAGNITUDE).coerceIn(0f, 1f)
        }

        // Step 3 – exponential smoothing: fast attack, slow decay
        for (i in 0 until BAR_COUNT) {
            smoothed[i] = if (raw[i] > smoothed[i]) {
                raw[i] * 0.65f + smoothed[i] * 0.35f  // fast rise
            } else {
                raw[i] * 0.12f + smoothed[i] * 0.88f  // slow fall
            }
        }

        _bands.value = smoothed.copyOf()
    }

    fun release() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
    }
}
