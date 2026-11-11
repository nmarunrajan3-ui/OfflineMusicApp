package com.auramusic.offline.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.auramusic.offline.model.AudioTrack
import java.io.File
import java.util.ArrayList

object ShareHelper {

    fun shareTrack(context: Context, track: AudioTrack) {
        shareTracks(context, listOf(track))
    }

    fun shareTracks(context: Context, tracks: List<AudioTrack>) {
        if (tracks.isEmpty()) {
            Toast.makeText(context, "No songs selected to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uris = ArrayList<Uri>()
            for (track in tracks) {
                val file = File(track.dataPath)
                if (file.exists()) {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    uris.add(contentUri)
                } else {
                    // Fallback to content URI if path file does not exist directly
                    uris.add(Uri.parse(track.contentUriString))
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(context, "Cannot locate audio files for sharing", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = tracks.first().mimeType ?: "audio/*"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    putExtra(Intent.EXTRA_SUBJECT, tracks.first().title)
                    putExtra(Intent.EXTRA_TEXT, "Sharing song: ${tracks.first().title} by ${tracks.first().artist}")
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "audio/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    putExtra(Intent.EXTRA_SUBJECT, "Sharing ${tracks.size} songs from Aura Music")
                    putExtra(Intent.EXTRA_TEXT, "Sharing ${tracks.size} songs with Aura Music QuickShare")
                }
            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(intent, "QuickShare ${tracks.size} Audio Track(s)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "QuickShare error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
