package com.auramusic.offline.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RecentlyPlayedRepository(context: Context) {

    private val prefs = context.getSharedPreferences("aura_recent_played", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val keyRecentList = "recent_track_ids"
    private val keyLastPlayedId = "last_played_id"
    private val keyLastPosition = "last_played_position"

    fun getRecentTrackIds(): List<Long> {
        val json = prefs.getString(keyRecentList, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordTrackPlayed(trackId: Long) {
        val list = getRecentTrackIds().toMutableList()
        list.remove(trackId)
        list.add(0, trackId) // newest first
        val trimmed = if (list.size > 20) list.take(20) else list
        prefs.edit()
            .putString(keyRecentList, gson.toJson(trimmed))
            .putLong(keyLastPlayedId, trackId)
            .apply()
    }

    fun saveLastPosition(positionMs: Long) {
        prefs.edit().putLong(keyLastPosition, positionMs).apply()
    }

    fun getLastPlayedTrackId(): Long? {
        val id = prefs.getLong(keyLastPlayedId, -1L)
        return if (id != -1L) id else null
    }

    fun getLastPosition(): Long {
        return prefs.getLong(keyLastPosition, 0L)
    }
}
