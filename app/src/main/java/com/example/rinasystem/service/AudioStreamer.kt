package com.example.rinasystem.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.rinasystem.data.ws.AriaWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "AudioStreamer"

/**
 * จับเสียง mic แบบ real-time แล้วส่งผ่าน WebSocket เป็น PCM16 binary frames
 *
 * Quality settings:
 *   low    → 8000 Hz, 16 bit, mono
 *   medium → 16000 Hz, 16 bit, mono
 *   high   → 44100 Hz, 16 bit, mono
 */
class AudioStreamer(
    private val ariaWebSocket: AriaWebSocket,
) {
    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isActive: Boolean = false
        private set

    fun start(quality: String) {
        if (isActive) return
        isActive = true

        val sampleRate = when (quality) {
            "low" -> 8_000
            "high" -> 44_100
            else -> 16_000  // medium (default)
        }

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            .coerceAtLeast(4096)

        recordJob = scope.launch {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
            )
            audioRecord = record

            try {
                record.startRecording()
                Log.i(TAG, "Audio streaming started: ${sampleRate}Hz quality=$quality")

                val buffer = ByteArray(bufferSize)
                while (isActive && coroutineContext.isActive) {
                    val bytesRead = record.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        ariaWebSocket.sendAudioFrame(buffer.copyOf(bytesRead))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio recording error: ${e.message}")
            } finally {
                record.stop()
                record.release()
                audioRecord = null
                Log.i(TAG, "Audio streaming stopped")
            }
        }
    }

    fun stop() {
        isActive = false
        recordJob?.cancel()
        recordJob = null
    }
}
