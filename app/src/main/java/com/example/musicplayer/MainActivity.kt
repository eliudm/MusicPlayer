package com.example.musicplayer

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musicplayer.data.preferences.ThemeMode
import com.example.musicplayer.presentation.artwork.ArtworkScreen
import com.example.musicplayer.presentation.artwork.ArtworkWebScreen
import com.example.musicplayer.presentation.dashboard.DashboardScreen
import com.example.musicplayer.presentation.drawer.AppDrawer
import com.example.musicplayer.presentation.drawer.DrawerViewModel
import com.example.musicplayer.presentation.equalizer.EqualizerScreen
import com.example.musicplayer.presentation.folder.FolderSongsScreen
import com.example.musicplayer.presentation.help.HelpScreen
import com.example.musicplayer.presentation.library.LibraryScreen
import com.example.musicplayer.presentation.player.MiniPlayer
import com.example.musicplayer.presentation.player.MiniPlayerViewModel
import com.example.musicplayer.presentation.player.PlayerScreen
import com.example.musicplayer.presentation.playlist.PlaylistScreen
import com.example.musicplayer.presentation.search.SearchScreen
import com.example.musicplayer.presentation.jam.JamScreen
import com.example.musicplayer.presentation.settings.GestureSettingsScreen
import com.example.musicplayer.presentation.settings.ScanFilterScreen
import com.example.musicplayer.presentation.settings.SettingsScreen
import com.example.musicplayer.presentation.settings.backup.BackupScreen
import com.example.musicplayer.presentation.themes.SkinThemeScreen
import com.example.musicplayer.presentation.themes.ThemeStoreScreen
import com.example.musicplayer.presentation.trim.TrimScreen
import com.example.musicplayer.presentation.widgets.WidgetsScreen
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.example.musicplayer.ui.theme.allColorThemes
import com.example.musicplayer.ui.theme.allSkins
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* library screen handles media result via its own LaunchedEffect */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()
        setContent {
            val drawerViewModel: DrawerViewModel = hiltViewModel()
            val themeMode by drawerViewModel.themeMode.collectAsState()
            val colorThemeId by drawerViewModel.colorThemeId.collectAsState()
            val skinId by drawerViewModel.skinId.collectAsState()
            MusicPlayerTheme(themeMode = themeMode, colorThemeId = colorThemeId, skinId = skinId) {
                MusicPlayerMainScreen(drawerViewModel = drawerViewModel)
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = buildList {
            add(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    android.Manifest.permission.READ_MEDIA_AUDIO
                else
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            add(android.Manifest.permission.RECORD_AUDIO)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicPlayerMainScreen(
    drawerViewModel: DrawerViewModel,
    miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val miniPlayerState by miniPlayerViewModel.state.collectAsState()
    val themeMode by drawerViewModel.themeMode.collectAsState()
    val colorThemeId by drawerViewModel.colorThemeId.collectAsState()
    val skinId by drawerViewModel.skinId.collectAsState()
    val pauseOnDetach by drawerViewModel.pauseOnDetach.collectAsState()
    val scanMessage by drawerViewModel.scanMessage.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scanMessage) {
        if (scanMessage != null) {
            snackbarHostState.showSnackbar(scanMessage!!)
            drawerViewModel.clearScanMessage()
        }
    }

    val navItems = listOf(
        Triple("dashboard", "Home", Icons.Default.Home),
        Triple("playlists", "Playlists", Icons.Default.PlaylistPlay),
        Triple("search", "Search", Icons.Default.Search)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showNav = navItems.any { (route, _, _) ->
        currentDestination?.hierarchy?.any { it.route == route } == true
    }
    val onPlayerScreen = currentRoute == "player"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                themeMode = themeMode,
                pauseOnDetach = pauseOnDetach,
                colorThemeName = allColorThemes.find { it.id == colorThemeId }
                    ?.let { "${it.emoji} ${it.name}" } ?: "💜 Purple Haze",
                skinName = allSkins.find { it.id == skinId }
                    ?.let { "${it.emoji}  ${it.name}" } ?: "✦  Modern",
                onThemeSelected = { drawerViewModel.setThemeMode(it) },
                onTogglePauseOnDetach = { drawerViewModel.togglePauseOnDetach() },
                onScanMusic = { drawerViewModel.scanMusic() },
                onNavigateToSettings = { navController.navigate("settings") { launchSingleTop = true } },
                onNavigateToHelp = { navController.navigate("help") { launchSingleTop = true } },
                onNavigateToThemeStore = { navController.navigate("themes") { launchSingleTop = true } },
                onNavigateToSkinStore = { navController.navigate("skins") { launchSingleTop = true } },
                onNavigateToWidgets = { navController.navigate("widgets") { launchSingleTop = true } },
                onNavigateToBackup = { navController.navigate("backup") { launchSingleTop = true } },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
            ) {
                if (showNav) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                            label = { Text("Menu") },
                            selected = false,
                            onClick = { scope.launch { drawerState.open() } }
                        )
                        navItems.forEach { (route, label, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                                onClick = {
                                    if (route == "dashboard") {
                                        navController.popBackStack("dashboard", inclusive = false)
                                    } else {
                                        navController.navigate(route) {
                                            popUpTo("dashboard") { saveState = true; inclusive = false }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            onSongClick = { navController.navigate("player") { launchSingleTop = true } },
                            onSeeAllLibrary = { navController.navigate("library") },
                            onSeeAllFolders = { navController.navigate("library") },
                            onFolderClick = { folder -> navController.navigate("folder/${folder.id}") }
                        )
                    }
                    composable(
                        route = "folder/{bucketId}",
                        arguments = listOf(navArgument("bucketId") { type = NavType.LongType })
                    ) {
                        FolderSongsScreen(
                            onSongClick = { navController.navigate("player") { launchSingleTop = true } },
                            onNavigateBack = { navController.popBackStack() },
                            onTrimSong = { song -> navController.navigate("trim/${song.id}") },
                            onArtworkSong = { song -> navController.navigate("artwork/${song.id}") }
                        )
                    }
                    composable("library") {
                        LibraryScreen(
                            onSongClick = { navController.navigate("player") { launchSingleTop = true } },
                            onTrimSong = { song -> navController.navigate("trim/${song.id}") },
                            onArtworkSong = { song -> navController.navigate("artwork/${song.id}") }
                        )
                    }
                    composable("player") {
                        PlayerScreen(
                            onNavigateToEqualizer = { navController.navigate("equalizer") },
                            onNavigateToJam = { navController.navigate("jam") { launchSingleTop = true } }
                        )
                    }
                    composable("playlists") {
                        PlaylistScreen(onPlaylistClick = { })
                    }
                    composable("search") {
                        SearchScreen(onSongClick = { navController.navigate("player") { launchSingleTop = true } })
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToEqualizer = { navController.navigate("equalizer") },
                            onNavigateToScanFilters = { navController.navigate("scan_filters") },
                            onNavigateToBackup = { navController.navigate("backup") },
                            onNavigateToGestureSettings = { navController.navigate("gesture_settings") }
                        )
                    }
                    composable("gesture_settings") {
                        GestureSettingsScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable("backup") {
                        BackupScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable("scan_filters") {
                        ScanFilterScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable("jam") {
                        JamScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable("equalizer") {
                        EqualizerScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable("help") {
                        HelpScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable("themes") {
                        ThemeStoreScreen(
                            currentThemeId = colorThemeId,
                            onThemeApplied = { drawerViewModel.setColorThemeId(it) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("skins") {
                        SkinThemeScreen(
                            currentSkinId = skinId,
                            onSkinApplied = { drawerViewModel.setSkinId(it) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "trim/{songId}",
                        arguments = listOf(navArgument("songId") { type = NavType.LongType })
                    ) {
                        TrimScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "artwork/{songId}",
                        arguments = listOf(navArgument("songId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val songId = backStackEntry.arguments?.getLong("songId") ?: -1L
                        ArtworkScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onPickFromInternet = { navController.navigate("artwork_web/$songId") }
                        )
                    }
                    composable("widgets") {
                        WidgetsScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "artwork_web/{songId}",
                        arguments = listOf(navArgument("songId") { type = NavType.LongType })
                    ) {
                        ArtworkWebScreen(onNavigateBack = { navController.popBackStack() })
                    }
                }

                if (showNav && !onPlayerScreen) {
                    miniPlayerState.currentSong?.let { song ->
                        MiniPlayer(
                            currentSong = song,
                            isPlaying = miniPlayerState.isPlaying,
                            onTogglePlayPause = { miniPlayerViewModel.togglePlayPause() },
                            onClick = { navController.navigate("player") { launchSingleTop = true } }
                        )
                    }
                }
            }
        }
    }
}
