package com.example.musicplayer.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).exoPlayer()
        if (ep.isPlaying) ep.pause() else ep.play()
    }
}

class SkipNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            .exoPlayer().seekToNextMediaItem()
    }
}

class SkipPrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).exoPlayer()
        if (ep.currentPosition > 3_000L) ep.seekTo(0L) else ep.seekToPreviousMediaItem()
    }
}
