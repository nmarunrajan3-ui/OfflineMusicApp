@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.auramusic.offline.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.offline.model.AudioTrack
import com.auramusic.offline.model.CustomAlbum
import com.auramusic.offline.theme.AppleCrimson
import com.auramusic.offline.ui.components.AlphabetFastScroller
import com.auramusic.offline.ui.components.AppleTrackItem
import com.auramusic.offline.ui.components.BatchSelectionBar
import com.auramusic.offline.ui.components.CreateAlbumDialog
import com.auramusic.offline.ui.components.RecentlyPlayedCarousel
import com.auramusic.offline.ui.components.bounceClick
import com.auramusic.offline.util.HapticHelper
import com.auramusic.offline.util.ShareHelper
import com.auramusic.offline.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onExpandPlayer: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var currentDestination by remember { mutableStateOf(DrawerDestination.HOME) }
    var selectedAlbumForDetail by remember { mutableStateOf<CustomAlbum?>(null) }

    // Dialog states
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var trackToAddToAlbum by remember { mutableStateOf<AudioTrack?>(null) }
    var showAddSongsToAlbumDialog by remember { mutableStateOf(false) }

    // Recycle Bin Confirmation State ("before movee to recycle bin and deleting ask again")
    var tracksToTrashConfirmation by remember { mutableStateOf<List<AudioTrack>?>(null) }

    // VM states
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val musicTracks by viewModel.musicTracks.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayedTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val customAlbums by viewModel.customAlbums.collectAsState()
    val voiceRecordings by viewModel.voiceRecordings.collectAsState()
    val callRecords by viewModel.callRecords.collectAsState()
    val recycleBinTracks by viewModel.recycleBinTracks.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsState()

    val tabs = listOf("All Tracks", "Favorites", "Custom Albums")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    val listState = rememberLazyListState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = (pagerState.currentPage == 0),
        drawerContent = {
            DrawerSheetContent(
                currentDestination = currentDestination,
                isDarkMode = isDarkMode,
                recycleBinCount = recycleBinTracks.size,
                duplicateCount = duplicateGroups.size,
                recordingsCount = voiceRecordings.size,
                callRecordsCount = callRecords.size,
                onNavigate = { dest ->
                    HapticHelper.tap(context)
                    currentDestination = dest
                    coroutineScope.launch { drawerState.close() }
                },
                onToggleTheme = {
                    HapticHelper.mediumClick(context)
                    viewModel.toggleDarkMode()
                },
                onRescanClick = {
                    HapticHelper.tap(context)
                    viewModel.loadData()
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                // Persistent currently played or last played song bar
                Column {
                    if (isSelectionMode) {
                        BatchSelectionBar(
                            selectedCount = selectedTrackIds.size,
                            totalCount = musicTracks.size,
                            onSelectAllClick = { viewModel.selectAll(musicTracks) },
                            onDeselectAllClick = { viewModel.deselectAll() },
                            onToggleFavoriteClick = { viewModel.toggleFavoritesForSelected() },
                            onQuickShareClick = { viewModel.shareSelectedTracks(context) },
                            onAddToAlbumClick = {
                                if (customAlbums.isNotEmpty()) {
                                    val firstTrack = musicTracks.firstOrNull { selectedTrackIds.contains(it.id) }
                                    trackToAddToAlbum = firstTrack
                                }
                            },
                            onMoveToTrashClick = {
                                val toTrash = musicTracks.filter { selectedTrackIds.contains(it.id) }
                                if (toTrash.isNotEmpty()) {
                                    tracksToTrashConfirmation = toTrash
                                }
                            },
                            onCloseClick = { viewModel.clearSelection() }
                        )
                    }

                    if (playbackState.currentTrack != null) {
                        NowPlayingBottomBar(
                            playbackState = playbackState,
                            onExpandClick = onExpandPlayer,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onPreviousClick = { viewModel.previous() },
                            onNextClick = { viewModel.next() }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(pagerState.currentPage, currentDestination) {
                        if (pagerState.currentPage == 0 && currentDestination == DrawerDestination.HOME) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (dragAmount > 25f && change.position.x < 140f) {
                                    change.consume()
                                    coroutineScope.launch { drawerState.open() }
                                }
                            }
                        }
                    }
            ) {
                when {
                    selectedAlbumForDetail != null -> {
                        CustomAlbumDetailScreen(
                            album = selectedAlbumForDetail!!,
                            allMusicTracks = musicTracks,
                            currentlyPlayingTrackId = playbackState.currentTrack?.id,
                            onBackClick = { selectedAlbumForDetail = null },
                            onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                            onShufflePlay = { list ->
                                viewModel.toggleShuffle()
                                if (list.isNotEmpty()) viewModel.playTrack(list.first(), list)
                            },
                            onAddSongsClick = { showAddSongsToAlbumDialog = true },
                            onRemoveTrackFromAlbum = { trackId ->
                                viewModel.removeTrackFromAlbum(selectedAlbumForDetail!!.id, trackId)
                                selectedAlbumForDetail = customAlbums.find { it.id == selectedAlbumForDetail!!.id }
                            },
                            onDeleteAlbum = { albumId ->
                                viewModel.deleteCustomAlbum(albumId)
                                selectedAlbumForDetail = null
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onPlayNext = { viewModel.playNext(it) },
                            onAddToQueue = { viewModel.addToQueue(it) }
                        )
                    }

                    currentDestination == DrawerDestination.RECYCLE_BIN -> {
                        RecycleBinScreen(
                            trashedTracks = recycleBinTracks,
                            onBackClick = { currentDestination = DrawerDestination.HOME },
                            onRestoreTrack = { viewModel.restoreFromRecycleBin(it) },
                            onDeletePermanently = { viewModel.deletePermanently(it) },
                            onEmptyBin = { viewModel.emptyRecycleBin() }
                        )
                    }

                    currentDestination == DrawerDestination.FIND_DUPLICATES -> {
                        DuplicateFinderScreen(
                            duplicateGroups = duplicateGroups,
                            onBackClick = { currentDestination = DrawerDestination.HOME },
                            onCleanDuplicates = { viewModel.cleanDuplicates(duplicateGroups) }
                        )
                    }

                    currentDestination == DrawerDestination.RECORDINGS -> {
                        RecordingsScreen(
                            recordings = voiceRecordings,
                            currentlyPlayingTrackId = playbackState.currentTrack?.id,
                            onBackClick = { currentDestination = DrawerDestination.HOME },
                            onPlayTrack = { viewModel.playTrack(it, voiceRecordings) }
                        )
                    }

                    currentDestination == DrawerDestination.CALL_RECORDS -> {
                        CallRecordsScreen(
                            callRecords = callRecords,
                            currentlyPlayingTrackId = playbackState.currentTrack?.id,
                            onBackClick = { currentDestination = DrawerDestination.HOME },
                            onPlayTrack = { viewModel.playTrack(it, callRecords) }
                        )
                    }

                    currentDestination == DrawerDestination.SETTINGS -> {
                        SettingsScreen(
                            isDarkMode = isDarkMode,
                            musicCount = musicTracks.size,
                            recordingsCount = voiceRecordings.size,
                            callRecordsCount = callRecords.size,
                            recycleBinCount = recycleBinTracks.size,
                            sleepTimerSecondsRemaining = playbackState.sleepTimerSecondsRemaining,
                            onBackClick = { currentDestination = DrawerDestination.HOME },
                            onToggleTheme = { viewModel.toggleDarkMode() },
                            onSetSleepTimer = { viewModel.setSleepTimer(it) },
                            onRescanClick = { viewModel.loadData() }
                        )
                    }

                    else -> {
                        // MAIN HOME SCREEN: Continuous unified Apple Music scroll
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Bar Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 3-Bar Toggle Button (Doggle)
                                    IconButton(
                                        onClick = {
                                            HapticHelper.tap(context)
                                            coroutineScope.launch { drawerState.open() }
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Menu,
                                            contentDescription = "Drawer Menu",
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // App Name
                                    Text(
                                        text = "Aura Music",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Auto Search Button
                                    IconButton(
                                        onClick = {
                                            HapticHelper.tap(context)
                                            isSearchActive = !isSearchActive
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = "Search",
                                            tint = if (isSearchActive) AppleCrimson else MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    // Theme Toggle Button
                                    IconButton(
                                        onClick = {
                                            HapticHelper.mediumClick(context)
                                            viewModel.toggleDarkMode()
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                            contentDescription = "Theme",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    // Multi-select / Select All button
                                    IconButton(
                                        onClick = {
                                            HapticHelper.mediumClick(context)
                                            viewModel.toggleSelectionMode()
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Checklist,
                                            contentDescription = "Multi Select",
                                            tint = if (isSelectionMode) AppleCrimson else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }

                            // Auto-search field everywhere
                            AnimatedVisibility(visible = isSearchActive) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.searchQuery.value = it },
                                        placeholder = { Text("Auto search tracks, artists, albums...") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Search, contentDescription = null, tint = AppleCrimson)
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // PROMINENT SELECT ALL HEADER WHEN IN SELECTION MODE
                            AnimatedVisibility(visible = isSelectionMode) {
                                val isAll = selectedTrackIds.size == musicTracks.size && musicTracks.isNotEmpty()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AppleCrimson.copy(alpha = 0.12f))
                                        .clickable {
                                            HapticHelper.mediumClick(context)
                                            if (isAll) viewModel.deselectAll() else viewModel.selectAll(musicTracks)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = AppleCrimson,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isAll) "Deselect All (${selectedTrackIds.size})" else "Select All (${selectedTrackIds.size}/${musicTracks.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AppleCrimson
                                        )
                                    }

                                    Text(
                                        text = if (isAll) "Tap to uncheck" else "Tap to select all songs",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Cupertino Segmented Control (Apple Music iOS Style)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                            .clickable {
                                                HapticHelper.lightTick(context)
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // CONTENT SECTION: Horizontal Pager for Fluid Swipe Navigation
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = true
                            ) { page ->
                                when (page) {
                                    // TAB 0: ALL TRACKS — Continuous scroll including Recently Played!
                                    0 -> {
                                    if (isLoading) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = AppleCrimson)
                                        }
                                    } else if (musicTracks.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No music tracks found in library.")
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            // Unified continuous scroll
                                            LazyColumn(
                                                state = listState,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(end = 28.dp) // Leave rail space for alphabet track
                                            ) {
                                                // ITEM 1: TOP 5 RECENTLY PLAYED CAROUSEL (Scrolls continuously into all tracks!)
                                                if (!isSearchActive && recentlyPlayed.isNotEmpty()) {
                                                    item(key = "recently_played_header") {
                                                        RecentlyPlayedCarousel(
                                                            recentTracks = recentlyPlayed,
                                                            currentlyPlayingTrackId = playbackState.currentTrack?.id,
                                                            onTrackClick = { viewModel.playTrack(it, musicTracks) },
                                                            modifier = Modifier.padding(bottom = 6.dp)
                                                        )
                                                    }
                                                }

                                                // ITEM 2: ALL SONGS SECTION LABEL
                                                item(key = "all_tracks_header_label") {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 20.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "ALL TRACKS",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${musicTracks.size} songs",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                // ALL TRACKS ITEMS
                                                items(musicTracks, key = { it.id }) { track ->
                                                    AppleTrackItem(
                                                        track = track,
                                                        isCurrentlyPlaying = track.id == playbackState.currentTrack?.id,
                                                        isSelected = selectedTrackIds.contains(track.id),
                                                        isSelectionMode = isSelectionMode,
                                                        onClick = { viewModel.playTrack(track, musicTracks) },
                                                        onToggleSelect = { viewModel.toggleSelectTrack(track.id) },
                                                        onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                                        onAddToAlbumClick = { trackToAddToAlbum = track },
                                                        onMoveToTrashClick = {
                                                            // Ask again before moving to recycle bin!
                                                            tracksToTrashConfirmation = listOf(track)
                                                        },
                                                        onPlayNextClick = { viewModel.playNext(track) },
                                                        onAddToQueueClick = { viewModel.addToQueue(track) },
                                                        onLongClick = { viewModel.startSelectionWith(track.id) }
                                                    )
                                                }

                                                item(key = "bottom_spacer") {
                                                    Spacer(modifier = Modifier.height(90.dp))
                                                }
                                            }

                                            // A-Z Alphabet Scroller with haptic tick and header item offset
                                            val headerOffset = if (!isSearchActive && recentlyPlayed.isNotEmpty()) 2 else 1
                                            AlphabetFastScroller(
                                                tracks = musicTracks,
                                                listState = listState,
                                                headerOffsetCount = headerOffset,
                                                modifier = Modifier.align(Alignment.CenterEnd)
                                            )
                                        }
                                    }
                                }

                                // TAB 1: FAVORITES TAB
                                1 -> {
                                    if (favoriteTracks.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No favorites yet. Tap the heart on any song to save it here!",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(favoriteTracks, key = { it.id }) { track ->
                                                AppleTrackItem(
                                                    track = track,
                                                    isCurrentlyPlaying = track.id == playbackState.currentTrack?.id,
                                                    isSelected = selectedTrackIds.contains(track.id),
                                                    isSelectionMode = isSelectionMode,
                                                    onClick = { viewModel.playTrack(track, favoriteTracks) },
                                                    onToggleSelect = { viewModel.toggleSelectTrack(track.id) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                                    onAddToAlbumClick = { trackToAddToAlbum = track },
                                                    onMoveToTrashClick = {
                                                        tracksToTrashConfirmation = listOf(track)
                                                    },
                                                    onPlayNextClick = { viewModel.playNext(track) },
                                                    onAddToQueueClick = { viewModel.addToQueue(track) },
                                                    onLongClick = { viewModel.startSelectionWith(track.id) }
                                                )
                                            }
                                            item {
                                                Spacer(modifier = Modifier.height(90.dp))
                                            }
                                        }
                                    }
                                }

                                // TAB 2: CUSTOM ALBUMS TAB
                                2 -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "YOUR ALBUMS (${customAlbums.size})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Button(
                                                onClick = {
                                                    HapticHelper.tap(context)
                                                    showCreateAlbumDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("New Album", fontSize = 12.sp)
                                            }
                                        }

                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            contentPadding = PaddingValues(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(customAlbums, key = { it.id }) { album ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable {
                                                            HapticHelper.mediumClick(context)
                                                            selectedAlbumForDetail = album
                                                        }
                                                        .padding(10.dp)
                                                ) {
                                                    Column {
                                                        // Custom photo or artistic gradient
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(1f)
                                                                .shadow(6.dp, RoundedCornerShape(14.dp))
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(
                                                                    Brush.linearGradient(
                                                                        listOf(AppleCrimson, Color(0xFF5856D6))
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
                                                                    modifier = Modifier.size(48.dp)
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(10.dp))

                                                        Text(
                                                            text = album.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        Text(
                                                            text = "${album.trackIds.size} songs",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // CONFIRMATION DIALOG: Before moving to recycle bin ask again!
    if (tracksToTrashConfirmation != null) {
        val count = tracksToTrashConfirmation!!.size
        val firstTitle = tracksToTrashConfirmation!!.first().title

        AlertDialog(
            onDismissRequest = { tracksToTrashConfirmation = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Move to Recycle Bin?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            },
            text = {
                Text(
                    text = if (count == 1) {
                        "Are you sure you want to move \"$firstTitle\" to the Recycle Bin? You can restore it anytime from the 3-bar menu."
                    } else {
                        "Are you sure you want to move $count songs to the Recycle Bin? You can restore them anytime from the 3-bar menu."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticHelper.alert(context)
                        viewModel.moveToRecycleBin(tracksToTrashConfirmation!!)
                        tracksToTrashConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Move to Recycle Bin")
                }
            },
            dismissButton = {
                TextButton(onClick = { tracksToTrashConfirmation = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Dialog to create new custom album with custom photo
    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateAlbumDialog = false },
            onCreateAlbum = { name, desc, photoUri ->
                viewModel.createCustomAlbum(name, desc, photoUri)
            }
        )
    }

    // Dialog to add track to an existing custom album
    if (trackToAddToAlbum != null) {
        AlertDialog(
            onDismissRequest = { trackToAddToAlbum = null },
            title = { Text("Add to Custom Album") },
            text = {
                Column {
                    if (customAlbums.isEmpty()) {
                        Text("No custom albums found. Create one first!")
                    } else {
                        customAlbums.forEach { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTracksToAlbum(album.id, listOf(trackToAddToAlbum!!.id))
                                        trackToAddToAlbum = null
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Album, contentDescription = null, tint = AppleCrimson)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(album.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { trackToAddToAlbum = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog to select tracks to add to the currently viewed album
    if (showAddSongsToAlbumDialog && selectedAlbumForDetail != null) {
        val availableToAdd = musicTracks.filter { !selectedAlbumForDetail!!.trackIds.contains(it.id) }
        var selectedIds by remember { mutableStateOf(setOf<Long>()) }

        AlertDialog(
            onDismissRequest = { showAddSongsToAlbumDialog = false },
            title = { Text("Add Songs to ${selectedAlbumForDetail!!.name}") },
            text = {
                if (availableToAdd.isEmpty()) {
                    Text("All songs in your library are already in this album.")
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(availableToAdd, key = { it.id }) { track ->
                            val isChecked = selectedIds.contains(track.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (isChecked) selectedIds - track.id else selectedIds + track.id
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isChecked) "☑" else "☐",
                                    fontSize = 18.sp,
                                    color = if (isChecked) AppleCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(track.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addTracksToAlbum(selectedAlbumForDetail!!.id, selectedIds.toList())
                        selectedAlbumForDetail = customAlbums.find { it.id == selectedAlbumForDetail!!.id }
                        showAddSongsToAlbumDialog = false
                    },
                    enabled = selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson)
                ) {
                    Text("Add ${selectedIds.size} Song(s)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongsToAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
