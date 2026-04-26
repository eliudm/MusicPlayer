package com.example.musicplayer.data.mediastore

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun querySongs(minDurationMs: Long = 30_000L, minSizeBytes: Long = 102_400L): Flow<List<Song>> = flow {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Include IS_MUSIC tracks plus additional hi-fi formats (FLAC, WAV, APE, DSD, ALAC)
        // that some devices' media scanners may not flag as IS_MUSIC
        val hiFiMimeClause = HIFI_MIME_TYPES.joinToString(" OR ") {
            "${MediaStore.Audio.Media.MIME_TYPE} LIKE '$it'"
        }
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ($hiFiMimeClause))" +
            " AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs" +
            " AND ${MediaStore.Audio.Media.SIZE} >= $minSizeBytes"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, sortOrder
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            val seen = mutableSetOf<Long>()
            val songs = mutableListOf<Song>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                if (!seen.add(id)) continue   // deduplicate if query returns duplicates
                songs += Song(
                    id = id,
                    title       = cursor.getString(titleCol) ?: "Unknown",
                    artist      = cursor.getString(artistCol) ?: "Unknown Artist",
                    album       = cursor.getString(albumCol) ?: "Unknown Album",
                    albumId     = cursor.getLong(albumIdCol),
                    duration    = cursor.getLong(durationCol),
                    uri         = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()),
                    trackNumber = cursor.getInt(trackCol),
                    year        = cursor.getInt(yearCol),
                    genre       = "",
                    size        = cursor.getLong(sizeCol),
                    bucketId    = cursor.getLong(bucketIdCol),
                    bucketName  = cursor.getString(bucketNameCol) ?: "Unknown Folder",
                    dateAdded   = cursor.getLong(dateAddedCol),
                    mimeType    = cursor.getString(mimeCol) ?: ""
                )
            }
            emit(songs)
        } ?: emit(emptyList())
    }.flowOn(Dispatchers.IO)

    companion object {
        // Hi-fi MIME patterns that some media scanners don't flag as IS_MUSIC
        private val HIFI_MIME_TYPES = listOf(
            "audio/flac", "audio/x-flac",
            "audio/wav", "audio/x-wav", "audio/wave",
            "audio/alac", "audio/x-alac",
            "audio/aiff", "audio/x-aiff",
            "audio/x-ape", "audio/x-monkeys-audio",
            "audio/x-dsf", "audio/dsd"
        )
    }

    fun queryAlbums(): Flow<List<Album>> = flow {
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )
        context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)

            val albums = mutableListOf<Album>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artUri = Uri.parse("content://media/external/audio/albumart/$id")
                albums += Album(
                    id = id,
                    name = cursor.getString(nameCol) ?: "Unknown Album",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    songCount = cursor.getInt(countCol),
                    year = cursor.getInt(yearCol),
                    artUri = artUri
                )
            }
            emit(albums)
        } ?: emit(emptyList())
    }.flowOn(Dispatchers.IO)
}
