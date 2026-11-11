package com.auramusic.offline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.offline.theme.AppleCrimson
import com.auramusic.offline.util.HapticHelper

@Composable
fun BatchSelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAllClick: () -> Unit,
    onDeselectAllClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onQuickShareClick: () -> Unit,
    onAddToAlbumClick: () -> Unit,
    onMoveToTrashClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAllSelected = selectedCount == totalCount && totalCount > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(18.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        HapticHelper.tap(context)
                        onCloseClick()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel Selection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Select All / Deselect All Action Button
                Button(
                    onClick = {
                        HapticHelper.mediumClick(context)
                        if (isAllSelected) onDeselectAllClick() else onSelectAllClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAllSelected) MaterialTheme.colorScheme.surface else AppleCrimson.copy(alpha = 0.15f),
                        contentColor = if (isAllSelected) MaterialTheme.colorScheme.onSurface else AppleCrimson
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.bounceScale(0.92f)
                ) {
                    Icon(
                        imageVector = if (isAllSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAllSelected) "Deselect All" else "Select All ($selectedCount/$totalCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Add to Favorite Button
                IconButton(
                    onClick = {
                        HapticHelper.mediumClick(context)
                        onToggleFavoriteClick()
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite Selected",
                        tint = if (selectedCount > 0) AppleCrimson else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // QuickShare Button
                IconButton(
                    onClick = {
                        HapticHelper.mediumClick(context)
                        onQuickShareClick()
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "QuickShare",
                        tint = if (selectedCount > 0) AppleCrimson else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // Add to Album Button
                IconButton(
                    onClick = {
                        HapticHelper.tap(context)
                        onAddToAlbumClick()
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add to Album",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // Move to Trash Button
                IconButton(
                    onClick = {
                        HapticHelper.alert(context)
                        onMoveToTrashClick()
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Move to Trash",
                        tint = if (selectedCount > 0) Color(0xFFFF453A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
