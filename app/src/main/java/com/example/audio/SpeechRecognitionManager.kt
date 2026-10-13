package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognitionManager(
    private val context: Context,
    private val onResultCallback: (String) -> Unit,
    private val onErrorCallback: ((Int, String) -> Unit)? = null
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun muteSystemBeep() {
        try {
            audioManager?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            audioManager?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
        } catch (e: Exception) {
            Log.d("SpeechRecognitionManager", "Could not adjust system mute", e)
        }
    }

    private fun restoreSystemAudio() {
        mainHandler.postDelayed({
            try {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                audioManager?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
            } catch (e: Exception) {
                Log.d("SpeechRecognitionManager", "Could not restore system audio", e)
            }
        }, 350)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _partialText.value = ""
            restoreSystemAudio()
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
            restoreSystemAudio()
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsDb.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _rmsDb.value = 0f
            restoreSystemAudio()
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _rmsDb.value = 0f
            restoreSystemAudio()

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Speech recognition error ($error)"
            }
            Log.d("SpeechRecognitionManager", "Speech error ($error): $errorMessage")
            onErrorCallback?.invoke(error, errorMessage)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _rmsDb.value = 0f
            restoreSystemAudio()

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                _partialText.value = recognizedText
                onResultCallback(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _partialText.value = matches[0]
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun createSpeechIntent(languagePreference: String = "hinglish"): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            // Suppress standard dictation UI audio prompts
            putExtra("android.speech.extra.DICTATION_MODE", true)

            val langTag = when (languagePreference.lowercase()) {
                "hindi" -> "hi-IN"
                "english" -> "en-US"
                else -> "en-IN" // Hinglish / Indian English default
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
        }
    }

    private fun cleanupRecognizerInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechRecognitionManager", "Error destroying speech recognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _rmsDb.value = 0f
            restoreSystemAudio()
        }
    }

    fun startListening(languagePreference: String = "hinglish"): Boolean {
        val runAction = {
            try {
                cleanupRecognizerInternal()

                // Mute beep sound before starting recognizer
                muteSystemBeep()

                val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer = recognizer
                recognizer.setRecognitionListener(recognitionListener)

                val intent = createSpeechIntent(languagePreference)
                recognizer.startListening(intent)
                _isListening.value = true
                Log.i("SpeechRecognitionManager", "Speech recognition started in-app without popup UI.")
            } catch (e: Exception) {
                Log.e("SpeechRecognitionManager", "Failed to start listening", e)
                _isListening.value = false
                restoreSystemAudio()
                onErrorCallback?.invoke(SpeechRecognizer.ERROR_CLIENT, e.localizedMessage ?: "Failed to start listening")
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            runAction()
        } else {
            mainHandler.post(Runnable { runAction() })
        }
        return true
    }

    fun stopListening() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cleanupRecognizerInternal()
        } else {
            mainHandler.post(Runnable { cleanupRecognizerInternal() })
        }
    }

    fun destroy() {
        stopListening()
    }
}
