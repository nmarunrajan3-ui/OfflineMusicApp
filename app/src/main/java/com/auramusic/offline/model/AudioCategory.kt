package com.auramusic.offline.model

/**
 * Strict category separation to prevent mingling music with recordings or calls.
 */
enum class AudioCategory {
    MUSIC,
    VOICE_RECORDING,
    CALL_RECORD,
    RECYCLE_BIN
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
