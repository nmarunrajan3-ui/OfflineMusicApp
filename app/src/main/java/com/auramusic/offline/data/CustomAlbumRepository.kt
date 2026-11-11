package com.auramusic.offline.data

import android.content.Context
import com.auramusic.offline.model.CustomAlbum
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CustomAlbumRepository(context: Context) {

    private val prefs = context.getSharedPreferences("aura_custom_albums", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "albums_json"

    fun getAlbums(): List<CustomAlbum> {
        val json = prefs.getString(key, null) ?: return defaultAlbums()
        return try {
            val type = object : TypeToken<List<CustomAlbum>>() {}.type
            val list: List<CustomAlbum> = gson.fromJson(json, type) ?: emptyList()
            if (list.isEmpty()) defaultAlbums() else list
        } catch (e: Exception) {
            defaultAlbums()
        }
    }

    fun saveAlbum(album: CustomAlbum) {
        val current = getAlbums().toMutableList()
        val index = current.indexOfFirst { it.id == album.id }
        if (index >= 0) {
            current[index] = album
        } else {
            current.add(album)
        }
        persist(current)
    }

    fun deleteAlbum(albumId: String) {
        val current = getAlbums().toMutableList()
        current.removeAll { it.id == albumId }
        persist(current)
    }

    fun addTrackToAlbum(albumId: String, trackId: Long) {
        val current = getAlbums().toMutableList()
        val index = current.indexOfFirst { it.id == albumId }
        if (index >= 0) {
            val album = current[index]
            if (!album.trackIds.contains(trackId)) {
                val updatedTracks = album.trackIds + trackId
                current[index] = album.copy(trackIds = updatedTracks)
                persist(current)
            }
        }
    }

    fun removeTrackFromAlbum(albumId: String, trackId: Long) {
        val current = getAlbums().toMutableList()
        val index = current.indexOfFirst { it.id == albumId }
        if (index >= 0) {
            val album = current[index]
            val updatedTracks = album.trackIds.filter { it != trackId }
            current[index] = album.copy(trackIds = updatedTracks)
            persist(current)
        }
    }

    private fun persist(albums: List<CustomAlbum>) {
        prefs.edit().putString(key, gson.toJson(albums)).apply()
    }

    private fun defaultAlbums(): List<CustomAlbum> {
        return listOf(
            CustomAlbum(
                id = "default_chill",
                name = "Acoustic & Chill",
                description = "Relaxing evening acoustic sessions",
                customPhotoUri = null
            ),
            CustomAlbum(
                id = "default_workout",
                name = "Workout Energy",
                description = "High tempo workout beats",
                customPhotoUri = null
            )
        )
    }
}
