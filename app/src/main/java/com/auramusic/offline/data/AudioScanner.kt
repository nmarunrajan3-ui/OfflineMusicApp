package com.auramusic.offline.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.auramusic.offline.model.AudioCategory
import com.auramusic.offline.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class AudioScanner(private val context: Context) {

    suspend fun scanDeviceAudio(trashedPaths: Set<String>): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Only query files longer than 5 seconds to skip notification pings and micro sounds
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("5000")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val rawTitle = it.getString(titleCol) ?: "Unknown Track"
                    val rawArtist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val dataPath = it.getString(dataCol) ?: ""
                    val albumId = it.getLong(albumIdCol)
                    val size = it.getLong(sizeCol)
                    val dateAdded = it.getLong(dateAddedCol)
                    val mimeType = it.getString(mimeCol)

                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val albumArtUri = ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()

                    val artist = if (rawArtist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else rawArtist
                    val title = if (rawTitle.isBlank()) File(dataPath).nameWithoutExtension else rawTitle

                    // Categorize meticulously to prevent mingling
                    val category = if (trashedPaths.contains(dataPath)) {
                        AudioCategory.RECYCLE_BIN
                    } else {
                        classifyAudio(dataPath, title)
                    }

                    tracks.add(
                        AudioTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            dataPath = dataPath,
                            contentUriString = contentUri,
                            albumId = albumId,
                            albumArtUri = albumArtUri,
                            size = size,
                            dateAdded = dateAdded,
                            mimeType = mimeType,
                            category = category
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    /**
     * Strict classification logic:
     * Voice recordings, Call recordings, and Music tracks are completely isolated.
     */
    private fun classifyAudio(filePath: String, title: String): AudioCategory {
        val lowerPath = filePath.lowercase(Locale.ROOT)
        val lowerTitle = title.lowercase(Locale.ROOT)

        // 1. Check Call Recordings
        val isCallRecord = lowerPath.contains("/call/") ||
                lowerPath.contains("/calls/") ||
                lowerPath.contains("call_recorder") ||
                lowerPath.contains("callrecorder") ||
                lowerPath.contains("callrecording") ||
                lowerPath.contains("phone_record") ||
                lowerPath.contains("phonecall") ||
                lowerTitle.startsWith("call@") ||
                lowerTitle.startsWith("call_") ||
                lowerTitle.startsWith("call-") ||
                lowerTitle.startsWith("call ")

        if (isCallRecord) return AudioCategory.CALL_RECORD

        // 2. Check Voice Recordings
        val isVoiceRecording = lowerPath.contains("/recordings/") ||
                lowerPath.contains("voicerecorder") ||
                lowerPath.contains("voice_recorder") ||
                lowerPath.contains("soundrecorder") ||
                lowerPath.contains("sound_recorder") ||
                lowerPath.contains("voicenotes") ||
                lowerPath.contains("voice memo") ||
                lowerPath.contains("audiorecorder") ||
                lowerPath.contains("dictaphone") ||
                lowerTitle.startsWith("voice ") ||
                lowerTitle.startsWith("recording_") ||
                lowerTitle.startsWith("rec_") ||
                lowerTitle.startsWith("aud-") ||
                lowerTitle.startsWith("voice-")

        if (isVoiceRecording) return AudioCategory.VOICE_RECORDING

        // 3. Normal Music Track
        return AudioCategory.MUSIC
    }
}
