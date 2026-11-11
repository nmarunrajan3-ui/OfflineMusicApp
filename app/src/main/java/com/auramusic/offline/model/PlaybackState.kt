package com.auramusic.offline.model

data class PlaybackState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val sleepTimerSecondsRemaining: Int? = null,
    val dominantColor: Long? = null,
    val secondaryColor: Long? = null,
    val queue: List<AudioTrack> = emptyList(),
    val currentQueueIndex: Int = 0
)
