package com.auramusic.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.offline.model.AudioTrack
import com.auramusic.offline.model.CustomAlbum
import com.auramusic.offline.theme.AppleCrimson
import com.auramusic.offline.ui.components.AppleTrackItem

@Composable
fun CustomAlbumDetailScreen(
    album: CustomAlbum,
    allMusicTracks: List<AudioTrack>,
    currentlyPlayingTrackId: Long?,
    isPlaying: Boolean = false,
    onBackClick: () -> Unit,
    onPlayTrack: (AudioTrack, List<AudioTrack>) -> Unit,
    onShufflePlay: (List<AudioTrack>) -> Unit,
    onAddSongsClick: () -> Unit,
    onRemoveTrackFromAlbum: (Long) -> Unit,
    onDeleteAlbum: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (AudioTrack) -> Unit = {},
    onAddToQueue: (AudioTrack) -> Unit = {}
) {
    val albumTracks = allMusicTracks.filter { album.trackIds.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Row {
                IconButton(onClick = onAddSongsClick) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Songs",
                        tint = AppleCrimson
                    )
                }

                IconButton(onClick = { onDeleteAlbum(album.id) }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Album",
                        tint = Color(0xFFFF453A)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album Artwork & Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom Album Photo Cover
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        AppleCrimson,
                                        Color(0xFF5856D6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!album.customPhotoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = album.customPhotoUri,
                                contentDescription = album.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (album.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = album.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${albumTracks.size} Track(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleCrimson,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play & Shuffle buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (albumTracks.isNotEmpty()) {
                                    onPlayTrack(albumTracks.first(), albumTracks)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (albumTracks.isNotEmpty()) {
                                    onShufflePlay(albumTracks)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = AppleCrimson, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", color = AppleCrimson, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (albumTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Album is Empty",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onAddSongsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Songs Now")
                            }
                        }
                    }
                }
            } else {
                items(albumTracks, key = { it.id }) { track ->
                    AppleTrackItem(
                        track = track,
                        isCurrentlyPlaying = track.id == currentlyPlayingTrackId,
                        isPlaying = isPlaying,
                        isSelected = false,
                        isSelectionMode = false,
                        onClick = { onPlayTrack(track, albumTracks) },
                        onToggleSelect = {},
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onAddToAlbumClick = {},
                        onMoveToTrashClick = { onRemoveTrackFromAlbum(track.id) },
                        onPlayNextClick = { onPlayNext(track) },
                        onAddToQueueClick = { onAddToQueue(track) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // padding for bottom player
            }
        }
    }
}
