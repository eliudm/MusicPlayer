package com.example.musicplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicplayer.data.local.entity.LyricsEntity

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId")
    suspend fun get(songId: Long): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsEntity)

    @Query("DELETE FROM lyrics_cache WHERE songId = :songId")
    suspend fun delete(songId: Long)
}
