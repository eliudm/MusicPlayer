package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Folder
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getSongs(): Flow<List<Song>>
    fun getAlbums(): Flow<List<Album>>
    fun getPlaylists(): Flow<List<Playlist>>
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun play(song: Song)
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)
    suspend fun createPlaylist(name: String): Long
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, position: Int)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun toggleFavorite(songId: Long): Boolean
    suspend fun isFavorite(songId: Long): Boolean
    fun getFavorites(): Flow<List<Song>>
    fun getRecentlyPlayed(limit: Int = 5): Flow<List<Song>>
    suspend fun recordPlay(songId: Long)
    fun getMostPlayed(limit: Int = 20): Flow<List<Song>>
    fun getFolders(): Flow<List<Folder>>
    fun getRecentlyAdded(limit: Int = 20): Flow<List<Song>>
    fun getSongsInFolder(bucketId: Long): Flow<List<Song>>
    fun getCurrentSong(): Flow<Song?>
    fun observeIsPlaying(): Flow<Boolean>
    suspend fun playWithQueue(songs: List<Song>, startIndex: Int)
    suspend fun playNext(song: Song)
    suspend fun enqueue(song: Song)
    suspend fun deleteSong(song: Song): Boolean
    fun getQueue(): Flow<List<Song>>
    suspend fun moveQueueItem(from: Int, to: Int)
    suspend fun removeFromQueue(index: Int)
    suspend fun clearQueue()
}
