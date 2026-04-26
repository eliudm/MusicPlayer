package com.example.musicplayer.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.musicplayer.R

class SmallMusicWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val title = prefs[WidgetKeys.SONG_TITLE] ?: "Not playing"
        val artist = prefs[WidgetKeys.SONG_ARTIST] ?: ""
        val isPlaying = prefs[WidgetKeys.IS_PLAYING] ?: false

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1A1A2E)))
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_music),
                    contentDescription = null,
                    modifier = GlanceModifier.size(36.dp)
                )

                Spacer(GlanceModifier.width(8.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    if (artist.isNotEmpty()) {
                        Text(
                            text = artist,
                            style = TextStyle(
                                color = ColorProvider(Color(0xCCFFFFFF)),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(GlanceModifier.width(4.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_widget_prev),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(32.dp)
                        .clickable(actionRunCallback<SkipPrevAction>())
                )

                Spacer(GlanceModifier.width(4.dp))

                Image(
                    provider = ImageProvider(
                        if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier
                        .size(36.dp)
                        .clickable(actionRunCallback<PlayPauseAction>())
                )

                Spacer(GlanceModifier.width(4.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_widget_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(32.dp)
                        .clickable(actionRunCallback<SkipNextAction>())
                )
            }
        }
    }
}

class SmallMusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallMusicWidget()
}
