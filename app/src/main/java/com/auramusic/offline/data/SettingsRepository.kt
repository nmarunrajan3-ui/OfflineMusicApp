package com.auramusic.offline.data

import android.content.Context

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)
    private val keyDarkMode = "pref_dark_mode"

    fun isDarkMode(): Boolean {
        // Default to true for Apple Music sleek dark theme
        return prefs.getBoolean(keyDarkMode, true)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(keyDarkMode, enabled).apply()
    }
}
