package com.example.rinasystem.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * รับ Intent เมื่อ User ปัดทิ้ง หรือกด TTS notification
 * - ACTION_CLEAR_TTS: ปัดทิ้ง → clear pending list
 * - ACTION_CLEAR_AND_OPEN: กด → clear แล้วเปิดแอป
 *
 * หมายเหตุ: setAutoCancel(true) ไม่ fire deleteIntent เมื่อกด tap (Android behavior)
 * ดังนั้น contentIntent ชี้มา ACTION_CLEAR_AND_OPEN แทน
 */
class TtsNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CLEAR_TTS -> TtsNotificationStore.clear()
            ACTION_CLEAR_AND_OPEN -> {
                TtsNotificationStore.clear()
                val launch = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
                if (launch != null) context.startActivity(launch)
            }
        }
    }

    companion object {
        const val ACTION_CLEAR_TTS = "com.example.rinasystem.ACTION_CLEAR_TTS"
        const val ACTION_CLEAR_AND_OPEN = "com.example.rinasystem.ACTION_CLEAR_AND_OPEN"
    }
}
