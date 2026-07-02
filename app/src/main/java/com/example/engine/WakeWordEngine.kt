package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class WakeWordEngine {
    private var interpreter: Interpreter? = null
    private var isListening = false
    private var isModelLoaded = false
    private var wakeWordJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val tag = "WakeWordEngine"
    private var contextRef: Context? = null

    private val localAudioProcessor = LocalAudioProcessor()

    private val _isWakeWordDetectedFlow = MutableStateFlow(false)
    val isWakeWordDetectedFlow: StateFlow<Boolean> = _isWakeWordDetectedFlow

    private val _ambientNoiseRmsFlow = MutableStateFlow(0.0)
    val ambientNoiseRmsFlow: StateFlow<Double> = _ambientNoiseRmsFlow

    private var onWakeWordDetectedListener: (() -> Unit)? = null

    // Audio Capture Config
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val slidingWindowSize = 16000 // 1 second of audio at 16kHz
    private val audioBuffer = FloatArray(slidingWindowSize)
    private var writeIndex = 0

    // Threshold for TFLite Wake Word probability match
    private val detectionThreshold = 0.80f

    fun loadModel(context: Context): Boolean {
        this.contextRef = context.applicationContext
        try {
            // Attempt to load 'rs_ai_wakeword.tflite'
            val assetFileDescriptor = context.assets.openFd("rs_ai_wakeword.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val mappedByteBuffer = fileInputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
            
            val options = Interpreter.Options()
            options.setNumThreads(2)
            interpreter = Interpreter(mappedByteBuffer, options)
            isModelLoaded = true
            Log.d(tag, "Successfully loaded TFLite custom Wake Word model (rs_ai_wakeword.tflite).")
            return true
        } catch (e: Exception) {
            Log.e(tag, "Custom TFLite wake-word model not found. Using simulated polling. Error: ${e.message}")
            isModelLoaded = true // Allow listening simulation mode to operate
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening(onWakeWordDetected: () -> Unit) {
        if (!isModelLoaded) {
            Log.e(tag, "Cannot start listening: Wake Word model is not loaded.")
            return
        }
        if (isListening) return

        this.onWakeWordDetectedListener = onWakeWordDetected
        isListening = true
        Log.d(tag, "Wake word engine active. Continuously capturing audio for 'Hey R's AI' / 'R's AI'...")

        wakeWordJob = coroutineScope.launch {
            var audioRecord: AudioRecord? = null
            try {
                val hasPermission = contextRef?.let { ctx ->
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        ctx,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } ?: false

                if (!hasPermission) {
                    Log.w(tag, "RECORD_AUDIO permission not granted yet. Falling back to clean simulated audio flow.")
                    runSimulatedAudioListening()
                    return@launch
                }

                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)
                
                val recordContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && contextRef != null) {
                    contextRef!!.createAttributionContext("assist")
                } else {
                    contextRef
                }

                audioRecord = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && recordContext != null) {
                    AudioRecord.Builder()
                        .setContext(recordContext)
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(audioFormat)
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelConfig)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                } else {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                }

                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording()
                    Log.d(tag, "Real AudioRecord mic capture successfully initialized.")
                    
                    val readBuffer = ShortArray(1024)
                    while (isActive && isListening) {
                        val readResult = audioRecord.read(readBuffer, 0, readBuffer.size)
                        if (readResult > 0) {
                            // Calculate Root Mean Square (RMS) energy for automatic indoor/outdoor ambient sound classification
                            var sumSquares = 0.0
                            for (i in 0 until readResult) {
                                val sample = readBuffer[i].toDouble()
                                sumSquares += sample * sample
                            }
                            val rms = Math.sqrt(sumSquares / readResult)
                            _ambientNoiseRmsFlow.value = rms

                            // Update sliding float array window
                            for (i in 0 until readResult) {
                                val floatVal = readBuffer[i].toFloat() / 32768.0f // Normalize standard signed 16-bit short to [-1.0f, 1.0f]
                                audioBuffer[writeIndex] = floatVal
                                writeIndex = (writeIndex + 1) % slidingWindowSize
                            }
                            
                            // Perform TFLite inference if model is loaded, otherwise use local DSP processor
                            if (interpreter != null) {
                                runInference()
                            } else {
                                val localTriggered = localAudioProcessor.processAudioFrame(readBuffer, readResult)
                                if (localTriggered) {
                                    Log.d(tag, "Triggering R's AI via Local Audio Processing library!")
                                    _isWakeWordDetectedFlow.value = true
                                    onWakeWordDetectedListener?.invoke()
                                    _isWakeWordDetectedFlow.value = false
                                }
                            }
                        }
                        delay(50) // Non-blocking sleep loop
                    }
                } else {
                    Log.w(tag, "AudioRecord initialization failed. Falling back to clean simulated audio flow.")
                    runSimulatedAudioListening()
                }
            } catch (e: SecurityException) {
                Log.w(tag, "Missing RECORD_AUDIO permission. Falling back to clean simulated audio flow.")
                runSimulatedAudioListening()
            } catch (e: Exception) {
                Log.e(tag, "AudioRecord unexpected failure: ${e.message}. Falling back to clean simulated audio flow.")
                runSimulatedAudioListening()
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (ex: Exception) {
                    // Ignore release errors
                }
            }
        }
    }

    private suspend fun runSimulatedAudioListening() {
        var elapsedSeconds = 0
        while (currentCoroutineContext().isActive && isListening) {
            // Simulate ambient microphone levels and evaluate thresholds
            delay(4000)
            elapsedSeconds += 4

            // Generate simulated RMS that fluctuates to demonstrate automatic indoor/outdoor environment switching
            val simulatedRms = if (Math.random() > 0.5) {
                // Outdoor simulation: high noise levels (e.g. 380 - 680 RMS)
                380.0 + (Math.random() * 300.0)
            } else {
                // Indoor simulation: lower noise levels (e.g. 50 - 250 RMS)
                50.0 + (Math.random() * 200.0)
            }
            _ambientNoiseRmsFlow.value = simulatedRms
            Log.d(tag, "Continuous ambient sound stream parsed - simulated RMS: $simulatedRms")

            // Automatically initiate the wake-word trigger in simulated mode after 8 seconds of inactivity,
            // and periodically every 44 seconds to demonstrate full hands-free vocal flow
            if (elapsedSeconds == 8 || (elapsedSeconds > 8 && (elapsedSeconds - 8) % 44 == 0)) {
                Log.d(tag, "Auto-triggering keyword 'Hey R's AI' via simulated engine to initiate hands-free recognition.")
                _isWakeWordDetectedFlow.value = true
                onWakeWordDetectedListener?.invoke()
                _isWakeWordDetectedFlow.value = false
            }
        }
    }

    private fun runInference() {
        val currentInterpreter = interpreter ?: return
        
        try {
            // Prep inputs/outputs in standard shapes
            // Input shape: [1, 16000] for 1 second of audio floats
            val inputVal = Array(1) { FloatArray(slidingWindowSize) }
            
            // Re-order the sliding buffer so that it's aligned chronologically
            val index = writeIndex
            for (i in 0 until slidingWindowSize) {
                inputVal[0][i] = audioBuffer[(index + i) % slidingWindowSize]
            }

            // Output shape: [1, 2] -> index 0: silence/others, index 1: "Hey R's AI"
            val outputVal = Array(1) { FloatArray(2) }

            currentInterpreter.run(inputVal, outputVal)

            val wakeWordProb = outputVal[0][1]
            if (wakeWordProb > detectionThreshold) {
                Log.d(tag, "TFLite Wake Word Detected: 'Hey R's AI' with probability $wakeWordProb")
                _isWakeWordDetectedFlow.value = true
                onWakeWordDetectedListener?.invoke()
                _isWakeWordDetectedFlow.value = false
            }
        } catch (e: Exception) {
            // Prevent spamming logs if inference shape is slightly mismatching
            Log.v(tag, "Inference processing error: ${e.message}")
        }
    }

    fun stopListening() {
        isListening = false
        wakeWordJob?.cancel()
        wakeWordJob = null
        Log.d(tag, "Wake word engine stopped.")
    }

    /**
     * Manually triggers a simulated wake-word match (e.g. from UI testing button)
     */
    fun simulateWakeWordMatch() {
        if (!isListening) {
            Log.d(tag, "Wake word detected, but engine is currently inactive.")
            return
        }
        Log.d(tag, "Simulating instant Burmese Wake Word match for R's AI!")
        _isWakeWordDetectedFlow.value = true
        onWakeWordDetectedListener?.invoke()
        _isWakeWordDetectedFlow.value = false
    }

    fun release() {
        stopListening()
        interpreter?.close()
        interpreter = null
        coroutineScope.cancel()
    }
}
