package com.auramusic.offline.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorExtractor {

    suspend fun extractColorsFromUri(context: Context, uriString: String?): Pair<Long, Long> = withContext(Dispatchers.IO) {
        if (uriString.isNullOrEmpty()) {
            return@withContext defaultColors()
        }

        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext defaultColors()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return@withContext defaultColors()

            val palette = Palette.from(bitmap).generate()

            val primary = palette.getVibrantColor(
                palette.getDominantColor(0xFF2C2C2E.toInt())
            ).toLong() and 0xFFFFFFFFL

            val secondary = palette.getDarkVibrantColor(
                palette.getMutedColor(0xFF1C1C1E.toInt())
            ).toLong() and 0xFFFFFFFFL

            Pair(primary, secondary)
        } catch (e: Exception) {
            defaultColors()
        }
    }

    private fun defaultColors(): Pair<Long, Long> {
        // Apple Music signature crimson / midnight violet
        return Pair(0xFFFA243CL, 0xFF1C1C1EL)
    }
}
