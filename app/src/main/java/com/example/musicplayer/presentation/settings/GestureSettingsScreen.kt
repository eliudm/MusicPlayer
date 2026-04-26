package com.example.musicplayer.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.domain.model.GestureAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class GestureSettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    data class State(
        val swipeEnabled: Boolean = true,
        val pinchEnabled: Boolean = true,
        val swipeUpAction: GestureAction = GestureAction.TOGGLE_LYRICS,
        val swipeDownAction: GestureAction = GestureAction.NONE
    )

    val state: StateFlow<State> = combine(
        prefs.gestureSwipeEnabled,
        prefs.gesturePinchEnabled,
        prefs.swipeUpAction,
        prefs.swipeDownAction
    ) { swipe, pinch, up, down -> State(swipe, pinch, up, down) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State())

    fun setSwipeEnabled(v: Boolean) = viewModelScope.launch { prefs.setGestureSwipeEnabled(v) }
    fun setPinchEnabled(v: Boolean) = viewModelScope.launch { prefs.setGesturePinchEnabled(v) }
    fun setSwipeUpAction(a: GestureAction) = viewModelScope.launch { prefs.setSwipeUpAction(a) }
    fun setSwipeDownAction(a: GestureAction) = viewModelScope.launch { prefs.setSwipeDownAction(a) }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: GestureSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Controls") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SectionLabel("Swipe Gestures") }

            item {
                ListItem(
                    headlineContent = { Text("Enable swipe gestures") },
                    supportingContent = {
                        Text("Swipe left / right on the player to skip tracks.\nSwipe up / down for custom actions.")
                    },
                    trailingContent = {
                        Switch(
                            checked = state.swipeEnabled,
                            onCheckedChange = { viewModel.setSwipeEnabled(it) }
                        )
                    }
                )
            }

            if (state.swipeEnabled) {
                item {
                    InfoRow(
                        label = "Swipe Left",
                        value = "Skip Next  (fixed)"
                    )
                }
                item {
                    InfoRow(
                        label = "Swipe Right",
                        value = "Skip Previous  (fixed)"
                    )
                }
                item {
                    ActionPickerRow(
                        label = "Swipe Up",
                        current = state.swipeUpAction,
                        onPick = { viewModel.setSwipeUpAction(it) }
                    )
                }
                item {
                    ActionPickerRow(
                        label = "Swipe Down",
                        current = state.swipeDownAction,
                        onPick = { viewModel.setSwipeDownAction(it) }
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) }
            item { SectionLabel("Pinch Gesture") }

            item {
                ListItem(
                    headlineContent = { Text("Pinch-to-zoom for volume") },
                    supportingContent = {
                        Text("Spread two fingers to raise volume, pinch to lower it.")
                    },
                    trailingContent = {
                        Switch(
                            checked = state.pinchEnabled,
                            onCheckedChange = { viewModel.setPinchEnabled(it) }
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) }
            item { SectionLabel("Tips") }

            item {
                Text(
                    text = "Gestures work on the Now Playing screen. Swipe gestures apply to:\n" +
                        "  • The album art area (Cover view)\n" +
                        "  • The full screen (Lyrics view)\n\n" +
                        "Pinch-to-zoom works anywhere on the player.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun ActionPickerRow(
    label: String,
    current: GestureAction,
    onPick: (GestureAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(current.label)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    GestureAction.entries.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.label) },
                            onClick = {
                                onPick(action)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}
