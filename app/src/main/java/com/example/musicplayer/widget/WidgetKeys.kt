package com.example.musicplayer.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetKeys {
    val SONG_TITLE = stringPreferencesKey("widget_song_title")
    val SONG_ARTIST = stringPreferencesKey("widget_song_artist")
    val SONG_ID = longPreferencesKey("widget_song_id")
    val ALBUM_ID = longPreferencesKey("widget_album_id")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")
}
