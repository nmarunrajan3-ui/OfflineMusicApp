package com.auramusic.offline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.auramusic.offline.theme.AuraMusicTheme
import com.auramusic.offline.ui.HomeScreen
import com.auramusic.offline.ui.NowPlayingFullScreen
import com.auramusic.offline.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        if (granted) {
            viewModel.loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val playbackState by viewModel.playbackState.collectAsState()
            var isPlayerExpanded by remember { mutableStateOf(false) }

            // Handle back press to collapse full player like iOS
            BackHandler(enabled = isPlayerExpanded) {
                isPlayerExpanded = false
            }

            AuraMusicTheme(darkTheme = isDarkMode) {
                SideEffect {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = !isDarkMode
                    insetsController.isAppearanceLightNavigationBars = !isDarkMode
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main App Library Screen
                        HomeScreen(
                            viewModel = viewModel,
                            onExpandPlayer = { isPlayerExpanded = true }
                        )

                        // Full Screen Apple Music Player Sheet (100% Solid & Opaque)
                        AnimatedVisibility(
                            visible = isPlayerExpanded && playbackState.currentTrack != null,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(320)
                            ) + fadeIn(animationSpec = tween(250)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(220))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black) // Solid backing
                            ) {
                                playbackState.currentTrack?.let { track ->
                                    NowPlayingFullScreen(
                                        playbackState = playbackState,
                                        isFavorite = track.isFavorite,
                                        onDismiss = { isPlayerExpanded = false },
                                        onPlayPauseClick = { viewModel.togglePlayPause() },
                                        onPreviousClick = { viewModel.previous() },
                                        onNextClick = { viewModel.next() },
                                        onSeekTo = { viewModel.seekTo(it) },
                                        onToggleShuffle = { viewModel.toggleShuffle() },
                                        onToggleRepeat = { viewModel.toggleRepeat() },
                                        onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                        onSetSleepTimer = { viewModel.setSleepTimer(it) },
                                        onPlayTrackFromQueue = { queueTrack ->
                                            viewModel.playTrack(queueTrack)
                                        },
                                        onPlayNext = { queueTrack ->
                                            viewModel.playNext(queueTrack)
                                        },
                                        onRemoveFromQueue = { index ->
                                            viewModel.removeFromQueue(index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
