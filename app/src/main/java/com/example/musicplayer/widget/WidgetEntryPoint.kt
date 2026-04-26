package com.example.musicplayer.widget

import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun exoPlayer(): ExoPlayer
}
