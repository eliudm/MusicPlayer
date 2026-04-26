package com.example.musicplayer.domain.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uri: Uri,
    val trackNumber: Int,
    val year: Int,
    val genre: String,
    val size: Long,
    val bucketId: Long = 0,
    val bucketName: String = "",
    val dateAdded: Long = 0,
    val mimeType: String = ""
) {
    val audioFormat: AudioFormat get() = AudioFormat.fromMimeType(mimeType)
}
