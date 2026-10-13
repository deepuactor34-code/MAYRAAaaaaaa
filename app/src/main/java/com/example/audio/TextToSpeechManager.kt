package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class TtsVoiceInfo(
    val name: String,
    val displayName: String,
    val localeTag: String,
    val isNetworkRequired: Boolean = false
)

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechFinishedFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val speechFinishedFlow: SharedFlow<String> = _speechFinishedFlow.asSharedFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private var currentPitch: Float = 1.15f
    private var currentRate: Float = 1.0f
    private var preferredLanguage: String = "hinglish"
    private var preferredVoiceName: String = ""

    // Queue for speech requested before TTS initialization completes
    private var pendingSpeechText: String? = null
    private var pendingOnDone: (() -> Unit)? = null

    // Track active callbacks by utterance ID
    private val utteranceCallbacks = ConcurrentHashMap<String, () -> Unit>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.w("TextToSpeechManager", "Could not set audio attributes", e)
            }

            loadAvailableVoices()
            setupVoice()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    Log.d("TextToSpeechManager", "TTS speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    Log.d("TextToSpeechManager", "TTS speech completed: $utteranceId")
                    utteranceId?.let { id ->
                        val cb = utteranceCallbacks.remove(id)
                        cb?.invoke()
                        _speechFinishedFlow.tryEmit(id)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    utteranceId?.let { id ->
                        val cb = utteranceCallbacks.remove(id)
                        cb?.invoke()
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.w("TextToSpeechManager", "TTS error ($errorCode) on utterance: $utteranceId")
                    utteranceId?.let { id ->
                        val cb = utteranceCallbacks.remove(id)
                        cb?.invoke()
                    }
                }
            })

            // Immediately speak any pending speech queued during initialization
            val pending = pendingSpeechText
            val callback = pendingOnDone
            pendingSpeechText = null
            pendingOnDone = null
            if (!pending.isNullOrBlank()) {
                speak(pending, callback)
            }

        } else {
            Log.e("TextToSpeechManager", "TTS Initialization failed with status: $status")
        }
    }

    private fun loadAvailableVoices() {
        try {
            val systemVoices = tts?.voices ?: emptySet()
            val mapped = systemVoices.map { voice ->
                val lang = voice.locale.displayLanguage.ifBlank { voice.locale.language }
                val country = voice.locale.displayCountry.ifBlank { voice.locale.country }
                val countrySuffix = if (country.isNotBlank()) " ($country)" else ""
                val cleanName = voice.name.substringAfterLast("/").substringAfterLast(":")
                TtsVoiceInfo(
                    name = voice.name,
                    displayName = "$lang$countrySuffix • $cleanName",
                    localeTag = voice.locale.toLanguageTag(),
                    isNetworkRequired = voice.isNetworkConnectionRequired
                )
            }.sortedBy { it.displayName }
            _availableVoices.value = mapped
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Error querying TTS voices", e)
        }
    }

    private fun setupVoice() {
        val ttsInstance = tts ?: return
        if (!isInitialized) return

        ttsInstance.setPitch(currentPitch)
        ttsInstance.setSpeechRate(currentRate)

        // 1. If user chose a specific voice from Settings, apply it
        if (preferredVoiceName.isNotBlank()) {
            val matchedVoice = ttsInstance.voices?.firstOrNull { it.name == preferredVoiceName }
            if (matchedVoice != null) {
                ttsInstance.voice = matchedVoice
                return
            }
        }

        // 2. Set target locale
        val targetLocale = when (preferredLanguage.lowercase()) {
            "hindi" -> Locale("hi", "IN")
            "english" -> Locale.US
            else -> Locale("en", "IN") // Hinglish / Indian English default
        }

        var result = ttsInstance.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            result = ttsInstance.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsInstance.setLanguage(Locale.getDefault())
            }
        }

        // 3. Auto-prefer natural female voice for Mayraa
        try {
            val available = ttsInstance.voices ?: emptySet()
            val langTag = targetLocale.language.lowercase()
            val femaleVoice = available.firstOrNull { voice ->
                val nameLower = voice.name.lowercase()
                voice.locale.language.equals(langTag, ignoreCase = true) &&
                        (nameLower.contains("female") || nameLower.contains("fem") || nameLower.contains("f0") || nameLower.contains("hie") || nameLower.contains("hid") || nameLower.contains("enc"))
            } ?: available.firstOrNull { voice ->
                val nameLower = voice.name.lowercase()
                nameLower.contains("female") || nameLower.contains("fem")
            }
            if (femaleVoice != null) {
                ttsInstance.voice = femaleVoice
                Log.d("TextToSpeechManager", "Selected natural female voice for Mayraa: ${femaleVoice.name}")
            }
        } catch (e: Exception) {
            Log.d("TextToSpeechManager", "Voice filter not supported", e)
        }
    }

    fun updateSettings(pitch: Float, rate: Float, language: String, voiceName: String = "") {
        currentPitch = pitch
        currentRate = rate
        preferredLanguage = language
        preferredVoiceName = voiceName
        setupVoice()
    }

    fun setSpecificVoice(voiceName: String) {
        preferredVoiceName = voiceName
        val ttsInstance = tts ?: return
        if (!isInitialized) return
        val matchedVoice = ttsInstance.voices?.firstOrNull { it.name == voiceName }
        if (matchedVoice != null) {
            ttsInstance.voice = matchedVoice
        }
    }

    var isMuted: Boolean = false
        set(value) {
            field = value
            if (value) stop()
        }

    private var lastSpokenText: String = ""
    private var lastSpokenTimeMs: Long = 0L

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (isMuted) {
            Log.d("TextToSpeechManager", "TTS is muted, skipping speech playback")
            onDone?.invoke()
            return
        }

        val cleanText = text
            .replace(Regex("[*#_`~>|\\[\\]]"), "")
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            .trim()

        if (cleanText.isEmpty()) {
            onDone?.invoke()
            return
        }

        // Prevent duplicate robot repetition of same text in short window
        val now = System.currentTimeMillis()
        if (cleanText == lastSpokenText && (now - lastSpokenTimeMs) < 1200L) {
            Log.d("TextToSpeechManager", "Duplicate speech request skipped to prevent echo")
            onDone?.invoke()
            return
        }
        lastSpokenText = cleanText
        lastSpokenTimeMs = now

        if (!isInitialized || tts == null) {
            Log.i("TextToSpeechManager", "TTS not initialized yet, queueing message: \"$cleanText\"")
            pendingSpeechText = cleanText
            pendingOnDone = onDone
            return
        }

        val utteranceId = "mayraa_${System.currentTimeMillis()}"
        if (onDone != null) {
            utteranceCallbacks[utteranceId] = onDone
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        val res = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (res != TextToSpeech.SUCCESS) {
            Log.e("TextToSpeechManager", "Failed to queue TTS speech: $res")
            utteranceCallbacks.remove(utteranceId)?.invoke()
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        utteranceCallbacks.clear()
        pendingSpeechText = null
        pendingOnDone = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
