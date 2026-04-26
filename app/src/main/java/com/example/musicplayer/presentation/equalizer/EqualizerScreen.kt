package com.example.musicplayer.presentation.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onNavigateBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = if (state.isEnabled) "On" else "Off",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled(it) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.presets.isNotEmpty()) {
                EqCard("Preset") {
                    PresetSelector(
                        presets = state.presets,
                        selectedIndex = state.selectedPreset,
                        onPresetSelected = { viewModel.applyPreset(it) }
                    )
                }
            }

            EqCard("Bands") {
                if (state.bands.isEmpty()) {
                    Text(
                        "Equalizer not available on this device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        state.bands.forEachIndexed { index, level ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val db = (level / 100).toInt()
                                Text(
                                    text = "${if (db >= 0) "+" else ""}$db",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = if (state.isEnabled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                VerticalSlider(
                                    value = level,
                                    onValueChange = { if (state.isEnabled) viewModel.setBandLevel(index, it) },
                                    valueRange = state.bandLevelMin.toFloat()..state.bandLevelMax.toFloat(),
                                    enabled = state.isEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                                Text(
                                    text = state.bandFrequencies.getOrElse(index) { "${index + 1}" },
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            EqCard("Bass Boost") {
                StrengthSlider(
                    value = state.bassBoostStrength,
                    onValueChange = { viewModel.setBassBoost(it) },
                    enabled = state.isEnabled
                )
            }

            EqCard("Virtualizer (Surround)") {
                StrengthSlider(
                    value = state.virtualizerStrength,
                    onValueChange = { viewModel.setVirtualizer(it) },
                    enabled = state.isEnabled
                )
            }

            if (state.reverbPresets.isNotEmpty()) {
                EqCard("3D Reverb") {
                    ReverbPresetSelector(
                        presets = state.reverbPresets,
                        selectedIndex = state.selectedReverbPreset,
                        onPresetSelected = { viewModel.setReverbPreset(it) },
                        enabled = state.isEnabled
                    )
                }
            }

            EqCard("Loudness Enhancer") {
                LoudnessSlider(
                    gainMb = state.loudnessGainMb,
                    onValueChange = { viewModel.setLoudnessGain(it) },
                    enabled = state.isEnabled
                )
            }
        }
    }
}

@Composable
private fun EqCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun StrengthSlider(value: Float, onValueChange: (Float) -> Unit, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1000f,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${(value / 10).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSelector(presets: List<String>, selectedIndex: Int, onPresetSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selectedIndex in presets.indices) presets[selectedIndex] else "Custom"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEachIndexed { index, preset ->
                DropdownMenuItem(
                    text = { Text(preset) },
                    onClick = { onPresetSelected(index); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReverbPresetSelector(
    presets: List<String>,
    selectedIndex: Int,
    onPresetSelected: (Int) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val label = presets.getOrElse(selectedIndex) { "None" }

    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            presets.forEachIndexed { index, preset ->
                DropdownMenuItem(
                    text = { Text(preset) },
                    onClick = { onPresetSelected(index); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun LoudnessSlider(gainMb: Float, onValueChange: (Float) -> Unit, enabled: Boolean) {
    val gainDb = gainMb / 100f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = gainMb,
            onValueChange = onValueChange,
            valueRange = 0f..1500f,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "+${"%.1f".format(gainDb)}dB",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Vertical slider: layout modifier swaps width/height so the Slider
 * measures itself using the column's height as its width, then rotate(-90f)
 * flips both the rendering and the touch hitbox into the vertical orientation.
 */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth  = constraints.minHeight,
                        maxWidth  = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = -(placeable.width  - placeable.height) / 2,
                        y = -(placeable.height - placeable.width)  / 2
                    )
                }
            }
            .rotate(-90f)
    )
}
