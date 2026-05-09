package com.example.rinasystem.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rinasystem.data.local.TokenManager
import com.example.rinasystem.data.model.AudioPresetItem
import com.example.rinasystem.data.model.TtsModelItem
import com.example.rinasystem.data.model.TtsSpeakerItem
import androidx.compose.runtime.mutableStateMapOf
import com.example.rinasystem.data.repository.AdminRepository
import com.example.rinasystem.data.repository.ApiResult
import com.example.rinasystem.data.ws.AdminWsListener
import com.example.rinasystem.data.ws.AriaWebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "StreamViewModel"

// Frame type tags
private val AUDIO_TAG = byteArrayOf('A'.code.toByte(), 'U'.code.toByte(), 'D'.code.toByte(), 'I'.code.toByte())
private val VIDEO_TAG = byteArrayOf('V'.code.toByte(), 'I'.code.toByte(), 'D'.code.toByte(), 'E'.code.toByte())

/** TTS state แยกต่อ User — draft + history + model/speaker */
data class UserTtsState(
    val draft: String = "",
    val modelId: Int = 0,
    val speakerId: Int = 0,
    val speakersCached: List<TtsSpeakerItem> = emptyList(),
    val history: List<String> = emptyList(), // last 10 sent texts
)

/** Event เมื่อ User กระตุ้น Character — ใช้ navigate ไปยัง User ใน Sidebar */
data class CharacterTrigger(
    val userId: String,
    val userName: String,
    val characterName: String,
    val modelId: Int,
    val speakerId: Int,
    val emoji: String = "🎭",
)

/** Per-user streaming state */
data class UserStreamState(
    val userId: String,
    val micActive: Boolean = false,
    val camActive: Boolean = false,
    val audioAmplitudes: List<Float> = emptyList(),
    val currentFrame: Bitmap? = null,
    val cameraSide: String = "back",
    val audioMuted: Boolean = false,
)

data class StreamUiState(
    // Connection
    val isConnected: Boolean = false,

    // Online users
    val onlineUsers: List<String> = emptyList(),
    val userNameMap: Map<String, String> = emptyMap(),

    // Multi-user: per-user streaming state
    val activeStreams: Map<String, UserStreamState> = emptyMap(),

    // Global quality settings (apply to new streams)
    val audioQuality: String = "medium",
    val videoResolution: String = "720p",
    val videoFps: Int = 24,

    // TTS (targets selected user)
    val selectedTtsUserId: String? = null,
    val ttsText: String = "",
    val isTtsGenerating: Boolean = false,
    val ttsModelId: Int = 0,
    val ttsSpeakerId: Int = 0,
    val ttsModels: List<TtsModelItem> = emptyList(),
    val ttsSpeakers: List<TtsSpeakerItem> = emptyList(),

    // Timeout
    val isTtsTimeoutEnabled: Boolean = true,

    // Audio file playback
    val isSendingAudio: Boolean = false,

    // Audio presets
    val audioPresets: List<AudioPresetItem> = emptyList(),
    val isLoadingPresets: Boolean = false,

    // Screen status (per selected user)
    val screenOn: Boolean? = null,
    val screenLocked: Boolean? = null,

    // TTS Queue progress (text แสดงสถานะคิว เช่น "[2/3] ยินดีต้อนรับ")
    val ttsQueueProgress: String = "",
    val isTtsQueueRunning: Boolean = false,

    // Pending character trigger จาก User (unread trigger event)
    val pendingTrigger: CharacterTrigger? = null,

    // Status
    val error: String? = null,
)

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val ariaWebSocket: AriaWebSocket,
    private val tokenManager: TokenManager,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StreamUiState())
    val state: StateFlow<StreamUiState> = _state.asStateFlow()

    /** Per-user TTS state (draft, history, model, speaker) — observable โดย Compose */
    val userTtsStates = mutableStateMapOf<String, UserTtsState>()

    // Per-user AudioTrack instances
    private val audioTracks = mutableMapOf<String, AudioTrack>()

    // TTS timeout — ถ้าไม่ได้ response ใน 15 วินาที ให้หยุด spinner
    private var ttsTimeoutJob: Job? = null
    private val TTS_TIMEOUT_MS = 15_000L

    fun connectAsAdmin(adminId: String) {
        ariaWebSocket.connectAdmin(adminId, object : AdminWsListener {
            override fun onBinaryFrame(data: ByteArray) {
                handleBinaryFrame(data)
            }

            override fun onStatusJson(json: String) {
                handleStatusJson(json)
            }

            override fun onConnected() {
                _state.value = _state.value.copy(isConnected = true, error = null)
                Log.i(TAG, "Admin WS connected")
                requestOnlineUsers()
            }

            override fun onDisconnected() {
                _state.value = _state.value.copy(isConnected = false)
                Log.i(TAG, "Admin WS disconnected")
            }

            override fun onError(message: String) {
                _state.value = _state.value.copy(error = message)
                Log.w(TAG, "Admin WS error: $message")
            }
        })
    }

    // ─── Binary Frame Parsing (with user_id) ─────────────────────────────────────
    // New format: [TAG 4 bytes] + [user_id 36 bytes] + [payload]

    private fun handleBinaryFrame(data: ByteArray) {
        if (data.size < 40) return  // 4 tag + 36 user_id minimum
        val tag = data.copyOfRange(0, 4)
        val userId = String(data, 4, 36, Charsets.UTF_8).trimEnd('\u0000')
        val payload = data.copyOfRange(40, data.size)

        when {
            tag.contentEquals(AUDIO_TAG) -> {
                val amplitude = computeAmplitude(payload)
                val streams = _state.value.activeStreams.toMutableMap()
                val userState = streams[userId] ?: return
                val amplitudes = (userState.audioAmplitudes + amplitude).takeLast(40)
                streams[userId] = userState.copy(audioAmplitudes = amplitudes)
                _state.value = _state.value.copy(activeStreams = streams)

                // Play audio if not muted
                if (!userState.audioMuted) {
                    audioTracks[userId]?.write(payload, 0, payload.size)
                }
            }
            tag.contentEquals(VIDEO_TAG) -> {
                val bitmap = BitmapFactory.decodeByteArray(payload, 0, payload.size)
                if (bitmap != null) {
                    val streams = _state.value.activeStreams.toMutableMap()
                    val userState = streams[userId] ?: return
                    streams[userId] = userState.copy(currentFrame = bitmap)
                    _state.value = _state.value.copy(activeStreams = streams)
                }
            }
        }
    }

    private fun handleStatusJson(json: String) {
        try {
            val obj = JSONObject(json)
            val type = obj.optString("type")
            val userId = obj.optString("user_id", "")

            when (type) {
                "MIC_STATUS" -> {
                    if (userId.isNotEmpty()) updateUserStream(userId) { it.copy(micActive = obj.optBoolean("active")) }
                }
                "CAM_STATUS" -> {
                    if (userId.isNotEmpty()) updateUserStream(userId) { it.copy(camActive = obj.optBoolean("active")) }
                }
                "TTS_QUEUE_START" -> {
                    val total = obj.optInt("total", 0)
                    _state.value = _state.value.copy(
                        isTtsQueueRunning = true,
                        ttsQueueProgress = "เริ่มส่งคิว $total รายการ...",
                    )
                }
                "TTS_QUEUE_PROGRESS" -> {
                    val idx = obj.optInt("index", 0) + 1
                    val total = obj.optInt("total", 0)
                    val text = obj.optString("text", "")
                    _state.value = _state.value.copy(
                        isTtsGenerating = true,
                        ttsQueueProgress = "[$idx/$total] ${text.take(30)}",
                    )
                    startTtsTimeout()
                }
                "TTS_QUEUE_DONE" -> {
                    val total = obj.optInt("total", 0)
                    ttsTimeoutJob?.cancel()
                    _state.value = _state.value.copy(
                        isTtsGenerating = false,
                        isTtsQueueRunning = false,
                        ttsQueueProgress = "✅ ส่งครบ $total รายการ",
                    )
                }
                "TTS_QUEUE_CANCELLED" -> {
                    val processed = obj.optInt("processed", 0)
                    ttsTimeoutJob?.cancel()
                    _state.value = _state.value.copy(
                        isTtsGenerating = false,
                        isTtsQueueRunning = false,
                        ttsQueueProgress = "🚫 ยกเลิกคิวแล้ว (ส่งไปแล้ว $processed รายการ)",
                    )
                }
                "TTS_QUEUE_ITEM_FAILED" -> {
                    val text = obj.optString("text", "")
                    _state.value = _state.value.copy(
                        ttsQueueProgress = "❌ ล้มเหลว: ${text.take(20)}",
                    )
                }
                "CHARACTER_TRIGGERED" -> {
                    val uid = obj.optString("user_id")
                    val uname = obj.optString("user_name", uid.take(8))
                    val charName = obj.optString("character_name")
                    val mId = obj.optInt("model_id", 0)
                    val sId = obj.optInt("speaker_id", 0)
                    val emoji = obj.optString("emoji", "🎭")
                    val trigger = CharacterTrigger(uid, uname, charName, mId, sId, emoji)
                    _state.value = _state.value.copy(pendingTrigger = trigger)
                    Log.i(TAG, "CHARACTER_TRIGGERED: $uname → $charName")
                }
                "TTS_GENERATING" -> {
                    _state.value = _state.value.copy(isTtsGenerating = true)
                    startTtsTimeout() // reset timeout เพราะ server เริ่ม generate แล้ว
                }
                "TTS_SENT", "TTS_USER_OFFLINE", "TTS_SENT_ALL" -> {
                    ttsTimeoutJob?.cancel()
                    _state.value = _state.value.copy(isTtsGenerating = false)
                }
                "TTS_ERROR" -> {
                    ttsTimeoutJob?.cancel()
                    val msg = obj.optString("message", "TTS generation failed")
                    _state.value = _state.value.copy(isTtsGenerating = false, error = msg)
                }
                "AUDIO_SENT" -> {
                    _state.value = _state.value.copy(isSendingAudio = false)
                }
                "SCREEN_STATUS" -> {
                    _state.value = _state.value.copy(
                        screenOn = obj.optBoolean("screen_on"),
                        screenLocked = obj.optBoolean("locked"),
                    )
                }
                "ERROR" -> {
                    val code = obj.optString("code")
                    val msg = obj.optString("message")
                    if (code == "USER_OFFLINE" && userId.isNotEmpty()) {
                        removeUserStream(userId)
                    }
                    // Reset generating/sending states on error
                    if (code in listOf("TTS_FAILED", "EMPTY_TEXT", "USER_OFFLINE")) {
                        ttsTimeoutJob?.cancel()
                        _state.value = _state.value.copy(
                            isTtsGenerating = false,
                            isSendingAudio = false,
                            error = "[$code] $msg",
                        )
                    } else {
                        _state.value = _state.value.copy(error = "[$code] $msg")
                    }
                }
                "ONLINE_USERS", "USER_CONNECTED", "USER_DISCONNECTED" -> {
                    val arr = obj.optJSONArray("user_ids") ?: obj.optJSONArray("online_users")
                    val list = buildList { if (arr != null) repeat(arr.length()) { add(arr.getString(it)) } }
                    // Remove streams of disconnected users
                    val streams = _state.value.activeStreams.toMutableMap()
                    val disconnected = streams.keys - list.toSet()
                    disconnected.forEach { uid ->
                        stopAudioPlayback(uid)
                        streams.remove(uid)
                    }
                    // Init UserTtsState for newly connected users
                    list.forEach { uid -> if (!userTtsStates.containsKey(uid)) userTtsStates[uid] = UserTtsState() }
                    _state.value = _state.value.copy(
                        onlineUsers = list,
                        activeStreams = streams,
                    )
                    fetchUserNames()
                }
            }
        } catch (_: Exception) { }
    }

    // ─── Multi-user Controls ─────────────────────────────────────────────────────

    fun startMicForUser(userId: String) {
        val quality = _state.value.audioQuality
        sendCommand(buildJsonCommand("START_MIC", userId, mapOf("quality" to quality)))
        startAudioPlayback(userId, quality)
        ensureUserStream(userId) { it.copy(micActive = true, audioAmplitudes = emptyList()) }
    }

    fun stopMicForUser(userId: String) {
        sendCommand(buildJsonCommand("STOP_MIC", userId))
        stopAudioPlayback(userId)
        updateUserStream(userId) { it.copy(micActive = false, audioAmplitudes = emptyList()) }
        maybeRemoveStream(userId)
    }

    fun startCamForUser(userId: String) {
        val s = _state.value
        sendCommand(buildJsonCommand("START_CAM", userId, mapOf(
            "camera" to "back",
            "resolution" to s.videoResolution,
            "fps" to s.videoFps,
        )))
        ensureUserStream(userId) { it.copy(camActive = true) }
    }

    fun stopCamForUser(userId: String) {
        sendCommand(buildJsonCommand("STOP_CAM", userId))
        updateUserStream(userId) { it.copy(camActive = false, currentFrame = null) }
        maybeRemoveStream(userId)
    }

    fun switchCameraForUser(userId: String) {
        val current = _state.value.activeStreams[userId]?.cameraSide ?: "back"
        val newSide = if (current == "back") "front" else "back"
        sendCommand(buildJsonCommand("SWITCH_CAM", userId, mapOf("camera" to newSide)))
        updateUserStream(userId) { it.copy(cameraSide = newSide) }
    }

    fun toggleMuteForUser(userId: String) {
        updateUserStream(userId) { it.copy(audioMuted = !it.audioMuted) }
    }

    // ─── TTS (targets a specific user) ──────────────────────────────────────────

    fun setSelectedTtsUserId(userId: String?) {
        _state.value = _state.value.copy(selectedTtsUserId = userId)
        if (userId != null) selectUserForTts(userId)
    }

    // ─── Per-User TTS State ──────────────────────────────────────────────────

    fun selectUserForTts(userId: String) {
        _state.value = _state.value.copy(selectedTtsUserId = userId)
        if (!userTtsStates.containsKey(userId)) {
            userTtsStates[userId] = UserTtsState()
        }
        val modelId = userTtsStates[userId]?.modelId ?: 0
        if ((userTtsStates[userId]?.speakersCached ?: emptyList()).isEmpty()) {
            fetchTtsSpeakersForUser(userId, modelId)
        }
    }

    fun setUserTtsDraft(userId: String, text: String) {
        userTtsStates[userId] = (userTtsStates[userId] ?: UserTtsState()).copy(draft = text)
    }

    fun setUserTtsModel(userId: String, modelId: Int) {
        userTtsStates[userId] = (userTtsStates[userId] ?: UserTtsState()).copy(modelId = modelId, speakerId = 0, speakersCached = emptyList())
        fetchTtsSpeakersForUser(userId, modelId)
    }

    fun setUserTtsSpeaker(userId: String, speakerId: Int) {
        userTtsStates[userId] = (userTtsStates[userId] ?: UserTtsState()).copy(speakerId = speakerId)
    }

    private fun addToUserHistory(userId: String, text: String) {
        val current = userTtsStates[userId] ?: UserTtsState()
        val newHistory = (listOf(text) + current.history).distinct().take(10)
        userTtsStates[userId] = current.copy(history = newHistory)
    }

    private fun fetchTtsSpeakersForUser(userId: String, modelId: Int) {
        viewModelScope.launch {
            when (val result = adminRepository.getTtsSpeakers(modelId)) {
                is ApiResult.Success -> {
                    userTtsStates[userId] = (userTtsStates[userId] ?: UserTtsState()).copy(speakersCached = result.data)
                    // ถ้า global speakers ยังไม่มีให้ sync ด้วย
                    _state.value = _state.value.copy(ttsSpeakers = result.data)
                }
                is ApiResult.Error -> Log.w(TAG, "fetchTtsSpeakersForUser failed: ${result.message}")
            }
        }
    }

    fun sendTtsForUser(userId: String) {
        val userState = userTtsStates[userId] ?: return
        val text = userState.draft.trim()
        if (text.isEmpty()) return
        val speakerName = userState.speakersCached.firstOrNull { it.id == userState.speakerId }?.name ?: ""
        val sent = sendCommand(buildJsonCommand("TTS_TEXT", userId, mapOf(
            "text" to text,
            "model_id" to userState.modelId,
            "speaker_id" to userState.speakerId,
            "speaker_name" to speakerName,
        )))
        if (!sent) {
            _state.value = _state.value.copy(error = "ไม่สามารถส่งได้ — ยังไม่ได้เชื่อมต่อ Server")
            return
        }
        addToUserHistory(userId, text)
        userTtsStates[userId] = userState.copy(draft = "")
        _state.value = _state.value.copy(isTtsGenerating = true)
        startTtsTimeout()
    }

    fun sendTtsForUserToAll(userId: String) {
        val userState = userTtsStates[userId] ?: return
        val text = userState.draft.trim()
        if (text.isEmpty()) return
        val speakerName = userState.speakersCached.firstOrNull { it.id == userState.speakerId }?.name ?: ""
        val sent = sendCommand(buildJsonCommand("TTS_TEXT", "ALL", mapOf(
            "text" to text,
            "model_id" to userState.modelId,
            "speaker_id" to userState.speakerId,
            "speaker_name" to speakerName,
        )))
        if (!sent) {
            _state.value = _state.value.copy(error = "ไม่สามารถส่งได้ — ยังไม่ได้เชื่อมต่อ Server")
            return
        }
        addToUserHistory(userId, text)
        userTtsStates[userId] = userState.copy(draft = "")
        _state.value = _state.value.copy(isTtsGenerating = true)
        startTtsTimeout()
    }

    /** ใช้เมื่อ Admin กด Notification หรือรับ CHARACTER_TRIGGERED — auto-select user + set speaker */
    fun applyCharacterTrigger(trigger: CharacterTrigger) {
        selectUserForTts(trigger.userId)
        val current = userTtsStates[trigger.userId] ?: UserTtsState()
        userTtsStates[trigger.userId] = current.copy(modelId = trigger.modelId, speakerId = trigger.speakerId)
        fetchTtsSpeakersForUser(trigger.userId, trigger.modelId)
        _state.value = _state.value.copy(pendingTrigger = null)
    }

    fun clearPendingTrigger() {
        _state.value = _state.value.copy(pendingTrigger = null)
    }

    private fun currentSpeakerName(): String {
        val speakers = _state.value.ttsSpeakers
        val speakerId = _state.value.ttsSpeakerId
        val name = speakers.firstOrNull { it.id == speakerId }?.name ?: ""
        Log.d(TAG, "currentSpeakerName: speakerId=$speakerId, speakers=${speakers.map { "${it.id}:${it.name}" }}, result='$name'")
        return name
    }

    fun sendTts() {
        val userId = _state.value.selectedTtsUserId ?: return
        val text = _state.value.ttsText.trim()
        if (text.isEmpty()) return
        val sent = sendCommand(buildJsonCommand("TTS_TEXT", userId, mapOf(
            "text" to text,
            "model_id" to _state.value.ttsModelId,
            "speaker_id" to _state.value.ttsSpeakerId,
            "speaker_name" to currentSpeakerName(),
        )))
        if (!sent) {
            _state.value = _state.value.copy(error = "ไม่สามารถส่งได้ — ยังไม่ได้เชื่อมต่อ Server")
            return
        }
        _state.value = _state.value.copy(isTtsGenerating = true)
        startTtsTimeout()
    }

    fun sendTtsToAll() {
        val text = _state.value.ttsText.trim()
        if (text.isEmpty()) return
        val sent = sendCommand(buildJsonCommand("TTS_TEXT", "ALL", mapOf(
            "text" to text,
            "model_id" to _state.value.ttsModelId,
            "speaker_id" to _state.value.ttsSpeakerId,
            "speaker_name" to currentSpeakerName(),
        )))
        if (!sent) {
            _state.value = _state.value.copy(error = "ไม่สามารถส่งได้ — ยังไม่ได้เชื่อมต่อ Server")
            return
        }
        _state.value = _state.value.copy(isTtsGenerating = true)
        startTtsTimeout()
    }

    fun clearTtsText() { _state.value = _state.value.copy(ttsText = "") }

    fun cancelTtsQueue() {
        sendCommand("""{"type":"CANCEL_TTS_QUEUE"}""")
    }

    fun setTtsText(text: String) { _state.value = _state.value.copy(ttsText = text) }
    fun setAudioQuality(quality: String) { _state.value = _state.value.copy(audioQuality = quality) }
    fun setVideoResolution(resolution: String) { _state.value = _state.value.copy(videoResolution = resolution) }
    fun setVideoFps(fps: Int) { _state.value = _state.value.copy(videoFps = fps) }
    fun setTtsModelId(modelId: Int) {
        _state.value = _state.value.copy(ttsModelId = modelId, ttsSpeakerId = 0)
        fetchTtsSpeakers(modelId)
    }
    fun setTtsSpeakerId(speakerId: Int) { _state.value = _state.value.copy(ttsSpeakerId = speakerId) }

    fun fetchTtsModels() {
        viewModelScope.launch {
            when (val result = adminRepository.getTtsModels()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(ttsModels = result.data)
                    if (result.data.isNotEmpty()) {
                        fetchTtsSpeakers(_state.value.ttsModelId)
                    }
                }
                is ApiResult.Error -> Log.w(TAG, "fetchTtsModels failed: ${result.message}")
            }
        }
    }

    private fun fetchTtsSpeakers(modelId: Int) {
        viewModelScope.launch {
            when (val result = adminRepository.getTtsSpeakers(modelId)) {
                is ApiResult.Success -> _state.value = _state.value.copy(ttsSpeakers = result.data)
                is ApiResult.Error -> Log.w(TAG, "fetchTtsSpeakers failed: ${result.message}")
            }
        }
    }

    fun requestOnlineUsers() {
        ariaWebSocket.sendAdminCommand(JSONObject().apply { put("type", "GET_ONLINE_USERS") }.toString())
    }

    fun requestScreenStatus(userId: String) {
        sendCommand(buildJsonCommand("GET_SCREEN_STATUS", userId))
    }

    // ─── Audio File Playback (targets a specific user) ───────────────────────────

    fun sendAudioToUser(userId: String, audioBase64: String, loop: Boolean = false, volume: Float = 1.0f) {
        if (audioBase64.isEmpty()) return
        val sent = sendCommand(buildJsonCommand("PLAY_AUDIO", userId, mapOf(
            "audio_base64" to audioBase64,
            "loop" to loop,
            "volume" to volume.coerceIn(0f, 1f),
        )))
        if (!sent) {
            _state.value = _state.value.copy(error = "ไม่สามารถส่งได้ — ยังไม่ได้เชื่อมต่อ Server")
            return
        }
        _state.value = _state.value.copy(isSendingAudio = true)
        // Timeout สำหรับ audio send
        if (_state.value.isTtsTimeoutEnabled) {
            viewModelScope.launch {
                delay(TTS_TIMEOUT_MS)
                if (_state.value.isSendingAudio) {
                    _state.value = _state.value.copy(isSendingAudio = false, error = "Audio send timeout — Server ไม่ตอบกลับ")
                }
            }
        }
    }

    fun stopAudioForUser(userId: String) {
        sendCommand(buildJsonCommand("STOP_AUDIO", userId))
    }

    /** สั่งให้ User ทุกคนตรวจสอบอัพเดตทันที */
    fun forceUpdateAllUsers() {
        sendCommand("""{"type":"FORCE_UPDATE"}""")
    }

    // ─── Audio Presets ───────────────────────────────────────────────────────────

    fun loadAudioPresets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingPresets = true)
            when (val result = adminRepository.getAudioPresets()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(audioPresets = result.data, isLoadingPresets = false)
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Failed to load presets: ${result.message}")
                    _state.value = _state.value.copy(isLoadingPresets = false)
                }
            }
        }
    }

    fun sendPresetToUser(userId: String, filename: String, loop: Boolean = false, volume: Float = 1.0f) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSendingAudio = true)
            when (val result = adminRepository.getAudioPreset(filename)) {
                is ApiResult.Success -> {
                    sendAudioToUser(userId, result.data.audioBase64, loop, volume)
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Failed to load preset $filename: ${result.message}")
                    _state.value = _state.value.copy(isSendingAudio = false, error = result.message)
                }
            }
        }
    }

    fun toggleTtsTimeout() {
        _state.value = _state.value.copy(isTtsTimeoutEnabled = !_state.value.isTtsTimeoutEnabled)
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private fun ensureUserStream(userId: String, update: (UserStreamState) -> UserStreamState) {
        val streams = _state.value.activeStreams.toMutableMap()
        val existing = streams[userId] ?: UserStreamState(userId = userId)
        streams[userId] = update(existing)
        _state.value = _state.value.copy(activeStreams = streams)
    }

    private fun updateUserStream(userId: String, update: (UserStreamState) -> UserStreamState) {
        val streams = _state.value.activeStreams.toMutableMap()
        val existing = streams[userId] ?: return
        streams[userId] = update(existing)
        _state.value = _state.value.copy(activeStreams = streams)
    }

    private fun removeUserStream(userId: String) {
        stopAudioPlayback(userId)
        val streams = _state.value.activeStreams.toMutableMap()
        streams.remove(userId)
        _state.value = _state.value.copy(activeStreams = streams)
    }

    private fun maybeRemoveStream(userId: String) {
        val s = _state.value.activeStreams[userId] ?: return
        if (!s.micActive && !s.camActive) removeUserStream(userId)
    }

    private fun fetchUserNames() {
        viewModelScope.launch {
            Log.d(TAG, "fetchUserNames: calling getUsers API...")
            when (val result = adminRepository.getUsers()) {
                is ApiResult.Success -> {
                    val nameMap = result.data.users.associate { it.id to it.displayName }
                    Log.d(TAG, "fetchUserNames: got ${nameMap.size} users: $nameMap")
                    _state.value = _state.value.copy(userNameMap = nameMap)
                }
                is ApiResult.Error -> Log.w(TAG, "fetchUserNames FAILED: ${result.message}")
            }
        }
    }

    private fun sendCommand(json: String): Boolean = ariaWebSocket.sendAdminCommand(json)

    private fun startTtsTimeout() {
        ttsTimeoutJob?.cancel()
        if (!_state.value.isTtsTimeoutEnabled) return
        ttsTimeoutJob = viewModelScope.launch {
            delay(TTS_TIMEOUT_MS)
            if (_state.value.isTtsGenerating) {
                _state.value = _state.value.copy(
                    isTtsGenerating = false,
                    error = "TTS timeout — Server ไม่ตอบกลับ"
                )
            }
        }
    }

    private fun buildJsonCommand(type: String, userId: String, extra: Map<String, Any> = emptyMap()): String {
        return JSONObject().apply {
            put("type", type)
            put("user_id", userId)
            extra.forEach { (k, v) -> put(k, v) }
        }.toString()
    }

    companion object {
        /** Compute RMS amplitude from PCM 16-bit LE bytes. Visible for testing. */
        @JvmStatic
        fun computeAmplitude(pcmBytes: ByteArray): Float {
            if (pcmBytes.size < 2) return 0f
            var sum = 0L
            var i = 0
            while (i + 1 < pcmBytes.size) {
                val sample = (pcmBytes[i + 1].toInt() shl 8) or (pcmBytes[i].toInt() and 0xFF)
                sum += (sample * sample).toLong()
                i += 2
            }
            val rms = Math.sqrt(sum.toDouble() / (pcmBytes.size / 2)).toFloat()
            return (rms / 32768f).coerceIn(0f, 1f)
        }
    }

    // ─── Audio Playback (per-user) ──────────────────────────────────────────────

    private fun startAudioPlayback(userId: String, quality: String) {
        stopAudioPlayback(userId)
        val sampleRate = when (quality) {
            "low" -> 8000
            "high" -> 44100
            else -> 16000
        }
        val bufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }
        audioTracks[userId] = track
        Log.i(TAG, "AudioTrack started for $userId: ${sampleRate}Hz")
    }

    private fun stopAudioPlayback(userId: String) {
        audioTracks.remove(userId)?.let {
            try { it.stop(); it.release() } catch (_: Exception) { }
        }
    }

    private fun stopAllAudioPlayback() {
        audioTracks.values.forEach { try { it.stop(); it.release() } catch (_: Exception) { } }
        audioTracks.clear()
    }

    override fun onCleared() {
        stopAllAudioPlayback()
        ariaWebSocket.disconnectAdmin()
        super.onCleared()
    }
}
