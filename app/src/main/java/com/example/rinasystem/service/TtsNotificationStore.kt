package com.example.rinasystem.service

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe store สำหรับ pending TTS notification texts
 * - add() เรียกจาก IO thread (StreamingService)
 * - clear() เรียกจาก main thread (TtsNotificationReceiver)
 */
object TtsNotificationStore {
    private const val MAX_ENTRIES = 5
    private val _texts = CopyOnWriteArrayList<String>()

    /** prepend ข้อความใหม่ (newest on top) แล้ว trim ให้เหลือ MAX_ENTRIES */
    fun add(text: String) {
        _texts.add(0, text)
        while (_texts.size > MAX_ENTRIES) _texts.removeAt(_texts.size - 1)
    }

    fun getAll(): List<String> = _texts.toList()
    fun size(): Int = _texts.size
    fun clear() { _texts.clear() }
}
