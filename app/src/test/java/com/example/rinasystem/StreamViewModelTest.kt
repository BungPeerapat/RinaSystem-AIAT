package com.example.rinasystem

import com.example.rinasystem.ui.viewmodel.StreamViewModel
import org.junit.Assert.*
import org.junit.Test

class StreamViewModelTest {

    // ─── computeAmplitude tests ─────────────────────────────────────────────────

    @Test
    fun `empty bytes returns 0`() {
        assertEquals(0f, StreamViewModel.computeAmplitude(byteArrayOf()), 0.0001f)
    }

    @Test
    fun `silence (all zeros) returns 0`() {
        // 4 samples of silence (16-bit LE)
        val silence = ByteArray(8) { 0 }
        assertEquals(0f, StreamViewModel.computeAmplitude(silence), 0.0001f)
    }

    @Test
    fun `max positive amplitude returns 1`() {
        // 16-bit LE max positive = 0x7FFF = 32767
        // Two samples of max amplitude
        val maxAmp = byteArrayOf(
            0xFF.toByte(), 0x7F,  // 32767 LE
            0xFF.toByte(), 0x7F,  // 32767 LE
        )
        val result = StreamViewModel.computeAmplitude(maxAmp)
        // RMS of [32767, 32767] = 32767, normalized = 32767/32768 ≈ 0.99997
        assertTrue("Expected ~1.0, got $result", result > 0.99f)
    }

    @Test
    fun `max negative amplitude returns ~1`() {
        // 16-bit LE min negative = 0x8000 = -32768
        val minAmp = byteArrayOf(
            0x00, 0x80.toByte(),  // -32768 LE
            0x00, 0x80.toByte(),  // -32768 LE
        )
        val result = StreamViewModel.computeAmplitude(minAmp)
        // Note: the code reads as signed short, so -32768^2 = 32768^2
        // RMS = 32768, normalized = 32768/32768 = 1.0
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun `mid-level amplitude returns reasonable value`() {
        // ~50% amplitude = 16384
        // 16384 LE = 0x00 0x40
        val midAmp = byteArrayOf(
            0x00, 0x40,  // 16384 LE
            0x00, 0x40,  // 16384 LE
        )
        val result = StreamViewModel.computeAmplitude(midAmp)
        // RMS = 16384, normalized = 16384/32768 = 0.5
        assertEquals(0.5f, result, 0.01f)
    }

    @Test
    fun `odd byte count ignores trailing byte`() {
        // 3 bytes = 1 full sample + 1 trailing byte (ignored)
        val data = byteArrayOf(0xFF.toByte(), 0x7F, 0x42)
        val result = StreamViewModel.computeAmplitude(data)
        // Only one sample: 32767, RMS = 32767, normalized ≈ 1.0
        assertTrue("Expected ~1.0, got $result", result > 0.99f)
    }

    @Test
    fun `single byte returns 0`() {
        // Not enough for a 16-bit sample — early return 0
        assertEquals(0f, StreamViewModel.computeAmplitude(byteArrayOf(0x42)), 0.0001f)
    }

    @Test
    fun `result is clamped between 0 and 1`() {
        // Any input should return value in [0, 1]
        val random = byteArrayOf(
            0x12, 0x34, 0x56, 0x78,
            0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x01,
        )
        val result = StreamViewModel.computeAmplitude(random)
        assertTrue("Result $result should be >= 0", result >= 0f)
        assertTrue("Result $result should be <= 1", result <= 1f)
    }
}
