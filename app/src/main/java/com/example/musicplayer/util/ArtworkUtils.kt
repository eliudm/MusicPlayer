package com.example.musicplayer.util

import android.content.Context
import com.example.musicplayer.domain.model.Song
import java.io.File

fun Song.artworkModel(context: Context): Any {
    // 1. Manually set artwork (highest priority)
    val custom = File(context.filesDir, "artworks/$id.jpg")
    if (custom.exists()) return custom
    // 2. Auto-fetched artwork
    val auto = File(context.filesDir, "auto_artworks/$id.jpg")
    if (auto.exists()) return auto
    // 3. MediaStore embedded art
    return "content://media/external/audio/albumart/$albumId"
}

fun artworkDir(context: Context): File =
    File(context.filesDir, "artworks").also { it.mkdirs() }

fun autoArtworkDir(context: Context): File =
    File(context.filesDir, "auto_artworks").also { it.mkdirs() }
