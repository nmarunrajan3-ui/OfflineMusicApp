package com.auramusic.offline.data

import android.content.Context
import android.net.Uri
import com.auramusic.offline.model.AudioTrack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RecycleBinManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("aura_recycle_bin", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "trashed_items_json"

    data class TrashedItem(
        val dataPath: String,
        val contentUriString: String,
        val dateTrashed: Long = System.currentTimeMillis()
    )

    fun getTrashedItems(): List<TrashedItem> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TrashedItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getTrashedPaths(): Set<String> {
        return getTrashedItems().map { it.dataPath }.toSet()
    }

    fun moveToTrash(track: AudioTrack) {
        val current = getTrashedItems().toMutableList()
        if (current.none { it.dataPath == track.dataPath }) {
            current.add(TrashedItem(track.dataPath, track.contentUriString))
            save(current)
        }
    }

    fun restore(track: AudioTrack) {
        val current = getTrashedItems().toMutableList()
        current.removeAll { it.dataPath == track.dataPath }
        save(current)
    }

    suspend fun deletePermanently(track: AudioTrack): Boolean = withContext(Dispatchers.IO) {
        var deleted = false
        try {
            val file = File(track.dataPath)
            if (file.exists()) {
                deleted = file.delete()
            }
            if (!deleted && track.contentUriString.isNotEmpty()) {
                val uri = Uri.parse(track.contentUriString)
                deleted = context.contentResolver.delete(uri, null, null) > 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Always remove from trash registry
        restore(track)
        deleted
    }

    suspend fun emptyBin(trashedTracks: List<AudioTrack>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (track in trashedTracks) {
            if (deletePermanently(track)) {
                count++
            }
        }
        save(emptyList())
        count
    }

    private fun save(items: List<TrashedItem>) {
        prefs.edit().putString(key, gson.toJson(items)).apply()
    }
}
