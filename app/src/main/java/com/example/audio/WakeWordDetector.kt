package com.example.audio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var isRunning = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val wakeWords = listOf("mayraa", "maira", "myra", "mayra", "mera", "mairaa")

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            _isListening.value = false
            if (isRunning) {
                // Restart listening after short delay to maintain wake-word loop safely
                mainHandler.postDelayed({
                    if (isRunning) startListeningLoop()
                }, 1000)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val detected = matches.any { phrase ->
                    val clean = phrase.lowercase()
                    wakeWords.any { clean.contains(it) }
                }
                if (detected) {
                    Log.d("WakeWordDetector", "Wake-word 'Mayraa' detected!")
                    onWakeWordDetected()
                }
            }

            if (isRunning) {
                mainHandler.postDelayed({
                    if (isRunning) startListeningLoop()
                }, 500)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val detected = matches.any { phrase ->
                    val clean = phrase.lowercase()
                    wakeWords.any { clean.contains(it) }
                }
                if (detected) {
                    Log.d("WakeWordDetector", "Wake-word detected in partial results!")
                    onWakeWordDetected()
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun start() {
        if (isRunning) return
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("WakeWordDetector", "Missing RECORD_AUDIO permission")
            return
        }

        isRunning = true
        startListeningLoop()
    }

    private fun startListeningLoop() {
        if (!isRunning) return
        mainHandler.post {
            try {
                recognizer?.destroy()
                if (!SpeechRecognizer.isRecognitionAvailable(context)) return@post

                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(listener)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("WakeWordDetector", "Error in startListeningLoop", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        mainHandler.post {
            try {
                recognizer?.stopListening()
                recognizer?.destroy()
                recognizer = null
            } catch (e: Exception) {
                Log.e("WakeWordDetector", "Error stopping wake-word detector", e)
            } finally {
                _isListening.value = false
            }
        }
    }
}
