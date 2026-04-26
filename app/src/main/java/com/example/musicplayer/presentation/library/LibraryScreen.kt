package com.example.musicplayer.presentation.library

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.presentation.common.AddToPlaylistDialog
import com.example.musicplayer.presentation.common.SetRingtoneDialog
import com.example.musicplayer.presentation.common.SongListItem
import com.example.musicplayer.presentation.common.SongMenuAction

@Composable
fun LibraryScreen(
    onSongClick: (Song) -> Unit,
    onTrimSong: (Song) -> Unit = {},
    onArtworkSong: (Song) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var ringtoneForSong by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(Unit) { viewModel.onEvent(LibraryEvent.ScanMedia) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onEvent(LibraryEvent.Search(it)) },
            placeholder = { Text("Search songs, albums, artists…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Category chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortCategory.entries.forEach { cat ->
                FilterChip(
                    selected = state.sortCategory == cat,
                    onClick = { viewModel.onEvent(LibraryEvent.SetSortCategory(cat)) },
                    label = { Text(cat.label) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Sort order row — only visible in SONGS view
        if (state.sortCategory == SortCategory.SONGS) {
            SortOrderRow(
                currentOrder = state.sortOrder,
                onOrderSelected = { viewModel.onEvent(LibraryEvent.SetSortOrder(it)) }
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (state.sortCategory) {
                SortCategory.SONGS -> {
                    if (state.displaySongs.isEmpty()) {
                        EmptyState("No songs found")
                    } else {
                        SongList(
                            songs = state.displaySongs,
                            onItemClick = { song ->
                                viewModel.onEvent(LibraryEvent.PlaySong(song))
                                onSongClick(song)
                            },
                            onTrimSong = onTrimSong,
                            onArtworkSong = onArtworkSong,
                            onAddToPlaylist = { addToPlaylistSong = it },
                            onRingtone = { ringtoneForSong = it },
                            onMenuAction = { song, action -> viewModel.onEvent(LibraryEvent.SongMenuAction(song, action)) },
                            context = context
                        )
                    }
                }
                SortCategory.PLAYLISTS -> {
                    if (state.playlists.isEmpty()) {
                        EmptyState("No playlists yet")
                    } else {
                        PlaylistList(
                            playlists = state.playlists,
                            onPlaylistSongClick = { song ->
                                viewModel.onEvent(LibraryEvent.PlaySong(song))
                                onSongClick(song)
                            }
                        )
                    }
                }
                else -> {
                    if (state.groupedSongs.isEmpty()) {
                        EmptyState("No songs found")
                    } else {
                        GroupedSongList(
                            grouped = state.groupedSongs,
                            onItemClick = { song ->
                                viewModel.onEvent(LibraryEvent.PlaySong(song))
                                onSongClick(song)
                            },
                            onTrimSong = onTrimSong,
                            onArtworkSong = onArtworkSong,
                            onAddToPlaylist = { addToPlaylistSong = it },
                            onRingtone = { ringtoneForSong = it },
                            onMenuAction = { song, action -> viewModel.onEvent(LibraryEvent.SongMenuAction(song, action)) },
                            context = context
                        )
                    }
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

@Composable
private fun SortOrderRow(
    currentOrder: SortOrder,
    onOrderSelected: (SortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(currentOrder.label, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.label) },
                        onClick = {
                            onOrderSelected(order)
                            expanded = false
                        },
                        leadingIcon = if (order == currentOrder) ({
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        }) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    onItemClick: (Song) -> Unit,
    onTrimSong: (Song) -> Unit,
    onArtworkSong: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onRingtone: (Song) -> Unit,
    onMenuAction: (Song, SongMenuAction) -> Unit,
    context: android.content.Context
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                onClick = { onItemClick(song) },
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
                        SongMenuAction.Ringtone -> onRingtone(song)
                        SongMenuAction.Trim -> onTrimSong(song)
                        SongMenuAction.Artwork -> onArtworkSong(song)
                        SongMenuAction.AddTo -> onAddToPlaylist(song)
                        else -> onMenuAction(song, action)
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedSongList(
    grouped: Map<String, List<Song>>,
    onItemClick: (Song) -> Unit,
    onTrimSong: (Song) -> Unit,
    onArtworkSong: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onRingtone: (Song) -> Unit,
    onMenuAction: (Song, SongMenuAction) -> Unit,
    context: android.content.Context
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (header, songs) ->
            stickyHeader(key = "header_$header") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            items(songs, key = { "${header}_${it.id}" }) { song ->
                SongListItem(
                    song = song,
                    onClick = { onItemClick(song) },
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
                            SongMenuAction.Ringtone -> onRingtone(song)
                            SongMenuAction.Trim -> onTrimSong(song)
                            SongMenuAction.Artwork -> onArtworkSong(song)
                            SongMenuAction.AddTo -> onAddToPlaylist(song)
                            else -> onMenuAction(song, action)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onPlaylistSongClick: (Song) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        playlists.forEach { playlist ->
            stickyHeader(key = "playlist_${playlist.id}") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${playlist.songs.size} songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (playlist.songs.isEmpty()) {
                item(key = "playlist_empty_${playlist.id}") {
                    Text(
                        text = "No songs in this playlist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(playlist.songs, key = { "pl_${playlist.id}_${it.id}" }) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onPlaylistSongClick(song) },
                        onMenuAction = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
