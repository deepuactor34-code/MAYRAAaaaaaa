package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioCaptureManager
import com.example.data.remote.GeminiSessionManager
import com.example.domain.model.ToolCall
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies Mayraa AI branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val devName = context.getString(R.string.developer_name)
        assertEquals("Mayraa AI", appName)
        assertEquals("Deepu Prankstar", devName)
    }

    @Test
    fun `local intent engine parses youtube command`() {
        val sessionManager = GeminiSessionManager()
        val result = sessionManager.parseLocalIntent("YouTube par song chalao")
        assertNotNull(result.toolCall)
        assertTrue(result.toolCall is ToolCall.OpenYouTube)
        assertTrue(result.replyText.contains("YouTube"))
    }

    @Test
    fun `local intent engine parses call command`() {
        val sessionManager = GeminiSessionManager()
        val result = sessionManager.parseLocalIntent("Rahul ko call karo")
        assertNotNull(result.toolCall)
        assertTrue(result.toolCall is ToolCall.PrepareCall)
    }

    @Test
    fun `local intent engine parses timer command`() {
        val sessionManager = GeminiSessionManager()
        val result = sessionManager.parseLocalIntent("10 minute ka timer lagao")
        assertNotNull(result.toolCall)
        assertTrue(result.toolCall is ToolCall.SetTimer)
        val timer = result.toolCall as ToolCall.SetTimer
        assertEquals(600, timer.durationSeconds)
    }

    @Test
    fun `local intent engine parses personality question`() {
        val sessionManager = GeminiSessionManager()
        val result = sessionManager.parseLocalIntent("Mayraa, kya kar rahi ho?")
        assertTrue(result.replyText.contains("Deepu"))
    }

    @Test
    fun `audio capture manager granular mute and lifecycle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val captureManager = AudioCaptureManager(context)

        assertFalse(captureManager.isCapturing.value)
        assertFalse(captureManager.isMuted.value)

        // Test granular mute control
        captureManager.setMuted(true)
        assertTrue(captureManager.isMuted.value)
        assertEquals(0f, captureManager.audioAmplitude.value, 0.001f)

        captureManager.setMuted(false)
        assertFalse(captureManager.isMuted.value)

        // Safe teardown
        captureManager.stopCapture()
        assertFalse(captureManager.isCapturing.value)
    }

    @Test
    fun `audio capture manager waveform flow has 32 bars`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val captureManager = AudioCaptureManager(context)

        val waveform = captureManager.waveformFlow.value
        assertEquals(32, waveform.size)
        assertTrue(waveform.all { it >= 0f })

        // When muted, waveform stays at baseline minimum
        captureManager.setMuted(true)
        val mutedWaveform = captureManager.waveformFlow.value
        assertEquals(32, mutedWaveform.size)
        assertTrue(mutedWaveform.all { it <= 0.05f })
    }

    @Test
    fun `settings repository stores always on listening and selected voice`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsRepo = com.example.data.repository.SettingsRepository(context)

        settingsRepo.setAlwaysOnListeningEnabled(true)
        settingsRepo.setSelectedVoiceName("hi-in-x-cfh#female")

        val current = settingsRepo.settingsFlow.first()
        assertTrue(current.alwaysOnListeningEnabled)
        assertEquals("hi-in-x-cfh#female", current.selectedVoiceName)
    }
}
