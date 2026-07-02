package com.example.engine

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioCueEngine {
    private val tag = "AudioCueEngine"
    private val sampleRate = 22050

    enum class CueType {
        WAKE_UP,       // Bright rising synthesizer pitch sweep
        SUCCESS,       // Gentle, melodic arpeggio chime (Success)
        ERROR          // Low flat flat buzz/falling tone (Error)
    }

    fun playCue(type: CueType) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val pcmData = when (type) {
                    CueType.WAKE_UP -> generateWakeUpChime()
                    CueType.SUCCESS -> generateSuccessChime()
                    CueType.ERROR -> generateErrorChime()
                }
                playPcm(pcmData)
            } catch (e: Exception) {
                Log.e(tag, "Failed to play dynamic audio cue: ${e.message}")
            }
        }
    }

    private fun playPcm(pcm: ShortArray) {
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            pcm.size * 2,
            AudioTrack.MODE_STATIC
        )
        track.write(pcm, 0, pcm.size)
        track.play()
        
        // Safely dispose the AudioTrack after playback finishes
        CoroutineScope(Dispatchers.Default).launch {
            val playbackDurationMs = (pcm.size.toFloat() / sampleRate * 1000).toLong()
            kotlinx.coroutines.delay(playbackDurationMs + 200)
            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                // Ignore silent release exceptions
            }
        }
    }

    private fun generateWakeUpChime(): ShortArray {
        // A cute quick bright futuristic double-chime rising sound
        val durationMs = 200
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        
        val note1Samples = numSamples / 2
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val isNote2 = i >= note1Samples
            val progress = if (!isNote2) {
                i.toDouble() / note1Samples
            } else {
                (i - note1Samples).toDouble() / note1Samples
            }
            // Frequencies: 660Hz (E5) and then 880Hz (A5)
            val freq = if (!isNote2) 660.0 else 880.0
            val envelope = if (progress < 0.15) {
                progress / 0.15 // Fade in
            } else {
                1.0 - progress  // Fade out
            }
            val angle = 2.0 * Math.PI * freq * t
            pcm[i] = (sin(angle) * 11000.0 * envelope).toInt().toShort()
        }
        return pcm
    }

    private fun generateSuccessChime(): ShortArray {
        // Beautiful bright high-pitch major arpeggio chime (e.g. C6 -> E6 -> G6)
        val durationMs = 450
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        
        val noteSamples = numSamples / 3
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIndex = i / noteSamples
            val progress = (i % noteSamples).toDouble() / noteSamples
            
            val freq = when (noteIndex) {
                0 -> 1046.50 // C6
                1 -> 1318.51 // E6
                else -> 1567.98 // G6
            }
            
            val envelope = (1.0 - progress) // Smooth linear decay
            val angle = 2.0 * Math.PI * freq * t
            pcm[i] = (sin(angle) * 9000.0 * envelope).toInt().toShort()
        }
        return pcm
    }

    private fun generateErrorChime(): ShortArray {
        // Deep buzzy warning signal
        val durationMs = 350
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 180.0 - (progress * 50.0) // slightly sliding downward pitch
            
            // Tremolo / pulse effect to sound alarmed
            val tremolo = 0.6 + 0.4 * sin(2.0 * Math.PI * 15.0 * t)
            val envelope = (1.0 - progress) * tremolo
            
            val angle = 2.0 * Math.PI * freq * t
            // Add rich harmonics (square-like buzz)
            val wave = sin(angle) + 0.4 * sin(3.0 * angle) + 0.2 * sin(5.0 * angle)
            pcm[i] = (wave * 11000.0 * envelope).coerceIn(-32768.0, 32767.0).toInt().toShort()
        }
        return pcm
    }
}
