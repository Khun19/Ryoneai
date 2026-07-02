package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechToTextManager(private val context: Context) {
    private val tag = "SpeechToTextManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    interface Listener {
        fun onReadyForSpeech()
        fun onBeginningOfSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun onEndOfSpeech()
        fun onError(errorDescription: String)
        fun onResults(text: String)
    }

    fun startListening(listener: Listener) {
        mainHandler.post {
            try {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    listener.onError("Insufficient permissions: RECORD_AUDIO not granted")
                    return@post
                }

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    listener.onError("Speech recognition not available on this device")
                    return@post
                }

                // Destroy old recognizer if any
                speechRecognizer?.destroy()

                val speechContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.createAttributionContext("assist")
                } else {
                    context
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(speechContext).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(tag, "SpeechRecognizer is ready for speech")
                            listener.onReadyForSpeech()
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(tag, "SpeechRecognizer beginning of speech")
                            listener.onBeginningOfSpeech()
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            listener.onRmsChanged(rmsdB)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d(tag, "SpeechRecognizer end of speech")
                            listener.onEndOfSpeech()
                        }

                        override fun onError(error: Int) {
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found (အသံဖမ်းယူမရပါ)"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input (အသံမကြားရပါ)"
                                else -> "Unknown error: $error"
                            }
                            Log.e(tag, "SpeechRecognizer error: $message")
                            listener.onError(message)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val transcribedText = matches?.getOrNull(0)
                            Log.d(tag, "SpeechRecognizer result: $transcribedText")
                            if (!transcribedText.isNullOrBlank()) {
                                listener.onResults(transcribedText)
                            } else {
                                listener.onError("Empty speech recognition result")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "my-MM") // Burmese language tag
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "my-MM")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "my-MM")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                Log.d(tag, "SpeechRecognizer.startListening() initiated on main thread")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start speech recognizer: ${e.message}")
                listener.onError(e.localizedMessage ?: "Unknown initialization error")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(tag, "Error stopping SpeechRecognizer: ${e.message}")
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(tag, "Error destroying SpeechRecognizer: ${e.message}")
            }
        }
    }
}
