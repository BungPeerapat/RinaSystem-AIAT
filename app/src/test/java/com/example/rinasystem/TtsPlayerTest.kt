package com.example.rinasystem

import com.example.rinasystem.service.TtsPlayer
import org.junit.Assert.*
import org.junit.Test

class TtsPlayerTest {

    @Test
    fun `WAV header detected - RIFF`() {
        // "RIFF" = 0x52 0x49 0x46 0x46
        val header = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00)
        assertEquals("wav", TtsPlayer.guessExtensionFromHeader(header))
    }

    @Test
    fun `MP3 header detected - ID3 tag`() {
        // "ID3" = 0x49 0x44 0x33
        val header = byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x00, 0x00)
        assertEquals("mp3", TtsPlayer.guessExtensionFromHeader(header))
    }

    @Test
    fun `MP3 header detected - sync word 0xFFEx`() {
        // MP3 sync: 0xFF 0xFB (MPEG1 Layer3)
        val header = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00)
        assertEquals("mp3", TtsPlayer.guessExtensionFromHeader(header))
    }

    @Test
    fun `M4A header detected - ftyp`() {
        // M4A: offset 4-5 = "ft" (part of "ftyp")
        val header = byteArrayOf(0x00, 0x00, 0x00, 0x20, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())
        assertEquals("m4a", TtsPlayer.guessExtensionFromHeader(header))
    }

    @Test
    fun `unknown header returns tmp`() {
        val header = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        assertEquals("tmp", TtsPlayer.guessExtensionFromHeader(header))
    }

    @Test
    fun `empty header returns tmp`() {
        assertEquals("tmp", TtsPlayer.guessExtensionFromHeader(byteArrayOf()))
    }

    @Test
    fun `short header returns tmp`() {
        assertEquals("tmp", TtsPlayer.guessExtensionFromHeader(byteArrayOf(0x01)))
    }
}
