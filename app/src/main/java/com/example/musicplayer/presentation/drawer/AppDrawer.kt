package com.example.musicplayer.presentation.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer(
    themeMode: com.example.musicplayer.data.preferences.ThemeMode,
    pauseOnDetach: Boolean,
    colorThemeName: String,
    skinName: String,
    onThemeSelected: (com.example.musicplayer.data.preferences.ThemeMode) -> Unit,
    onTogglePauseOnDetach: () -> Unit,
    onScanMusic: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToThemeStore: () -> Unit,
    onNavigateToSkinStore: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Music Player", style = MaterialTheme.typography.titleLarge)
                }
            }

            HorizontalDivider()

            // General
            DrawerSectionLabel("General")

            DrawerItem(
                icon = { Icon(Icons.Default.Settings, null) },
                label = "Settings",
                onClick = { onNavigateToSettings(); onClose() }
            )

            DrawerItem(
                icon = { Icon(Icons.Default.Refresh, null) },
                label = "Scan Music",
                onClick = { onScanMusic(); onClose() }
            )

            DrawerItem(
                icon = { Icon(Icons.Default.HeadsetOff, null) },
                label = "Pause on Detach",
                onClick = onTogglePauseOnDetach,
                trailing = {
                    Switch(
                        checked = pauseOnDetach,
                        onCheckedChange = { onTogglePauseOnDetach() }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Appearance
            DrawerSectionLabel("Appearance")

            DrawerItem(
                icon = { Icon(Icons.Default.Palette, null) },
                label = "Themes",
                sublabel = colorThemeName,
                onClick = { onNavigateToThemeStore(); onClose() }
            )

            DrawerItem(
                icon = { Icon(Icons.Default.ColorLens, null) },
                label = "Skin Theme",
                sublabel = skinName,
                onClick = { onNavigateToSkinStore(); onClose() }
            )

            DrawerItem(
                icon = { Icon(Icons.Default.Widgets, null) },
                label = "Widgets",
                sublabel = "Add to home screen",
                onClick = { onNavigateToWidgets(); onClose() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Data
            DrawerSectionLabel("Data")

            DrawerItem(
                icon = { Icon(Icons.Default.CloudUpload, null) },
                label = "Backup & Restore",
                sublabel = "Google Drive",
                onClick = { onNavigateToBackup(); onClose() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Region
            DrawerSectionLabel("Region")

            DrawerItem(
                icon = { Icon(Icons.Default.Language, null) },
                label = "Language",
                sublabel = "System language",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_LOCALE_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    onClose()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Help
            DrawerSectionLabel("Help")

            DrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) },
                label = "FAQ & Help",
                onClick = { onNavigateToHelp(); onClose() }
            )

            DrawerItem(
                icon = { Icon(Icons.Default.Feedback, null) },
                label = "Feedback",
                onClick = {
                    context.sendFeedbackEmail()
                    onClose()
                }
            )

            DrawerItem(
                icon = { Icon(Icons.Default.Star, null) },
                label = "Rate App",
                onClick = {
                    context.openPlayStoreListing()
                    onClose()
                }
            )

            DrawerItem(
                icon = { Icon(Icons.Default.Description, null) },
                label = "Terms of Use",
                onClick = { onNavigateToHelp(); onClose() }
            )

            Spacer(Modifier.height(16.dp))
        }
    }

}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerItem(
    icon: @Composable () -> Unit,
    label: String,
    sublabel: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp)) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (sublabel != null) {
                Text(
                    sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
    }
}


private fun Context.sendFeedbackEmail() {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:support@musicplayer.app")
        putExtra(Intent.EXTRA_SUBJECT, "Music Player Feedback")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
}

private fun Context.openPlayStoreListing() {
    val pkg = packageName
    val intent = runCatching {
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.getOrElse {
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { startActivity(intent) }
}
