package com.example.musicplayer.presentation.themes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.PlayerBackground
import com.example.musicplayer.ui.theme.SkinConfig
import com.example.musicplayer.ui.theme.allSkins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinThemeScreen(
    currentSkinId: String,
    onSkinApplied: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var pendingSkinId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skin Theme") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (pendingSkinId != null && pendingSkinId != currentSkinId) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pendingSkinId = null },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                onSkinApplied(pendingSkinId!!)
                                pendingSkinId = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Apply Skin") }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allSkins, key = { it.id }) { skin ->
                val isActive = skin.id == currentSkinId
                val isPending = skin.id == pendingSkinId
                SkinCard(
                    skin = skin,
                    isActive = isActive,
                    isPending = isPending,
                    onClick = { pendingSkinId = if (isPending) null else skin.id }
                )
            }
        }
    }
}

@Composable
private fun SkinCard(
    skin: SkinConfig,
    isActive: Boolean,
    isPending: Boolean,
    onClick: () -> Unit
) {
    val highlighted = isActive || isPending
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "border"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (highlighted) 2.dp else 0.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 6.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${skin.emoji}  ${skin.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        skin.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isActive) {
                        Text(
                            "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (highlighted) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Mini player preview
            PlayerPreview(skin = skin)
        }
    }
}

@Composable
private fun PlayerPreview(skin: SkinConfig) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val bgModifier = when (skin.playerBackground) {
        PlayerBackground.GRADIENT -> Modifier.background(
            Brush.verticalGradient(listOf(primary.copy(alpha = 0.7f), background))
        )
        PlayerBackground.ARTWORK -> Modifier.background(
            Brush.verticalGradient(
                listOf(
                    primary.copy(alpha = 0.4f),
                    primary.copy(alpha = 0.15f),
                    background.copy(alpha = 0.95f)
                )
            )
        )
        PlayerBackground.STANDARD -> Modifier.background(surface)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(skin.shapes.medium)
            .then(bgModifier)
            .padding(12.dp)
    ) {
        // Artwork bg hint for ARTWORK skin
        if (skin.playerBackground == PlayerBackground.ARTWORK) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .clip(skin.shapes.medium)
                    .background(primary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Song info row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val artworkBg = if (skin.playerBackground != PlayerBackground.STANDARD)
                    primary.copy(alpha = 0.3f) else primary.copy(alpha = 0.15f)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(skin.shapes.small)
                        .background(artworkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(8.dp)
                            .clip(skin.shapes.extraSmall)
                            .background(onSurface.copy(alpha = 0.8f))
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(6.dp)
                            .clip(skin.shapes.extraSmall)
                            .background(onSurfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(skin.shapes.extraSmall)
                    .background(onSurface.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .fillMaxHeight()
                        .background(primary)
                )
            }

            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Icons.Default.Equalizer to 14.dp,
                    Icons.Default.SkipPrevious to 18.dp,
                    Icons.Default.PlayArrow to 22.dp,
                    Icons.Default.SkipNext to 18.dp,
                    Icons.Default.Pause to 14.dp
                ).forEach { (icon, size) ->
                    val isPlayBtn = icon == Icons.Default.PlayArrow
                    if (isPlayBtn) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(size))
                        }
                    } else {
                        Icon(icon, null, tint = onSurface.copy(alpha = 0.7f), modifier = Modifier.size(size))
                    }
                }
            }
        }
    }
}
