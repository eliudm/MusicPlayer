package com.example.musicplayer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicplayer.domain.model.GestureAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_PAUSE_ON_DETACH = booleanPreferencesKey("pause_on_detach")
    private val KEY_COLOR_THEME = stringPreferencesKey("color_theme")
    private val KEY_SKIN = stringPreferencesKey("skin_id")
    private val KEY_MIN_DURATION_SEC = androidx.datastore.preferences.core.intPreferencesKey("min_duration_sec")
    private val KEY_MIN_SIZE_KB = androidx.datastore.preferences.core.intPreferencesKey("min_size_kb")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val pauseOnDetach: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PAUSE_ON_DETACH] ?: false
    }

    val colorThemeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_COLOR_THEME] ?: "purple_haze"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setPauseOnDetach(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PAUSE_ON_DETACH] = enabled }
    }

    val skinId: Flow<String> = context.dataStore.data.map { it[KEY_SKIN] ?: "modern" }

    suspend fun setSkinId(id: String) {
        context.dataStore.edit { it[KEY_SKIN] = id }
    }

    /** Minimum track duration in seconds (default 30 s). */
    val minDurationSec: Flow<Int> = context.dataStore.data.map { it[KEY_MIN_DURATION_SEC] ?: 30 }

    /** Minimum file size in kilobytes (default 100 KB). */
    val minSizeKb: Flow<Int> = context.dataStore.data.map { it[KEY_MIN_SIZE_KB] ?: 100 }

    suspend fun setColorThemeId(id: String) {
        context.dataStore.edit { it[KEY_COLOR_THEME] = id }
    }

    suspend fun setMinDurationSec(seconds: Int) {
        context.dataStore.edit { it[KEY_MIN_DURATION_SEC] = seconds }
    }

    suspend fun setMinSizeKb(kb: Int) {
        context.dataStore.edit { it[KEY_MIN_SIZE_KB] = kb }
    }

    // ── Gesture preferences ───────────────────────────────────────────────────

    private val KEY_GESTURE_SWIPE = booleanPreferencesKey("gesture_swipe_enabled")
    private val KEY_GESTURE_PINCH = booleanPreferencesKey("gesture_pinch_enabled")
    private val KEY_SWIPE_UP = stringPreferencesKey("swipe_up_action")
    private val KEY_SWIPE_DOWN = stringPreferencesKey("swipe_down_action")

    val gestureSwipeEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_GESTURE_SWIPE] ?: true }

    val gesturePinchEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_GESTURE_PINCH] ?: true }

    val swipeUpAction: Flow<GestureAction> = context.dataStore.data.map { prefs ->
        prefs[KEY_SWIPE_UP]?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() }
            ?: GestureAction.TOGGLE_LYRICS
    }

    val swipeDownAction: Flow<GestureAction> = context.dataStore.data.map { prefs ->
        prefs[KEY_SWIPE_DOWN]?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() }
            ?: GestureAction.NONE
    }

    suspend fun setGestureSwipeEnabled(enabled: Boolean) =
        context.dataStore.edit { it[KEY_GESTURE_SWIPE] = enabled }

    suspend fun setGesturePinchEnabled(enabled: Boolean) =
        context.dataStore.edit { it[KEY_GESTURE_PINCH] = enabled }

    suspend fun setSwipeUpAction(action: GestureAction) =
        context.dataStore.edit { it[KEY_SWIPE_UP] = action.name }

    suspend fun setSwipeDownAction(action: GestureAction) =
        context.dataStore.edit { it[KEY_SWIPE_DOWN] = action.name }

    // ── Crossfade preferences ─────────────────────────────────────────────────

    private val KEY_CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
    private val KEY_CROSSFADE_DURATION = floatPreferencesKey("crossfade_duration_sec")

    val crossfadeEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_CROSSFADE_ENABLED] ?: false }

    val crossfadeDuration: Flow<Float> =
        context.dataStore.data.map { it[KEY_CROSSFADE_DURATION] ?: 3f }

    suspend fun setCrossfadeEnabled(enabled: Boolean) =
        context.dataStore.edit { it[KEY_CROSSFADE_ENABLED] = enabled }

    suspend fun setCrossfadeDuration(seconds: Float) =
        context.dataStore.edit { it[KEY_CROSSFADE_DURATION] = seconds }

    // ── Auto-DJ ───────────────────────────────────────────────────────────────

    private val KEY_AUTO_DJ = booleanPreferencesKey("auto_dj_enabled")

    val autoDjEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_AUTO_DJ] ?: false }

    suspend fun setAutoDjEnabled(enabled: Boolean) =
        context.dataStore.edit { it[KEY_AUTO_DJ] = enabled }
}
