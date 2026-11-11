package com.auramusic.offline.model

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dataPath: String,
    val contentUriString: String,
    val albumId: Long,
    val albumArtUri: String? = null,
    val size: Long = 0L,
    val dateAdded: Long = 0L,
    val mimeType: String? = null,
    val category: AudioCategory = AudioCategory.MUSIC,
    val isFavorite: Boolean = false,
    val dateTrashed: Long? = null
)
