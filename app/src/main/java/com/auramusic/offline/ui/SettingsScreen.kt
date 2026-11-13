package com.auramusic.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.offline.theme.AppleCrimson
import com.auramusic.offline.ui.components.SleepTimerDialog
import com.auramusic.offline.util.HapticHelper

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    musicCount: Int,
    recordingsCount: Int,
    callRecordsCount: Int,
    recycleBinCount: Int,
    sleepTimerSecondsRemaining: Int?,
    onBackClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onRescanClick: () -> Unit
) {
    val context = LocalContext.current
    var showTimerDialog by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    HapticHelper.tap(context)
                    onBackClick()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APPEARANCE SECTION
            SettingsSectionHeader(title = "APPEARANCE")
            SettingsCard {
                SettingsSwitchRow(
                    icon = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    title = if (isDarkMode) "Dark Theme (Active)" else "Light Theme (Active)",
                    subtitle = if (isDarkMode) "OLED pure black interface" else "iOS clean bright interface",
                    checked = isDarkMode,
                    onCheckedChange = {
                        HapticHelper.mediumClick(context)
                        onToggleTheme()
                    }
                )
            }

            // AUDIO PLAYBACK & TIMERS
            SettingsSectionHeader(title = "AUDIO & PLAYBACK")
            SettingsCard {
                SettingsClickableRow(
                    icon = Icons.Rounded.Bedtime,
                    title = "Sleep Timer",
                    subtitle = if (sleepTimerSecondsRemaining != null) {
                        "Timer active (${sleepTimerSecondsRemaining / 60}m remaining)"
                    } else {
                        "Fade out audio automatically"
                    },
                    trailingText = if (sleepTimerSecondsRemaining != null) "Active" else "Off",
                    onClick = {
                        HapticHelper.tap(context)
                        showTimerDialog = true
                    }
                )

                SettingsDivider()

                SettingsInfoRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "AndroidX Media3 (ExoPlayer)",
                    subtitle = "Pro low-latency 44.1kHz 16-bit audio engine • Gapless playback & auto audio focus"
                )
            }

            // ISOLATION & STORAGE
            SettingsSectionHeader(title = "STORAGE & PRIVACY ISOLATION")
            SettingsCard {
                SettingsInfoRow(
                    icon = Icons.Rounded.Security,
                    title = "Strict Content Isolation",
                    subtitle = "Recordings ($recordingsCount), Calls ($callRecordsCount) & Trash ($recycleBinCount) isolated from Library"
                )

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Rounded.Storage,
                    title = "Music Library Size",
                    subtitle = "$musicCount songs organized in local database",
                    trailingText = "Rescan",
                    onClick = {
                        HapticHelper.mediumClick(context)
                        onRescanClick()
                    }
                )

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Rounded.CleaningServices,
                    title = "Clear Audio Cache",
                    subtitle = cacheClearedMessage ?: "Clear transient artwork and audio buffers",
                    trailingText = "Clear",
                    onClick = {
                        HapticHelper.tap(context)
                        try {
                            context.cacheDir.deleteRecursively()
                        } catch (_: Exception) {}
                        cacheClearedMessage = "Cache cleaned successfully"
                    }
                )
            }

            // ABOUT & VERSION
            SettingsSectionHeader(title = "ABOUT")
            SettingsCard {
                SettingsInfoRow(
                    icon = Icons.Rounded.Info,
                    title = "Aura Music Pro",
                    subtitle = "v2.5 Pro Edition • Apple Design Theory"
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            activeRemainingSeconds = sleepTimerSecondsRemaining,
            onSetTimer = onSetSleepTimer,
            onDismiss = { showTimerDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppleCrimson.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppleCrimson,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppleCrimson
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppleCrimson.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppleCrimson,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = trailingText,
            color = AppleCrimson,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AppleCrimson.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleCrimson,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
