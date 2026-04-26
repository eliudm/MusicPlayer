package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Lyrics
import com.example.musicplayer.domain.model.Song

interface LyricsRepository {
    suspend fun getLyrics(song: Song): Result<Lyrics?>
    suspend fun saveManualLyrics(songId: Long, text: String)
    suspend fun clearLyrics(songId: Long)
}
