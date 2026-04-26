package com.example.musicplayer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.musicplayer.R
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var repository: MusicRepository

    private lateinit var mediaSession: MediaLibrarySession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Local cache of songs for the browse tree — kept fresh via a Flow collector
    @Volatile private var cachedSongs: List<Song> = emptyList()
    @Volatile private var cachedAlbums: Map<Long, List<Song>> = emptyMap()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                serviceScope.launch {
                    if (appPreferences.pauseOnDetach.first()) playerController.exoPlayer.pause()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(PLAYBACK_CHANNEL_ID)
                .setChannelName(R.string.notification_channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )

        mediaSession = MediaLibrarySession.Builder(this, playerController.exoPlayer, AutoBrowserCallback())
            .build()
        addSession(mediaSession)

        // Swap the underlying player when Cast connects / disconnects
        serviceScope.launch {
            playerController.isCastActive.collect { castActive ->
                val targetPlayer = playerController.activePlayer
                if (mediaSession.player != targetPlayer) {
                    mediaSession.setPlayer(targetPlayer)
                }
            }
        }

        // Keep cachedSongs up to date for browse tree
        serviceScope.launch(Dispatchers.IO) {
            repository.getSongs().collect { songs ->
                cachedSongs = songs
                cachedAlbums = songs.groupBy { it.albumId }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            super.onStartCommand(intent, flags, startId)
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession

    override fun onDestroy() {
        unregisterReceiver(noisyReceiver)
        serviceScope.cancel()
        removeSession(mediaSession)
        mediaSession.release()
        super.onDestroy()
    }

    // ── Android Auto browse callback ──────────────────────────────────────────

    private inner class AutoBrowserCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Music Player")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, null))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch(Dispatchers.IO) {
                val result = when {
                    parentId == ROOT_ID -> buildRootChildren()
                    parentId == SONGS_ID -> cachedSongs.map { it.toAutoItem() }
                    parentId == ALBUMS_ID -> buildAlbumBrowseList()
                    parentId == RECENT_ID -> {
                        repository.getRecentlyAdded(30).first().map { it.toAutoItem() }
                    }
                    parentId.startsWith(ALBUM_PREFIX) -> {
                        val albumId = parentId.removePrefix(ALBUM_PREFIX).toLongOrNull()
                        cachedAlbums[albumId].orEmpty()
                            .sortedBy { it.trackNumber }
                            .map { it.toAutoItem() }
                    }
                    else -> emptyList()
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(result), null))
            }
            return future
        }

        // Resolve mediaId → full MediaItem with URI so ExoPlayer can play it
        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                cachedSongs.find { it.id.toString() == item.mediaId }?.toPlayableItem() ?: item
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolved = mediaItems.map { item ->
                cachedSongs.find { it.id.toString() == item.mediaId }?.toPlayableItem() ?: item
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
            )
        }
    }

    // ── Browse tree builders ──────────────────────────────────────────────────

    private fun buildRootChildren(): List<MediaItem> = listOf(
        browsableItem(SONGS_ID, "Songs", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        browsableItem(ALBUMS_ID, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
        browsableItem(RECENT_ID, "Recently Added", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
    )

    private fun buildAlbumBrowseList(): List<MediaItem> =
        cachedAlbums.entries
            .sortedBy { it.value.firstOrNull()?.album?.lowercase() }
            .map { (albumId, songs) ->
                val first = songs.first()
                browsableItem(
                    id = "$ALBUM_PREFIX$albumId",
                    title = first.album,
                    type = MediaMetadata.MEDIA_TYPE_ALBUM,
                    subtitle = first.artist,
                    artUri = Uri.parse("content://media/external/audio/albumart/$albumId")
                )
            }

    private fun browsableItem(
        id: String,
        title: String,
        type: Int,
        subtitle: String? = null,
        artUri: Uri? = null
    ): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setArtworkUri(artUri)
                .setMediaType(type)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current playback track"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ROOT_ID = "root"
        private const val SONGS_ID = "songs"
        private const val ALBUMS_ID = "albums"
        private const val RECENT_ID = "recent"
        private const val ALBUM_PREFIX = "album_"
    }
}

// ── MediaItem builders for songs ──────────────────────────────────────────────

private fun Song.toPlayableItem(): MediaItem =
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

private fun Song.toAutoItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setTrackNumber(trackNumber)
                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/$albumId"))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .build()
