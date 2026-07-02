package com.example.engine

import android.util.Log

/**
 * A local, real-time Keyword Spotting (KWS) digital signal processing (DSP) engine
 * designed to spot the acoustic pattern of "Hey R's AI" or "R's AI".
 * It computes Short-Time Energy (RMS) and Zero Crossing Rate (ZCR),
 * and implements a state-machine that matches the temporal and rhythmic syllables of the phrase.
 */
class LocalAudioProcessor {
    private val tag = "LocalAudioProcessor"
    private var lastTriggerTime = 0L
    private val triggerCooldownMs = 4000L

    // Adaptive thresholding: background noise estimation
    private var noiseFloorRms = 150.0
    private val alpha = 0.98 // Exponential moving average coefficient

    // Syllable/Pattern Detection State Machine for "Hey R's AI"
    private enum class KwsState {
        IDLE,
        SYLLABLE_1,      // Detecting "Hey" (First vocal energy burst)
        GAP,             // Detecting the brief silence/dip between "Hey" and "R's AI"
        SYLLABLE_2       // Detecting "R's AI" (Second elongated vocal energy burst)
    }

    private var currentState = KwsState.IDLE
    private var stateFrameCount = 0

    // Frame duration at 16kHz with 1024 buffer size is ~64ms
    private val frameDurationMs = 64

    /**
     * Processes incoming PCM-16bit short buffer.
     * Returns true if the distinct rhythmic syllable cadence of the wake phrase is detected.
     */
    fun processAudioFrame(shortBuffer: ShortArray, length: Int): Boolean {
        if (length <= 0) return false

        // 1. Calculate Root Mean Square (RMS) energy
        var sumSquares = 0.0
        for (i in 0 until length) {
            val sample = shortBuffer[i].toDouble()
            sumSquares += sample * sample
        }
        val rms = Math.sqrt(sumSquares / length)

        // 2. Calculate Zero Crossing Rate (ZCR) for pitch/voicing classification
        var zeroCrossings = 0
        for (i in 1 until length) {
            val currentSign = shortBuffer[i] >= 0
            val prevSign = shortBuffer[i - 1] >= 0
            if (currentSign != prevSign) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toDouble() / length

        // 3. Update background noise floor estimation dynamically during quiet frames
        if (rms < noiseFloorRms * 1.5) {
            noiseFloorRms = alpha * noiseFloorRms + (1 - alpha) * rms
        }
        noiseFloorRms = noiseFloorRms.coerceAtLeast(30.0)

        // 4. Voice Activity Detection (VAD) / speech classification
        val energyRatio = rms / noiseFloorRms
        val isSpeechLike = energyRatio > 3.0 && rms > 450.0 && zcr in 0.03..0.45

        // Update state machine timers
        stateFrameCount++
        val durationMs = stateFrameCount * frameDurationMs

        var triggered = false

        when (currentState) {
            KwsState.IDLE -> {
                if (isSpeechLike) {
                    currentState = KwsState.SYLLABLE_1
                    stateFrameCount = 1
                    Log.d(tag, "🎙️ KWS State Change: IDLE -> SYLLABLE_1 ('Hey')")
                }
            }
            KwsState.SYLLABLE_1 -> {
                if (isSpeechLike) {
                    // Maximum duration of "Hey" syllable is 500ms
                    if (durationMs > 500) {
                        currentState = KwsState.IDLE
                        Log.v(tag, "KWS Reset: Syllable 1 exceeded max duration ($durationMs ms)")
                    }
                } else {
                    // Syllable 1 ended. Check if its duration was valid (100ms - 500ms)
                    if (durationMs in 100..500) {
                        currentState = KwsState.GAP
                        stateFrameCount = 1
                        Log.d(tag, "🎙️ KWS State Change: SYLLABLE_1 -> GAP (Syllable 1 duration: $durationMs ms)")
                    } else {
                        currentState = KwsState.IDLE
                        Log.v(tag, "KWS Reset: Syllable 1 invalid duration ($durationMs ms)")
                    }
                }
            }
            KwsState.GAP -> {
                if (!isSpeechLike) {
                    // Maximum duration of the gap is 400ms
                    if (durationMs > 400) {
                        currentState = KwsState.IDLE
                        Log.v(tag, "KWS Reset: Gap exceeded max duration ($durationMs ms)")
                    }
                } else {
                    // Gap ended with new speech activity. Check if gap duration was valid (50ms - 400ms)
                    if (durationMs in 50..400) {
                        currentState = KwsState.SYLLABLE_2
                        stateFrameCount = 1
                        Log.d(tag, "🎙️ KWS State Change: GAP -> SYLLABLE_2 ('R's AI') (Gap duration: $durationMs ms)")
                    } else {
                        currentState = KwsState.IDLE
                        Log.v(tag, "KWS Reset: Gap invalid duration ($durationMs ms)")
                    }
                }
            }
            KwsState.SYLLABLE_2 -> {
                if (isSpeechLike) {
                    // Maximum duration of "R's AI" syllable is 1000ms
                    if (durationMs > 1000) {
                        currentState = KwsState.IDLE
                        Log.v(tag, "KWS Reset: Syllable 2 exceeded max duration ($durationMs ms)")
                    }
                } else {
                    // Syllable 2 ended. Check if its duration was valid (250ms - 1000ms)
                    if (durationMs in 250..1000) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTriggerTime > triggerCooldownMs) {
                            lastTriggerTime = currentTime
                            triggered = true
                            Log.d(tag, "🎉 LOCAL KEYWORD SPOTTED SUCCESS: 'Hey R's AI' detected locally!")
                        }
                        currentState = KwsState.IDLE
                    } else {
                        currentState = KwsState.IDLE
                        Log.v(tag, "KWS Reset: Syllable 2 invalid duration ($durationMs ms)")
                    }
                }
            }
        }

        return triggered
    }
}
