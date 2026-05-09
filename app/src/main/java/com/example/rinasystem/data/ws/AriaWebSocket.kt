package com.example.rinasystem.data.ws

import android.util.Log
import com.example.rinasystem.data.local.ServerConfigManager
import com.example.rinasystem.data.local.TokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AriaWebSocket"

/** Callback สำหรับ User WS — รับ commands จาก Admin */
interface UserWsListener {
    fun onCommand(command: WsCommand)
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
}

/** Callback สำหรับ Admin WS — รับ binary stream + status จาก User */
interface AdminWsListener {
    fun onBinaryFrame(data: ByteArray)  // audio หรือ video frame
    fun onStatusJson(json: String)      // MIC_STATUS, CAM_STATUS, ERROR, etc.
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
}

@Singleton
class AriaWebSocket @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val serverConfig: ServerConfigManager,
) {
    private var userWs: WebSocket? = null
    private var adminWs: WebSocket? = null
    private var userListener: UserWsListener? = null
    private var adminListener: AdminWsListener? = null

    private var userReconnectJob: Job? = null
    private var adminReconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // ─── User WebSocket ───────────────────────────────────────────────────────

    fun connectUser(userId: String, listener: UserWsListener) {
        userListener = listener
        userReconnectJob?.cancel()
        userReconnectJob = scope.launch {
            connectUserWithRetry(userId)
        }
    }

    private suspend fun connectUserWithRetry(userId: String) {
        var backoffMs = 1_000L
        while (currentCoroutineContext().isActive) {
            val disconnected = CompletableDeferred<Unit>()
            try {
                // Refresh token ก่อน connect เพื่อกัน expired
                refreshTokenIfNeeded()
                val token = tokenManager.getAccessToken()
                if (token == null) {
                    Log.w(TAG, "No access token, cannot connect WS")
                    delay(5_000)
                    continue
                }
                val ip = serverConfig.getServerIp()
                val port = serverConfig.getServerPort()
                val url = "ws://$ip:$port/ws/user/$userId?token=$token"

                val request = Request.Builder().url(url).build()
                userWs = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        backoffMs = 1_000L
                        Log.i(TAG, "User WS connected")
                        userListener?.onConnected()
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        val cmd = parseWsCommand(text)
                        userListener?.onCommand(cmd)
                    }

                    override fun onMessage(ws: WebSocket, bytes: ByteString) {
                        // User รับ binary เฉพาะ TTS audio (กรณีส่งเป็น binary แทน base64)
                        // ปกติ TTS จะส่งเป็น JSON TTS_PLAY แต่รองรับ binary ด้วย
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        Log.i(TAG, "User WS closed: $code $reason")
                        userListener?.onDisconnected()
                        disconnected.complete(Unit)
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        Log.w(TAG, "User WS failure: ${t.message}")
                        userListener?.onError(t.message ?: "Connection failed")
                        userListener?.onDisconnected()
                        disconnected.complete(Unit)
                    }
                })

                disconnected.await()  // รอจนกว่า WS จะปิดหรือ fail

            } catch (e: CancellationException) {
                throw e  // ต้อง rethrow ให้ coroutine cancel ได้
            } catch (e: Exception) {
                Log.w(TAG, "User WS connect error: ${e.message}")
            }

            if (!currentCoroutineContext().isActive) break
            Log.i(TAG, "User WS reconnecting in ${backoffMs}ms...")
            delay(backoffMs)
            backoffMs = minOf(backoffMs * 2, 30_000L)
        }
    }

    fun disconnectUser() {
        userReconnectJob?.cancel()
        userReconnectJob = null
        userWs?.close(1000, "User disconnected")
        userWs = null
        userListener = null
    }

    // ─── Admin WebSocket ──────────────────────────────────────────────────────

    fun connectAdmin(adminId: String, listener: AdminWsListener) {
        adminListener = listener
        adminReconnectJob?.cancel()
        adminReconnectJob = scope.launch {
            connectAdminWithRetry(adminId)
        }
    }

    private suspend fun connectAdminWithRetry(adminId: String) {
        var backoffMs = 1_000L
        while (currentCoroutineContext().isActive) {
            val disconnected = CompletableDeferred<Unit>()
            try {
                // Refresh token ก่อน connect เพื่อกัน expired
                refreshTokenIfNeeded()
                val token = tokenManager.getAccessToken()
                if (token == null) {
                    delay(5_000)
                    continue
                }
                val ip = serverConfig.getServerIp()
                val port = serverConfig.getServerPort()
                val url = "ws://$ip:$port/ws/admin/$adminId?token=$token"

                val request = Request.Builder().url(url).build()
                adminWs = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        backoffMs = 1_000L
                        Log.i(TAG, "Admin WS connected")
                        adminListener?.onConnected()
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        adminListener?.onStatusJson(text)
                    }

                    override fun onMessage(ws: WebSocket, bytes: ByteString) {
                        adminListener?.onBinaryFrame(bytes.toByteArray())
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        Log.i(TAG, "Admin WS closed: $code $reason")
                        adminListener?.onDisconnected()
                        disconnected.complete(Unit)
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        Log.w(TAG, "Admin WS failure: ${t.message}")
                        adminListener?.onError(t.message ?: "Connection failed")
                        adminListener?.onDisconnected()
                        disconnected.complete(Unit)
                    }
                })

                disconnected.await()  // รอจนกว่า WS จะปิดหรือ fail

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Admin WS connect error: ${e.message}")
            }

            if (!currentCoroutineContext().isActive) break
            Log.i(TAG, "Admin WS reconnecting in ${backoffMs}ms...")
            delay(backoffMs)
            backoffMs = minOf(backoffMs * 2, 30_000L)
        }
    }

    fun sendAdminCommand(command: String): Boolean {
        val sent = adminWs?.send(command) ?: false
        if (!sent) Log.w(TAG, "Admin WS not connected, command dropped: $command")
        return sent
    }

    fun disconnectAdmin() {
        adminReconnectJob?.cancel()
        adminReconnectJob = null
        adminWs?.close(1000, "Admin disconnected")
        adminWs = null
        adminListener = null
    }

    // ─── User send binary / status ────────────────────────────────────────────

    /** ส่ง status JSON จาก User → Server (MIC_STATUS, CAM_STATUS, ERROR) */
    fun sendStatusJson(json: String) {
        val sent = userWs?.send(json) ?: false
        if (!sent) Log.w(TAG, "User WS not connected, status dropped: $json")
    }

    /** ส่ง audio frame จาก User → Server (binary) */
    fun sendAudioFrame(pcmBytes: ByteArray) {
        val frame = AUDIO_TAG + pcmBytes
        userWs?.send(ByteString.of(*frame))
    }

    /** ส่ง video frame จาก User → Server (binary JPEG) */
    fun sendVideoFrame(jpegBytes: ByteArray) {
        val frame = VIDEO_TAG + jpegBytes
        userWs?.send(ByteString.of(*frame))
    }

    // ─── Token Refresh ────────────────────────────────────────────────────────

    /**
     * Refresh access token โดยเรียก API ตรง (ไม่ผ่าน Retrofit เพื่อหลีกเลี่ยง circular DI)
     * คืน true ถ้า refresh สำเร็จ
     */
    private suspend fun refreshTokenIfNeeded(): Boolean {
        val refreshToken = tokenManager.getRefreshToken() ?: return false
        try {
            val ip = serverConfig.getServerIp()
            val port = serverConfig.getServerPort()
            val url = "http://$ip:$port/api/auth/refresh"
            val jsonBody = JSONObject().apply {
                put("refresh_token", refreshToken)
            }.toString()
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return false
                val json = JSONObject(body)
                val newAccessToken = json.getString("access_token")
                val newRefreshToken = json.getString("refresh_token")
                tokenManager.saveTokens(newAccessToken, newRefreshToken)
                Log.i(TAG, "Token refreshed successfully")
                return true
            } else {
                Log.w(TAG, "Token refresh failed: ${response.code}")
                return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Token refresh error: ${e.message}")
            return false
        }
    }

    companion object {
        val AUDIO_TAG = byteArrayOf('A'.code.toByte(), 'U'.code.toByte(), 'D'.code.toByte(), 'I'.code.toByte())
        val VIDEO_TAG = byteArrayOf('V'.code.toByte(), 'I'.code.toByte(), 'D'.code.toByte(), 'E'.code.toByte())
    }
}
