package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class AudioCaptureManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    companion object {
        private const val TAG = "AudioCaptureManager"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val WAVEFORM_BAR_COUNT = 32
    }

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _waveformFlow = MutableStateFlow<List<Float>>(List(WAVEFORM_BAR_COUNT) { 0.05f })
    val waveformFlow: StateFlow<List<Float>> = _waveformFlow.asStateFlow()

    private val _audioDataFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val audioDataFlow: SharedFlow<ByteArray> = _audioDataFlow.asSharedFlow()

    private val _captureError = MutableStateFlow<String?>(null)
    val captureError: StateFlow<String?> = _captureError.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private val lock = Any()

    private val smoothedWaveform = FloatArray(WAVEFORM_BAR_COUNT) { 0.05f }

    /**
     * Checks whether microphone recording permission is granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts microphone audio capture with complete lifecycle and error safeguards.
     * Returns true if capture successfully initiated.
     */
    fun startCapture(): Boolean {
        synchronized(lock) {
            if (isRunning.get()) {
                Log.d(TAG, "Audio capture is already running.")
                return true
            }

            if (!hasPermission()) {
                val error = "RECORD_AUDIO permission is missing."
                Log.w(TAG, error)
                _captureError.value = error
                return false
            }

            try {
                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )

                if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    val error = "Failed to determine minimum audio buffer size ($minBufferSize)"
                    Log.e(TAG, error)
                    _captureError.value = error
                    return false
                }

                // Buffer size for ~100ms chunks (16000 samples/sec * 2 bytes/sample * 0.1s = 3200 bytes)
                val bufferSize = (minBufferSize * 2).coerceAtLeast(3200)

                val record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    // Try fallback to MIC source if VOICE_RECOGNITION fails on certain hardware
                    val fallbackRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                    )
                    if (fallbackRecord.state != AudioRecord.STATE_INITIALIZED) {
                        fallbackRecord.release()
                        val error = "AudioRecord initialization failed on device hardware."
                        Log.e(TAG, error)
                        _captureError.value = error
                        return false
                    }
                    audioRecord = fallbackRecord
                } else {
                    audioRecord = record
                }

                audioRecord?.startRecording()
                if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    val error = "AudioRecord failed to start recording."
                    Log.e(TAG, error)
                    _captureError.value = error
                    releaseAudioRecord()
                    return false
                }

                isRunning.set(true)
                _isCapturing.value = true
                _captureError.value = null

                // Launch audio read loop
                recordingJob = scope.launch {
                    readAudioLoop(bufferSize)
                }

                Log.i(TAG, "Microphone audio capture started successfully at ${SAMPLE_RATE}Hz.")
                return true

            } catch (e: Exception) {
                val error = "Exception while starting audio capture: ${e.localizedMessage}"
                Log.e(TAG, error, e)
                _captureError.value = error
                releaseAudioRecord()
                return false
            }
        }
    }

    /**
     * Continuous audio read loop on background Dispatcher.
     */
    private suspend fun readAudioLoop(bufferSize: Int) {
        val audioBuffer = ByteArray(bufferSize)
        val shortBuffer = ShortArray(bufferSize / 2)

        while (scope.isActive && isRunning.get()) {
            val record = audioRecord ?: break
            val bytesRead = record.read(audioBuffer, 0, audioBuffer.size)

            if (bytesRead > 0) {
                if (_isMuted.value) {
                    // Granular mute: zero out audio samples while session remains active
                    audioBuffer.fill(0, 0, bytesRead)
                    _audioAmplitude.value = 0f
                    _waveformFlow.value = List(WAVEFORM_BAR_COUNT) { 0.02f }
                    _audioDataFlow.emit(audioBuffer.copyOf(bytesRead))
                } else {
                    // Calculate RMS amplitude and real-time waveform from 16-bit PCM samples
                    ByteBuffer.wrap(audioBuffer, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                        .get(shortBuffer, 0, bytesRead / 2)

                    val sampleCount = bytesRead / 2
                    var totalSumSquared = 0.0

                    // Calculate overall RMS
                    for (i in 0 until sampleCount) {
                        val sample = shortBuffer[i].toDouble()
                        totalSumSquared += sample * sample
                    }

                    val overallRms = if (sampleCount > 0) sqrt(totalSumSquared / sampleCount) else 0.0
                    val normalizedAmplitude = (overallRms / 8000.0).coerceIn(0.0, 1.0).toFloat()
                    _audioAmplitude.value = normalizedAmplitude

                    // Compute multi-bin real-time waveform bars across sample chunks
                    if (sampleCount >= WAVEFORM_BAR_COUNT) {
                        val samplesPerBin = sampleCount / WAVEFORM_BAR_COUNT
                        for (bin in 0 until WAVEFORM_BAR_COUNT) {
                            var binSum = 0.0
                            val startIdx = bin * samplesPerBin
                            for (s in 0 until samplesPerBin) {
                                val sample = shortBuffer[startIdx + s].toDouble()
                                binSum += abs(sample)
                            }
                            val binAverage = (binSum / samplesPerBin) / 6000.0
                            val targetHeight = (binAverage * 1.4).coerceIn(0.06, 1.0).toFloat()

                            // Smooth decay filter for natural fluid visualizer movement
                            smoothedWaveform[bin] = (smoothedWaveform[bin] * 0.35f + targetHeight * 0.65f)
                                .coerceIn(0.05f, 1.0f)
                        }
                        _waveformFlow.value = smoothedWaveform.toList()
                    }

                    _audioDataFlow.emit(audioBuffer.copyOf(bytesRead))
                }
            } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                Log.w(TAG, "AudioRecord read error: $bytesRead")
                break
            }
        }

        // Reset amplitude & waveform when loop exits
        _audioAmplitude.value = 0f
        _waveformFlow.value = List(WAVEFORM_BAR_COUNT) { 0.05f }
    }

    /**
     * Granular mute/unmute control during an active capture session.
     */
    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            _audioAmplitude.value = 0f
            _waveformFlow.value = List(WAVEFORM_BAR_COUNT) { 0.02f }
        }
        Log.d(TAG, "Microphone capture muted: $muted")
    }

    /**
     * Toggles mute state.
     */
    fun toggleMute(): Boolean {
        val newMuted = !_isMuted.value
        setMuted(newMuted)
        return newMuted
    }

    /**
     * Updates waveform and amplitude dynamically from an external audio level source (e.g. SpeechRecognizer).
     * This provides fluid, responsive waveform feedback during speech recognition without audio hardware conflicts.
     */
    fun feedRmsLevel(normalizedRms: Float) {
        if (_isMuted.value) {
            _audioAmplitude.value = 0f
            _waveformFlow.value = List(WAVEFORM_BAR_COUNT) { 0.02f }
            return
        }

        val clampedRms = normalizedRms.coerceIn(0f, 1f)
        _audioAmplitude.value = clampedRms

        val nowMs = System.currentTimeMillis()
        for (bin in 0 until WAVEFORM_BAR_COUNT) {
            // Mid-range frequencies have higher natural acoustic energy
            val centerDist = abs(bin - WAVEFORM_BAR_COUNT / 2f) / (WAVEFORM_BAR_COUNT / 2f)
            val bellFactor = (1f - centerDist * 0.45f).coerceIn(0.4f, 1f)
            val harmonic = sin((bin * 0.55 + (nowMs % 10000) * 0.008).toDouble()).toFloat() * 0.18f
            val targetHeight = (clampedRms * bellFactor * (1f + harmonic)).coerceIn(0.06f, 1.0f)

            // Smooth interpolation
            smoothedWaveform[bin] = (smoothedWaveform[bin] * 0.35f + targetHeight * 0.65f)
                .coerceIn(0.05f, 1.0f)
        }
        _waveformFlow.value = smoothedWaveform.toList()
    }

    /**
     * Stops microphone capture gracefully, releasing hardware resources.
     */
    fun stopCapture() {
        synchronized(lock) {
            if (!isRunning.get()) {
                return
            }

            isRunning.set(false)
            _isCapturing.value = false
            _audioAmplitude.value = 0f
            _waveformFlow.value = List(WAVEFORM_BAR_COUNT) { 0.05f }

            recordingJob?.cancel()
            recordingJob = null

            releaseAudioRecord()
            Log.i(TAG, "Microphone audio capture stopped and resources released.")
        }
    }

    private fun releaseAudioRecord() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
            isRunning.set(false)
            _isCapturing.value = false
        }
    }

    /**
     * Clears any active error message.
     */
    fun clearError() {
        _captureError.value = null
    }

    /**
     * Full teardown for component cleanup.
     */
    fun destroy() {
        stopCapture()
    }
}
