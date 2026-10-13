package com.example

import com.example.data.remote.GeminiSessionManager
import com.example.domain.model.ToolCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMotivationIntent() {
        val manager = GeminiSessionManager()
        val result = manager.parseLocalIntent("Mayraa mujhe motivate karo")
        assertTrue(result.replyText.contains("Deepu"))
        assertTrue(result.replyText.contains("ideas") || result.replyText.contains("reality"))
    }

    @Test
    fun testCameraIntent() {
        val manager = GeminiSessionManager()
        val result = manager.parseLocalIntent("Camera kholo")
        assertNotNull(result.toolCall)
        assertTrue(result.toolCall is ToolCall.OpenApp)
        assertEquals("camera", (result.toolCall as ToolCall.OpenApp).appName)
    }

    @Test
    fun testVolumeIntent() {
        val manager = GeminiSessionManager()
        val result = manager.parseLocalIntent("Volume badhao")
        assertNotNull(result.toolCall)
        assertTrue(result.toolCall is ToolCall.AdjustVolume)
        assertEquals("up", (result.toolCall as ToolCall.AdjustVolume).direction)
    }

    @Test
    fun testMuteIntent() {
        val manager = GeminiSessionManager()
        val result = manager.parseLocalIntent("Volume mute karo")
        assertNotNull(result.toolCall)
        assertTrue(result.toolCall is ToolCall.AdjustVolume)
        assertEquals("mute", (result.toolCall as ToolCall.AdjustVolume).direction)
    }

    @Test
    fun testSilenceIntent() {
        val manager = GeminiSessionManager()
        val result = manager.parseLocalIntent("Mayraa chup ho jao")
        assertTrue(result.replyText.contains("shant") || result.replyText.contains("chup"))
    }
}
