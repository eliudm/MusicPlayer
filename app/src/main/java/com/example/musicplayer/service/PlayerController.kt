package com.example.musicplayer.service

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicplayer.di.ApplicationScope
import com.google.android.gms.cast.framework.CastContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    val exoPlayer: ExoPlayer,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val _isCastActive = MutableStateFlow(false)
    val isCastActive: StateFlow<Boolean> = _isCastActive.asStateFlow()

    private val _castPlayer = MutableStateFlow<CastPlayer?>(null)
    val castPlayer: StateFlow<CastPlayer?> = _castPlayer.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val activePlayer: Player
        get() = if (_isCastActive.value) _castPlayer.value ?: exoPlayer else exoPlayer

    private val exoListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!_isCastActive.value) _isPlaying.value = isPlaying
        }
    }

    init {
        exoPlayer.addListener(exoListener)
        appScope.launch {
            initCast()
        }
    }

    private fun initCast() {
        try {
            CastContext.getSharedInstance(context, ContextCompat.getMainExecutor(context))
                .addOnSuccessListener { castCtx ->
                    val cp = CastPlayer(castCtx)
                    cp.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (_isCastActive.value) _isPlaying.value = isPlaying
                        }
                    })
                    cp.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                        override fun onCastSessionAvailable() {
                            _isCastActive.value = true
                            transferToCast(cp)
                        }
                        override fun onCastSessionUnavailable() {
                            _isCastActive.value = false
                            transferToLocal()
                        }
                    })
                    _castPlayer.value = cp
                }
        } catch (_: Exception) {
            // Play Services unavailable (emulator without GMS, tests, etc.)
        }
    }

    private fun transferToCast(castPlayer: CastPlayer) {
        val currentItem = exoPlayer.currentMediaItem ?: return
        val currentPos = exoPlayer.currentPosition
        exoPlayer.pause()
        castPlayer.setMediaItem(currentItem, currentPos)
        castPlayer.prepare()
        castPlayer.play()
    }

    private fun transferToLocal() {
        val castPos = _castPlayer.value?.currentPosition ?: 0L
        val castItem = _castPlayer.value?.currentMediaItem
        _castPlayer.value?.stop()
        if (castItem != null) {
            exoPlayer.seekTo(castPos)
            exoPlayer.play()
        }
    }

    fun release() {
        exoPlayer.removeListener(exoListener)
        _castPlayer.value?.release()
    }
}
