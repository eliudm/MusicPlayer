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

class StandardMusicWidget : GlanceAppWidget() {

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
                .cornerRadius(20.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(72.dp)
                            .background(ColorProvider(Color(0xFF2D2D4E)))
                            .cornerRadius(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_music),
                            contentDescription = null,
                            modifier = GlanceModifier.size(36.dp)
                        )
                    }

                    Spacer(GlanceModifier.width(12.dp))

                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        if (artist.isNotEmpty()) {
                            Text(
                                text = artist,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xCCFFFFFF)),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(GlanceModifier.height(12.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_prev),
                        contentDescription = "Previous",
                        modifier = GlanceModifier
                            .size(40.dp)
                            .clickable(actionRunCallback<SkipPrevAction>())
                    )

                    Spacer(GlanceModifier.width(16.dp))

                    Box(
                        modifier = GlanceModifier
                            .size(52.dp)
                            .background(ColorProvider(Color(0xFF7B61FF)))
                            .cornerRadius(26.dp)
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }

                    Spacer(GlanceModifier.width(16.dp))

                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_next),
                        contentDescription = "Next",
                        modifier = GlanceModifier
                            .size(40.dp)
                            .clickable(actionRunCallback<SkipNextAction>())
                    )
                }
            }
        }
    }
}

class StandardMusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StandardMusicWidget()
}
