package com.auramusic.offline.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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

class MusicPlaybackService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var currentPlaylist: List<AudioTrack> = emptyList()
    private var currentTrackIndex: Int = -1
    private var shuffleIndices: List<Int> = emptyList()
    private var isPrepared = false

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
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}

        isPrepared = false
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(this@MusicPlaybackService)
            setOnCompletionListener(this@MusicPlaybackService)
            setOnErrorListener(this@MusicPlaybackService)
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

    fun restoreTrackWithoutPlaying(track: AudioTrack, playlist: List<AudioTrack>) {
        currentPlaylist = playlist
        currentTrackIndex = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        generateShuffleList()

        serviceScope.launch {
            val (primary, secondary) = ColorExtractor.extractColorsFromUri(this@MusicPlaybackService, track.albumArtUri)
            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                isPlaying = false,
                currentPositionMs = recentRepo.getLastPosition(),
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
            requestAudioFocus()
            initMediaPlayer()

            val file = File(track.dataPath)
            val uri: Uri = if (file.exists() && file.length() > 0) {
                Uri.fromFile(file)
            } else if (track.contentUriString.isNotBlank()) {
                try {
                    val parsed = Uri.parse(track.contentUriString)
                    contentResolver.openInputStream(parsed)?.close()
                    parsed
                } catch (e: Exception) {
                    Uri.fromFile(AudioSynthFallback.getOrCreateDemoAudioFile(this))
                }
            } else {
                Uri.fromFile(AudioSynthFallback.getOrCreateDemoAudioFile(this))
            }

            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepareAsync()

            recentRepo.recordTrackPlayed(track.id)

            // Extract dynamic Apple Music palette
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
            isPrepared = false
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        isPrepared = true
        mp?.start()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            durationMs = mp?.duration?.toLong() ?: 0L,
            queue = currentPlaylist,
            currentQueueIndex = currentTrackIndex
        )
        startProgressTracking()
        updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING, mp?.currentPosition?.toLong() ?: 0L)
        updateNotification()
    }

    fun play() {
        val current = _playbackState.value.currentTrack
        if (mediaPlayer == null) {
            initMediaPlayer()
        }

        if (current != null) {
            if (isPrepared && mediaPlayer != null) {
                try {
                    requestAudioFocus()
                    mediaPlayer?.start()
                    _playbackState.value = _playbackState.value.copy(isPlaying = true)
                    startProgressTracking()
                    updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer?.currentPosition?.toLong() ?: 0L)
                    updateNotification()
                    return
                } catch (_: Exception) {
                    // Fall back to clean reload
                }
            }
            loadAndPlay(current)
        } else if (currentPlaylist.isNotEmpty()) {
            playTrackList(currentPlaylist, 0)
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}

        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        stopProgressTracking()
        updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer?.currentPosition?.toLong() ?: 0L)
        updateNotification()
        recentRepo.saveLastPosition(mediaPlayer?.currentPosition?.toLong() ?: 0L)
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun next() {
        if (currentPlaylist.isEmpty()) return
        val repeat = _playbackState.value.repeatMode
        if (repeat == RepeatMode.ONE) {
            loadAndPlay(currentPlaylist[currentTrackIndex])
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
                seekTo(0)
                return
            }
        }
        loadAndPlay(currentPlaylist[currentTrackIndex])
    }

    fun previous() {
        if (currentPlaylist.isEmpty()) return
        // If current position is > 3 seconds, rewind to start like Apple Music
        val currentPos = mediaPlayer?.currentPosition ?: 0
        if (currentPos > 3000) {
            seekTo(0)
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
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (_: Exception) {}
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

            // Smooth fade out over 3 seconds before pausing
            if (remaining <= 0) {
                fadeOutAndPause()
                _playbackState.value = _playbackState.value.copy(sleepTimerSecondsRemaining = null)
            }
        }
    }

    private suspend fun fadeOutAndPause() {
        try {
            val steps = 10
            for (i in steps downTo 0) {
                val volume = i.toFloat() / steps.toFloat()
                mediaPlayer?.setVolume(volume, volume)
                delay(250L)
            }
            pause()
            mediaPlayer?.setVolume(1.0f, 1.0f) // reset volume for next play
        } catch (e: Exception) {
            pause()
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (_playbackState.value.repeatMode == RepeatMode.ONE) {
            seekTo(0)
            play()
        } else {
            next()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        isPrepared = false
        initMediaPlayer()
        return true
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressTrackingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _playbackState.value = _playbackState.value.copy(currentPositionMs = current)
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

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            try { mediaPlayer?.setVolume(0.2f, 0.2f) } catch (_: Exception) {}
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            try { mediaPlayer?.setVolume(1.0f, 1.0f) } catch (_: Exception) {}
                            play()
                        }
                    }
                }
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!)
        }
    }

    fun stopService() {
        stopProgressTracking()
        sleepTimerJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPrepared = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopProgressTracking()
        sleepTimerJob?.cancel()
        mediaSession?.release()
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPrepared = false
        super.onDestroy()
    }
}
