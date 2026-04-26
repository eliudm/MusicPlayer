package com.example.musicplayer.presentation.queue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onDismiss: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Up Next",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-DJ", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = state.autoDjEnabled,
                        onCheckedChange = { viewModel.toggleAutoDj() },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = viewModel::clearQueue) { Text("Clear") }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (state.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Queue is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ReorderableQueueList(
                    songs = state.queue,
                    currentIndex = state.currentIndex,
                    onRemove = viewModel::removeItem,
                    onMove = viewModel::moveItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableQueueList(
    songs: List<Song>,
    currentIndex: Int,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(modifier = modifier) {
        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
            val isDragging = index == draggingIndex
            Box(
                modifier = Modifier
                    .then(
                        if (isDragging) Modifier
                            .graphicsLayer { translationY = dragOffsetY }
                            .zIndex(1f)
                        else Modifier
                    )
            ) {
                QueueSongRow(
                    song = song,
                    isCurrent = index == currentIndex,
                    onRemove = { onRemove(index) },
                    dragHandleModifier = Modifier.pointerInput(index) {
                        val itemPx = 64.dp.toPx()
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingIndex = index; dragOffsetY = 0f },
                            onDragEnd = { draggingIndex = -1; dragOffsetY = 0f },
                            onDragCancel = { draggingIndex = -1; dragOffsetY = 0f },
                            onDrag = { _, delta ->
                                dragOffsetY += delta.y
                                val steps = (dragOffsetY / itemPx).toInt()
                                if (steps != 0) {
                                    val newIdx = (draggingIndex + steps)
                                        .coerceIn(0, songs.lastIndex)
                                    if (newIdx != draggingIndex) {
                                        onMove(draggingIndex, newIdx)
                                        draggingIndex = newIdx
                                        dragOffsetY -= steps * itemPx
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun QueueSongRow(
    song: Song,
    isCurrent: Boolean,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier
) {
    val bg = if (isCurrent)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            modifier = dragHandleModifier
                .size(28.dp)
                .padding(horizontal = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isCurrent) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
