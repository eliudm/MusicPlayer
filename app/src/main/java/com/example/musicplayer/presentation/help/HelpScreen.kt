package com.example.musicplayer.presentation.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class FaqItem(val question: String, val answer: String)

private val faqItems = listOf(
    FaqItem(
        "How do I create a playlist?",
        "Go to the Playlists tab and tap the '+' button. Give your playlist a name and tap Create. You can then add songs from the library by tapping the three-dot menu on any song."
    ),
    FaqItem(
        "How do I trim a song?",
        "Long press a song in the Library or Folder view and select 'Trim'. Use the range slider to set the start and end points, then tap 'Trim & Save'."
    ),
    FaqItem(
        "How do I change song artwork?",
        "Long press a song and select 'Edit Artwork'. You can reset to the original, pick an image from your gallery, or search for one online."
    ),
    FaqItem(
        "How do I use the equalizer?",
        "Tap the equalizer icon on the Now Playing screen or go to Settings > Equalizer. Adjust the band levels, bass boost, and virtualizer to your preference. Changes are saved automatically."
    ),
    FaqItem(
        "What does 'Pause on Detach' do?",
        "When enabled, music will automatically pause when you unplug your headphones or disconnect a Bluetooth audio device."
    ),
    FaqItem(
        "How do I back up my playlists?",
        "Go to the drawer menu > Backup & Restore. This feature is coming soon and will support Google Drive backup."
    ),
    FaqItem(
        "Can I play music in the background?",
        "Yes. Music continues playing when you leave the app. A notification appears in the status bar with playback controls."
    )
)

private const val TERMS_TEXT = """
Music Player — Terms of Use

1. License
This app is provided for personal, non-commercial use. You may not redistribute, reverse-engineer, or modify the app.

2. Content
You are responsible for ensuring that you have the rights to the music files you play using this app. We do not host, upload, or distribute any copyrighted audio content.

3. Privacy
This app does not collect or transmit personal data. Artwork searches use the iTunes Search API; your query is sent to Apple's servers solely to retrieve artwork images.

4. Disclaimer
This app is provided "as is" without warranty of any kind. We are not liable for any loss of data or damage caused by use of the app.

5. Changes
These terms may be updated at any time. Continued use of the app constitutes acceptance of the current terms.
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("FAQ") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Terms") })
            }

            when (selectedTab) {
                0 -> FaqTab()
                1 -> TermsTab()
            }
        }
    }
}

@Composable
private fun FaqTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(faqItems.size) { index ->
            FaqCard(faqItems[index])
        }
    }
}

@Composable
private fun FaqCard(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.question, style = MaterialTheme.typography.titleSmall)
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TermsTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                TERMS_TEXT.trim(),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
    }
}
