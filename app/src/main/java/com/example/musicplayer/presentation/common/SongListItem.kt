package com.example.musicplayer.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.example.musicplayer.domain.model.AudioFormat
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.util.formatDuration

enum class SongMenuAction {
    PlayNext, AddTo, Enqueue, Ringtone, Trim, Artwork, Share, Delete
}

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onMenuAction: (SongMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            val fmt = song.audioFormat
            val showBadge = fmt != AudioFormat.MP3 && fmt != AudioFormat.AAC &&
                fmt != AudioFormat.M4A && fmt != AudioFormat.UNKNOWN
            if (showBadge) {
                val badgeColor = when {
                    !fmt.isPlayable -> MaterialTheme.colorScheme.error
                    fmt.isHighFidelity -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = buildAnnotatedString {
                        append("${song.artist} · ${song.duration.formatDuration()} · ")
                        withStyle(SpanStyle(color = badgeColor, fontWeight = FontWeight.SemiBold)) {
                            append(fmt.label)
                            if (!fmt.isPlayable) append(" !")
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "${song.artist} · ${song.duration.formatDuration()}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    SongMenuItem(Icons.Default.SkipNext, "Play next") {
                        menuExpanded = false; onMenuAction(SongMenuAction.PlayNext)
                    }
                    SongMenuItem(Icons.Default.PlaylistAdd, "Add to playlist") {
                        menuExpanded = false; onMenuAction(SongMenuAction.AddTo)
                    }
                    SongMenuItem(Icons.Default.Queue, "Enqueue") {
                        menuExpanded = false; onMenuAction(SongMenuAction.Enqueue)
                    }
                    SongMenuItem(Icons.Default.Audiotrack, "Set as ringtone") {
                        menuExpanded = false; onMenuAction(SongMenuAction.Ringtone)
                    }
                    SongMenuItem(Icons.Default.ContentCut, "Trim") {
                        menuExpanded = false; onMenuAction(SongMenuAction.Trim)
                    }
                    SongMenuItem(Icons.Default.Image, "Edit artwork") {
                        menuExpanded = false; onMenuAction(SongMenuAction.Artwork)
                    }
                    SongMenuItem(Icons.Default.Share, "Share") {
                        menuExpanded = false; onMenuAction(SongMenuAction.Share)
                    }
                    SongMenuItem(Icons.Default.Delete, "Delete") {
                        menuExpanded = false; onMenuAction(SongMenuAction.Delete)
                    }
                }
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SongMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}
