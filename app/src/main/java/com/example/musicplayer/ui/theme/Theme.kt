package com.example.musicplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.example.musicplayer.data.preferences.ThemeMode

@Composable
fun MusicPlayerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorThemeId: String = "purple_haze",
    skinId: String = "modern",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val appColorTheme = colorThemeById(colorThemeId)
    val colorScheme = when {
        colorThemeId == "classic_blue" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> appColorTheme.darkScheme
        else -> appColorTheme.lightScheme
    }

    val skin = skinById(skinId)

    CompositionLocalProvider(LocalSkin provides skin) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = skin.shapes,
            typography = Typography(),
            content = content
        )
    }
}
