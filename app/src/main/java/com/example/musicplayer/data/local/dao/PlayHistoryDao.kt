package com.example.musicplayer.data.local.dao

import androidx.room.*
import com.example.musicplayer.data.local.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE songId = :songId LIMIT 1")
    suspend fun getForSong(songId: Long): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlayHistoryEntity)

    @Query("SELECT * FROM play_history")
    suspend fun getAllOnce(): List<PlayHistoryEntity>

    @Query("DELETE FROM play_history")
    suspend fun deleteAll()
}
