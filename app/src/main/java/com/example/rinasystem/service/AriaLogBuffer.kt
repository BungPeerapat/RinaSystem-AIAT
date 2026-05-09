package com.example.rinasystem.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton log buffer สำหรับแสดง log ใน UI (debug camera/mic/service)
 * เก็บ log ล่าสุดไม่เกิน MAX_LINES
 */
object AriaLogBuffer {
    private const val MAX_LINES = 200

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun log(tag: String, message: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
        val entry = "[$ts] $tag: $message"
        _lines.value = (_lines.value + entry).takeLast(MAX_LINES)
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
