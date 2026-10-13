package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mayraa_settings")

data class UserSettings(
    val wakeWordEnabled: Boolean = false,
    val backgroundListeningEnabled: Boolean = false,
    val alwaysOnListeningEnabled: Boolean = false,
    val language: String = "hinglish", // "hinglish", "hindi", "english"
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.15f,
    val userNickname: String = "Deepu",
    val apiKeyOverride: String = "",
    val selectedVoiceName: String = ""
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val BACKGROUND_LISTENING = booleanPreferencesKey("background_listening")
        val ALWAYS_ON_LISTENING = booleanPreferencesKey("always_on_listening")
        val LANGUAGE = stringPreferencesKey("language")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val USER_NICKNAME = stringPreferencesKey("user_nickname")
        val API_KEY_OVERRIDE = stringPreferencesKey("api_key_override")
        val SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            wakeWordEnabled = preferences[Keys.WAKE_WORD_ENABLED] ?: false,
            backgroundListeningEnabled = preferences[Keys.BACKGROUND_LISTENING] ?: false,
            alwaysOnListeningEnabled = preferences[Keys.ALWAYS_ON_LISTENING] ?: false,
            language = preferences[Keys.LANGUAGE] ?: "hinglish",
            speechRate = preferences[Keys.SPEECH_RATE] ?: 1.0f,
            speechPitch = preferences[Keys.SPEECH_PITCH] ?: 1.15f,
            userNickname = preferences[Keys.USER_NICKNAME] ?: "Deepu",
            apiKeyOverride = preferences[Keys.API_KEY_OVERRIDE] ?: "",
            selectedVoiceName = preferences[Keys.SELECTED_VOICE_NAME] ?: ""
        )
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setBackgroundListeningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_LISTENING] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.SPEECH_PITCH] = pitch }
    }

    suspend fun setUserNickname(nickname: String) {
        context.dataStore.edit { it[Keys.USER_NICKNAME] = nickname }
    }

    suspend fun setAlwaysOnListeningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALWAYS_ON_LISTENING] = enabled }
    }

    suspend fun setSelectedVoiceName(voiceName: String) {
        context.dataStore.edit { it[Keys.SELECTED_VOICE_NAME] = voiceName }
    }

    suspend fun setApiKeyOverride(apiKey: String) {
        context.dataStore.edit { it[Keys.API_KEY_OVERRIDE] = apiKey }
    }
}
