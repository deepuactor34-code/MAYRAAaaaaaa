package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MayraaApplication
import com.example.audio.AudioCaptureManager
import com.example.audio.SpeechRecognitionManager
import com.example.audio.TextToSpeechManager
import com.example.data.repository.UserSettings
import com.example.domain.model.AssistantState
import com.example.domain.model.ChatMessage
import com.example.domain.model.ContactInfo
import com.example.domain.model.MessageSender
import com.example.domain.model.ToolCall
import com.example.domain.model.ToolExecutionResult
import com.example.service.MayraaVoiceService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val assistantState: AssistantState = AssistantState.IDLE,
    val messages: List<ChatMessage> = emptyList(),
    val partialVoiceText: String = "",
    val rmsLevel: Float = 0f,
    val disambiguationResult: ToolExecutionResult? = null,
    val errorMessage: String? = null,
    val isFallbackMode: Boolean = false,
    val isMicActive: Boolean = false,
    val isMuted: Boolean = false,
    val waveform: List<Float> = List(32) { 0.05f }
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MayraaApplication
    private val settingsRepo = app.settingsRepository
    private val activityRepo = app.activityLogRepository
    private val toolEngine = app.toolExecutionEngine
    private val geminiSession = app.geminiSessionManager

    val userSettings: StateFlow<UserSettings> = settingsRepo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserSettings()
    )

    val activityLogs = activityRepo.logs

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Robust low-level audio capture manager
    val audioCaptureManager = AudioCaptureManager(application, viewModelScope)

    private val textToSpeechManager = TextToSpeechManager(application)
    private val speechRecognitionManager = SpeechRecognitionManager(
        context = application,
        onResultCallback = { finalResult -> onSpeechRecognized(finalResult) },
        onErrorCallback = { errorCode, errorMsg -> onSpeechError(errorCode, errorMsg) }
    )

    // Expose available system TTS voices
    val availableTtsVoices = textToSpeechManager.availableVoices

    init {
        // Observe speech recognition state
        viewModelScope.launch {
            speechRecognitionManager.isListening.collect { listening ->
                _uiState.value = _uiState.value.copy(
                    isMicActive = listening,
                    assistantState = if (listening) AssistantState.LISTENING else {
                        if (_uiState.value.assistantState == AssistantState.LISTENING) AssistantState.IDLE else _uiState.value.assistantState
                    }
                )
            }
        }

        // Observe real-time multi-bin waveform from AudioCaptureManager
        viewModelScope.launch {
            audioCaptureManager.waveformFlow.collect { wf ->
                _uiState.value = _uiState.value.copy(waveform = wf)
            }
        }

        // Observe high-fidelity audio amplitude from AudioCaptureManager
        viewModelScope.launch {
            audioCaptureManager.audioAmplitude.collect { amplitude ->
                if (_uiState.value.isMicActive && !_uiState.value.isMuted) {
                    _uiState.value = _uiState.value.copy(rmsLevel = amplitude)
                } else if (_uiState.value.isMuted) {
                    _uiState.value = _uiState.value.copy(rmsLevel = 0f)
                }
            }
        }

        // Observe RMS audio level from SpeechRecognizer, update rmsLevel and feed waveform generator
        viewModelScope.launch {
            speechRecognitionManager.rmsDb.collect { rms ->
                if (!_uiState.value.isMuted && _uiState.value.isMicActive) {
                    _uiState.value = _uiState.value.copy(rmsLevel = rms)
                    audioCaptureManager.feedRmsLevel(rms)
                } else if (_uiState.value.isMuted) {
                    _uiState.value = _uiState.value.copy(rmsLevel = 0f)
                }
            }
        }

        // Observe partial spoken text
        viewModelScope.launch {
            speechRecognitionManager.partialText.collect { partial ->
                _uiState.value = _uiState.value.copy(partialVoiceText = partial)
            }
        }

        // Observe TTS speaking state
        viewModelScope.launch {
            textToSpeechManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    _uiState.value = _uiState.value.copy(assistantState = AssistantState.SPEAKING)
                } else if (_uiState.value.assistantState == AssistantState.SPEAKING) {
                    _uiState.value = _uiState.value.copy(assistantState = AssistantState.IDLE)
                }
            }
        }

        // Keep TTS tuned to user preferences including chosen voice
        viewModelScope.launch {
            userSettings.collect { settings ->
                textToSpeechManager.updateSettings(
                    pitch = settings.speechPitch,
                    rate = settings.speechRate,
                    language = settings.language,
                    voiceName = settings.selectedVoiceName
                )
            }
        }

        // Initial welcome message from Mayraa
        val welcomeMsg = ChatMessage(
            sender = MessageSender.MAYRAA,
            text = "Namaste Deepu! Mayraa AI online hai. 🔥 Bolo aaj kya karwana hai?"
        )
        _uiState.value = _uiState.value.copy(messages = listOf(welcomeMsg))
        textToSpeechManager.speak(welcomeMsg.text)
    }

    fun isSpeechRecognizerAvailable(): Boolean = speechRecognitionManager.isAvailable()

    fun createSpeechIntent(): Intent = speechRecognitionManager.createSpeechIntent(userSettings.value.language)

    // Continuous multi-turn voice session state
    private var isContinuousVoiceActive: Boolean = false
    private var silenceRetryCount: Int = 0
    private val maxSilenceRetries = 5

    /**
     * Granular mute/unmute control for active sessions.
     */
    fun setMuteState(muted: Boolean) {
        if (muted) {
            isContinuousVoiceActive = false
            if (_uiState.value.isMicActive) {
                stopListening()
            }
        }
        _uiState.value = _uiState.value.copy(
            isMuted = muted,
            rmsLevel = if (muted) 0f else _uiState.value.rmsLevel,
            errorMessage = if (muted) "Microphone Muted 🔇 (Tap mic to unmute & speak)" else null
        )
        audioCaptureManager.setMuted(muted)
        textToSpeechManager.isMuted = muted
        if (muted) {
            textToSpeechManager.stop()
        }
    }

    fun toggleMute() {
        setMuteState(!_uiState.value.isMuted)
    }

    /**
     * Starts continuous listening with lifecycle management.
     */
    fun startListening(): Boolean {
        isContinuousVoiceActive = true
        silenceRetryCount = 0

        // Automatically unmute when user triggers listening
        if (_uiState.value.isMuted) {
            setMuteState(false)
        }

        textToSpeechManager.stop()
        _uiState.value = _uiState.value.copy(
            assistantState = AssistantState.LISTENING,
            errorMessage = null,
            partialVoiceText = ""
        )

        // Stop raw AudioRecord capture first so it does not block SpeechRecognizer hardware mic access
        audioCaptureManager.stopCapture()

        // Start speech recognizer in-app
        val started = speechRecognitionManager.startListening(userSettings.value.language)
        if (!started) {
            _uiState.value = _uiState.value.copy(
                assistantState = AssistantState.IDLE,
                errorMessage = "Microphone ready hai. Mic tap karke phir se bolo!"
            )
        }
        return started
    }

    /**
     * Stops listening gracefully and releases hardware capture.
     */
    fun stopListening() {
        isContinuousVoiceActive = false
        silenceRetryCount = 0
        audioCaptureManager.stopCapture()
        speechRecognitionManager.stopListening()
        if (_uiState.value.assistantState == AssistantState.LISTENING) {
            _uiState.value = _uiState.value.copy(
                assistantState = AssistantState.IDLE,
                rmsLevel = 0f
            )
        }
    }

    fun toggleListening(): Boolean {
        return if (_uiState.value.isMicActive) {
            stopListening()
            false
        } else {
            startListening()
        }
    }

    private fun onSpeechError(errorCode: Int, errorMessage: String) {
        if (isContinuousVoiceActive && !_uiState.value.isMuted) {
            if (errorCode == SpeechRecognizer.ERROR_NO_MATCH || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                silenceRetryCount++
                if (silenceRetryCount <= maxSilenceRetries) {
                    // Do not kill continuous session! Seamlessly restart listening after brief interval
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(350)
                        if (isContinuousVoiceActive && !_uiState.value.isMuted && _uiState.value.assistantState != AssistantState.SPEAKING) {
                            speechRecognitionManager.startListening(userSettings.value.language)
                        }
                    }
                    return
                } else {
                    // Session idle timeout reached
                    isContinuousVoiceActive = false
                    _uiState.value = _uiState.value.copy(
                        assistantState = AssistantState.IDLE,
                        rmsLevel = 0f
                    )
                    return
                }
            } else if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || errorCode == SpeechRecognizer.ERROR_CLIENT) {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(450)
                    if (isContinuousVoiceActive && !_uiState.value.isMuted && _uiState.value.assistantState != AssistantState.SPEAKING) {
                        speechRecognitionManager.startListening(userSettings.value.language)
                    }
                }
                return
            }
        }

        isContinuousVoiceActive = false
        val friendlyMsg = when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "Sun rahi hoon Deepu, jab bhi bolna ho bolo ya mic tap karo!"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission allow kijiye voice commands ke liye!"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Network issue aaya. Phir se koshish karein ya likh kar bhejein."
            SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_AUDIO ->
                "Mic ready hai. Tap karke bolo!"
            else ->
                "Voice command notice: $errorMessage"
        }
        _uiState.value = _uiState.value.copy(
            errorMessage = friendlyMsg,
            assistantState = AssistantState.IDLE
        )
    }

    private fun onSpeechRecognized(text: String) {
        if (text.isBlank()) return
        silenceRetryCount = 0
        handleUserQuery(text)
    }

    fun handleUserQuery(query: String) {
        if (query.isBlank()) return

        val isExit = query.trim().lowercase().let { q ->
            q.contains("bye") || q.contains("alvida") || q.contains("chup") || q.contains("stop") || q.contains("bas") || q.contains("band karo")
        }
        if (isExit) {
            isContinuousVoiceActive = false
        }

        // Stop any current audio output
        textToSpeechManager.stop()

        val userMessage = ChatMessage(sender = MessageSender.USER, text = query)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            assistantState = AssistantState.THINKING,
            partialVoiceText = "",
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val apiKeyOverride = userSettings.value.apiKeyOverride.takeIf { it.isNotBlank() }
                val aiResult = geminiSession.processUserMessage(query, apiKeyOverride)

                _uiState.value = _uiState.value.copy(isFallbackMode = aiResult.isFallbackMode)

                var responseText = aiResult.replyText
                var toolExecuted = false
                var toolName: String? = null
                var toolDetail: String? = null

                if (aiResult.toolCall != null) {
                    _uiState.value = _uiState.value.copy(assistantState = AssistantState.EXECUTING)
                    val execResult = toolEngine.execute(aiResult.toolCall)
                    toolExecuted = true
                    toolName = aiResult.toolCall.javaClass.simpleName
                    toolDetail = execResult.feedbackMessage

                    activityRepo.logAction(
                        actionType = toolName,
                        description = execResult.feedbackMessage,
                        isSuccess = execResult.success
                    )

                    if (execResult.requiresDisambiguation) {
                        _uiState.value = _uiState.value.copy(
                            disambiguationResult = execResult,
                            assistantState = AssistantState.IDLE
                        )
                        responseText = execResult.feedbackMessage
                    } else {
                        // Append tool feedback if different from general reply
                        if (!responseText.contains(execResult.feedbackMessage)) {
                            responseText = "$responseText\n${execResult.feedbackMessage}"
                        }
                    }
                }

                val assistantMessage = ChatMessage(
                    sender = MessageSender.MAYRAA,
                    text = responseText,
                    isToolExecuted = toolExecuted,
                    toolName = toolName,
                    toolDetail = toolDetail
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage
                )

                // Speak response aloud if not muted, and auto-listen for next command
                if (!_uiState.value.isMuted) {
                    _uiState.value = _uiState.value.copy(assistantState = AssistantState.SPEAKING)
                    textToSpeechManager.speak(responseText) {
                        if (!isExit && isContinuousVoiceActive) {
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(450)
                                if (!_uiState.value.isMuted && isContinuousVoiceActive && _uiState.value.assistantState != AssistantState.SPEAKING) {
                                    startListening()
                                }
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(assistantState = AssistantState.IDLE)
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(assistantState = AssistantState.IDLE)
                }

            } catch (e: Exception) {
                val errorMsg = "Kuch gadbad ho gayi Deepu: ${e.localizedMessage ?: "Unknown error"}"
                _uiState.value = _uiState.value.copy(
                    assistantState = AssistantState.ERROR,
                    errorMessage = errorMsg
                )
                if (!_uiState.value.isMuted) {
                    textToSpeechManager.speak("Oops! Ek issue aaya Deepu, dobara koshish karein.") {
                        if (isContinuousVoiceActive) {
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(450)
                                if (!_uiState.value.isMuted && isContinuousVoiceActive) startListening()
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectDisambiguatedContact(contact: ContactInfo) {
        val pending = _uiState.value.disambiguationResult?.pendingAction
        _uiState.value = _uiState.value.copy(disambiguationResult = null)

        if (pending != null) {
            val updatedToolCall = when (pending) {
                is ToolCall.PrepareCall -> ToolCall.PrepareCall(contact.name, contact.phoneNumber)
                is ToolCall.PrepareWhatsAppMessage -> ToolCall.PrepareWhatsAppMessage(contact.name, contact.phoneNumber, pending.message)
                is ToolCall.PrepareSMS -> ToolCall.PrepareSMS(contact.name, contact.phoneNumber, pending.message)
                else -> pending
            }

            val execResult = toolEngine.execute(updatedToolCall)
            activityRepo.logAction(
                actionType = updatedToolCall.javaClass.simpleName,
                description = execResult.feedbackMessage,
                isSuccess = execResult.success
            )

            val replyMsg = ChatMessage(
                sender = MessageSender.MAYRAA,
                text = "${contact.name} ke liye: ${execResult.feedbackMessage}",
                isToolExecuted = true,
                toolName = updatedToolCall.javaClass.simpleName,
                toolDetail = execResult.feedbackMessage
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + replyMsg
            )
            if (!_uiState.value.isMuted) {
                textToSpeechManager.speak(execResult.feedbackMessage) {
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(450)
                        if (!_uiState.value.isMuted && _uiState.value.assistantState != AssistantState.SPEAKING) {
                            startListening()
                        }
                    }
                }
            }
        }
    }

    fun dismissDisambiguation() {
        _uiState.value = _uiState.value.copy(disambiguationResult = null)
    }

    fun stopSpeaking() {
        textToSpeechManager.stop()
        _uiState.value = _uiState.value.copy(assistantState = AssistantState.IDLE)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setWakeWordEnabled(enabled)
            if (enabled && userSettings.value.backgroundListeningEnabled) {
                MayraaVoiceService.startService(getApplication())
            } else if (!enabled) {
                MayraaVoiceService.stopService(getApplication())
            }
        }
    }

    fun setBackgroundListeningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setBackgroundListeningEnabled(enabled)
            if (enabled && userSettings.value.wakeWordEnabled) {
                MayraaVoiceService.startService(getApplication())
            } else {
                MayraaVoiceService.stopService(getApplication())
            }
        }
    }

    fun setAlwaysOnListening(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAlwaysOnListeningEnabled(enabled)
            if (enabled) {
                MayraaVoiceService.startService(getApplication())
                startListening()
            } else {
                if (!userSettings.value.backgroundListeningEnabled && !userSettings.value.wakeWordEnabled) {
                    MayraaVoiceService.stopService(getApplication())
                }
            }
        }
    }

    fun setSelectedVoice(voiceName: String) {
        viewModelScope.launch {
            settingsRepo.setSelectedVoiceName(voiceName)
            textToSpeechManager.setSpecificVoice(voiceName)
        }
    }

    fun testVoice(voiceName: String) {
        textToSpeechManager.setSpecificVoice(voiceName)
        textToSpeechManager.speak("Namaste Deepu! Ye Mayraa ki voice preview hai. Kaisi lagi?")
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepo.setLanguage(language)
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepo.setSpeechRate(rate)
        }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            settingsRepo.setSpeechPitch(pitch)
        }
    }

    fun setApiKeyOverride(apiKey: String) {
        viewModelScope.launch {
            settingsRepo.setApiKeyOverride(apiKey)
        }
    }

    fun clearChat() {
        val freshWelcome = ChatMessage(
            sender = MessageSender.MAYRAA,
            text = "Chat history clear ho gayi hai! Fresh shuru karte hain, Deepu. ✨"
        )
        _uiState.value = _uiState.value.copy(messages = listOf(freshWelcome))
    }

    fun clearActivityLogs() {
        activityRepo.clearLogs()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        audioCaptureManager.destroy()
        speechRecognitionManager.destroy()
        textToSpeechManager.shutdown()
        super.onCleared()
    }
}
