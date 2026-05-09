package com.example.rinasystem.service

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val TAG = "TtsPlayer"

/**
 * เล่นเสียง TTS / Audio ที่ได้รับจาก Server (base64 encoded)
 * - รับ audio_base64 string → Decode → เล่นผ่าน MediaPlayer
 * - รองรับ queue (เล่นทีละอัน)
 * - รองรับ loop mode (เล่นซ้ำจนกว่าจะ stop)
 */
class TtsPlayer(
    private val cacheDir: File,
) {
    private data class AudioItem(val base64: String, val volume: Float = 1.0f)

    private val audioQueue = Channel<AudioItem>(capacity = Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentPlayer: MediaPlayer? = null

    @Volatile
    private var currentDone: CompletableDeferred<Unit>? = null

    @Volatile
    private var isLooping = false

    @Volatile
    private var loopItem: AudioItem? = null

    init {
        scope.launch {
            for (item in audioQueue) {
                playAudioItem(item)
            }
        }
    }

    /** เพิ่ม TTS/audio เข้า queue (เล่นต่อจาก audio ก่อนหน้าถ้ามี) */
    fun enqueue(audioBase64: String, volume: Float = 1.0f) {
        audioQueue.trySend(AudioItem(audioBase64, volume.coerceIn(0f, 1f)))
    }

    /** เล่น audio แบบ loop (เล่นซ้ำจนกว่าจะ stop) */
    fun enqueueLoop(audioBase64: String, volume: Float = 1.0f) {
        stop() // หยุด audio ที่กำลังเล่นอยู่
        isLooping = true
        val item = AudioItem(audioBase64, volume.coerceIn(0f, 1f))
        loopItem = item
        audioQueue.trySend(item)
    }

    /** หยุดเล่น audio ปัจจุบันและล้าง queue + หยุด loop */
    fun stop() {
        isLooping = false
        loopItem = null
        currentPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) { }
        }
        currentPlayer = null
        // unblock the suspended playFileSync coroutine so the queue loop can continue
        currentDone?.complete(Unit)
        currentDone = null
        // ล้าง queue ที่ค้างอยู่
        while (!audioQueue.isEmpty) {
            audioQueue.tryReceive()
        }
    }

    private suspend fun playAudioItem(item: AudioItem) {
        val ext = guessExtension(item.base64)
        val tempFile = File(cacheDir, "audio_${System.currentTimeMillis()}.$ext")
        try {
            val audioBytes = Base64.decode(item.base64, Base64.DEFAULT)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            playFileSync(tempFile.absolutePath, item.volume)

            // ถ้า loop mode → enqueue ซ้ำ
            if (isLooping && loopItem?.base64 == item.base64) {
                audioQueue.trySend(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback error: ${e.message}")
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun playFileSync(filePath: String, volume: Float = 1.0f) {
        val done = CompletableDeferred<Unit>()
        currentDone = done

        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(filePath)
            setOnCompletionListener {
                release()
                currentPlayer = null
                currentDone = null
                done.complete(Unit)
            }
            setOnErrorListener { _, _, _ ->
                release()
                currentPlayer = null
                currentDone = null
                done.complete(Unit)
                true
            }
            prepare()
            setVolume(volume, volume)
            start()
        }

        currentPlayer = player
        done.await()
    }

    /** ลองเดา extension จาก base64 header bytes */
    private fun guessExtension(base64: String): String {
        return try {
            val header = Base64.decode(base64.take(16), Base64.DEFAULT)
            guessExtensionFromHeader(header)
        } catch (_: Exception) {
            "tmp"
        }
    }

    companion object {
        /** Guess audio file extension from raw header bytes. Visible for testing. */
        @JvmStatic
        fun guessExtensionFromHeader(header: ByteArray): String {
            return when {
                header.size >= 4 && header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() -> "wav"
                header.size >= 3 && header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() -> "mp3"
                header.size >= 4 && header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0 -> "mp3"
                header.size >= 8 && header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() -> "m4a"
                else -> "tmp"
            }
        }
    }

    fun release() {
        stop()
        audioQueue.close()
    }
}
