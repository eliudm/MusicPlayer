package com.example.musicplayer.presentation.folder

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.presentation.common.AddToPlaylistDialog
import com.example.musicplayer.presentation.common.SetRingtoneDialog
import com.example.musicplayer.presentation.common.SongListItem
import com.example.musicplayer.presentation.common.SongMenuAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSongsScreen(
    onSongClick: (Song) -> Unit,
    onNavigateBack: () -> Unit,
    onTrimSong: (Song) -> Unit = {},
    onArtworkSong: (Song) -> Unit = {},
    viewModel: FolderSongsViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val folderName by viewModel.folderName.collectAsState()
    val context = LocalContext.current
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var ringtoneForSong by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(folderName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${songs.size} songs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No songs in this folder", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = {
                            viewModel.play(song)
                            onSongClick(song)
                        },
                        onMenuAction = { action ->
                            when (action) {
                                SongMenuAction.Share -> {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, song.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share ${song.title}"))
                                }
                                SongMenuAction.Ringtone -> ringtoneForSong = song
                                SongMenuAction.Trim -> onTrimSong(song)
                                SongMenuAction.Artwork -> onArtworkSong(song)
                                SongMenuAction.AddTo -> addToPlaylistSong = song
                                else -> viewModel.handleMenuAction(song, action)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }

    addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(song = song, onDismiss = { addToPlaylistSong = null })
    }

    ringtoneForSong?.let { song ->
        SetRingtoneDialog(song = song, onDismiss = { ringtoneForSong = null })
    }
}

