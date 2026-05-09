package com.example.rinasystem.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton event สำหรับแจ้ง MessageViewModel ว่ามี TTS ใหม่เข้ามา
 */
object TtsNewMessageEvent {
    private val _event = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val event: SharedFlow<Unit> = _event.asSharedFlow()

    fun emit() {
        _event.tryEmit(Unit)
    }
}
