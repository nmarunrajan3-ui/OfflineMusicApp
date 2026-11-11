package com.auramusic.offline.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object AudioSynthFallback {

    private var cachedDemoFile: File? = null

    /**
     * Generates a pleasant 30-second harmonic acoustic ambient wav track
     * so that demo tracks and virtual library files play real audio cleanly on any device.
     */
    @Synchronized
    fun getOrCreateDemoAudioFile(context: Context): File {
        cachedDemoFile?.let {
            if (it.exists() && it.length() > 1000) return it
        }

        val file = File(context.cacheDir, "aura_ambient_demo.wav")
        if (file.exists() && file.length() > 1000) {
            cachedDemoFile = file
            return file
        }

        try {
            val sampleRate = 44100
            val durationSec = 30
            val totalSamples = sampleRate * durationSec
            val numChannels = 2
            val bytesPerSample = 2 // 16-bit PCM

            val pcmData = ByteArray(totalSamples * numChannels * bytesPerSample)
            val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

            // Peaceful Pentatonic chord frequencies: C4 (261.6), E4 (329.6), G4 (392.0), A4 (440.0), C5 (523.2)
            val chords = listOf(
                doubleArrayOf(261.63, 329.63, 392.00), // C major
                doubleArrayOf(220.00, 261.63, 329.63), // A minor
                doubleArrayOf(174.61, 220.00, 261.63), // F major
                doubleArrayOf(196.00, 246.94, 293.66)  // G major
            )

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val chordIndex = ((t / 7.5).toInt()) % chords.size
                val currentChord = chords[chordIndex]

                var sample = 0.0
                for (freq in currentChord) {
                    // Soft envelope with gentle harmonic undertones
                    val envelope = 0.5 + 0.5 * sin(2.0 * Math.PI * 0.4 * t)
                    sample += sin(2.0 * Math.PI * freq * t) * envelope
                }

                // Master amplitude scale (warm and not distorted)
                val masterGain = 0.22
                val finalSample = (sample * masterGain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                buffer.putShort(finalSample) // Left channel
                buffer.putShort(finalSample) // Right channel
            }

            FileOutputStream(file).use { fos ->
                writeWavHeader(fos, numChannels, sampleRate, bytesPerSample * 8, pcmData.size)
                fos.write(pcmData)
            }

            cachedDemoFile = file
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return file
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        channels: Int,
        sampleRate: Int,
        bitsPerSample: Int,
        dataLength: Int
    ) {
        val totalDataLen = dataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buf.put("RIFF".toByteArray())
        buf.putInt(totalDataLen)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16) // Subchunk1Size for PCM
        buf.putShort(1) // AudioFormat: 1 = PCM
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort((channels * bitsPerSample / 8).toShort()) // BlockAlign
        buf.putShort(bitsPerSample.toShort())
        buf.put("data".toByteArray())
        buf.putInt(dataLength)

        out.write(header)
    }
}
