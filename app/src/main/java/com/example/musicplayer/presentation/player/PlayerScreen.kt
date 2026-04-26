package com.example.musicplayer.presentation.player

import android.app.Activity
import android.view.WindowManager
import com.example.musicplayer.service.SpectrumProvider
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import com.example.musicplayer.presentation.queue.QueueScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.domain.model.AudioFormat
import com.example.musicplayer.domain.model.GestureAction
import com.example.musicplayer.domain.model.GestureSettings
import com.example.musicplayer.domain.model.Lyrics
import com.example.musicplayer.domain.model.LyricsLine
import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.presentation.lyrics.LyricsStatus
import com.example.musicplayer.presentation.lyrics.LyricsViewModel
import com.example.musicplayer.ui.theme.LocalSkin
import com.example.musicplayer.ui.theme.PlayerBackground
import com.example.musicplayer.util.artworkModel
import com.example.musicplayer.util.formatDuration
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val PAGE_VISUALIZER = 0
private const val PAGE_IMAGE = 1
private const val PAGE_ICON = 2
private val pageLabels = listOf("Visualizer", "Cover", "Icon")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    onNavigateToEqualizer: () -> Unit,
    onNavigateToJam: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lyricsState by lyricsViewModel.uiState.collectAsState()
    val gestureSettings by viewModel.gestureSettings.collectAsState()
    val isCastActive by viewModel.isCastActive.collectAsState()
    val context = LocalContext.current
    val skin = LocalSkin.current
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val scope = rememberCoroutineScope()

    var showLyrics by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    if (showQueueSheet) {
        QueueScreen(onDismiss = { showQueueSheet = false })
    }

    // Gesture feedback toast
    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            kotlinx.coroutines.delay(900)
            gestureFeedback = null
        }
    }

    fun fireGestureAction(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> return
            GestureAction.SKIP_NEXT -> {
                viewModel.onEvent(PlayerEvent.SkipNext)
                gestureFeedback = "Skip Next"
            }
            GestureAction.SKIP_PREVIOUS -> {
                viewModel.onEvent(PlayerEvent.SkipPrevious)
                gestureFeedback = "Skip Previous"
            }
            GestureAction.TOGGLE_PLAY_PAUSE -> {
                viewModel.onEvent(PlayerEvent.TogglePlayPause)
                gestureFeedback = "Play / Pause"
            }
            GestureAction.TOGGLE_FAVORITE -> {
                viewModel.onEvent(PlayerEvent.ToggleFav)
                gestureFeedback = "Toggle Favourite"
            }
            GestureAction.TOGGLE_SHUFFLE -> {
                viewModel.onEvent(PlayerEvent.ToggleShuffle)
                gestureFeedback = "Toggle Shuffle"
            }
            GestureAction.CYCLE_REPEAT -> {
                viewModel.onEvent(PlayerEvent.CycleRepeat)
                gestureFeedback = "Cycle Repeat"
            }
            GestureAction.TOGGLE_LYRICS -> {
                showLyrics = !showLyrics
                gestureFeedback = if (!showLyrics) "Lyrics" else "Cover"
            }
        }
    }

    // Pinch-to-zoom → volume
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        if (gestureSettings.pinchEnabled) {
            if (zoomChange > 1.04f) viewModel.adjustVolume(raise = true)
            else if (zoomChange < 0.96f) viewModel.adjustVolume(raise = false)
        }
    }

    val backgroundModifier = when (skin.playerBackground) {
        PlayerBackground.GRADIENT -> Modifier.background(
            Brush.verticalGradient(
                listOf(primary.copy(alpha = 0.55f), primary.copy(alpha = 0.15f), background)
            )
        )
        PlayerBackground.ARTWORK, PlayerBackground.STANDARD -> Modifier.background(background)
    }

    if (showEditDialog) {
        LyricsEditDialog(
            onDismiss = { showEditDialog = false },
            onSave = { text ->
                state.currentSong?.let { song ->
                    lyricsViewModel.saveManualLyrics(song.id, text, song)
                }
                showEditDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(transformableState, enabled = gestureSettings.pinchEnabled)
    ) {
        if (skin.playerBackground == PlayerBackground.ARTWORK && state.currentSong != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(state.currentSong!!.artworkModel(context))
                    .memoryCacheKey("bg_${state.currentSong!!.id}_v${state.artworkVersion}")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.18f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                background.copy(alpha = 0.3f),
                                background.copy(alpha = 0.85f),
                                background
                            )
                        )
                    )
            )
        }

        if (showLyrics) {
            KeepScreenOn()
            // Lyrics layout — non-scrollable Column so weight works
            Box(modifier = Modifier.fillMaxSize()) {
                // Album art as dimmed background
                if (state.currentSong != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(state.currentSong!!.artworkModel(context))
                            .memoryCacheKey("lbg_${state.currentSong!!.id}_v${state.artworkVersion}")
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.22f
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        background.copy(alpha = 0.45f),
                                        background.copy(alpha = 0.88f),
                                        background
                                    )
                                )
                            )
                    )
                }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    // Horizontal swipe on lyrics = skip; vertical stays with LazyColumn scroll
                    .pointerInput(gestureSettings.swipeEnabled) {
                        if (!gestureSettings.swipeEnabled) return@pointerInput
                        var totalX = 0f
                        var totalY = 0f
                        detectDragGestures(
                            onDragStart = { totalX = 0f; totalY = 0f },
                            onDragEnd = {
                                val threshold = 80.dp.toPx()
                                if (abs(totalX) > abs(totalY) && abs(totalX) > threshold) {
                                    if (totalX < 0) fireGestureAction(GestureAction.SKIP_NEXT)
                                    else fireGestureAction(GestureAction.SKIP_PREVIOUS)
                                } else if (abs(totalY) > abs(totalX) && abs(totalY) > threshold) {
                                    if (totalY < 0) fireGestureAction(gestureSettings.swipeUpAction)
                                    else fireGestureAction(gestureSettings.swipeDownAction)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row
                PlayerTopRow(
                    isFavorite = state.isFavorite,
                    showLyrics = true,
                    isCastActive = isCastActive,
                    onEqualizer = onNavigateToEqualizer,
                    onToggleLyrics = { showLyrics = false },
                    onToggleFav = { viewModel.onEvent(PlayerEvent.ToggleFav) }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = state.currentSong?.title ?: "No song selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.currentSong?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // Karaoke view — fills remaining space
                LyricsArea(
                    lyricsState = lyricsState,
                    positionMs = state.positionMs,
                    currentSong = state.currentSong,
                    onRetry = { lyricsViewModel.retry(state.currentSong) },
                    onEdit = { showEditDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                PlayerSliderRow(
                    progress = state.progress,
                    positionMs = state.positionMs,
                    duration = state.currentSong?.duration,
                    onSeek = { progress ->
                        val duration = state.currentSong?.duration ?: 0L
                        viewModel.onEvent(PlayerEvent.Seek((progress * duration).toLong()))
                    }
                )

                Spacer(Modifier.height(12.dp))

                PlayerControls(
                    isPlaying = state.isPlaying,
                    shuffleEnabled = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    onTogglePlayPause = { viewModel.onEvent(PlayerEvent.TogglePlayPause) },
                    onSkipNext = { viewModel.onEvent(PlayerEvent.SkipNext) },
                    onSkipPrevious = { viewModel.onEvent(PlayerEvent.SkipPrevious) },
                    onToggleShuffle = { viewModel.onEvent(PlayerEvent.ToggleShuffle) },
                    onCycleRepeat = { viewModel.onEvent(PlayerEvent.CycleRepeat) }
                )
                Spacer(Modifier.height(8.dp))
            }
            } // close outer Box (artwork background)
        } else {
            // Original album art layout — scrollable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backgroundModifier)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerTopRow(
                    isFavorite = state.isFavorite,
                    showLyrics = false,
                    isCastActive = isCastActive,
                    onEqualizer = onNavigateToEqualizer,
                    onToggleLyrics = { showLyrics = true },
                    onToggleFav = { viewModel.onEvent(PlayerEvent.ToggleFav) }
                )

                Spacer(Modifier.height(16.dp))

                val pagerState = rememberPagerState(initialPage = PAGE_IMAGE) { 3 }

                Box(modifier = Modifier.size(260.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = true,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            PAGE_VISUALIZER -> VisualizerPage(
                                isPlaying = state.isPlaying,
                                spectrumProvider = viewModel.spectrumProvider,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.large)
                            )
                            PAGE_IMAGE -> AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(state.currentSong?.artworkModel(context))
                                    .memoryCacheKey("art_${state.currentSong?.id}_v${state.artworkVersion}")
                                    .build(),
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.large)
                            )
                            PAGE_ICON -> StillIconPage(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.large)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        val selected = pagerState.currentPage == i
                        Box(
                            modifier = Modifier
                                .size(if (selected) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                        )
                    }
                }
                Text(
                    text = pageLabels[pagerState.currentPage],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = state.currentSong?.title ?: "No song selected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.currentSong?.artist ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                state.currentSong?.let { FormatBadgeRow(it) }
                state.unsupportedFormatMessage?.let { msg ->
                    Spacer(Modifier.height(4.dp))
                    UnsupportedFormatBanner(msg)
                }

                Spacer(Modifier.height(20.dp))

                PlayerSliderRow(
                    progress = state.progress,
                    positionMs = state.positionMs,
                    duration = state.currentSong?.duration,
                    onSeek = { progress ->
                        val duration = state.currentSong?.duration ?: 0L
                        viewModel.onEvent(PlayerEvent.Seek((progress * duration).toLong()))
                    }
                )

                Spacer(Modifier.height(20.dp))

                PlayerControls(
                    isPlaying = state.isPlaying,
                    shuffleEnabled = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    onTogglePlayPause = { viewModel.onEvent(PlayerEvent.TogglePlayPause) },
                    onSkipNext = { viewModel.onEvent(PlayerEvent.SkipNext) },
                    onSkipPrevious = { viewModel.onEvent(PlayerEvent.SkipPrevious) },
                    onToggleShuffle = { viewModel.onEvent(PlayerEvent.ToggleShuffle) },
                    onCycleRepeat = { viewModel.onEvent(PlayerEvent.CycleRepeat) }
                )

                Spacer(Modifier.height(4.dp))
                PlayerSecondaryRow(
                    onOpenQueue = { showQueueSheet = true },
                    onOpenJam = onNavigateToJam
                )
            }
        }

        // Gesture action feedback overlay
        AnimatedVisibility(
            visible = gestureFeedback != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(350)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
                tonalElevation = 6.dp
            ) {
                Text(
                    text = gestureFeedback ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ─── Shared sub-composables ──────────────────────────────────────────────────

@Composable
private fun PlayerTopRow(
    isFavorite: Boolean,
    showLyrics: Boolean,
    isCastActive: Boolean,
    onEqualizer: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleFav: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEqualizer) {
            Icon(
                Icons.Default.Equalizer,
                contentDescription = "Equalizer",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        FilledTonalButton(
            onClick = onToggleLyrics,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (showLyrics) "Cover" else "Lyrics",
                style = MaterialTheme.typography.labelMedium
            )
        }
        CastRouteButton(isCastActive = isCastActive)
        IconButton(onClick = onToggleFav) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Toggle Favourite",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CastRouteButton(isCastActive: Boolean) {
    AndroidView(
        factory = { ctx ->
            runCatching {
                MediaRouteButton(ctx).also { btn ->
                    runCatching { CastButtonFactory.setUpMediaRouteButton(ctx, btn) }
                }
            }.getOrElse { android.view.View(ctx) }
        },
        update = { },
        modifier = Modifier.size(48.dp)
    )
}

@Composable
private fun PlayerSliderRow(
    progress: Float,
    positionMs: Long,
    duration: Long?,
    onSeek: (Float) -> Unit
) {
    Slider(
        value = progress,
        onValueChange = onSeek,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = positionMs.formatDuration(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = duration?.formatDuration() ?: "--:--",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onSkipPrevious) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(36.dp)
            )
        }
        FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(64.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(onClick = onSkipNext) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PlayerSecondaryRow(onOpenQueue: () -> Unit, onOpenJam: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(onClick = onOpenQueue) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue"
            )
        }
        Spacer(Modifier.width(24.dp))
        FilledTonalIconButton(onClick = onOpenJam) {
            Icon(
                Icons.Default.People,
                contentDescription = "Jam session"
            )
        }
    }
}

// ─── Lyrics area ─────────────────────────────────────────────────────────────

@Composable
private fun LyricsArea(
    lyricsState: com.example.musicplayer.presentation.lyrics.LyricsUiState,
    positionMs: Long,
    currentSong: com.example.musicplayer.domain.model.Song?,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (lyricsState.status) {
            LyricsStatus.LOADING -> CircularProgressIndicator()

            LyricsStatus.LOADED -> {
                lyricsState.lyrics?.let { lyrics ->
                    LyricsKaraokeView(
                        lyrics = lyrics,
                        positionMs = positionMs,
                        onEdit = onEdit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            LyricsStatus.NOT_FOUND -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.MusicOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No lyrics found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                        Button(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Lyrics")
                        }
                    }
                }
            }

            LyricsStatus.ERROR -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Couldn't load lyrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                        Button(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Lyrics")
                        }
                    }
                }
            }

            LyricsStatus.IDLE -> {}
        }
    }
}

@Composable
private fun LyricsKaraokeView(
    lyrics: Lyrics,
    positionMs: Long,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = lyrics.lines
    val listState = rememberLazyListState()

    val currentIndex = remember(positionMs, lines) {
        if (lines.isEmpty()) 0
        else lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }

    LaunchedEffect(currentIndex) {
        val viewport = listState.layoutInfo.viewportSize.height
        val offset = if (viewport > 0) -(viewport / 3) else 0
        listState.animateScrollToItem(currentIndex, scrollOffset = offset)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 100.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics.lines, key = { i, _ -> i }) { index, line ->
            LyricsLineItem(
                line = line,
                isCurrent = index == currentIndex,
                isPast = index < currentIndex,
                isSynced = lyrics.isSynced
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (lyrics.isManual) "Edit Lyrics" else "Edit / Override Lyrics",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun LyricsLineItem(
    line: LyricsLine,
    isCurrent: Boolean,
    isPast: Boolean,
    isSynced: Boolean
) {
    val targetColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    }
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "lyric_color"
    )

    Text(
        text = line.text,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = if (isCurrent) 20.sp else 16.sp,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = if (isCurrent) 10.dp else 6.dp
            )
    )
}

// ─── Edit dialog ─────────────────────────────────────────────────────────────

@Composable
private fun LyricsEditDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lyrics, contentDescription = null) },
        title = { Text("Add / Edit Lyrics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste plain text or LRC-format lyrics (e.g. [01:23.45] Line).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    placeholder = { Text("[00:12.34] First line\n[00:15.00] Second line\n...") },
                    maxLines = 20
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSave(text) },
                enabled = text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Format badge & unsupported banner ───────────────────────────────────────

@Composable
private fun FormatBadgeRow(song: com.example.musicplayer.domain.model.Song) {
    val fmt = song.audioFormat
    if (fmt == AudioFormat.MP3 || fmt == AudioFormat.AAC ||
        fmt == AudioFormat.M4A || fmt == AudioFormat.UNKNOWN) return

    val containerColor = when {
        !fmt.isPlayable -> MaterialTheme.colorScheme.errorContainer
        fmt.isHighFidelity -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        !fmt.isPlayable -> MaterialTheme.colorScheme.onErrorContainer
        fmt.isHighFidelity -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Spacer(Modifier.height(4.dp))
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = if (fmt.isHighFidelity && fmt.isPlayable) "${fmt.label}  •  Lossless"
                   else if (!fmt.isPlayable) "${fmt.label}  •  Not supported"
                   else fmt.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun UnsupportedFormatBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = androidx.compose.ui.Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// ─── Screen wake lock ────────────────────────────────────────────────────────

@Composable
private fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

// ─── Visualizer & icon pages (unchanged) ─────────────────────────────────────

@Composable
private fun VisualizerPage(
    isPlaying: Boolean,
    spectrumProvider: SpectrumProvider,
    modifier: Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surfaceVariant

    if (spectrumProvider.isAvailable) {
        val bands by spectrumProvider.bands.collectAsState()
        androidx.compose.foundation.Canvas(modifier = modifier) {
            drawRect(color = surface)
            drawSpectrum(
                bands = if (isPlaying) bands else FloatArray(SpectrumProvider.BAR_COUNT) { 0.03f },
                primary = primary,
                secondary = secondary
            )
        }
    } else {
        var frameMs by remember { mutableLongStateOf(0L) }
        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                val start = System.currentTimeMillis()
                while (isActive) {
                    frameMs = System.currentTimeMillis() - start
                    kotlinx.coroutines.delay(16L)
                }
            }
        }
        androidx.compose.foundation.Canvas(modifier = modifier) {
            drawRect(color = surface)
            val t = frameMs * 0.003f
            val fallback = FloatArray(SpectrumProvider.BAR_COUNT) { i ->
                val phase = i * (PI.toFloat() * 2f / SpectrumProvider.BAR_COUNT)
                if (isPlaying) {
                    (sin(t + phase) * 0.35f +
                     sin(t * 1.7f + phase * 0.6f) * 0.25f +
                     abs(sin(t * 0.5f + phase * 1.3f)) * 0.2f +
                     0.2f).coerceIn(0.06f, 1f)
                } else {
                    0.03f + abs(sin(phase)) * 0.02f
                }
            }
            drawSpectrum(bands = fallback, primary = primary, secondary = secondary)
        }
    }
}

private fun DrawScope.drawSpectrum(
    bands: FloatArray,
    primary: androidx.compose.ui.graphics.Color,
    secondary: androidx.compose.ui.graphics.Color
) {
    val count = bands.size
    val barWidth = size.width / (count * 1.75f)
    val gap = barWidth * 0.75f
    val totalWidth = count * (barWidth + gap) - gap
    val startX = (size.width - totalWidth) / 2f

    bands.forEachIndexed { i, height ->
        val h = height.coerceIn(0.02f, 1f)
        val barH = size.height * 0.88f * h
        val x = startX + i * (barWidth + gap)
        val y = size.height - barH
        val barColor = androidx.compose.ui.graphics.lerp(secondary, primary, h)
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, y),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(barWidth / 2f)
        )
    }
}

@Composable
private fun StillIconPage(modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier.background(container),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}
