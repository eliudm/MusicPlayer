package com.example.musicplayer.presentation.trim

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrimViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val song = state.song
    val duration = song?.duration ?: 1L

    // Convert ms positions to 0f..1f for the range slider
    var sliderRange by remember(duration) {
        mutableStateOf(0f..1f)
    }

    // Sync slider with state when song loads
    LaunchedEffect(song) {
        if (song != null) sliderRange = 0f..1f
    }

    val startMs = (sliderRange.start * duration).toLong()
    val endMs = (sliderRange.endInclusive * duration).toLong()

    // Show result/error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.savedFileName) {
        state.savedFileName?.let {
            snackbarHostState.showSnackbar("Saved: $it")
            viewModel.clearResult()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Trim Audio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (song == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            // Time labels above the slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(startMs.formatDuration(), style = MaterialTheme.typography.labelLarge)
                Text(
                    "Duration: ${(endMs - startMs).formatDuration()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(endMs.formatDuration(), style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(8.dp))

            RangeSlider(
                value = sliderRange,
                onValueChange = { range ->
                    sliderRange = range
                    viewModel.setRange(
                        (range.start * duration).toLong(),
                        (range.endInclusive * duration).toLong()
                    )
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )

            // Full duration label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0:00", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(duration.formatDuration(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(48.dp))

            if (state.isProcessing) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Trimming…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(
                    onClick = { viewModel.trim() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Trim & Save to Music Folder")
                }
            }
        }
    }
}
