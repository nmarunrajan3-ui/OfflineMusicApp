package com.auramusic.offline.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.offline.model.AudioTrack
import com.auramusic.offline.model.PlaybackState
import com.auramusic.offline.model.RepeatMode as AudioRepeatMode
import com.auramusic.offline.theme.AppleCrimson
import com.auramusic.offline.ui.components.EqualizerBars
import com.auramusic.offline.ui.components.SleepTimerDialog
import com.auramusic.offline.ui.components.bounceScale
import com.auramusic.offline.util.FormatUtils
import com.auramusic.offline.util.HapticHelper
import com.auramusic.offline.util.ShareHelper
import kotlin.math.roundToInt

@Composable
fun NowPlayingFullScreen(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onPlayTrackFromQueue: (AudioTrack) -> Unit,
    onPlayNext: (AudioTrack) -> Unit = {},
    onRemoveFromQueue: (Int) -> Unit = {}
) {
    val track = playbackState.currentTrack ?: return
    val context = LocalContext.current
    var showTimerDialog by remember { mutableStateOf(false) }

    // 100% SOLID OPAQUE Apple Music Dynamic Gradient
    val dominantLong = playbackState.dominantColor ?: 0xFFFA243CL
    val secondaryLong = playbackState.secondaryColor ?: 0xFF1C1C1EL

    val rawDominant = Color(dominantLong or 0xFF000000L)
    val rawSecondary = Color(secondaryLong or 0xFF000000L)

    val topGradientColor = Color(
        red = (rawDominant.red * 0.72f + 0.05f).coerceIn(0f, 1f),
        green = (rawDominant.green * 0.72f + 0.05f).coerceIn(0f, 1f),
        blue = (rawDominant.blue * 0.72f + 0.05f).coerceIn(0f, 1f),
        alpha = 1.0f
    )
    val midGradientColor = Color(
        red = (rawSecondary.red * 0.48f + 0.03f).coerceIn(0f, 1f),
        green = (rawSecondary.green * 0.48f + 0.03f).coerceIn(0f, 1f),
        blue = (rawSecondary.blue * 0.48f + 0.03f).coerceIn(0f, 1f),
        alpha = 1.0f
    )
    val deepBottomColor = Color(0xFF09090D)

    val solidAppleBackground = Brush.verticalGradient(
        colors = listOf(
            topGradientColor,
            midGradientColor,
            deepBottomColor
        )
    )

    // Interactive Swipe Down To Dismiss with Apple spring physics
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "swipeToDismissOffset"
    )

    val scrollState = rememberScrollState()

    // NestedScrollConnection to intercept downward pulling from the top of the screen
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If user is at top of screen and dragging downward
                if (available.y > 0f && scrollState.value == 0) {
                    dragOffsetY = (dragOffsetY + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                } else if (available.y < 0f && dragOffsetY > 0f) {
                    dragOffsetY = (dragOffsetY + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetY > 90f || available.y > 700f) {
                    HapticHelper.mediumClick(context)
                    onDismiss()
                    dragOffsetY = 0f
                    return available
                } else {
                    dragOffsetY = 0f
                }
                return Velocity.Zero
            }
        }
    }

    // Spring physics for Album Artwork
    val targetScale = if (playbackState.isPlaying) 1.0f else 0.84f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "artScale"
    )

    val targetElevation = if (playbackState.isPlaying) 28.dp else 6.dp
    val animatedElevation by animateDpAsState(
        targetValue = targetElevation,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "artElevation"
    )

    // Ambient pulsing glow behind the cover when playing
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Interactive Scrubber
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderPosition by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayFraction = if (isDraggingSlider) dragSliderPosition else currentFraction
    val displayedElapsedMs = (displayFraction * playbackState.durationMs).toLong()
    val displayedRemainingMs = (playbackState.durationMs - displayedElapsedMs).coerceAtLeast(0L)

    // Compute upcoming songs from queue
    val upcomingSongs = remember(playbackState.queue, playbackState.currentQueueIndex) {
        if (playbackState.queue.isNotEmpty() && playbackState.currentQueueIndex in playbackState.queue.indices) {
            playbackState.queue.drop(playbackState.currentQueueIndex + 1)
        } else {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .background(solidAppleBackground)
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR & GRABBER AREA: With dedicated vertical drag detection to dismiss
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0f || dragOffsetY > 0f) {
                                    change.consume()
                                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                }
                            },
                            onDragEnd = {
                                if (dragOffsetY > 80f) {
                                    HapticHelper.mediumClick(context)
                                    onDismiss()
                                }
                                dragOffsetY = 0f
                            },
                            onDragCancel = { dragOffsetY = 0f }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pull-down Grabber Handle (Tap or Drag down to dismiss!)
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 8.dp)
                        .size(width = 42.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.40f))
                        .clickable {
                            HapticHelper.tap(context)
                            onDismiss()
                        }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            HapticHelper.tap(context)
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Playing from Library / Album subtitle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "PLAYING FROM LIBRARY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = track.album,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // QuickShare Button in Top Bar
                    IconButton(
                        onClick = {
                            HapticHelper.tap(context)
                            ShareHelper.shareTrack(context, track)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "QuickShare",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // MAIN PLAYER CONTENT (Artwork, Metadata, Scrubber, Controls, Directly Scrollable Upcoming Queue)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Artwork View with ambient breathing glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (playbackState.isPlaying) {
                        Box(
                            modifier = Modifier
                                .sizeIn(maxWidth = 230.dp, maxHeight = 230.dp)
                                .aspectRatio(1f)
                                .scale(animatedScale * (0.95f + 0.1f * glowPulse))
                                .clip(RoundedCornerShape(30.dp))
                                .background(rawDominant.copy(alpha = 0.35f * glowPulse))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .sizeIn(maxWidth = 220.dp, maxHeight = 220.dp)
                            .aspectRatio(1f)
                            .scale(animatedScale)
                            .shadow(animatedElevation, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF222226)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!track.albumArtUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = AppleCrimson,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Track Title, Artist & Favorite Heart Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticHelper.mediumClick(context)
                            onToggleFavorite()
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) AppleCrimson else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Time Scrubber & Labels
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = displayFraction,
                        onValueChange = {
                            isDraggingSlider = true
                            dragSliderPosition = it
                            HapticHelper.lightTick(context)
                        },
                        onValueChangeFinished = {
                            val targetMs = (dragSliderPosition * playbackState.durationMs).toLong()
                            onSeekTo(targetMs)
                            isDraggingSlider = false
                            HapticHelper.tap(context)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = FormatUtils.formatDuration(displayedElapsedMs),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "-${FormatUtils.formatDuration(displayedRemainingMs)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Apple Music Playback Controls (BORDERLESS WHITE PLAY/PAUSE GLYPH — SINGLE CLICK!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Shuffle Button
                    IconButton(
                        onClick = {
                            HapticHelper.tap(context)
                            onToggleShuffle()
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) AppleCrimson else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous (Skip to Past)
                    IconButton(
                        onClick = {
                            HapticHelper.mediumClick(context)
                            onPreviousClick()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Authentic Apple Music Play / Pause Borderless White Glyph
                    val playPauseSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = {
                            HapticHelper.mediumClick(context)
                            onPlayPauseClick()
                        },
                        interactionSource = playPauseSource,
                        modifier = Modifier
                            .size(64.dp)
                            .bounceScale(playPauseSource, 0.86f)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    // Next (Skip Forward)
                    IconButton(
                        onClick = {
                            HapticHelper.mediumClick(context)
                            onNextClick()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(
                        onClick = {
                            HapticHelper.tap(context)
                            onToggleRepeat()
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        val icon = if (playbackState.repeatMode == AudioRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                        val tint = if (playbackState.repeatMode != AudioRepeatMode.OFF) AppleCrimson else Color.White.copy(alpha = 0.65f)
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // DIRECTLY SCROLLABLE UPCOMING SONGS QUEUE (NO "See All" button required!)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PLAYING NEXT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.75f),
                                letterSpacing = 0.9.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AppleCrimson.copy(alpha = 0.28f))
                                    .padding(horizontal = 7.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${upcomingSongs.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleCrimson
                                )
                            }
                        }

                        Text(
                            text = "Scrollable Queue",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (upcomingSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "End of queue. Choose songs from library to Play Next!",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Smoothly scrollable upcoming songs container right inside the card
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 175.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            itemsIndexed(upcomingSongs, key = { index, item -> "${item.id}_${index}" }) { idx, nextSong ->
                                val originalQueueIndex = playbackState.currentQueueIndex + 1 + idx

                                UpcomingSongItemRow(
                                    song = nextSong,
                                    onPlayNow = {
                                        HapticHelper.mediumClick(context)
                                        onPlayTrackFromQueue(nextSong)
                                    },
                                    onPlayNext = {
                                        HapticHelper.mediumClick(context)
                                        onPlayNext(nextSong)
                                    },
                                    onRemove = {
                                        HapticHelper.tap(context)
                                        onRemoveFromQueue(originalQueueIndex)
                                    }
                                )

                                if (idx < upcomingSongs.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 44.dp),
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.10f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // BOTTOM BAR: Sleep Timer Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleep Timer Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            HapticHelper.tap(context)
                            showTimerDialog = true
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (playbackState.sleepTimerSecondsRemaining != null) AppleCrimson else Color.White,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playbackState.sleepTimerSecondsRemaining != null) {
                                "Timer: " + FormatUtils.formatTimerCountdown(playbackState.sleepTimerSecondsRemaining)
                            } else "Sleep Timer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (playbackState.sleepTimerSecondsRemaining != null) AppleCrimson else Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Sleep Timer Dialog
        if (showTimerDialog) {
            SleepTimerDialog(
                activeRemainingSeconds = playbackState.sleepTimerSecondsRemaining,
                onSetTimer = onSetSleepTimer,
                onDismiss = { showTimerDialog = false }
            )
        }
    }
}

/**
 * Reusable Apple-styled row for upcoming songs with 3-dot menu supporting "Play Next"
 */
@Composable
private fun UpcomingSongItemRow(
    song: AudioTrack,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onPlayNow() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF28282C)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.albumArtUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = AppleCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${FormatUtils.formatDuration(song.durationMs)}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3-Dots Menu with Play Next action
        Box {
            IconButton(
                onClick = {
                    HapticHelper.tap(context)
                    showMenu = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF24242A))
            ) {
                DropdownMenuItem(
                    text = { Text("Play Next", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onPlayNext()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = AppleCrimson
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Play Now", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onPlayNow()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("QuickShare", color = Color.White) },
                    onClick = {
                        showMenu = false
                        HapticHelper.tap(context)
                        ShareHelper.shareTrack(context, song)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = AppleCrimson
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Remove from Queue", color = Color(0xFFFF453A)) },
                    onClick = {
                        showMenu = false
                        onRemove()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF453A)
                        )
                    }
                )
            }
        }
    }
}
