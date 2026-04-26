package com.example.musicplayer.data.local.dao

import androidx.room.*
import com.example.musicplayer.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun delete(songId: Long)

    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE songId = :songId")
    suspend fun isFavorite(songId: Long): Boolean

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllOnce(): List<FavoriteEntity>

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
