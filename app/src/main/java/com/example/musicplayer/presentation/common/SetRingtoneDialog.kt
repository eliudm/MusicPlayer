package com.example.musicplayer.presentation.common

import android.content.ContentValues
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.musicplayer.domain.model.Song

@Composable
fun SetRingtoneDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
        title = { Text("Set as ringtone") },
        text = {
            Text(
                text = "Set \"${song.title}\" as your default ringtone?",
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.System.canWrite(context)
                ) {
                    Toast.makeText(
                        context,
                        "Grant permission to modify system settings, then try again",
                        Toast.LENGTH_LONG
                    ).show()
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } else {
                    try {
                        // Mark in MediaStore as ringtone so it appears in system picker
                        val values = ContentValues().apply {
                            put(MediaStore.Audio.Media.IS_RINGTONE, true)
                            put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                            put(MediaStore.Audio.Media.IS_ALARM, false)
                            put(MediaStore.Audio.Media.IS_MUSIC, false)
                        }
                        context.contentResolver.update(song.uri, values, null, null)
                        RingtoneManager.setActualDefaultRingtoneUri(
                            context,
                            RingtoneManager.TYPE_RINGTONE,
                            song.uri
                        )
                        Toast.makeText(context, "\"${song.title}\" set as ringtone", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to set ringtone: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                }
            }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
