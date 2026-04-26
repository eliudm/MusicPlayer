package com.example.musicplayer.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.musicplayer.di.ApplicationScope
import com.example.musicplayer.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    // Called from MusicModule after the repository is constructed to avoid a DI cycle
    fun init(repository: MusicRepository) {
        scope.launch {
            combine(
                repository.getCurrentSong(),
                repository.observeIsPlaying()
            ) { song, isPlaying -> song to isPlaying }
                .collect { (song, isPlaying) ->
                    updateAll(
                        title = song?.title ?: "",
                        artist = song?.artist ?: "",
                        songId = song?.id ?: -1L,
                        albumId = song?.albumId ?: -1L,
                        isPlaying = isPlaying
                    )
                }
        }
    }

    private suspend fun updateAll(
        title: String,
        artist: String,
        songId: Long,
        albumId: Long,
        isPlaying: Boolean
    ) {
        val manager = GlanceAppWidgetManager(context)

        manager.getGlanceIds(SmallMusicWidget::class.java).forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WidgetKeys.SONG_TITLE] = title
                    this[WidgetKeys.SONG_ARTIST] = artist
                    this[WidgetKeys.SONG_ID] = songId
                    this[WidgetKeys.ALBUM_ID] = albumId
                    this[WidgetKeys.IS_PLAYING] = isPlaying
                }
            }
            SmallMusicWidget().update(context, id)
        }

        manager.getGlanceIds(StandardMusicWidget::class.java).forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WidgetKeys.SONG_TITLE] = title
                    this[WidgetKeys.SONG_ARTIST] = artist
                    this[WidgetKeys.SONG_ID] = songId
                    this[WidgetKeys.ALBUM_ID] = albumId
                    this[WidgetKeys.IS_PLAYING] = isPlaying
                }
            }
            StandardMusicWidget().update(context, id)
        }
    }
}
