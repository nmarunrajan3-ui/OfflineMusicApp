package com.auramusic.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.offline.theme.AppleCrimson

enum class DrawerDestination {
    HOME,
    RECYCLE_BIN,
    FIND_DUPLICATES,
    RECORDINGS,
    CALL_RECORDS,
    SETTINGS
}

@Composable
fun DrawerSheetContent(
    currentDestination: DrawerDestination,
    isDarkMode: Boolean,
    recycleBinCount: Int,
    duplicateCount: Int,
    recordingsCount: Int,
    callRecordsCount: Int,
    onNavigate: (DrawerDestination) -> Unit,
    onToggleTheme: () -> Unit,
    onRescanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 32.dp, horizontal = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // App Branding Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleCrimson),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Aura Music",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pro Offline Studio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "ORGANIZATION & TOOLS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 1. Recycle Bin
            DrawerMenuItem(
                icon = Icons.Rounded.DeleteOutline,
                title = "Recycle Bin",
                badgeText = if (recycleBinCount > 0) "$recycleBinCount" else null,
                badgeColor = Color(0xFFFF453A),
                isSelected = currentDestination == DrawerDestination.RECYCLE_BIN,
                onClick = { onNavigate(DrawerDestination.RECYCLE_BIN) }
            )

            // 2. Find Duplicates
            DrawerMenuItem(
                icon = Icons.Rounded.ContentCopy,
                title = "Find Duplicates",
                badgeText = if (duplicateCount > 0) "$duplicateCount sets" else null,
                badgeColor = Color(0xFFFF9F0A),
                isSelected = currentDestination == DrawerDestination.FIND_DUPLICATES,
                onClick = { onNavigate(DrawerDestination.FIND_DUPLICATES) }
            )

            // 3. Isolated Recordings
            DrawerMenuItem(
                icon = Icons.Rounded.Mic,
                title = "Recordings",
                badgeText = if (recordingsCount > 0) "$recordingsCount" else null,
                badgeColor = Color(0xFF30B0C7),
                isSelected = currentDestination == DrawerDestination.RECORDINGS,
                onClick = { onNavigate(DrawerDestination.RECORDINGS) }
            )

            // 4. Isolated Call Records
            DrawerMenuItem(
                icon = Icons.Rounded.Call,
                title = "Call Records",
                badgeText = if (callRecordsCount > 0) "$callRecordsCount" else null,
                badgeColor = Color(0xFF34C759),
                isSelected = currentDestination == DrawerDestination.CALL_RECORDS,
                onClick = { onNavigate(DrawerDestination.CALL_RECORDS) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // Rescan Library
            DrawerMenuItem(
                icon = Icons.Rounded.Refresh,
                title = "Rescan Library",
                badgeText = null,
                badgeColor = null,
                isSelected = false,
                onClick = onRescanClick
            )

            // Settings
            DrawerMenuItem(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                badgeText = null,
                badgeColor = null,
                isSelected = currentDestination == DrawerDestination.SETTINGS,
                onClick = { onNavigate(DrawerDestination.SETTINGS) }
            )
        }

        // Bottom Theme Switcher (Dark & Light Theme)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = null,
                        tint = AppleCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isDarkMode) "Dark Theme" else "Light Theme",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppleCrimson
                    )
                )
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    badgeText: String?,
    badgeColor: Color?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AppleCrimson.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) AppleCrimson else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AppleCrimson else MaterialTheme.colorScheme.onSurface
            )
        }

        if (badgeText != null && badgeColor != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
