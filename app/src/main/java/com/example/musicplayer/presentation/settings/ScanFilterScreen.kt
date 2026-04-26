package com.example.musicplayer.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanFilterScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanFilterViewModel = hiltViewModel()
) {
    val minDurationSec by viewModel.minDurationSec.collectAsState()
    val minSizeKb by viewModel.minSizeKb.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Filters") },
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
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Songs that don't meet these thresholds are excluded from your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Minimum duration
            FilterCard(
                title = "Minimum Duration",
                value = formatDuration(minDurationSec),
                hint = "Excludes short clips, ringtones, and sound effects"
            ) {
                Slider(
                    value = minDurationSec.toFloat(),
                    onValueChange = { viewModel.setMinDurationSec(it.toInt()) },
                    valueRange = 0f..300f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0s", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("5 min", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DurationPresets(
                    current = minDurationSec,
                    presets = listOf(0 to "Off", 15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m"),
                    onSelect = { viewModel.setMinDurationSec(it) }
                )
            }

            // Minimum file size
            FilterCard(
                title = "Minimum File Size",
                value = formatSize(minSizeKb),
                hint = "Excludes low-quality or incomplete audio files"
            ) {
                Slider(
                    value = minSizeKb.toFloat(),
                    onValueChange = { viewModel.setMinSizeKb(it.toInt()) },
                    valueRange = 0f..5120f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0 KB", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("5 MB", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DurationPresets(
                    current = minSizeKb,
                    presets = listOf(0 to "Off", 100 to "100 KB", 500 to "500 KB", 1024 to "1 MB", 2048 to "2 MB"),
                    onSelect = { viewModel.setMinSizeKb(it) }
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    title: String,
    value: String,
    hint: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Text(hint, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun DurationPresets(
    current: Int,
    presets: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (value, label) ->
            FilterChip(
                selected = current == value,
                onClick = { onSelect(value) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

private fun formatDuration(seconds: Int): String = when {
    seconds == 0 -> "Off"
    seconds < 60 -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60}m"
    else -> "${seconds / 60}m ${seconds % 60}s"
}

private fun formatSize(kb: Int): String = when {
    kb == 0 -> "Off"
    kb < 1024 -> "$kb KB"
    kb % 1024 == 0 -> "${kb / 1024} MB"
    else -> "${"%.1f".format(kb / 1024f)} MB"
}
