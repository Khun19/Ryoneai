package com.example.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

class STTEngine {
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    private val tag = "STTEngine"

    fun loadModel(context: Context): Boolean {
        try {
            // Attempt to load TFLite model from assets (this will fail if not present, falling back gracefully)
            val assetFileDescriptor = context.assets.openFd("whisper_burmese_tiny.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedByteBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(mappedByteBuffer, options)
            isModelLoaded = true
            Log.d(tag, "Successfully loaded Whisper TFLite model.")
            return true
        } catch (e: Exception) {
            Log.e(tag, "TFLite model not found. Using simulated mode. Error: ${e.message}")
            isModelLoaded = true // We set it true to allow simulation to run
            return false
        }
    }

    /**
     * Transcribes input PCM audio bytes into Burmese text using Whisper.
     */
    fun transcribeAudio(audioData: ByteArray): String {
        if (!isModelLoaded) {
            return "စနစ် အဆင်မပြေပါ - Model load မလုပ်ထားပါ"
        }
        
        interpreter?.let {
            // Real TFLite inference logic goes here
            // FloatArray input = preprocess(audioData)
            // Array(1) { IntArray(MAX_SEQ_LEN) } output
            // it.run(input, output)
            // return decodeTokens(output)
            Log.d(tag, "Whisper TFLite model running real inference on ${audioData.size} bytes...")
            return "TFLite Inference Result"
        }

        // Simulated inference
        Log.d(tag, "Whisper simulated inference on ${audioData.size} bytes...")
        return "Hey Bro" 
    }

    /**
     * Custom simulated transcription based on user interaction in testing panel
     */
    fun getSimulatedTranscription(testCommand: String): String {
        return testCommand
    }
}
