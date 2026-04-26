package com.example.musicplayer.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.domain.model.Folder
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.util.formatDuration

@Composable
fun DashboardScreen(
    onSongClick: (Song) -> Unit,
    onSeeAllLibrary: () -> Unit,
    onSeeAllFolders: () -> Unit,
    onFolderClick: (Folder) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            DashboardSection(
                title = "Library",
                itemCount = state.allSongs.size,
                onSeeAll = onSeeAllLibrary
            ) {
                SongRow(songs = state.allSongs.take(10), onSongClick = {
                    viewModel.play(it)
                    onSongClick(it)
                })
            }
        }
        item {
            DashboardSection(
                title = "Folders",
                itemCount = state.folders.size,
                onSeeAll = onSeeAllFolders
            ) {
                FolderRow(folders = state.folders.take(10), onFolderClick = onFolderClick)
            }
        }
        item {
            DashboardSection(
                title = "Favorites",
                itemCount = state.favorites.size,
                onSeeAll = onSeeAllLibrary
            ) {
                if (state.favorites.isEmpty()) {
                    EmptySection("No favorites yet — heart a song to add it here")
                } else {
                    SongRow(songs = state.favorites.take(10), onSongClick = {
                        viewModel.play(it)
                        onSongClick(it)
                    })
                }
            }
        }
        item {
            DashboardSection(
                title = "Recently Played",
                itemCount = state.recentlyPlayed.size,
                onSeeAll = onSeeAllLibrary
            ) {
                if (state.recentlyPlayed.isEmpty()) {
                    EmptySection("Play some songs to see them here")
                } else {
                    SongRow(songs = state.recentlyPlayed, onSongClick = {
                        viewModel.play(it)
                        onSongClick(it)
                    })
                }
            }
        }
        item {
            DashboardSection(
                title = "Recently Added",
                itemCount = state.recentlyAdded.size,
                onSeeAll = onSeeAllLibrary
            ) {
                SongRow(songs = state.recentlyAdded.take(10), onSongClick = {
                    viewModel.play(it)
                    onSongClick(it)
                })
            }
        }
        item {
            DashboardSection(
                title = "Most Played",
                itemCount = state.mostPlayed.size,
                onSeeAll = onSeeAllLibrary
            ) {
                if (state.mostPlayed.isEmpty()) {
                    EmptySection("Play some songs to see your top tracks here")
                } else {
                    SongRow(songs = state.mostPlayed.take(10), onSongClick = {
                        viewModel.play(it)
                        onSongClick(it)
                    })
                }
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    itemCount: Int,
    onSeeAll: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (itemCount > 0) {
                TextButton(onClick = onSeeAll) { Text("See All ($itemCount)") }
            }
        }
        content()
    }
}

@Composable
private fun SongRow(songs: List<Song>, onSongClick: (Song) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongCard(song = song, onClick = { onSongClick(song) })
        }
    }
}

@Composable
private fun SongCard(song: Song, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.duration.formatDuration(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val folderColors = listOf(
    Color(0xFFE57373) to Color(0xFFB71C1C), // red
    Color(0xFF81C784) to Color(0xFF1B5E20), // green
    Color(0xFF64B5F6) to Color(0xFF0D47A1), // blue
    Color(0xFFFFB74D) to Color(0xFFE65100), // orange
    Color(0xFFBA68C8) to Color(0xFF4A148C), // purple
    Color(0xFF4DD0E1) to Color(0xFF006064), // cyan
    Color(0xFFF06292) to Color(0xFF880E4F), // pink
    Color(0xFFAED581) to Color(0xFF33691E), // light green
)

@Composable
private fun FolderRow(folders: List<Folder>, onFolderClick: (Folder) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(folders, key = { _, f -> f.id }) { index, folder ->
            val (bgColor, iconColor) = folderColors[index % folderColors.size]
            FolderCard(folder = folder, bgColor = bgColor, iconColor = iconColor, onClick = { onFolderClick(folder) })
        }
    }
}

@Composable
private fun FolderCard(folder: Folder, bgColor: Color, iconColor: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = iconColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${folder.songCount} songs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySection(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
