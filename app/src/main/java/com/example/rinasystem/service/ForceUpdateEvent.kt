package com.example.rinasystem.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton event สำหรับแจ้ง UI ว่า Admin สั่ง Force Update
 */
object ForceUpdateEvent {
    private val _event = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val event: SharedFlow<Unit> = _event.asSharedFlow()

    fun emit() {
        _event.tryEmit(Unit)
    }
}
