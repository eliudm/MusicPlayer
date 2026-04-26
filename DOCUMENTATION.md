# MusicPlayer — Comprehensive Project Documentation

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Tech Stack & Dependencies](#3-tech-stack--dependencies)
4. [Permissions](#4-permissions)
5. [Project Structure](#5-project-structure)
6. [Features](#6-features)
   - 6.1 [Playback Engine](#61-playback-engine)
   - 6.2 [Dashboard](#62-dashboard)
   - 6.3 [Library](#63-library)
   - 6.4 [Queue & Auto-DJ](#64-queue--auto-dj)
   - 6.5 [Jam Session](#65-jam-session-collaborative-queue)
   - 6.6 [Lyrics](#66-lyrics)
   - 6.7 [Equalizer & Audio Effects](#67-equalizer--audio-effects)
   - 6.8 [Artwork Management](#68-artwork-management)
   - 6.9 [Audio Trimmer](#69-audio-trimmer)
   - 6.10 [Playlists](#610-playlists)
   - 6.11 [Search](#611-search)
   - 6.12 [Themes & Skins](#612-themes--skins)
   - 6.13 [Gestures](#613-gestures)
   - 6.14 [App Widgets](#614-app-widgets)
   - 6.15 [Android Auto](#615-android-auto)
   - 6.16 [Chromecast](#616-chromecast)
   - 6.17 [Backup & Restore](#617-backup--restore)
   - 6.18 [Scan Filters](#618-scan-filters)
7. [Domain Layer](#7-domain-layer)
   - 7.1 [Models](#71-models)
   - 7.2 [Repository Interfaces](#72-repository-interfaces)
   - 7.3 [Use Cases](#73-use-cases)
8. [Data Layer](#8-data-layer)
   - 8.1 [MediaStore Source](#81-mediastore-source)
   - 8.2 [Room Database](#82-room-database)
   - 8.3 [AppPreferences (DataStore)](#83-apppreferences-datastore)
   - 8.4 [MusicRepositoryImpl](#84-musicrepositoryimpl)
   - 8.5 [LyricsRepositoryImpl](#85-lyricsrepositoryimpl)
   - 8.6 [BackupRepositoryImpl](#86-backuprepositoryimpl)
9. [Service Layer](#9-service-layer)
   - 9.1 [MusicService](#91-musicservice)
   - 9.2 [PlayerController](#92-playercontroller)
   - 9.3 [EqualizerManager](#93-equalizermanager)
   - 9.4 [JamSessionManager](#94-jamsessionmanager)
   - 9.5 [SpectrumProvider](#95-spectrumprovider)
10. [Presentation Layer](#10-presentation-layer)
    - 10.1 [Navigation](#101-navigation)
    - 10.2 [PlayerScreen & PlayerViewModel](#102-playerscreen--playerviewmodel)
    - 10.3 [DashboardScreen](#103-dashboardscreen)
    - 10.4 [LibraryScreen](#104-libraryscreen)
    - 10.5 [QueueScreen](#105-queuescreen)
    - 10.6 [JamScreen](#106-jamscreen)
    - 10.7 [EqualizerScreen](#107-equalizerscreen)
    - 10.8 [Settings Screens](#108-settings-screens)
    - 10.9 [Drawer & Theme System](#109-drawer--theme-system)
11. [Dependency Injection](#11-dependency-injection)
12. [Widget System](#12-widget-system)
13. [Build Configuration](#13-build-configuration)
14. [Known Considerations](#14-known-considerations)

---

## 1. Project Overview

**MusicPlayer** is a full-featured, production-grade Android music player built entirely with Jetpack Compose and the Media3 framework. It targets Android 8.0 (API 26) through Android 15 (API 35) and is designed around clean architecture principles with a clear separation between domain, data, and presentation layers.

### Core Capabilities at a Glance

| Category | Features |
|---|---|
| Playback | ExoPlayer, crossfade, ReplayGain, gapless, shuffle, repeat |
| Library | Songs, Albums, Artists, Genres, Folders, Playlists |
| Audio | 10-band equalizer, bass boost, virtualizer, reverb, loudness enhancer |
| Collaboration | Local-network Jam Session (Spotify Jam–style) |
| Smart Queue | Auto-DJ with artist/album/random song suggestions |
| Lyrics | Synced LRC karaoke view, plain text, manual editing |
| Artwork | Local gallery, iTunes API auto-fetch, online search |
| Editing | Non-destructive audio trimmer (M4A/AAC output) |
| Themes | 8+ color palettes, 3 UI skins, light/dark/system modes |
| Integration | Android Auto, Chromecast, Bluetooth AVRCP, home screen widgets |
| Backup | Google Drive backup/restore (playlists, favorites, history) |
| Gestures | Configurable swipe and pinch gestures on the player screen |

---

## 2. Architecture

The project follows **Clean Architecture** with three clearly separated layers, combined with the **MVVM** pattern in the presentation layer.

```
┌─────────────────────────────────────────────────────┐
│                 Presentation Layer                   │
│  Compose Screens  ◄──►  ViewModels  ◄──►  UI State  │
└────────────────────────┬────────────────────────────┘
                         │  Use Cases
┌────────────────────────▼────────────────────────────┐
│                   Domain Layer                       │
│   Models  │  Repository Interfaces  │  Use Cases     │
└────────────────────────┬────────────────────────────┘
                         │  Repository Implementations
┌────────────────────────▼────────────────────────────┐
│                    Data Layer                        │
│  MediaStore  │  Room DB  │  DataStore  │  Network    │
└─────────────────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                  Service Layer                       │
│  MusicService  │  PlayerController  │  Equalizer     │
│  JamSessionManager  │  SpectrumProvider              │
└─────────────────────────────────────────────────────┘
```

### Key Design Decisions

- **Unidirectional Data Flow**: ViewModels expose `StateFlow<UiState>` and `SharedFlow<Event>`; screens observe and dispatch sealed-class events.
- **Single Activity**: All navigation is handled inside `MainActivity` via Jetpack Navigation Compose.
- **Application-scoped coroutines**: A `@ApplicationScope CoroutineScope` (Dispatchers.Default + SupervisorJob) is injected for long-lived operations that must survive ViewModel teardown.
- **Hilt DI**: All dependencies are wired through Hilt. Singletons (ExoPlayer, repository, equalizer) are created once at the application level.
- **StateFlow everywhere**: Reactive state propagation between layers uses Kotlin `StateFlow` and `SharedFlow` exclusively; no LiveData.

---

## 3. Tech Stack & Dependencies

### Core Framework

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.04.01 | UI framework |
| Compose Material 3 | BOM-managed | Design system, components |
| Compose Navigation | 2.7.7 | In-app navigation |
| Hilt | 2.56 | Dependency injection |
| Media3 ExoPlayer | 1.3.1 | Audio playback engine |
| Media3 Session | 1.3.1 | MediaSession / MediaLibraryService |
| Media3 Cast | 1.3.1 | Chromecast integration |
| Room | 2.7.0-alpha11 | Local SQLite database |
| DataStore Preferences | 1.1.1 | Key-value preference storage |
| Glance (App Widgets) | 1.1.0 | Home screen widgets |

### Supporting Libraries

| Library | Purpose |
|---|---|
| Coil Compose | Asynchronous image loading / artwork display |
| Play Services Cast Framework | Chromecast SDK |
| Play Services Auth | Google Drive authentication |
| Gson | JSON serialization for backup |
| KotlinX Coroutines Android | Coroutine support |

### Testing

| Library | Purpose |
|---|---|
| JUnit 4 | Unit testing |
| MockK | Kotlin-first mocking |
| Turbine | Flow testing |
| KotlinX Coroutines Test | Coroutine testing utilities |
| Espresso | UI testing |
| Compose UI Test | Compose testing |

---

## 4. Permissions

| Permission | When Used |
|---|---|
| `INTERNET` | Lyrics fetch, artwork fetch, Chromecast, backup |
| `READ_MEDIA_AUDIO` | Read audio files (API 33+) |
| `READ_EXTERNAL_STORAGE` | Read audio files (API 26–32) |
| `RECORD_AUDIO` | Audio visualizer (AudioRecord session) |
| `FOREGROUND_SERVICE` | Keep playback alive in background |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Classified foreground service type |
| `WAKE_LOCK` | Prevent CPU sleep during playback |
| `POST_NOTIFICATIONS` | Show playback notification (API 33+) |
| `WRITE_SETTINGS` | Optional system volume control |
| `BLUETOOTH_CONNECT` | AVRCP metadata on API 31+ |
| `CHANGE_WIFI_MULTICAST_STATE` | NSD multicast for Jam Session discovery |

Permissions `READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`, and `RECORD_AUDIO` are requested at runtime in `MainActivity.onCreate`.

---

## 5. Project Structure

```
app/src/main/java/com/example/musicplayer/
│
├── MusicPlayerApp.kt              # Application class (Hilt + Coil setup)
├── MainActivity.kt                # Single activity, navigation host
│
├── di/
│   ├── AppScope.kt                # @ApplicationScope qualifier
│   └── MusicModule.kt             # Hilt module: DB, ExoPlayer, Repository, etc.
│
├── domain/
│   ├── model/                     # Pure Kotlin data models
│   │   ├── Song.kt
│   │   ├── Album.kt
│   │   ├── Playlist.kt
│   │   ├── Folder.kt
│   │   ├── Lyrics.kt / LyricsLine.kt
│   │   ├── AudioFormat.kt
│   │   ├── GestureAction.kt
│   │   ├── RepeatMode.kt
│   │   ├── BackupData.kt
│   │   └── DriveBackupFile.kt
│   ├── repository/                # Repository interfaces (contracts)
│   │   ├── MusicRepository.kt
│   │   ├── LyricsRepository.kt
│   │   └── BackupRepository.kt
│   └── usecase/                   # Single-responsibility use cases
│       ├── PlaySongUseCase.kt
│       ├── PauseSongUseCase.kt
│       ├── SeekUseCase.kt
│       ├── GetLibraryUseCase.kt
│       ├── GetQueueUseCase.kt
│       ├── CreatePlaylistUseCase.kt
│       └── AddToPlaylistUseCase.kt
│
├── data/
│   ├── mediastore/
│   │   └── MediaStoreSource.kt    # Android MediaStore queries
│   ├── local/
│   │   ├── MusicDatabase.kt       # Room database definition
│   │   ├── dao/                   # DAO interfaces
│   │   └── entity/                # Room entities
│   ├── preferences/
│   │   └── AppPreferences.kt      # DataStore preferences
│   ├── repository/
│   │   └── MusicRepositoryImpl.kt # Main repository implementation
│   ├── lyrics/
│   │   ├── LrcParser.kt           # LRC format parser/generator
│   │   └── LyricsRepositoryImpl.kt
│   └── backup/
│       └── BackupRepositoryImpl.kt # Google Drive backup
│
├── service/
│   ├── MusicService.kt            # MediaLibraryService (Auto/BT/Cast)
│   ├── PlayerController.kt        # ExoPlayer + CastPlayer wrapper
│   ├── EqualizerManager.kt        # 10-band EQ + audio effects
│   ├── JamSessionManager.kt       # NSD + HTTP p2p session
│   ├── CastOptionsProvider.kt     # Chromecast configuration
│   └── SpectrumProvider.kt        # Audio visualizer data
│
├── widget/
│   ├── SmallMusicWidget.kt        # 2×1 Glance widget
│   ├── StandardMusicWidget.kt     # 4×1 Glance widget
│   ├── WidgetUpdater.kt           # Reactive widget refresh
│   ├── WidgetActions.kt           # Widget button actions
│   ├── WidgetKeys.kt              # Glance preference keys
│   └── WidgetEntryPoint.kt        # Hilt entry point for widgets
│
├── util/
│   ├── Extensions.kt              # formatDuration()
│   └── ArtworkUtils.kt            # Artwork directory helpers
│
├── ui/theme/
│   ├── Theme.kt                   # MusicPlayerTheme composable
│   ├── AppColorTheme.kt           # 8+ color palette definitions
│   ├── SkinConfig.kt              # Skin layout/style definitions
│   └── LocalSkin.kt               # CompositionLocal for skin access
│
└── presentation/
    ├── common/                    # Reusable UI components
    │   ├── SongListItem.kt
    │   ├── AddToPlaylistDialog.kt
    │   ├── AddToPlaylistViewModel.kt
    │   └── SetRingtoneDialog.kt
    ├── player/                    # Full-screen player
    ├── dashboard/                 # Home screen
    ├── library/                   # Music library browser
    ├── playlist/                  # Playlist management
    ├── folder/                    # Folder browsing
    ├── search/                    # Search
    ├── queue/                     # Queue panel
    ├── jam/                       # Jam Session
    ├── lyrics/                    # Lyrics (embedded in player)
    ├── equalizer/                 # EQ screen
    ├── artwork/                   # Artwork picker
    ├── trim/                      # Audio trimmer
    ├── settings/                  # Settings & filters
    │   └── backup/                # Backup screen
    ├── themes/                    # Theme & skin store screens
    ├── drawer/                    # Navigation drawer
    ├── widgets/                   # Widget preview screen
    └── help/                      # Help screen
```

---

## 6. Features

### 6.1 Playback Engine

The playback engine is built on **Media3 ExoPlayer** wrapped by `PlayerController` and managed through `MusicService`.

**Capabilities:**
- Gapless playback across queue items
- Shuffle mode with `player.shuffleModeEnabled`
- Three repeat modes: Off, Repeat All, Repeat One (`Player.REPEAT_MODE_*`)
- **Crossfade**: configurable 1–10 second fade between tracks; implemented via volume ramping in `PlayerViewModel` using a coroutine timer
- **ReplayGain**: reads `METADATA_KEY_REPLAYGAIN_TRACK_GAIN` via `MediaMetadataRetriever` (API 35+) and applies gain through `EqualizerManager`
- **Format support detection**: `AudioFormat.fromMimeType()` determines whether a file is playable; unsupported formats (e.g., DSD) show a banner rather than silently failing

**Playback Flow:**
```
User taps song
    → ViewModel calls repository.play() or repository.playWithQueue()
    → startServiceIfNeeded() starts MusicService as foreground
    → withContext(Dispatchers.Main): player.setMediaItem(s) → prepare() → play()
    → onMediaItemTransition fires → _currentSong updated
    → onTimelineChanged fires → _queue updated
```

**Audio Session:** `MusicService` binds ExoPlayer's audio session ID to `SpectrumProvider` for the live visualizer.

---

### 6.2 Dashboard

`DashboardScreen` is the home screen, displaying curated song rows:

| Section | Source | Limit |
|---|---|---|
| Library | All songs | 10 |
| Folders | Folder list | 10 |
| Favorites | Marked favorites | 10 |
| Recently Played | Play history (sorted by last played) | 5 |
| Recently Added | MediaStore dateAdded desc | 20 |
| Most Played | Play history (sorted by play count) | 20 |

Tapping a song in any row calls `DashboardViewModel.play(song)` which triggers `PlaySongUseCase` and then navigates to `PlayerScreen`.

---

### 6.3 Library

`LibraryScreen` provides a comprehensive, filterable view of the entire music collection.

**Categories** (tab-switched):
- Songs, Artists, Albums, Genres, Playlists, Folders

**Sorting options** (per category):
- Title A–Z / Z–A
- Artist A–Z
- Date Added (newest first)
- Duration (longest first)

**Search**: real-time search with 300 ms debounce, matching against title, artist, and album fields.

**Per-song context menu actions:**
- Play Next (inserts immediately after current track)
- Add to Queue (appends to end)
- Add to Playlist (dialog with existing playlists)
- Edit Artwork
- Trim Audio
- Delete (removes from MediaStore)

---

### 6.4 Queue & Auto-DJ

#### Queue Panel

Accessible from the player screen via the Queue button. Rendered as a `ModalBottomSheet`.

**Features:**
- Shows all queued songs in play order
- Highlights the currently playing track
- **Drag-to-reorder**: long-press the drag handle and drag to swap positions (`detectDragGesturesAfterLongPress`; calls `player.moveMediaItem()`)
- **Remove**: tap the × button to remove a song (`player.removeMediaItem()`)
- **Clear All**: empties the entire queue

The queue state is driven by a `MutableStateFlow<List<Song>>` in `MusicRepositoryImpl`, updated every time ExoPlayer's `onTimelineChanged` fires.

#### Auto-DJ

Toggled via a switch in the Queue panel. When enabled, `checkAndRefillQueue()` runs on every `onTimelineChanged` event.

**Refill logic** (triggered when fewer than 3 songs remain after the current):
1. Add songs by the same **artist** (not already in queue)
2. Add songs from the same **album** (not already in queue)
3. Fill remainder from the full library (random, not already in queue)
4. Take up to 5 candidates, shuffled, and append via `player.addMediaItem()`

The setting is persisted to DataStore via `AppPreferences.autoDjEnabled`.

---

### 6.5 Jam Session (Collaborative Queue)

Jam Session allows multiple devices on the same Wi-Fi network to collaboratively add songs to a shared queue — similar to Spotify's Group Session feature.

#### Architecture

```
Host device                          Client devices
──────────────────────────────────   ─────────────────────────────
JamSessionManager                    JamSessionManager
  startHosting(name, songs)            startDiscovery()
  → ServerSocket(random port)          → NsdManager.discoverServices()
  → NsdManager.registerService()       → resolveService() → JamSession(ip, port)
  → accept() loop (daemon thread)      
  → handleRequest(socket)              fetchHostSongs(session)
    GET /songs → JSON array              → httpGet /songs
    GET /add?id=X → emit songAddEvents   → display browsable list
                                       addSongToHost(session, id)
                                         → httpGet /add?id=X
```

**NSD Service Type:** `_musicjam._tcp.`

**HTTP Protocol** (raw socket, no library):
- `GET /songs` → JSON array of `{id, title, artist, album}`
- `GET /add?id={songId}` → `{"success": true}` or `{"error": "bad id"}`

**Host side:** when a `/add` request is received, `songAddEvents: SharedFlow<Long>` emits the song ID. `JamViewModel` listens to this flow and calls `repository.enqueue(song)` to add it to the local ExoPlayer queue.

**Client side:** browses the host's song list and adds songs. Once added, the song card shows a checkmark.

---

### 6.6 Lyrics

`LyricsViewModel` manages the lifecycle of lyrics for the currently playing song.

**Fetch sources** (in priority order):
1. **Local Room DB cache** — retrieved by song ID
2. **Genius API** — searches by artist + title
3. **NetEase API** — fallback source
4. **LyricFind** — second fallback

**LRC Format support:**
- `LrcParser` parses timestamped lines: `[mm:ss.xx] lyric text`
- Stores as `List<LyricsLine>` with millisecond timestamps
- Powers the **karaoke-style view**: the current line is highlighted and the list auto-scrolls to keep it centred (`LazyListState.animateScrollToItem`)

**Plain text support**: stored and displayed as-is when no timestamps are available.

**Manual editing**: users can open a full-text editor dialog, type or paste lyrics, and save them. Manually saved lyrics are always preferred over auto-fetched ones.

**Cache invalidation**: "Retry" button clears the DB entry and re-fetches.

---

### 6.7 Equalizer & Audio Effects

`EqualizerManager` wraps Android's `android.media.audiofx` package and is bound to ExoPlayer's audio session ID.

**Controls:**
| Effect | API Class | Range |
|---|---|---|
| 10-band EQ | `Equalizer` | ±15 dB per band |
| Bass Boost | `BassBoost` | 0–1000 strength |
| Virtualizer | `Virtualizer` | 0–1000 strength |
| Reverb | `PresetReverb` | 0–6 preset |
| Loudness Enhancer | `LoudnessEnhancer` | 0–1000 mB gain |

**EQ Presets:** Normal, Classical, Dance, Flat, Folk, Heavy Metal, Hip Hop, Jazz, Pop, Rock — applied by writing all 10 bands at once.

**Persistence:** all EQ settings are saved to `SharedPreferences` immediately on change, and reloaded when `EqualizerManager` is re-bound (e.g., after service restart).

**ReplayGain integration:** if a track has a ReplayGain tag, `applyReplayGain(gainDb)` adjusts the `LoudnessEnhancer` target gain accordingly.

---

### 6.8 Artwork Management

Two paths for artwork:

**1. Auto-fetch (background)**

`PlayerViewModel.autoFetchCoverArt()` runs automatically on every track change:
1. Checks for a manually set custom artwork file at `{filesDir}/artwork/{id}.jpg`
2. Checks for a previously auto-fetched file at `{filesDir}/auto_artwork/{id}.jpg`
3. If neither exists, queries the iTunes Search API: `https://itunes.apple.com/search?term=...&media=music&entity=album&limit=3`
4. Downloads and saves the 600×600 artwork image
5. Bumps `artworkVersion` in UI state to invalidate Coil's memory cache

**2. Manual selection (ArtworkScreen)**

- **Local gallery**: standard photo picker intent
- **Online search**: `ArtworkWebScreen` renders an in-app web search; user selects an image URL
- Selected image is saved to the custom artwork directory and takes permanent priority

---

### 6.9 Audio Trimmer

`TrimScreen` / `TrimViewModel` provides non-destructive audio segment extraction.

**Process:**
1. User selects start/end trim points via a range slider (millisecond precision)
2. A preview plays the selected range in a loop
3. On confirm, `TrimViewModel` runs on `Dispatchers.IO`:
   - `MediaExtractor` reads the source file
   - `MediaMuxer` writes only the selected segment to a new `.m4a` file
   - Output is saved to the public Music directory
   - `MediaStore` is notified via `ContentResolver.insert()` so the new file appears in the library
4. The original file is not modified

**Supported input formats:** any format decodable by `MediaExtractor` (MP3, AAC, FLAC, OGG, etc.)  
**Output format:** AAC in M4A container

---

### 6.10 Playlists

`PlaylistViewModel` / `PlaylistScreen` manage user-created playlists backed by Room DB.

**Operations:**
- Create playlist (name dialog)
- Add song to playlist (from Library/Folder context menu → `AddToPlaylistDialog`)
- Remove song from playlist
- Delete playlist
- Display playlist with song count and total duration

Playlist data is persisted across app restarts via Room's `PlaylistEntity` and `PlaylistSongEntity` tables with a foreign-key relationship.

---

### 6.11 Search

`SearchScreen` performs real-time full-text search against the in-memory song list.

**Match fields:** title, artist, album (case-insensitive `contains`)  
**Architecture:** `LibraryViewModel` exposes a search query `StateFlow`; `searchSongs()` in the repository uses `cachedSongs.map { songs -> songs.filter { ... } }` so no DB round-trip is needed.

---

### 6.12 Themes & Skins

The app has a two-axis visual customization system.

#### Color Themes

8+ built-in palettes, each defining a complete Material3 color scheme:

| ID | Name |
|---|---|
| `purple_haze` | 💜 Purple Haze (default) |
| `ocean_blue` | 🌊 Ocean Blue |
| `forest_green` | 🌿 Forest Green |
| `sunset_orange` | 🌅 Sunset Orange |
| `rose_gold` | 🌸 Rose Gold |
| `midnight_black` | 🖤 Midnight Black |
| `arctic_white` | 🤍 Arctic White |
| `cherry_red` | ❤️ Cherry Red |

Selected via `ThemeStoreScreen`. Persisted to DataStore (`color_theme` key).

#### UI Skins

3 skins that change the player screen layout and background style:

| ID | Name | Player Background |
|---|---|---|
| `modern` | ✦ Modern | Gradient overlay |
| `classic` | 🎵 Classic | Solid surface |
| `artwork` | 🖼 Artwork | Full-bleed artwork |

Selected via `SkinThemeScreen`. Persisted to DataStore (`skin_id` key).

#### Theme Mode

System / Light / Dark — selected from the navigation drawer. Persisted to DataStore (`theme_mode` key).

**Flash prevention:** `DrawerViewModel` reads initial theme values synchronously (`runBlocking { prefs.X.first() }`) so the first rendered frame already has the correct theme, eliminating the "flash of default theme" on startup.

---

### 6.13 Gestures

Configurable gestures on the player screen, managed by `GestureSettings`:

| Gesture | Configurable? | Default Action |
|---|---|---|
| Swipe left/right | Yes | Skip Next / Skip Previous |
| Swipe up | Yes | Toggle Lyrics |
| Swipe down | Yes | Toggle Lyrics |
| Pinch in/out | Yes (toggle) | Volume Down / Volume Up |

Available actions for swipe gestures: Skip Next, Skip Previous, Toggle Play/Pause, Toggle Favorite, Toggle Shuffle, Cycle Repeat, Toggle Lyrics, None.

Configured via `GestureSettingsScreen`. All settings persisted to DataStore.

**Implementation:** `detectDragGestures` on the player's outer Column measures total X/Y displacement. Whichever axis exceeds an 80dp threshold first determines the gesture direction, and the mapped action fires on `onDragEnd`.

---

### 6.14 App Widgets

Two home screen widgets built with **Jetpack Glance**:

| Widget | Size | Controls |
|---|---|---|
| Small Music Widget | 2×1 | Title, artist, play/pause |
| Standard Music Widget | 4×1 | Album art, title, artist, prev, play/pause, next |

`WidgetUpdater` is a singleton that subscribes to `MusicRepository.getCurrentSong()` and `observeIsPlaying()`, then calls `GlanceAppWidgetManager.updateAll()` on every change.

Widget state is stored in `GlanceStateDefinition` preference keys (`WidgetKeys`): current song title, artist, and playing state. Widget button actions are handled by `WidgetActions` which sends Intents back to `MusicService`.

---

### 6.15 Android Auto

`MusicService` extends `MediaLibraryService` and implements `MediaLibrarySession.Callback`, providing a browsable media tree for Android Auto and automotive OS:

**Browse tree:**
```
root
├── Songs          (flat list of all songs)
├── Albums
│   ├── Album A
│   │   ├── Track 1
│   │   └── Track 2
│   └── Album B ...
└── Recently Added (last 30 songs by dateAdded)
```

`onSetMediaItems` and `onAddMediaItems` callbacks resolve bare `mediaId` strings back to full `MediaItem` objects with URIs, so Auto can trigger playback from browse items.

The manifest includes `<intent-filter>` for both `MediaSessionService` and `MediaBrowserService` actions, plus the `automotive_app_desc.xml` meta-data entry.

---

### 6.16 Chromecast

Chromecast support is provided via **Media3 Cast** + **Play Services Cast Framework**.

**Configuration (`CastOptionsProvider`):**
- Receiver app ID: `CC1AD845` (Default Media Receiver)
- Notification options point back to `MainActivity`

**`PlayerController` integration:**
- `CastContext.getSharedInstance()` initializes asynchronously on a background coroutine
- `CastPlayer` is created on success and listens for session availability
- On `onCastSessionAvailable`: current ExoPlayer item + position transferred to CastPlayer, ExoPlayer paused
- On `onCastSessionUnavailable`: reverse transfer back to ExoPlayer
- `activePlayer` computed property always returns the currently active player (local or Cast)

`CastRouteButton` in `PlayerScreen` renders a `MediaRouteButton` inside `AndroidView`. The Activity theme explicitly sets `android:colorBackground` to prevent a `MediaRouterThemeHelper` contrast calculation crash.

---

### 6.17 Backup & Restore

`BackupScreen` / `BackupViewModel` / `BackupRepositoryImpl` provide Google Drive–based backup.

**What is backed up:**
- All playlists and their song references
- All favorite song IDs
- Complete play history (play count + last played timestamp)

**Backup format:** JSON serialized via Gson, uploaded as a single `.json` file to Google Drive app data folder.

**Flow:**
1. User signs in with Google (`GoogleSignInClient`)
2. `BackupRepositoryImpl` uses `Drive.Builder` with the obtained credential
3. `createBackup()`: reads Room DB → serializes → uploads
4. `listBackups()`: lists `.json` files in Drive app folder with timestamps
5. `restoreBackup(fileId)`: downloads → deserializes → clears and re-inserts DB rows → triggers MediaStore rescan
6. `deleteBackup(fileId)`: removes from Drive

---

### 6.18 Scan Filters

`ScanFilterScreen` / `ScanFilterViewModel` configure which files appear in the library.

| Filter | Default | Storage |
|---|---|---|
| Minimum duration | 30 seconds | DataStore `min_duration_sec` |
| Minimum file size | 100 KB | DataStore `min_size_kb` |

`MediaStoreSource.querySongs(minDurMs, minSizeBytes)` applies these as `WHERE` clauses in the MediaStore query. `cachedSongs` in `MusicRepositoryImpl` is a `flatMapLatest` over these preferences, so the library updates reactively when filters change.

---

## 7. Domain Layer

### 7.1 Models

| Model | Key Fields |
|---|---|
| `Song` | id, title, artist, album, albumId, duration, uri, trackNumber, year, genre, size, bucketId, bucketName, dateAdded, mimeType; computed `audioFormat` |
| `Album` | id, name, artist, songCount, albumArtUri |
| `Playlist` | id, name, songs: List\<Song\>, createdAt, modifiedAt |
| `Folder` | id, name, songCount |
| `Lyrics` | songId, lines: List\<LyricsLine\>?, plainText: String?, isSynced |
| `LyricsLine` | timeMs: Long, text: String |
| `AudioFormat` | enum (MP3, FLAC, WAV, AAC, OGG, ALAC, APE, DSD, WMA, UNKNOWN); `isPlayable`, `label` |
| `GestureAction` | enum (NONE, SKIP_NEXT, SKIP_PREVIOUS, TOGGLE_PLAY_PAUSE, TOGGLE_FAVORITE, TOGGLE_SHUFFLE, CYCLE_REPEAT, TOGGLE_LYRICS) |
| `RepeatMode` | enum (OFF, ALL, ONE) |
| `BackupData` | playlists, favorites, playHistory |

### 7.2 Repository Interfaces

**`MusicRepository`** — 20+ methods covering:
- `getSongs() / getAlbums() / getPlaylists() / searchSongs()` — reactive `Flow` queries
- `play() / pause() / seekTo() / playWithQueue()` — playback control
- `playNext() / enqueue()` — queue insertion
- `getQueue() / moveQueueItem() / removeFromQueue() / clearQueue()` — queue management
- `getFolders() / getSongsInFolder()` — folder browsing
- `getRecentlyAdded() / getRecentlyPlayed() / getMostPlayed()` — discovery
- `toggleFavorite() / getFavorites()` — favorites
- `createPlaylist() / addSongToPlaylist() / removeSongFromPlaylist() / deletePlaylist()` — playlists
- `getCurrentSong() / observeIsPlaying()` — current state
- `recordPlay() / deleteSong()` — history and deletion

**`LyricsRepository`** — `getLyrics()`, `saveLyrics()`, `clearLyrics()`

**`BackupRepository`** — `createBackup()`, `listBackups()`, `restoreBackup()`, `deleteBackup()`, `signIn()`, `signOut()`

### 7.3 Use Cases

Each use case is a single-method class following the `operator fun invoke()` convention:

| Use Case | Delegates To |
|---|---|
| `PlaySongUseCase` | `repository.play(song)` |
| `PauseSongUseCase` | `repository.pause()` |
| `SeekUseCase` | `repository.seekTo(positionMs)` |
| `GetLibraryUseCase` | `repository.getSongs()` |
| `GetQueueUseCase` | `repository.getQueue()` |
| `CreatePlaylistUseCase` | `repository.createPlaylist(name)` |
| `AddToPlaylistUseCase` | `repository.addSongToPlaylist(playlistId, songId, position)` |

---

## 8. Data Layer

### 8.1 MediaStore Source

`MediaStoreSource.querySongs(minDurMs, minSizeBytes)` queries `MediaStore.Audio.Media` with:
- `IS_MUSIC = 1`
- `DURATION >= minDurMs`
- `SIZE >= minSizeBytes`

Returns a `Flow<List<Song>>` that re-emits on content changes via `ContentObserver`. Mapped columns include all Song fields plus `MIME_TYPE` for format detection. Results are deduplicated by song ID.

Hi-Fi format detection is performed via `mimeType` matching against known MIME types for FLAC, WAV, ALAC, APE, and DSD.

### 8.2 Room Database

**Database name:** `music_db`  
**Migration policy:** `fallbackToDestructiveMigration` (drops and recreates on schema change)

| Entity | Table | Key Columns |
|---|---|---|
| `PlaylistEntity` | `playlists` | id (autoGen), name, createdAt, modifiedAt |
| `PlaylistSongEntity` | `playlist_songs` | playlistId (FK), songId, position |
| `FavoriteEntity` | `favorites` | songId (PK) |
| `PlayHistoryEntity` | `play_history` | songId (PK), playCount, lastPlayedAt |
| `LyricsEntity` | `lyrics` | songId (PK), lrcContent, plainText, isManual |

**DAOs:**
- `PlaylistDao`: full CRUD + `getSongsForPlaylist(playlistId)`
- `FavoriteDao`: `insert`, `delete`, `isFavorite`, `getAll`
- `PlayHistoryDao`: `upsert`, `getForSong`, `getRecent(limit)`, `getMostPlayed(limit)`
- `LyricsDao`: `upsert`, `getForSong`, `deleteForSong`

### 8.3 AppPreferences (DataStore)

All preferences stored in DataStore file `app_prefs`:

| Key | Type | Default | Purpose |
|---|---|---|---|
| `theme_mode` | String | `"SYSTEM"` | Light/Dark/System |
| `color_theme` | String | `"purple_haze"` | Color palette ID |
| `skin_id` | String | `"modern"` | UI skin ID |
| `pause_on_detach` | Boolean | `false` | Pause on headphone disconnect |
| `min_duration_sec` | Int | `30` | Library scan filter |
| `min_size_kb` | Int | `100` | Library scan filter |
| `gesture_swipe_enabled` | Boolean | `true` | Master swipe toggle |
| `gesture_pinch_enabled` | Boolean | `true` | Pinch-to-volume toggle |
| `swipe_up_action` | String | `"TOGGLE_LYRICS"` | Gesture mapping |
| `swipe_down_action` | String | `"TOGGLE_LYRICS"` | Gesture mapping |
| `crossfade_enabled` | Boolean | `false` | Crossfade toggle |
| `crossfade_duration` | Float | `3.0` | Crossfade length (seconds) |
| `auto_dj_enabled` | Boolean | `false` | Auto-DJ toggle |

### 8.4 MusicRepositoryImpl

The core singleton repository. Key implementation details:

**`cachedSongs`**: a `StateFlow<List<Song>>` derived from `MediaStoreSource.querySongs()`, with scan filter preferences applied via `flatMapLatest`. All other reactive queries (`getRecentlyAdded`, `getFavorites`, etc.) are `combine` or `map` operations on this single source.

**Queue tracking**: `_queue: MutableStateFlow<List<Song>>` is updated from `Player.Listener.onTimelineChanged`. `buildQueueFromPlayer()` iterates `player.mediaItemCount` and resolves each `mediaId` back to a `Song` via `cachedSongs`.

**Thread safety**: all ExoPlayer mutations (`setMediaItems`, `addMediaItem`, `moveMediaItem`, etc.) are dispatched via `withContext(Dispatchers.Main)`.

**Play history**: `recordPlay(songId)` upserts the `PlayHistoryEntity`, incrementing `playCount` and refreshing `lastPlayedAt`.

### 8.5 LyricsRepositoryImpl

Multi-source lyrics with Room caching:

1. Check `LyricsDao.getForSong(songId)` — return immediately if found
2. Call Genius API search endpoint
3. If no result, call NetEase Cloud Music API
4. If still no result, call LyricFind
5. Parse LRC format with `LrcParser` if the response has timestamps
6. Cache result to Room

`LrcParser` handles both `[mm:ss.xx]` and `[mm:ss:xx]` timestamp formats. It also generates LRC text from a `Lyrics` object for storage.

### 8.6 BackupRepositoryImpl

Uses Google Drive REST API via `com.google.api.services.drive.Drive` (OAuth2 via Play Services):
- Stores backups in the app-private Drive folder (not visible in Google Drive UI)
- Each backup file is named with an ISO timestamp
- Restore re-creates all playlists and favorites in Room then refreshes the library

---

## 9. Service Layer

### 9.1 MusicService

Extends `MediaLibraryService`. Lifecycle:

- **`onCreate`**: creates notification channel, registers `BroadcastReceiver` for `ACTION_AUDIO_BECOMING_NOISY` (pause on headphone unplug), builds `MediaLibrarySession` with `playerController.exoPlayer`, subscribes to Cast state changes to hot-swap the session's player.
- **`onStartCommand`**: delegates to super; wrapped in try/catch to prevent foreground service crashes.
- **`onGetSession`**: returns the single `MediaLibrarySession`.
- **`onDestroy`**: unregisters receiver, cancels service scope, releases session.

The `AutoBrowserCallback` inner class implements the browse tree for Android Auto.

### 9.2 PlayerController

Singleton wrapping the two player instances:

```kotlin
val activePlayer: Player
    get() = if (_isCastActive.value) _castPlayer.value ?: exoPlayer else exoPlayer
```

Cast initialization is done asynchronously via `CastContext.getSharedInstance()` with `ContextCompat.getMainExecutor`. On success, a `CastPlayer` is configured and stored. Session transitions are handled by `SessionAvailabilityListener`.

`isPlaying: StateFlow<Boolean>` is kept in sync by listening to `onIsPlayingChanged` on whichever player is currently active.

### 9.3 EqualizerManager

Bound to ExoPlayer's `audioSessionId`. All five audio effects (`Equalizer`, `BassBoost`, `Virtualizer`, `PresetReverb`, `LoudnessEnhancer`) are initialized in `init`. Settings load from `SharedPreferences` immediately.

`applyReplayGain(gainDb)` converts a dB gain value to millibels and sets `LoudnessEnhancer.setTargetGain()`.

### 9.4 JamSessionManager

**Host mode (`startHosting`):**
1. Opens `ServerSocket(0)` (OS assigns free port)
2. Updates `_state` to `isHosting = true`
3. Starts a daemon thread accepting connections in a loop
4. Each accepted socket is handled in its own daemon thread via `handleRequest()`
5. Registers NSD service so other devices can discover the session

**Client mode (`startDiscovery`):**
1. Starts `NsdManager.discoverServices()` for `_musicjam._tcp.`
2. Each `onServiceFound` triggers `resolveService()` which populates `availableSessions`
3. `fetchHostSongs()` and `addSongToHost()` use raw HTTP/1.0 over `Socket`

**`handleRequest(socket)`:**
- Reads first line of HTTP request
- Routes `GET /songs` → `buildSongsJson(hostSongs)`
- Routes `GET /add?id=X` → `_songAddEvents.emit(id)`
- Tracks `connectedPeers` count by incrementing on accept and decrementing in finally block

### 9.5 SpectrumProvider

Wraps `android.media.audiofx.Visualizer` bound to ExoPlayer's audio session. Exposes waveform data as a `FloatArray` polled by the `VisualizerPage` composable on each animation frame. `isAvailable` is false if `RECORD_AUDIO` permission is not granted.

---

## 10. Presentation Layer

### 10.1 Navigation

`MainActivity` hosts a single `NavHost` with `rememberNavController()`. Navigation structure:

```
dashboard (start)
├── player
│   ├── equalizer
│   └── jam
├── library
│   ├── trim/{songId}
│   └── artwork/{songId}
│       └── artwork_web/{songId}
├── folder/{bucketId}
├── playlists
├── search
├── settings
│   ├── scan_filters
│   ├── gesture_settings
│   ├── backup
│   └── equalizer
├── themes
├── skins
├── widgets
└── help
```

**Bottom navigation bar** (visible on dashboard, playlists, search): Menu (opens drawer), Home, Playlists, Search.

**MiniPlayer**: shown above the nav bar on all screens except `player`, when a song is currently loaded.

**Navigation drawer**: always accessible via the Menu button; contains theme toggles, settings links, scan trigger.

### 10.2 PlayerScreen & PlayerViewModel

`PlayerScreen` is the most complex screen, supporting two layouts:

**Normal layout (non-lyrics):**
- `PlayerTopRow`: EQ button, Lyrics toggle, Cast button, Favorite button
- `HorizontalPager` (swipeable, 3 pages): Visualizer / Cover Art / Music Icon
- Page indicator dots (tappable)
- Song title + artist
- Format badge (FLAC, Hi-Res badge for qualifying formats)
- Progress slider with position/duration labels
- `PlayerControls`: shuffle, prev, play/pause, next, repeat
- `PlayerSecondaryRow`: Queue button, Jam button

**Lyrics layout:**
- Dimmed artwork background
- `PlayerTopRow` (with "Cover" toggle instead of "Lyrics")
- Song title + artist
- Synced lyrics `LazyColumn` (auto-scrolling karaoke view) or plain text
- Edit lyrics button
- `PlayerControls`

**Gesture handling (normal layout):**
- Swipe anywhere on the screen (outside the pager) → configured gesture action
- Pinch anywhere → volume up/down
- Pager swipe → switch between Visualizer / Cover / Icon pages

**Crossfade**: when crossfade is enabled, `PlayerViewModel` ramps `player.volume` down toward the end of each track and ramps it back up at the start of the next.

**`PlayerViewModel` state:**
```kotlin
data class PlayerUiState(
    val currentSong: Song?,
    val isPlaying: Boolean,
    val progress: Float,         // 0.0–1.0
    val positionMs: Long,
    val queue: List<Song>,
    val isFavorite: Boolean,
    val repeatMode: RepeatMode,
    val shuffleEnabled: Boolean,
    val artworkVersion: Int,     // incremented to bust Coil cache
    val unsupportedFormatMessage: String?
)
```

### 10.3 DashboardScreen

Rendered as a `LazyColumn` of horizontal `LazyRow` song carousels. Each `SongCard` shows album art (via Coil + MediaStore album art URI), title, and artist. Tapping calls `DashboardViewModel.play(song)` then navigates to player.

### 10.4 LibraryScreen

Tab-based multi-category browser. Uses `HorizontalPager` for category tabs. Sort controls are shown in a `DropdownMenu`. Search bar is always visible at the top with a clear button. Long-press on a song item opens a `ModalBottomSheet` context menu.

### 10.5 QueueScreen

`ModalBottomSheet` overlaid on `PlayerScreen`. Contains:
- Header row: "Up Next" title, Auto-DJ switch, Clear button
- `ReorderableQueueList` (custom drag-to-reorder `LazyColumn`)

Drag reorder works by tracking `draggingIndex` and `dragOffsetY` as `mutableStateOf` variables. When the Y offset crosses a full item height (64dp), `onMove(from, to)` is called and the offset is corrected.

### 10.6 JamScreen

Two-tab screen (Host / Join) using `TabRow`.

**Host tab:**
- Session name text field
- Start/Stop hosting button
- Connected peers count card

**Join tab:**
- Search button (triggers NSD discovery)
- Session list with "Browse" button per session
- Expanded song list from the host with add buttons
- Added songs show a green checkmark

### 10.7 EqualizerScreen

Visual EQ with:
- 10 vertical `Slider` components (one per band), labeled with center frequency
- Preset `DropdownMenu`
- Effect cards: Bass Boost, Virtualizer, Reverb (preset picker), Loudness Enhancer

All changes write immediately to `EqualizerManager` and persist to `SharedPreferences`.

### 10.8 Settings Screens

**`SettingsScreen`:**
- Detects audio capabilities (max sample rate, USB DAC presence, Hi-Res certification)
- Crossfade toggle + duration slider
- Links to Equalizer, Scan Filters, Gesture Settings, Backup

**`ScanFilterScreen`:**
- Two sliders: minimum duration (0–300 seconds) and minimum file size (0–5000 KB)

**`GestureSettingsScreen`:**
- Toggle for swipe gestures (master on/off)
- Toggle for pinch gesture
- Dropdowns for swipe-up and swipe-down action mappings

**`BackupScreen`:**
- Google sign-in button
- Create Backup button
- Scrollable list of existing backups (date + size) with Restore/Delete actions

### 10.9 Drawer & Theme System

`AppDrawer` (rendered as `ModalNavigationDrawer` content) contains:
- Theme mode radio buttons (System/Light/Dark)
- Color theme selector (shows current theme name + emoji)
- Skin selector
- Pause on detach toggle
- Scan Music button
- Navigation links: Settings, Help, Theme Store, Skin Store, Widgets, Backup

`MusicPlayerTheme` composable applies the selected `colorThemeId` to build a `ColorScheme` and passes `skinId` down via `LocalSkin` `CompositionLocal`.

---

## 11. Dependency Injection

`MusicModule` (Hilt `@Module`, `@InstallIn(SingletonComponent::class)`) provides:

| Binding | Scope | Notes |
|---|---|---|
| `MusicDatabase` | Singleton | Room DB, destructive migration |
| `MediaStoreSource` | Singleton | Content-observer-based flow |
| `CoroutineScope` (`@ApplicationScope`) | Singleton | Default + SupervisorJob |
| `ExoPlayer` | Singleton | Built on main thread via Hilt injection chain |
| `MusicRepository` | Singleton | `MusicRepositoryImpl` + `widgetUpdater.init()` |
| `BackupRepository` | Singleton | `BackupRepositoryImpl` |
| `LyricsRepository` | Singleton | `LyricsRepositoryImpl` |

`PlayerController`, `EqualizerManager`, `JamSessionManager`, and `AppPreferences` are `@Singleton` with `@Inject constructor` (auto-provided by Hilt, no explicit `@Provides` needed).

`WidgetEntryPoint` is a Hilt `@EntryPoint` on `SingletonComponent` providing `WidgetUpdater` to Glance widgets, which cannot use standard Hilt injection.

---

## 12. Widget System

```
Song changes in MusicRepository
    ↓
WidgetUpdater.init(repository) subscribes in appScope
    ↓
getCurrentSong() + observeIsPlaying() combined
    ↓
GlanceStateDefinition preference store updated
    ↓
GlanceAppWidgetManager.updateAll() called
    ↓
SmallMusicWidget / StandardMusicWidget recomposed
```

Widget actions (prev, play/pause, next) send `Intent` to `MusicService` using action strings defined in `WidgetActions`. `MusicService.onStartCommand` handles these intents.

Widget layout files reference `widget_placeholder.xml` for Glance's `AndroidRemoteViews` compatibility.

---

## 13. Build Configuration

| Property | Value |
|---|---|
| `compileSdk` | 35 |
| `minSdk` | 26 (Android 8.0 Oreo) |
| `targetSdk` | 35 (Android 15) |
| Java source/target compatibility | 17 |
| Kotlin JVM target | 17 |
| KSP | 2.1.0-1.0.29 |

**Build types:**

| Type | Minify | Shrink | App ID suffix |
|---|---|---|---|
| `debug` | No | No | `.debug` |
| `staging` | Yes | No | — |
| `release` | Yes | Yes | — |

Release signing uses environment variables: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

**Proguard:** default Android optimization rules + `proguard-rules.pro`.

**Kotlin compiler flags:**
- `-opt-in=androidx.media3.common.util.UnstableApi` (allows use of Media3 unstable APIs required for certain session and cast features)

---

## 14. Known Considerations

### Thread Safety
- All ExoPlayer read/write operations must be on the main thread. `MusicRepositoryImpl` enforces this with `withContext(Dispatchers.Main)` wrappers around every player mutation.
- `EqualizerManager` effect objects are also main-thread bound.

### MediaRouteButton (Chromecast)
`MediaRouteButton` reads `android:colorBackground` from the Activity theme via `TypedArray.getColor()`. The theme explicitly defines this attribute in both `values/themes.xml` (light: `#FFFAFAFA`) and `values-night/themes.xml` (dark: `#FF1C1B1F`) to prevent an `IllegalArgumentException` crash inside `MediaRouterThemeHelper`.

### First-Launch DataStore
On the very first app launch, DataStore reads from disk. Theme preferences are resolved synchronously via `runBlocking { prefs.X.first() }` in `DrawerViewModel` to ensure the correct theme is applied on the first frame.

### Auto-DJ Recursion Bounds
`checkAndRefillQueue()` is called from `onTimelineChanged`. Adding media items triggers further `onTimelineChanged` calls. The `remaining >= 3` early-return guard bounds recursion depth to at most 3 levels before the check passes and no further items are added.

### Scoped Storage (API 29+)
`TrimViewModel` uses `ContentResolver.insert()` to register trimmed audio files with MediaStore rather than writing directly to the file system, ensuring proper visibility on all supported API levels.

### Foreground Service
`MusicService` uses `foregroundServiceType="mediaPlayback"`. On API 34+, this type must be declared in the manifest AND the app must hold `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission. Both are present in `AndroidManifest.xml`.
