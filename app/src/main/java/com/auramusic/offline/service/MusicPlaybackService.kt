package com.auramusic.offline.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.auramusic.offline.data.RecentlyPlayedRepository
import com.auramusic.offline.model.AudioTrack
import com.auramusic.offline.model.PlaybackState
import com.auramusic.offline.model.RepeatMode
import com.auramusic.offline.util.AudioSynthFallback
import com.auramusic.offline.util.ColorExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class MusicPlaybackService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSessionCompat? = null

    private var currentPlaylist: List<AudioTrack> = emptyList()
    private var currentTrackIndex: Int = -1
    private var shuffleIndices: List<Int> = emptyList()

    private var progressTrackingJob: Job? = null
    private var sleepTimerJob: Job? = null

    private lateinit var recentRepo: RecentlyPlayedRepository

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    companion object {
        private val _playbackState = MutableStateFlow(PlaybackState())
        val playbackState = _playbackState.asStateFlow()

        fun updateDominantColors(primary: Long, secondary: Long) {
            _playbackState.value = _playbackState.value.copy(
                dominantColor = primary,
                secondaryColor = secondary
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        recentRepo = RecentlyPlayedRepository(this)

        MediaNotificationHelper.createNotificationChannel(this)

        mediaSession = MediaSessionCompat(this, "AuraMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { play() }
                override fun onPause() { pause() }
                override fun onSkipToNext() { next() }
                override fun onSkipToPrevious() { previous() }
                override fun onSeekTo(pos: Long) { seekTo(pos) }
                override fun onStop() { stopService() }
            })
            isActive = true
        }

        initExoPlayer()
    }

    private fun initExoPlayer() {
        try {
            exoPlayer?.release()
        } catch (_: Exception) {}

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                val dur = duration.coerceAtLeast(0L)
                                _playbackState.value = _playbackState.value.copy(
                                    durationMs = dur,
                                    queue = currentPlaylist,
                                    currentQueueIndex = currentTrackIndex
                                )
                                updateMediaSessionState(
                                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                                    currentPosition
                                )
                            }
                            Player.STATE_ENDED -> {
                                if (_playbackState.value.repeatMode == RepeatMode.ONE) {
                                    this@MusicPlaybackService.seekTo(0L)
                                    this@MusicPlaybackService.play()
                                } else {
                                    this@MusicPlaybackService.next()
                                }
                            }
                            Player.STATE_BUFFERING -> {
                                updateMediaSessionState(PlaybackStateCompat.STATE_BUFFERING, currentPosition)
                            }
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) {
                            startProgressTracking()
                            updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING, currentPosition)
                        } else {
                            stopProgressTracking()
                            updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED, currentPosition)
                            recentRepo.saveLastPosition(currentPosition)
                        }
                        updateNotification()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        error.printStackTrace()
                    }
                })
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MediaNotificationHelper.ACTION_PLAY -> play()
            MediaNotificationHelper.ACTION_PAUSE -> pause()
            MediaNotificationHelper.ACTION_TOGGLE -> togglePlayPause()
            MediaNotificationHelper.ACTION_PREV -> previous()
            MediaNotificationHelper.ACTION_NEXT -> next()
            MediaNotificationHelper.ACTION_STOP -> stopService()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun playTrackList(tracks: List<AudioTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        currentPlaylist = tracks
        currentTrackIndex = startIndex.coerceIn(0, tracks.lastIndex)
        generateShuffleList()
        loadAndPlay(currentPlaylist[currentTrackIndex])
    }

    fun playSingleTrack(track: AudioTrack) {
        val index = currentPlaylist.indexOfFirst { it.id == track.id }
        if (index != -1) {
            currentTrackIndex = index
            loadAndPlay(currentPlaylist[currentTrackIndex])
        } else {
            playTrackList(listOf(track), 0)
        }
    }

    fun playNext(track: AudioTrack) {
        val list = currentPlaylist.toMutableList()
        val insertIndex = (currentTrackIndex + 1).coerceIn(0, list.size)
        // Remove if already present further down so it moves to next
        val existingIndex = list.indexOfFirst { it.id == track.id }
        if (existingIndex > currentTrackIndex) {
            list.removeAt(existingIndex)
        }
        list.add(insertIndex, track)
        currentPlaylist = list
        generateShuffleList()
        _playbackState.value = _playbackState.value.copy(
            queue = currentPlaylist,
            currentQueueIndex = currentTrackIndex
        )
    }

    fun addToQueue(track: AudioTrack) {
        val list = currentPlaylist.toMutableList()
        if (list.none { it.id == track.id }) {
            list.add(track)
            currentPlaylist = list
            generateShuffleList()
            _playbackState.value = _playbackState.value.copy(
                queue = currentPlaylist,
                currentQueueIndex = currentTrackIndex
            )
        }
    }

    fun removeFromQueue(index: Int) {
        if (index in currentPlaylist.indices && index != currentTrackIndex) {
            val list = currentPlaylist.toMutableList()
            list.removeAt(index)
            if (index < currentTrackIndex) {
                currentTrackIndex--
            }
            currentPlaylist = list
            generateShuffleList()
            _playbackState.value = _playbackState.value.copy(
                queue = currentPlaylist,
                currentQueueIndex = currentTrackIndex
            )
        }
    }

    private fun resolveTrackUri(track: AudioTrack): Uri {
        val file = File(track.dataPath)
        return if (file.exists() && file.length() > 0) {
            Uri.fromFile(file)
        } else if (track.contentUriString.isNotBlank()) {
            try {
                val parsed = Uri.parse(track.contentUriString)
                contentResolver.openInputStream(parsed)?.close()
                parsed
            } catch (_: Exception) {
                Uri.fromFile(AudioSynthFallback.getOrCreateDemoAudioFile(this))
            }
        } else {
            Uri.fromFile(AudioSynthFallback.getOrCreateDemoAudioFile(this))
        }
    }

    fun restoreTrackWithoutPlaying(track: AudioTrack, playlist: List<AudioTrack>) {
        currentPlaylist = playlist
        currentTrackIndex = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        generateShuffleList()

        if (exoPlayer == null) {
            initExoPlayer()
        }

        val lastPos = recentRepo.getLastPosition().coerceAtLeast(0L)
        try {
            val uri = resolveTrackUri(track)
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                pause()
                if (lastPos > 0) {
                    seekTo(lastPos)
                }
            }
        } catch (_: Exception) {}

        serviceScope.launch {
            val (primary, secondary) = ColorExtractor.extractColorsFromUri(this@MusicPlaybackService, track.albumArtUri)
            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                isPlaying = false,
                currentPositionMs = lastPos,
                durationMs = track.durationMs,
                dominantColor = primary,
                secondaryColor = secondary,
                queue = currentPlaylist,
                currentQueueIndex = currentTrackIndex
            )
        }
    }

    private fun loadAndPlay(track: AudioTrack) {
        try {
            if (exoPlayer == null) {
                initExoPlayer()
            }

            val uri: Uri = resolveTrackUri(track)
            val mediaItem = MediaItem.fromUri(uri)

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            recentRepo.recordTrackPlayed(track.id)

            // Dynamic Apple Music gradient palette extraction
            serviceScope.launch {
                val (primary, secondary) = ColorExtractor.extractColorsFromUri(this@MusicPlaybackService, track.albumArtUri)
                _playbackState.value = _playbackState.value.copy(
                    currentTrack = track,
                    isPlaying = true,
                    currentPositionMs = 0L,
                    durationMs = track.durationMs,
                    dominantColor = primary,
                    secondaryColor = secondary,
                    queue = currentPlaylist,
                    currentQueueIndex = currentTrackIndex
                )
            }

            updateMediaSessionState(PlaybackStateCompat.STATE_BUFFERING, 0L)
            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        if (exoPlayer == null) {
            initExoPlayer()
        }

        val p = exoPlayer ?: return
        val current = _playbackState.value.currentTrack

        if (p.playbackState == Player.STATE_ENDED) {
            p.seekTo(0L)
            p.play()
            return
        }

        if (p.mediaItemCount > 0) {
            p.play()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracking()
            updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING, p.currentPosition)
            updateNotification()
        } else if (current != null) {
            loadAndPlay(current)
        } else if (currentPlaylist.isNotEmpty()) {
            playTrackList(currentPlaylist, 0)
        }
    }

    fun pause() {
        val p = exoPlayer
        p?.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        stopProgressTracking()
        val pos = p?.currentPosition ?: 0L
        updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED, pos)
        updateNotification()
        recentRepo.saveLastPosition(pos)
    }

    fun togglePlayPause() {
        val p = exoPlayer
        if (p != null && p.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun next() {
        if (currentPlaylist.isEmpty()) return
        val repeat = _playbackState.value.repeatMode

        if (repeat == RepeatMode.ONE) {
            exoPlayer?.seekTo(0L)
            exoPlayer?.play()
            return
        }

        if (_playbackState.value.isShuffle) {
            val nextShuffleIndex = (shuffleIndices.indexOf(currentTrackIndex) + 1) % shuffleIndices.size
            currentTrackIndex = shuffleIndices[nextShuffleIndex]
        } else {
            if (currentTrackIndex < currentPlaylist.lastIndex) {
                currentTrackIndex++
            } else if (repeat == RepeatMode.ALL) {
                currentTrackIndex = 0
            } else {
                pause()
                seekTo(0L)
                return
            }
        }
        loadAndPlay(currentPlaylist[currentTrackIndex])
    }

    fun previous() {
        if (currentPlaylist.isEmpty()) return
        val currentPos = exoPlayer?.currentPosition ?: 0L
        if (currentPos > 3000L) {
            seekTo(0L)
            return
        }

        if (_playbackState.value.isShuffle) {
            val prevShuffleIndex = (shuffleIndices.indexOf(currentTrackIndex) - 1 + shuffleIndices.size) % shuffleIndices.size
            currentTrackIndex = shuffleIndices[prevShuffleIndex]
        } else {
            if (currentTrackIndex > 0) {
                currentTrackIndex--
            } else {
                currentTrackIndex = currentPlaylist.lastIndex
            }
        }
        loadAndPlay(currentPlaylist[currentTrackIndex])
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        updateMediaSessionState(
            if (_playbackState.value.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
            positionMs
        )
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffle
        _playbackState.value = _playbackState.value.copy(isShuffle = newShuffle)
        if (newShuffle) generateShuffleList()
    }

    fun toggleRepeat() {
        val nextRepeat = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = nextRepeat)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _playbackState.value = _playbackState.value.copy(sleepTimerSecondsRemaining = null)
            return
        }

        val totalSeconds = minutes * 60
        _playbackState.value = _playbackState.value.copy(sleepTimerSecondsRemaining = totalSeconds)

        sleepTimerJob = serviceScope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _playbackState.value = _playbackState.value.copy(sleepTimerSecondsRemaining = remaining)
            }

            // Smooth fade out over 2.5 seconds before pausing
            if (remaining <= 0) {
                fadeOutAndPause()
                _playbackState.value = _playbackState.value.copy(sleepTimerSecondsRemaining = null)
            }
        }
    }

    private suspend fun fadeOutAndPause() {
        try {
            val steps = 10
            val p = exoPlayer
            for (i in steps downTo 0) {
                val volume = i.toFloat() / steps.toFloat()
                p?.volume = volume
                delay(250L)
            }
            pause()
            p?.volume = 1.0f // reset volume for next play
        } catch (_: Exception) {
            pause()
        }
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressTrackingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val p = exoPlayer
                    if (p != null) {
                        val current = p.currentPosition
                        _playbackState.value = _playbackState.value.copy(currentPositionMs = current)
                    }
                } catch (_: Exception) {}
                delay(400L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    private fun generateShuffleList() {
        if (currentPlaylist.isEmpty()) return
        val indices = currentPlaylist.indices.toMutableList()
        indices.shuffle(Random(System.currentTimeMillis()))
        shuffleIndices = indices
    }

    private fun updateNotification() {
        mediaSession?.let { session ->
            val notif = MediaNotificationHelper.buildNotification(
                this,
                _playbackState.value.currentTrack,
                _playbackState.value.isPlaying,
                session
            )
            startForeground(MediaNotificationHelper.NOTIFICATION_ID, notif)
        }
    }

    private fun updateMediaSessionState(state: Int, position: Long) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO

        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, 1.0f)
                .build()
        )
    }

    fun stopService() {
        stopProgressTracking()
        sleepTimerJob?.cancel()
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (_: Exception) {}
        exoPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopProgressTracking()
        sleepTimerJob?.cancel()
        mediaSession?.release()
        try {
            exoPlayer?.release()
        } catch (_: Exception) {}
        exoPlayer = null
        super.onDestroy()
    }
}
