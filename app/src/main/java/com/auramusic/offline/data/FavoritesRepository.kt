package com.auramusic.offline.data

import android.content.Context

class FavoritesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("aura_favorites", Context.MODE_PRIVATE)
    private val key = "fav_ids"

    fun getFavoriteIds(): Set<Long> {
        val stringSet = prefs.getStringSet(key, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun toggleFavorite(trackId: Long): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val isNowFavorite = if (current.contains(trackId)) {
            current.remove(trackId)
            false
        } else {
            current.add(trackId)
            true
        }
        val stringSet = current.map { it.toString() }.toSet()
        prefs.edit().putStringSet(key, stringSet).apply()
        return isNowFavorite
    }
}
