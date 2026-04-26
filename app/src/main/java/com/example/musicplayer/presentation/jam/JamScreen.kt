package com.example.musicplayer.presentation.jam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.service.JamSession
import com.example.musicplayer.service.RemoteSong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamScreen(
    onNavigateBack: () -> Unit,
    viewModel: JamViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jam Session") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Host") },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Join") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> HostTab(
                    isHosting = state.jamState.isHosting,
                    sessionName = state.sessionName,
                    connectedPeers = state.jamState.connectedPeers,
                    onSessionNameChange = viewModel::setSessionName,
                    onStartHosting = viewModel::startHosting,
                    onStopHosting = viewModel::stopHosting
                )
                1 -> JoinTab(
                    isSearching = state.jamState.isSearching,
                    sessions = state.jamState.availableSessions,
                    joinedSession = state.jamState.joinedSession,
                    hostSongs = state.hostSongs,
                    isLoadingSongs = state.isLoadingHostSongs,
                    addedSongIds = state.addedSongIds,
                    onSearch = viewModel::startDiscovery,
                    onStopSearch = viewModel::stopDiscovery,
                    onBrowse = viewModel::browseSession,
                    onLeave = viewModel::leaveSession,
                    onAddSong = { session, songId -> viewModel.addSongToHost(session, songId) }
                )
            }
        }
    }
}

// ── Host tab ──────────────────────────────────────────────────────────────────

@Composable
private fun HostTab(
    isHosting: Boolean,
    sessionName: String,
    connectedPeers: Int,
    onSessionNameChange: (String) -> Unit,
    onStartHosting: () -> Unit,
    onStopHosting: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isHosting) {
            Text(
                "Share your music library with others on the same Wi-Fi network.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = sessionName,
                onValueChange = onSessionNameChange,
                label = { Text("Session name") },
                leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onStartHosting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Wifi, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Hosting")
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Wifi,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Hosting: $sessionName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$connectedPeers ${if (connectedPeers == 1) "person" else "people"} connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Friends on the same Wi-Fi can join and add songs to your queue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
            OutlinedButton(
                onClick = onStopHosting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Stop Hosting")
            }
        }
    }
}

// ── Join tab ──────────────────────────────────────────────────────────────────

@Composable
private fun JoinTab(
    isSearching: Boolean,
    sessions: List<JamSession>,
    joinedSession: JamSession?,
    hostSongs: List<RemoteSong>,
    isLoadingSongs: Boolean,
    addedSongIds: Set<Long>,
    onSearch: () -> Unit,
    onStopSearch: () -> Unit,
    onBrowse: (JamSession) -> Unit,
    onLeave: () -> Unit,
    onAddSong: (JamSession, Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (joinedSession == null) {
            // Discovery panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Find nearby Jam sessions",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isSearching) {
                    TextButton(onClick = onStopSearch) { Text("Stop") }
                } else {
                    Button(onClick = onSearch) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Search")
                    }
                }
            }

            if (isSearching && sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Searching for sessions…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (!isSearching && sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No sessions found. Ask the host to start a Jam.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions) { session ->
                        SessionCard(session = session, onBrowse = onBrowse)
                    }
                }
            }
        } else {
            // Browsing host's songs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    joinedSession.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onLeave) { Text("Leave") }
            }
            Text(
                "Tap a song to add it to the host's queue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (isLoadingSongs) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(hostSongs) { song ->
                        val added = song.id in addedSongIds
                        RemoteSongRow(
                            song = song,
                            added = added,
                            onAdd = { if (!added) onAddSong(joinedSession, song.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: JamSession, onBrowse: (JamSession) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Wifi,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(session.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(session.host, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = { onBrowse(session) }) { Text("Browse") }
        }
    }
}

@Composable
private fun RemoteSongRow(song: RemoteSong, added: Boolean, onAdd: () -> Unit) {
    Surface(
        onClick = onAdd,
        enabled = !added,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    "${song.artist} • ${song.album}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Icon(
                    imageVector = if (added) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    contentDescription = if (added) "Added" else "Add to queue",
                    tint = if (added) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}
