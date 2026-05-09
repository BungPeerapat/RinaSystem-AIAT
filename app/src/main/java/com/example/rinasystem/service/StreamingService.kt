package com.example.rinasystem.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.rinasystem.data.local.TokenManager
import com.example.rinasystem.data.ws.AriaWebSocket
import com.example.rinasystem.data.ws.UserWsListener
import com.example.rinasystem.data.ws.WsCommand
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "StreamingService"
private const val CHANNEL_ID = "aria_streaming_channel"
private const val TTS_CHANNEL_ID = "aria_tts_channel"
private const val NOTIFICATION_ID = 1001
private const val TTS_NOTIFICATION_ID = 1002

/**
 * Foreground Service สำหรับ streaming mic/camera ไปยัง Admin
 *
 * User ไม่เห็น notification (channel IMPORTANCE_MIN + silent)
 * ทำงานต่อแม้ User ปิดแอป
 */
@AndroidEntryPoint
class StreamingService : LifecycleService() {

    @Inject lateinit var ariaWebSocket: AriaWebSocket
    @Inject lateinit var tokenManager: TokenManager

    private var audioStreamer: AudioStreamer? = null
    private var videoStreamer: VideoStreamer? = null
    private var ttsPlayer: TtsPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    inner class StreamingBinder : Binder() {
        fun getService(): StreamingService = this@StreamingService
    }

    private val binder = StreamingBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        TtsNotificationStore.clear()
        createHiddenNotificationChannel()
        createTtsNotificationChannel()
        // Android 14+ requires specifying foreground service type in startForeground()
        startForeground(
            NOTIFICATION_ID,
            buildHiddenNotification(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
        )

        // WakeLock กัน CPU sleep เมื่อปิดจอ — ให้ WebSocket ทำงานต่อได้
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aria:streaming").apply {
            acquire()
        }

        audioStreamer = AudioStreamer(ariaWebSocket)
        videoStreamer = VideoStreamer(applicationContext, ariaWebSocket)
        ttsPlayer = TtsPlayer(cacheDir)

        // ขอ Battery Optimization Exemption — ป้องกัน Doze kill service
        requestBatteryOptimizationExemption()

        connectUserWebSocket()
        Log.i(TAG, "StreamingService created")
        AriaLogBuffer.log(TAG, "Service created")
    }

    private fun connectUserWebSocket() {
        scope.launch {
            val userId = getUserId() ?: return@launch

            ariaWebSocket.connectUser(userId, object : UserWsListener {
                override fun onCommand(command: WsCommand) {
                    // CameraX ต้อง Main thread, แต่ TTS/Audio ต้องไม่ block Main
                    when (command) {
                        is WsCommand.TtsPlay, is WsCommand.AudioPlay, is WsCommand.StopAudio ->
                            ioScope.launch { handleCommand(command) }
                        else ->
                            scope.launch { handleCommand(command) }
                    }
                }

                override fun onConnected() {
                    Log.i(TAG, "User WS connected in StreamingService")
                    AriaLogBuffer.log(TAG, "WS connected")
                }

                override fun onDisconnected() {
                    Log.i(TAG, "User WS disconnected in StreamingService")
                    AriaLogBuffer.log(TAG, "WS disconnected")
                }

                override fun onError(message: String) {
                    Log.w(TAG, "User WS error: $message")
                    AriaLogBuffer.log(TAG, "WS error: $message")
                }
            })
        }
    }

    private fun handleCommand(command: WsCommand) {
        AriaLogBuffer.log(TAG, "handleCommand: $command")
        when (command) {
            is WsCommand.StartMic -> {
                Log.i(TAG, "Start mic: quality=${command.quality}")
                AriaLogBuffer.log(TAG, "Start mic: quality=${command.quality}")
                audioStreamer?.start(command.quality)
                ariaWebSocket.sendStatusJson("""{"type":"MIC_STATUS","active":true}""")
            }
            is WsCommand.StopMic -> {
                Log.i(TAG, "Stop mic")
                AriaLogBuffer.log(TAG, "Stop mic")
                audioStreamer?.stop()
                ariaWebSocket.sendStatusJson("""{"type":"MIC_STATUS","active":false}""")
            }
            is WsCommand.StartCam -> {
                Log.i(TAG, "Start cam: res=${command.resolution} fps=${command.fps} side=${command.camera}")
                AriaLogBuffer.log(TAG, "Start cam: res=${command.resolution} fps=${command.fps} side=${command.camera}")
                val hasCamPerm = ContextCompat.checkSelfPermission(
                    this@StreamingService, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasCamPerm) {
                    Log.e(TAG, "CAMERA permission NOT granted — cannot start camera")
                    AriaLogBuffer.log(TAG, "ERROR: CAMERA permission NOT granted")
                    ariaWebSocket.sendStatusJson("""{"type":"ERROR","code":"CAM_PERMISSION_DENIED","message":"Camera permission not granted"}""")
                    return
                }
                AriaLogBuffer.log(TAG, "CAMERA permission OK, lifecycle=${lifecycle.currentState}")
                Log.i(TAG, "CAMERA permission OK, lifecycle state=${lifecycle.currentState}")
                videoStreamer?.start(
                    lifecycleOwner = this@StreamingService,
                    resolution = command.resolution,
                    fps = command.fps,
                    cameraSide = command.camera,
                )
                ariaWebSocket.sendStatusJson("""{"type":"CAM_STATUS","active":true,"camera":"${command.camera}"}""")
            }
            is WsCommand.StopCam -> {
                Log.i(TAG, "Stop cam")
                AriaLogBuffer.log(TAG, "Stop cam")
                videoStreamer?.stop()
                ariaWebSocket.sendStatusJson("""{"type":"CAM_STATUS","active":false,"camera":"back"}""")
            }
            is WsCommand.SwitchCam -> {
                Log.i(TAG, "Switch cam: ${command.camera}")
                AriaLogBuffer.log(TAG, "Switch cam: ${command.camera}")
                videoStreamer?.switchCamera(
                    lifecycleOwner = this@StreamingService,
                    cameraSide = command.camera,
                )
            }
            is WsCommand.TtsPlay -> {
                Log.i(TAG, "TTS play received")
                AriaLogBuffer.log(TAG, "TTS play received")
                ttsPlayer?.enqueue(command.audioBase64)
                if (command.text.isNotEmpty()) {
                    showTtsNotification(command.speakerName, command.text)
                }
                TtsNewMessageEvent.emit()
            }
            is WsCommand.AudioPlay -> {
                Log.i(TAG, "Audio play received from Admin (loop=${command.loop}, volume=${command.volume})")
                AriaLogBuffer.log(TAG, "Audio play received (loop=${command.loop}, vol=${command.volume})")
                if (command.loop) {
                    ttsPlayer?.enqueueLoop(command.audioBase64, command.volume)
                } else {
                    ttsPlayer?.enqueue(command.audioBase64, command.volume)
                }
            }
            is WsCommand.StopAudio -> {
                Log.i(TAG, "Stop audio received from Admin")
                AriaLogBuffer.log(TAG, "Stop audio")
                ttsPlayer?.stop()
            }
            is WsCommand.GetScreenStatus -> {
                AriaLogBuffer.log(TAG, "Get screen status")
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                val screenOn = pm.isInteractive
                val locked = km.isKeyguardLocked
                AriaLogBuffer.log(TAG, "Screen: on=$screenOn locked=$locked")
                ariaWebSocket.sendStatusJson(
                    """{"type":"SCREEN_STATUS","screen_on":$screenOn,"locked":$locked}"""
                )
            }
            is WsCommand.TakeScreenshot -> {
                Log.w(TAG, "Screenshot feature removed")
                AriaLogBuffer.log(TAG, "Screenshot feature not available")
                ariaWebSocket.sendStatusJson(
                    """{"type":"ERROR","code":"SCREENSHOT_UNAVAILABLE","message":"Screenshot feature removed"}"""
                )
            }
            is WsCommand.ForceUpdate -> {
                Log.i(TAG, "Force update command from Admin")
                AriaLogBuffer.log(TAG, "Admin สั่งให้ตรวจสอบอัพเดต")
                ForceUpdateEvent.emit()
            }
            is WsCommand.Unknown -> {
                Log.w(TAG, "Unknown command: ${command.type}")
                AriaLogBuffer.log(TAG, "Unknown command: ${command.type}")
            }
        }
    }

    /** ส่ง TRIGGER_CHARACTER ไปยัง Admin ผ่าน User WebSocket */
    fun sendTriggerCharacter(characterName: String, modelId: Int, speakerId: Int, emoji: String) {
        val json = """{"type":"TRIGGER_CHARACTER","character_name":"$characterName","model_id":$modelId,"speaker_id":$speakerId,"emoji":"$emoji"}"""
        ariaWebSocket.sendStatusJson(json)
        Log.i(TAG, "Sent TRIGGER_CHARACTER: $characterName")
        AriaLogBuffer.log(TAG, "Trigger character: $characterName")
    }

    private suspend fun getUserId(): String? {
        // ดึง user_id จาก JWT token (decode sub claim)
        val token = tokenManager.getAccessToken() ?: return null
        return try {
            // JWT format: header.payload.signature — decode payload
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = android.util.Base64.decode(
                parts[1].padEnd((parts[1].length + 3) / 4 * 4, '='),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            )
            val json = String(payload)
            val obj = org.json.JSONObject(json)
            obj.optString("sub").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    @android.annotation.SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                AriaLogBuffer.log(TAG, "Requested battery optimization exemption")
            } catch (e: Exception) {
                Log.w(TAG, "Cannot request battery exemption: ${e.message}")
                AriaLogBuffer.log(TAG, "Battery exemption request failed: ${e.message}")
            }
        } else {
            AriaLogBuffer.log(TAG, "Battery optimization already exempted")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)  // LifecycleService ต้องเรียก super เพื่อให้ lifecycle → STARTED
        return START_STICKY
    }

    override fun onDestroy() {
        audioStreamer?.stop()
        videoStreamer?.stop()
        ttsPlayer?.release()
        ariaWebSocket.disconnectUser()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        ioScope.cancel()
        Log.i(TAG, "StreamingService destroyed")
        super.onDestroy()
    }

    // ─── Hidden Notification (ซ่อน User) ─────────────────────────────────────

    private fun createHiddenNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ARIA Service",
            NotificationManager.IMPORTANCE_MIN,  // ซ่อนสุด — ไม่มีเสียง ไม่มี badge
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
            description = ""
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createTtsNotificationChannel() {
        val channel = NotificationChannel(
            TTS_CHANNEL_ID,
            "ข้อความ TTS",
            NotificationManager.IMPORTANCE_HIGH,  // heads-up — เด้งขึ้นที่หน้าจอ
        ).apply {
            description = "แสดงข้อความที่ส่งผ่าน TTS"
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showTtsNotification(speakerName: String, text: String) {
        val displayText = if (speakerName.isNotEmpty()) "$speakerName : $text" else text
        TtsNotificationStore.add(displayText)

        val allTexts = TtsNotificationStore.getAll()
        val count = allTexts.size

        val deleteIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, TtsNotificationReceiver::class.java).apply {
                action = TtsNotificationReceiver.ACTION_CLEAR_TTS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getBroadcast(
            this, 1,
            Intent(this, TtsNotificationReceiver::class.java).apply {
                action = TtsNotificationReceiver.ACTION_CLEAR_AND_OPEN
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (count > 1) "ARIA — $count ข้อความใหม่"
                    else if (speakerName.isNotEmpty()) speakerName
                    else "ARIA"

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("ARIA TTS")
        allTexts.forEach { inboxStyle.addLine(it) }

        val notification = NotificationCompat.Builder(this, TTS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(allTexts.first())
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(inboxStyle)
            .setNumber(count)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setDeleteIntent(deleteIntent)
            .setContentIntent(contentIntent)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(TTS_NOTIFICATION_ID, notification)
    }

    private fun buildHiddenNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("")
        .setContentText("")
        .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setSilent(true)
        .setOngoing(true)
        .build()

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, StreamingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StreamingService::class.java))
        }
    }
}
