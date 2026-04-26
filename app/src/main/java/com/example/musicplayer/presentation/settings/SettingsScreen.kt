package com.example.musicplayer.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToScanFilters: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToGestureSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { SectionHeader("Audio") }

            item {
                ListItem(
                    headlineContent = { Text("Equalizer") },
                    supportingContent = { Text("EQ bands, bass boost, reverb and loudness") },
                    leadingContent = {
                        Icon(Icons.Default.Equalizer, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickableListItem(onClick = onNavigateToEqualizer)
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader("Library") }

            item {
                ListItem(
                    headlineContent = { Text("Scan Filters") },
                    supportingContent = { Text("Exclude songs by duration or file size") },
                    leadingContent = {
                        Icon(Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickableListItem(onClick = onNavigateToScanFilters)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Backup & Restore") },
                    supportingContent = { Text("Save and restore your data via Google Drive") },
                    leadingContent = {
                        Icon(Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickableListItem(onClick = onNavigateToBackup)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Gesture Controls") },
                    supportingContent = { Text("Swipe to skip, pinch for volume, custom actions") },
                    leadingContent = {
                        Icon(Icons.Default.Swipe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickableListItem(onClick = onNavigateToGestureSettings)
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader("Playback") }

            item {
                ListItem(
                    headlineContent = { Text("Crossfade") },
                    supportingContent = { Text("Blend between tracks") },
                    trailingContent = {
                        Switch(
                            checked = state.crossfadeEnabled,
                            onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                        )
                    }
                )
            }
            if (state.crossfadeEnabled) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Duration: ${state.crossfadeDurationSec.toInt()}s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = state.crossfadeDurationSec,
                            onValueChange = { viewModel.setCrossfadeDuration(it) },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader("Audio Device") }

            item {
                val sampleKhz = if (state.outputSampleRateHz > 0)
                    "${"%.0f".format(state.outputSampleRateHz / 1000f)} kHz" else "Unknown"
                ListItem(
                    headlineContent = { Text("Output Sample Rate") },
                    supportingContent = { Text(sampleKhz) },
                    leadingContent = {
                        Icon(Icons.Default.Usb, contentDescription = null,
                            tint = if (state.hasUsbDac) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
            }

            if (state.hasUsbDac) {
                item {
                    ListItem(
                        headlineContent = { Text("USB DAC connected") },
                        supportingContent = { Text("External DAC/headphone amplifier detected") },
                        leadingContent = {
                            Icon(Icons.Default.Usb, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }

            if (state.hasHighResPlatform) {
                item {
                    ListItem(
                        headlineContent = { Text("Hi-Res Audio certified") },
                        supportingContent = { Text("This device supports professional-grade audio latency") }
                    )
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader("About") }
            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text("1.0.0") }
                )
            }
        }
    }
}

private fun Modifier.clickableListItem(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
