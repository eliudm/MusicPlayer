package com.example.musicplayer.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.example.musicplayer.data.local.MusicDatabase
import com.example.musicplayer.data.local.entity.FavoriteEntity
import com.example.musicplayer.data.local.entity.PlayHistoryEntity
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongEntity
import com.example.musicplayer.data.mediastore.MediaStoreSource
import com.example.musicplayer.di.ApplicationScope
import com.example.musicplayer.service.EqualizerManager
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Folder
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val db: MusicDatabase,
    private val mediaStore: MediaStoreSource,
    private val playerController: PlayerController,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val equalizerManager: EqualizerManager,
    private val appPreferences: AppPreferences
) : MusicRepository {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val cachedSongs: StateFlow<List<Song>> =
        combine(appPreferences.minDurationSec, appPreferences.minSizeKb) { dur, size ->
            dur * 1000L to size * 1024L
        }.flatMapLatest { (minDurMs, minSizeBytes) ->
            mediaStore.querySongs(minDurMs, minSizeBytes)
        }.map { songs -> songs.distinctBy { it.id } }
            .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _queue = MutableStateFlow<List<Song>>(emptyList())

    private val autoDjEnabled = appPreferences.autoDjEnabled
        .stateIn(appScope, SharingStarted.Eagerly, false)

    private val mediaItemTransitionListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return
            val song = cachedSongs.value.find { it.uri.toString() == uri } ?: return
            if (song.id != _currentSong.value?.id) {
                _currentSong.value = song
                appScope.launch {
                    recordPlay(song.id)
                    readAndApplyReplayGain(song)
                }
            }
            _queue.value = buildQueueFromPlayer()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            _queue.value = buildQueueFromPlayer()
            if (autoDjEnabled.value) checkAndRefillQueue()
        }
    }

    private fun buildQueueFromPlayer(): List<Song> {
        val player = playerController.exoPlayer
        return (0 until player.mediaItemCount).mapNotNull { i ->
            cachedSongs.value.find { it.id.toString() == player.getMediaItemAt(i).mediaId }
        }
    }

    private fun checkAndRefillQueue() {
        val player = playerController.exoPlayer
        val remaining = player.mediaItemCount - player.currentMediaItemIndex - 1
        if (remaining >= 3) return
        val current = _currentSong.value ?: return
        val usedIds = (0 until player.mediaItemCount)
            .map { player.getMediaItemAt(it).mediaId }.toSet()
        val all = cachedSongs.value
        val candidates = buildList {
            addAll(all.filter { it.artist == current.artist && it.id.toString() !in usedIds })
            addAll(all.filter { it.album == current.album && it.id.toString() !in usedIds })
            addAll(all.filter { it.id.toString() !in usedIds })
        }.distinctBy { it.id }.shuffled().take(5)
        candidates.forEach { player.addMediaItem(it.toMediaItem()) }
    }

    init {
        // Register transition listener on ExoPlayer (always active for local playback)
        appScope.launch(Dispatchers.Main) {
            playerController.exoPlayer.addListener(mediaItemTransitionListener)
            _queue.value = buildQueueFromPlayer()
        }
        // When CastPlayer becomes available, also register on it
        appScope.launch {
            playerController.castPlayer.collect { castPlayer ->
                if (castPlayer != null) {
                    withContext(Dispatchers.Main) {
                        castPlayer.addListener(mediaItemTransitionListener)
                    }
                }
            }
        }
    }

    override fun getSongs(): Flow<List<Song>> = cachedSongs

    override fun getAlbums(): Flow<List<Album>> = mediaStore.queryAlbums()

    override fun getPlaylists(): Flow<List<Playlist>> =
        db.playlistDao().getAll().map { entities ->
            entities.map { entity ->
                val songRefs = db.playlistDao().getSongsForPlaylist(entity.id).first()
                val allSongs = cachedSongs.value
                val songs = songRefs.mapNotNull { ref -> allSongs.find { it.id == ref.songId } }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    songs = songs,
                    createdAt = entity.createdAt,
                    modifiedAt = entity.modifiedAt
                )
            }
        }

    override fun searchSongs(query: String): Flow<List<Song>> =
        cachedSongs.map { songs ->
            val q = query.lowercase()
            songs.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }

    private fun startServiceIfNeeded() {
        val intent = Intent(context, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    override suspend fun playWithQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val idx = startIndex.coerceIn(0, songs.lastIndex)
        val startSong = songs[idx]
        _currentSong.value = startSong
        startServiceIfNeeded()
        withContext(Dispatchers.Main) {
            playerController.activePlayer.setMediaItems(songs.map { it.toMediaItem() }, idx, C.TIME_UNSET)
            playerController.activePlayer.prepare()
            playerController.activePlayer.play()
        }
        recordPlay(startSong.id)
        readAndApplyReplayGain(startSong)
    }

    override suspend fun play(song: Song) {
        _currentSong.value = song
        startServiceIfNeeded()
        withContext(Dispatchers.Main) {
            playerController.activePlayer.setMediaItem(song.toMediaItem())
            playerController.activePlayer.prepare()
            playerController.activePlayer.play()
        }
        recordPlay(song.id)
        readAndApplyReplayGain(song)
    }

    private suspend fun readAndApplyReplayGain(song: Song) {
        if (Build.VERSION.SDK_INT < 35) return
        val gainDb = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().use { r ->
                    r.setDataSource(context, song.uri)
                    r.extractMetadata(29 /* METADATA_KEY_REPLAYGAIN_TRACK_GAIN */)
                        ?.trim()?.removeSuffix(" dB")?.toFloatOrNull()
                }
            }.getOrNull()
        } ?: return
        equalizerManager.applyReplayGain(gainDb)
    }

    override suspend fun pause() {
        playerController.activePlayer.pause()
    }

    override suspend fun seekTo(positionMs: Long) {
        playerController.activePlayer.seekTo(positionMs)
    }

    override fun getFolders(): Flow<List<Folder>> =
        cachedSongs.map { songs ->
            songs.groupBy { it.bucketId }
                .map { (id, group) ->
                    Folder(id = id, name = group.first().bucketName, songCount = group.size)
                }
                .distinctBy { it.id }
                .sortedBy { it.name }
        }

    override fun getRecentlyAdded(limit: Int): Flow<List<Song>> =
        cachedSongs.map { songs ->
            songs.sortedByDescending { it.dateAdded }.distinctBy { it.id }.take(limit)
        }

    override fun getSongsInFolder(bucketId: Long): Flow<List<Song>> =
        cachedSongs.map { songs -> songs.filter { it.bucketId == bucketId }.distinctBy { it.id } }

    override fun getCurrentSong(): Flow<Song?> = _currentSong

    override fun observeIsPlaying(): Flow<Boolean> = playerController.isPlaying

    override suspend fun createPlaylist(name: String): Long =
        db.playlistDao().insert(PlaylistEntity(name = name))

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long, position: Int) =
        db.playlistDao().insertSong(PlaylistSongEntity(playlistId = playlistId, songId = songId, position = position))

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        db.playlistDao().removeSong(playlistId, songId)

    override suspend fun deletePlaylist(playlistId: Long) =
        db.playlistDao().delete(PlaylistEntity(id = playlistId, name = ""))

    override suspend fun toggleFavorite(songId: Long): Boolean {
        return if (db.favoriteDao().isFavorite(songId)) {
            db.favoriteDao().delete(songId)
            false
        } else {
            db.favoriteDao().insert(FavoriteEntity(songId = songId))
            true
        }
    }

    override suspend fun isFavorite(songId: Long): Boolean = db.favoriteDao().isFavorite(songId)

    override fun getFavorites(): Flow<List<Song>> =
        combine(db.favoriteDao().getAll(), cachedSongs) { favs, songs ->
            val favIds = favs.map { it.songId }.toSet()
            songs.filter { it.id in favIds }.distinctBy { it.id }
        }

    override fun getRecentlyPlayed(limit: Int): Flow<List<Song>> =
        combine(db.playHistoryDao().getRecent(limit), cachedSongs) { history, songs ->
            history.mapNotNull { h -> songs.find { it.id == h.songId } }.distinctBy { it.id }
        }

    override suspend fun recordPlay(songId: Long) {
        val existing = db.playHistoryDao().getForSong(songId)
        if (existing != null) {
            db.playHistoryDao().upsert(existing.copy(playCount = existing.playCount + 1, lastPlayedAt = System.currentTimeMillis()))
        } else {
            db.playHistoryDao().upsert(PlayHistoryEntity(songId = songId))
        }
    }

    override fun getMostPlayed(limit: Int): Flow<List<Song>> =
        combine(db.playHistoryDao().getMostPlayed(limit), cachedSongs) { history, songs ->
            history.mapNotNull { h -> songs.find { it.id == h.songId } }.distinctBy { it.id }
        }

    override suspend fun playNext(song: Song) {
        val p = playerController.activePlayer
        val insertIndex = (p.currentMediaItemIndex + 1).coerceAtLeast(0)
        p.addMediaItem(insertIndex, song.toMediaItem())
        if (!p.isPlaying && p.mediaItemCount == 1) {
            p.prepare()
            p.play()
        }
    }

    override suspend fun enqueue(song: Song) {
        val p = playerController.activePlayer
        p.addMediaItem(song.toMediaItem())
        if (!p.isPlaying && p.mediaItemCount == 1) {
            p.prepare()
            p.play()
        }
    }

    override suspend fun deleteSong(song: Song): Boolean {
        return try {
            val deleted = context.contentResolver.delete(song.uri, null, null)
            deleted > 0
        } catch (e: Exception) {
            false
        }
    }

    override fun getQueue(): Flow<List<Song>> = _queue

    override suspend fun moveQueueItem(from: Int, to: Int) = withContext(Dispatchers.Main) {
        playerController.activePlayer.moveMediaItem(from, to)
    }

    override suspend fun removeFromQueue(index: Int) = withContext(Dispatchers.Main) {
        playerController.activePlayer.removeMediaItem(index)
    }

    override suspend fun clearQueue() = withContext(Dispatchers.Main) {
        playerController.activePlayer.clearMediaItems()
    }
}

fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setAlbumArtist(artist)
                .setTrackNumber(trackNumber)
                .setRecordingYear(year.takeIf { it > 0 })
                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/$albumId"))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .build()
