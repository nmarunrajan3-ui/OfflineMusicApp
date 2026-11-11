package com.auramusic.offline.data

import com.auramusic.offline.model.AudioTrack
import com.auramusic.offline.model.DuplicateGroup
import java.util.Locale

object DuplicateFinder {

    fun findDuplicates(tracks: List<AudioTrack>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()

        // 1. Group by normalized title + artist
        val titleArtistMap = mutableMapOf<String, MutableList<AudioTrack>>()
        for (track in tracks) {
            val normTitle = normalize(track.title)
            val normArtist = normalize(track.artist)
            if (normTitle.isNotBlank()) {
                val key = "$normTitle||$normArtist"
                titleArtistMap.getOrPut(key) { mutableListOf() }.add(track)
            }
        }

        val processedTracks = mutableSetOf<Long>()

        for ((_, list) in titleArtistMap) {
            if (list.size > 1) {
                val wastedBytes = list.drop(1).sumOf { it.size }
                groups.add(
                    DuplicateGroup(
                        titleKey = list.first().title,
                        tracks = list,
                        totalWastedBytes = wastedBytes
                    )
                )
                processedTracks.addAll(list.map { it.id })
            }
        }

        // 2. Also check identical file sizes (> 100KB) and similar duration (+/- 2 sec)
        val remainingTracks = tracks.filter { it.id !in processedTracks && it.size > 100_000 }
        val sizeMap = mutableMapOf<Long, MutableList<AudioTrack>>()
        for (track in remainingTracks) {
            sizeMap.getOrPut(track.size) { mutableListOf() }.add(track)
        }

        for ((_, list) in sizeMap) {
            if (list.size > 1) {
                // Verify duration is within 2 seconds
                val firstDuration = list.first().durationMs
                val matching = list.filter { kotlin.math.abs(it.durationMs - firstDuration) <= 2000 }
                if (matching.size > 1) {
                    val wastedBytes = matching.drop(1).sumOf { it.size }
                    groups.add(
                        DuplicateGroup(
                            titleKey = "${matching.first().title} (Exact Size Match)",
                            tracks = matching,
                            totalWastedBytes = wastedBytes
                        )
                    )
                }
            }
        }

        return groups.sortedByDescending { it.totalWastedBytes }
    }

    private fun normalize(str: String): String {
        return str.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }
}
