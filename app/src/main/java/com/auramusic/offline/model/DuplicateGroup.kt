package com.auramusic.offline.model

data class DuplicateGroup(
    val titleKey: String,
    val tracks: List<AudioTrack>,
    val totalWastedBytes: Long
)
