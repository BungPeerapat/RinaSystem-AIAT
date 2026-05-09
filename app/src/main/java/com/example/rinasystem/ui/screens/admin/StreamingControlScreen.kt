package com.example.rinasystem.ui.screens.admin

import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import com.example.rinasystem.data.model.AudioPresetItem
import com.example.rinasystem.data.model.TtsModelItem
import com.example.rinasystem.data.model.TtsSpeakerItem
import com.example.rinasystem.ui.theme.AriaBgSurface
import com.example.rinasystem.ui.viewmodel.CharacterTrigger
import com.example.rinasystem.ui.viewmodel.UserTtsState
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.components.HudPanel
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaBorder
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaGreen
import com.example.rinasystem.ui.theme.AriaOrange
import com.example.rinasystem.ui.theme.AriaRed
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.viewmodel.StreamViewModel
import com.example.rinasystem.ui.viewmodel.UserStreamState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingControlScreen(
    adminId: String,
    modifier: Modifier = Modifier,
    streamViewModel: StreamViewModel = hiltViewModel()
) {
    val state by streamViewModel.state.collectAsState()

    LaunchedEffect(adminId) {
        streamViewModel.connectAsAdmin(adminId)
        streamViewModel.loadAudioPresets()
        streamViewModel.fetchTtsModels()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Connection status
            ConnectionStatusBar(isConnected = state.isConnected, error = state.error)

            Spacer(modifier = Modifier.height(12.dp))

            // Online users — per-user stream controls
            OnlineUsersPanel(
                onlineUsers = state.onlineUsers,
                userNameMap = state.userNameMap,
                activeStreams = state.activeStreams,
                audioQuality = state.audioQuality,
                videoResolution = state.videoResolution,
                videoFps = state.videoFps,
                onStartMic = { streamViewModel.startMicForUser(it) },
                onStopMic = { streamViewModel.stopMicForUser(it) },
                onStartCam = { streamViewModel.startCamForUser(it) },
                onStopCam = { streamViewModel.stopCamForUser(it) },
                onSwitchCam = { streamViewModel.switchCameraForUser(it) },
                onToggleMute = { streamViewModel.toggleMuteForUser(it) },
            )

            // Active streams — video grid + waveforms
            if (state.activeStreams.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ActiveStreamsSection(activeStreams = state.activeStreams, userNameMap = state.userNameMap)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Global quality settings
            QualitySettingsCard(
                audioQuality = state.audioQuality,
                videoResolution = state.videoResolution,
                videoFps = state.videoFps,
                onAudioQualityChange = { streamViewModel.setAudioQuality(it) },
                onResolutionChange = { streamViewModel.setVideoResolution(it) },
                onFpsChange = { streamViewModel.setVideoFps(it) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // TTS control — Sidebar layout (per-user state)
            TtsSidebarCard(
                onlineUsers = state.onlineUsers,
                userNameMap = state.userNameMap,
                selectedUserId = state.selectedTtsUserId,
                userTtsStates = streamViewModel.userTtsStates,
                ttsModels = state.ttsModels,
                isGenerating = state.isTtsGenerating,
                ttsQueueProgress = state.ttsQueueProgress,
                isTtsQueueRunning = state.isTtsQueueRunning,
                pendingTrigger = state.pendingTrigger,
                onSelectUser = { streamViewModel.selectUserForTts(it) },
                onTextChange = { uid, text -> streamViewModel.setUserTtsDraft(uid, text) },
                onSend = { uid -> streamViewModel.sendTtsForUser(uid) },
                onSendAll = { uid -> streamViewModel.sendTtsForUserToAll(uid) },
                onModelChange = { uid, mid -> streamViewModel.setUserTtsModel(uid, mid) },
                onSpeakerChange = { uid, sid -> streamViewModel.setUserTtsSpeaker(uid, sid) },
                onRefreshModels = { streamViewModel.fetchTtsModels() },
                isTimeoutEnabled = state.isTtsTimeoutEnabled,
                onToggleTimeout = { streamViewModel.toggleTtsTimeout() },
                onCancelQueue = { streamViewModel.cancelTtsQueue() },
                onApplyTrigger = { streamViewModel.applyCharacterTrigger(it) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Audio file playback — send audio file to user
            AudioPlaybackCard(
                isSending = state.isSendingAudio,
                onlineUsers = state.onlineUsers,
                userNameMap = state.userNameMap,
                onSendAudio = { userId, base64, volume ->
                    streamViewModel.sendAudioToUser(userId, base64, volume = volume)
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Audio preset — send preset audio files to user
            AudioPresetCard(
                presets = state.audioPresets,
                isLoading = state.isLoadingPresets,
                isSending = state.isSendingAudio,
                onlineUsers = state.onlineUsers,
                userNameMap = state.userNameMap,
                onSendPreset = { userId, filename, loop, volume ->
                    streamViewModel.sendPresetToUser(userId, filename, loop, volume)
                },
                onStopAudio = { userId -> streamViewModel.stopAudioForUser(userId) },
                onRefreshPresets = { streamViewModel.loadAudioPresets() },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Device status — targets a specific user
            DeviceStatusCard(
                screenOn = state.screenOn,
                screenLocked = state.screenLocked,
                onlineUsers = state.onlineUsers,
                userNameMap = state.userNameMap,
                onRefreshStatus = { streamViewModel.requestScreenStatus(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Connection Status ─────────────────────────────────────────────────────────

@Composable
private fun ConnectionStatusBar(isConnected: Boolean, error: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isConnected) AriaGreen.copy(alpha = 0.1f) else AriaRed.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (isConnected) AriaGreen else AriaRed, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isConnected) "WebSocket เชื่อมต่อแล้ว" else "ยังไม่ได้เชื่อมต่อ",
            color = if (isConnected) AriaGreen else AriaRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        if (error != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "• $error", color = AriaRed, fontSize = 11.sp)
        }
    }
}

// ─── Online Users Panel (multi-user controls) ────────────────────────────────

@Composable
private fun OnlineUsersPanel(
    onlineUsers: List<String>,
    userNameMap: Map<String, String>,
    activeStreams: Map<String, UserStreamState>,
    audioQuality: String,
    videoResolution: String,
    videoFps: Int,
    onStartMic: (String) -> Unit,
    onStopMic: (String) -> Unit,
    onStartCam: (String) -> Unit,
    onStopCam: (String) -> Unit,
    onSwitchCam: (String) -> Unit,
    onToggleMute: (String) -> Unit,
) {
    HudPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("User Online", color = AriaCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${onlineUsers.size} คน",
                color = if (onlineUsers.isEmpty()) AriaTextMuted else AriaGreen,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (onlineUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AriaRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("ไม่มี User Online", color = AriaTextMuted, fontSize = 13.sp)
            }
        } else {
            onlineUsers.forEach { userId ->
                val displayName = userNameMap[userId] ?: userId.take(8) + "…"
                val stream = activeStreams[userId]
                UserStreamControlRow(
                    userId = userId,
                    displayName = displayName,
                    stream = stream,
                    onStartMic = { onStartMic(userId) },
                    onStopMic = { onStopMic(userId) },
                    onStartCam = { onStartCam(userId) },
                    onStopCam = { onStopCam(userId) },
                    onSwitchCam = { onSwitchCam(userId) },
                    onToggleMute = { onToggleMute(userId) },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun UserStreamControlRow(
    userId: String,
    displayName: String,
    stream: UserStreamState?,
    onStartMic: () -> Unit,
    onStopMic: () -> Unit,
    onStartCam: () -> Unit,
    onStopCam: () -> Unit,
    onSwitchCam: () -> Unit,
    onToggleMute: () -> Unit,
) {
    val micActive = stream?.micActive == true
    val camActive = stream?.camActive == true
    val audioMuted = stream?.audioMuted == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (micActive || camActive) AriaCyan.copy(alpha = 0.05f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (micActive || camActive) AriaCyan.copy(alpha = 0.2f) else AriaBorder.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Online dot + name
        Box(modifier = Modifier.size(6.dp).background(AriaGreen, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, color = AriaTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (micActive || camActive) {
                Row {
                    if (micActive) Text("MIC ", color = AriaCyan, fontSize = 10.sp)
                    if (camActive) Text("CAM ", color = AriaOrange, fontSize = 10.sp)
                    if (audioMuted) Text("MUTED", color = AriaRed, fontSize = 10.sp)
                }
            }
        }

        // Mute toggle (only if mic active)
        if (micActive) {
            IconButton(onClick = onToggleMute, modifier = Modifier.size(30.dp)) {
                Icon(
                    if (audioMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (audioMuted) AriaRed else AriaTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Mic toggle
        IconButton(
            onClick = { if (micActive) onStopMic() else onStartMic() },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                if (micActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = "Mic",
                tint = if (micActive) AriaCyan else AriaTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        // Cam toggle
        IconButton(
            onClick = { if (camActive) onStopCam() else onStartCam() },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                if (camActive) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                contentDescription = "Camera",
                tint = if (camActive) AriaOrange else AriaTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        // Switch camera (only if cam active)
        if (camActive) {
            IconButton(onClick = onSwitchCam, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = "สลับกล้อง",
                    tint = AriaOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Active Streams (video grid + waveforms) ─────────────────────────────────

@Composable
private fun ActiveStreamsSection(
    activeStreams: Map<String, UserStreamState>,
    userNameMap: Map<String, String>,
) {
    val streamList = activeStreams.values.toList()

    // Video feeds
    val videoStreams = streamList.filter { it.camActive && it.currentFrame != null }
    if (videoStreams.isNotEmpty()) {
        GlowCard {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("กล้อง (${videoStreams.size})", color = AriaOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                videoStreams.forEach { stream ->
                    val name = userNameMap[stream.userId] ?: stream.userId.take(8)
                    Text(
                        text = "$name — ${if (stream.cameraSide == "front") "หน้า" else "หลัง"}",
                        color = AriaTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    stream.currentFrame?.let { VideoPiP(bitmap = it) }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Audio waveforms
    val audioStreams = streamList.filter { it.micActive && it.audioAmplitudes.isNotEmpty() }
    if (audioStreams.isNotEmpty()) {
        GlowCard {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ไมโครโฟน (${audioStreams.size})", color = AriaCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                audioStreams.forEach { stream ->
                    val name = userNameMap[stream.userId] ?: stream.userId.take(8)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, color = AriaTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("● LIVE", color = AriaRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (stream.audioMuted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MUTED", color = AriaRed.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    WaveformVisualizer(amplitudes = stream.audioAmplitudes)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─── Quality Settings ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySettingsCard(
    audioQuality: String,
    videoResolution: String,
    videoFps: Int,
    onAudioQualityChange: (String) -> Unit,
    onResolutionChange: (String) -> Unit,
    onFpsChange: (Int) -> Unit,
) {
    HudPanel {
        Text("ตั้งค่าคุณภาพ", color = AriaCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("ใช้กับ stream ที่เริ่มใหม่", color = AriaTextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Audio quality
        var audioExpanded by remember { mutableStateOf(false) }
        val qualityOptions = listOf("low" to "ต่ำ (8kHz)", "medium" to "กลาง (16kHz)", "high" to "สูง (44kHz)")
        ExposedDropdownMenuBox(expanded = audioExpanded, onExpandedChange = { audioExpanded = it }) {
            OutlinedTextField(
                value = qualityOptions.find { it.first == audioQuality }?.second ?: audioQuality,
                onValueChange = {},
                readOnly = true,
                label = { Text("คุณภาพเสียง", color = AriaTextMuted, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaCyan,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            ExposedDropdownMenu(expanded = audioExpanded, onDismissRequest = { audioExpanded = false }) {
                qualityOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onAudioQualityChange(value); audioExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Resolution
            var resExpanded by remember { mutableStateOf(false) }
            val resOptions = listOf("480p", "720p", "1080p")
            ExposedDropdownMenuBox(
                expanded = resExpanded,
                onExpandedChange = { resExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = videoResolution,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ความละเอียด", color = AriaTextMuted, fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AriaOrange,
                        unfocusedBorderColor = AriaBorder,
                        focusedTextColor = AriaTextPrimary,
                        unfocusedTextColor = AriaTextPrimary,
                    )
                )
                ExposedDropdownMenu(expanded = resExpanded, onDismissRequest = { resExpanded = false }) {
                    resOptions.forEach { res ->
                        DropdownMenuItem(
                            text = { Text(res) },
                            onClick = { onResolutionChange(res); resExpanded = false }
                        )
                    }
                }
            }

            // FPS
            var fpsExpanded by remember { mutableStateOf(false) }
            val fpsOptions = listOf(15, 24, 30)
            ExposedDropdownMenuBox(
                expanded = fpsExpanded,
                onExpandedChange = { fpsExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = "$videoFps fps",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("FPS", color = AriaTextMuted, fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fpsExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AriaOrange,
                        unfocusedBorderColor = AriaBorder,
                        focusedTextColor = AriaTextPrimary,
                        unfocusedTextColor = AriaTextPrimary,
                    )
                )
                ExposedDropdownMenu(expanded = fpsExpanded, onDismissRequest = { fpsExpanded = false }) {
                    fpsOptions.forEach { f ->
                        DropdownMenuItem(
                            text = { Text("$f fps") },
                            onClick = { onFpsChange(f); fpsExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

// ─── TTS Sidebar Card (new multi-user layout) ─────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TtsSidebarCard(
    onlineUsers: List<String>,
    userNameMap: Map<String, String>,
    selectedUserId: String?,
    userTtsStates: Map<String, UserTtsState>,
    ttsModels: List<TtsModelItem>,
    isGenerating: Boolean,
    ttsQueueProgress: String,
    isTtsQueueRunning: Boolean,
    pendingTrigger: CharacterTrigger?,
    onSelectUser: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onSend: (String) -> Unit,
    onSendAll: (String) -> Unit,
    onModelChange: (String, Int) -> Unit,
    onSpeakerChange: (String, Int) -> Unit,
    onRefreshModels: () -> Unit,
    isTimeoutEnabled: Boolean,
    onToggleTimeout: () -> Unit,
    onCancelQueue: () -> Unit,
    onApplyTrigger: (CharacterTrigger) -> Unit,
) {
    // Auto-apply pending trigger
    LaunchedEffect(pendingTrigger) {
        pendingTrigger?.let { onApplyTrigger(it) }
    }

    HudPanel {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "TTS — ส่งเสียงพูด",
                color = AriaCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (isGenerating) {
                CircularProgressIndicator(color = AriaCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("กำลัง generate...", color = AriaTextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onRefreshModels, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = AriaCyan, modifier = Modifier.size(18.dp))
            }
        }

        // Pending trigger banner
        if (pendingTrigger != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AriaCyan.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(pendingTrigger.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "🔔 ${pendingTrigger.userName} ขอคุย: ${pendingTrigger.characterName}",
                    color = AriaCyan, fontSize = 12.sp, modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { onApplyTrigger(pendingTrigger) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp),
                ) { Text("ไปที่ User", fontSize = 11.sp) }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (onlineUsers.isEmpty()) {
            Text("ไม่มี User ออนไลน์", color = AriaTextMuted, fontSize = 13.sp)
            return@HudPanel
        }

        // Sidebar + Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
        ) {
            // ─── Left Sidebar: user list ────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .background(AriaBgDark, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(onlineUsers) { uid ->
                    val name = userNameMap[uid] ?: uid.take(6)
                    val isSelected = uid == selectedUserId
                    val hasTrigger = pendingTrigger?.userId == uid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) AriaCyan.copy(alpha = 0.15f)
                                else Color.Transparent,
                                RoundedCornerShape(6.dp),
                            )
                            .clickable { onSelectUser(uid) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isSelected) AriaCyan.copy(alpha = 0.3f) else AriaBgSurface,
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    name.firstOrNull()?.uppercase() ?: "?",
                                    color = if (isSelected) AriaCyan else AriaTextMuted,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                            if (hasTrigger) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(AriaOrange, CircleShape)
                                        .align(Alignment.TopEnd),
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            name.take(8),
                            color = if (isSelected) AriaCyan else AriaTextMuted,
                            fontSize = 9.sp, maxLines = 1,
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(AriaGreen, CircleShape),
                        )
                    }
                }
            }

            // ─── Right Panel: TTS for selected user ─────────────────────────
            val uid = selectedUserId
            if (uid == null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AriaBgDark),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("เลือก User ทางซ้าย", color = AriaTextMuted, fontSize = 13.sp)
                }
            } else {
                val userState = userTtsStates[uid] ?: UserTtsState()
                val speakers = userState.speakersCached
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AriaBgDark, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Model dropdown
                    if (ttsModels.isNotEmpty()) {
                        var modelExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = modelExpanded,
                            onExpandedChange = { modelExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = ttsModels.firstOrNull { it.id == userState.modelId }?.name ?: "Model ${userState.modelId}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Model", color = AriaTextMuted, fontSize = 10.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AriaCyan, unfocusedBorderColor = AriaBorder,
                                    focusedTextColor = AriaTextPrimary, unfocusedTextColor = AriaTextPrimary,
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            )
                            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                                ttsModels.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.name, fontSize = 12.sp) },
                                        onClick = { onModelChange(uid, m.id); modelExpanded = false },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // Speaker dropdown
                    if (speakers.isNotEmpty()) {
                        var speakerExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = speakerExpanded,
                            onExpandedChange = { speakerExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = speakers.firstOrNull { it.id == userState.speakerId }?.name ?: "Speaker ${userState.speakerId}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Character", color = AriaTextMuted, fontSize = 10.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speakerExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AriaCyan, unfocusedBorderColor = AriaBorder,
                                    focusedTextColor = AriaTextPrimary, unfocusedTextColor = AriaTextPrimary,
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            )
                            ExposedDropdownMenu(expanded = speakerExpanded, onDismissRequest = { speakerExpanded = false }) {
                                speakers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name, fontSize = 12.sp) },
                                        onClick = { onSpeakerChange(uid, s.id); speakerExpanded = false },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // Text input
                    OutlinedTextField(
                        value = userState.draft,
                        onValueChange = { onTextChange(uid, it) },
                        placeholder = { Text("พิมพ์ข้อความ...", color = AriaTextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AriaCyan, unfocusedBorderColor = AriaBorder,
                            focusedTextColor = AriaTextPrimary, unfocusedTextColor = AriaTextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        maxLines = 3,
                    )

                    // History chips
                    if (userState.history.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(userState.history) { h ->
                                AssistChip(
                                    onClick = { onTextChange(uid, h) },
                                    label = { Text(h.take(20), fontSize = 10.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = AriaBgSurface,
                                        labelColor = AriaTextSecondary,
                                    ),
                                    border = AssistChipDefaults.assistChipBorder(true),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Send buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { onSend(uid) },
                            enabled = userState.draft.isNotEmpty() && !isGenerating,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ส่ง", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = { onSendAll(uid) },
                            enabled = userState.draft.isNotEmpty() && !isGenerating,
                            colors = ButtonDefaults.filledTonalButtonColors(contentColor = AriaOrange),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) { Text("ทุกคน", fontSize = 11.sp, color = AriaOrange) }
                    }

                    // Queue progress + cancel
                    if (isTtsQueueRunning || ttsQueueProgress.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                ttsQueueProgress,
                                color = when {
                                    ttsQueueProgress.startsWith("✅") -> AriaGreen
                                    ttsQueueProgress.startsWith("❌") -> AriaRed
                                    ttsQueueProgress.startsWith("🚫") -> AriaOrange
                                    else -> AriaCyan
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                            )
                            if (isTtsQueueRunning) {
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = onCancelQueue,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = AriaRed.copy(alpha = 0.2f),
                                        contentColor = AriaRed,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ยกเลิก", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // Timeout toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("⏱ Timeout 15s", color = AriaTextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = isTimeoutEnabled,
                            onCheckedChange = { onToggleTimeout() },
                            modifier = Modifier.size(width = 36.dp, height = 20.dp),
                            colors = SwitchDefaults.colors(checkedTrackColor = AriaCyan),
                        )
                    }
                }
            }
        }
    }
}

// ─── TTS Control ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TtsControlCard(
    text: String,
    isGenerating: Boolean,
    modelId: Int,
    speakerId: Int,
    ttsModels: List<TtsModelItem>,
    ttsSpeakers: List<TtsSpeakerItem>,
    selectedTtsUserId: String?,
    onlineUsers: List<String>,
    userNameMap: Map<String, String>,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendAll: () -> Unit,
    onModelChange: (Int) -> Unit,
    onSpeakerChange: (Int) -> Unit,
    onSelectUser: (String?) -> Unit,
    onRefreshModels: () -> Unit = {},
    onClearText: () -> Unit = {},
    isTimeoutEnabled: Boolean = true,
    onToggleTimeout: () -> Unit = {},
    ttsQueueProgress: String = "",
    isTtsQueueRunning: Boolean = false,
    onCancelQueue: () -> Unit = {},
) {
    HudPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TTS — ส่งเสียงพูด",
                color = AriaCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isGenerating) {
                CircularProgressIndicator(color = AriaCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("กำลัง generate...", color = AriaTextMuted, fontSize = 11.sp)
            }
            IconButton(onClick = onRefreshModels, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "โหลด Models ใหม่",
                    tint = AriaCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Request Timeout toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Request Timeout (${if (isTimeoutEnabled) "15s" else "OFF"})",
                color = if (isTimeoutEnabled) AriaTextSecondary else AriaOrange,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isTimeoutEnabled,
                onCheckedChange = { onToggleTimeout() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AriaCyan,
                    checkedTrackColor = AriaCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = AriaOrange,
                    uncheckedTrackColor = AriaOrange.copy(alpha = 0.3f),
                ),
                modifier = Modifier.height(24.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Target user selector
        var userExpanded by remember { mutableStateOf(false) }
        val selectedName = selectedTtsUserId?.let { userNameMap[it] ?: it.take(8) + "…" } ?: "เลือก User เป้าหมาย"
        ExposedDropdownMenuBox(expanded = userExpanded, onExpandedChange = { userExpanded = it }) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text("ส่ง TTS ไปยัง User", color = AriaTextMuted, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaCyan,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            ExposedDropdownMenu(expanded = userExpanded, onDismissRequest = { userExpanded = false }) {
                onlineUsers.forEach { userId ->
                    val name = userNameMap[userId] ?: userId.take(8) + "…"
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                color = if (userId == selectedTtsUserId) AriaCyan else AriaTextPrimary,
                                fontSize = 13.sp
                            )
                        },
                        onClick = { onSelectUser(userId); userExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Model selector
        var modelExpanded by remember { mutableStateOf(false) }
        val currentModelName = ttsModels.find { it.id == modelId }?.name ?: "Model $modelId"
        ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
            OutlinedTextField(
                value = currentModelName,
                onValueChange = {},
                readOnly = true,
                label = { Text("โมเดลเสียง", color = AriaTextMuted, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaCyan,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                if (ttsModels.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("ไม่พบ models (TTS Server อาจยังไม่เปิด)", color = AriaTextMuted, fontSize = 12.sp) },
                        onClick = {}
                    )
                } else {
                    ttsModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${model.name} (${model.numSpeakers} เสียง)",
                                    color = if (model.id == modelId) AriaCyan else AriaTextPrimary,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = { onModelChange(model.id); modelExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speaker selector
        var speakerExpanded by remember { mutableStateOf(false) }
        val currentSpeakerName = ttsSpeakers.find { it.id == speakerId }?.name ?: "Speaker $speakerId"
        ExposedDropdownMenuBox(expanded = speakerExpanded, onExpandedChange = { speakerExpanded = it }) {
            OutlinedTextField(
                value = currentSpeakerName,
                onValueChange = {},
                readOnly = true,
                label = { Text("เสียงตัวละคร", color = AriaTextMuted, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speakerExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaCyan,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            ExposedDropdownMenu(expanded = speakerExpanded, onDismissRequest = { speakerExpanded = false }) {
                if (ttsSpeakers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("ไม่พบ speakers", color = AriaTextMuted, fontSize = 12.sp) },
                        onClick = {}
                    )
                } else {
                    ttsSpeakers.forEach { speaker ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    speaker.name,
                                    color = if (speaker.id == speakerId) AriaCyan else AriaTextPrimary,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = { onSpeakerChange(speaker.id); speakerExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("พิมพ์ข้อความที่จะพูด", color = AriaTextMuted) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaCyan,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            FilledTonalButton(
                onClick = onSend,
                enabled = text.isNotEmpty() && selectedTtsUserId != null,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AriaCyan.copy(alpha = 0.2f),
                    contentColor = AriaCyan
                )
            ) {
                Icon(Icons.Filled.Send, contentDescription = "ส่ง", modifier = Modifier.size(18.dp))
            }
            FilledTonalButton(
                onClick = onSendAll,
                enabled = text.isNotEmpty() && onlineUsers.isNotEmpty(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AriaOrange.copy(alpha = 0.2f),
                    contentColor = AriaOrange
                )
            ) {
                Text("ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (text.isNotEmpty()) {
                IconButton(onClick = onClearText, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "ล้างข้อความ",
                        tint = AriaTextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ─── Queue Progress ───────────────────────────────────────────────────
        if (isTtsQueueRunning || ttsQueueProgress.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ttsQueueProgress,
                    color = when {
                        ttsQueueProgress.startsWith("✅") -> AriaGreen
                        ttsQueueProgress.startsWith("❌") -> AriaRed
                        ttsQueueProgress.startsWith("🚫") -> AriaOrange
                        else -> AriaCyan
                    },
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                if (isTtsQueueRunning) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onCancelQueue,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AriaRed.copy(alpha = 0.2f),
                            contentColor = AriaRed,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ยกเลิก", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ─── Audio File Playback ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioPlaybackCard(
    isSending: Boolean,
    onlineUsers: List<String>,
    userNameMap: Map<String, String>,
    onSendAudio: (userId: String, audioBase64: String, volume: Float) -> Unit,
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var audioBase64 by remember { mutableStateOf<String?>(null) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    val context = LocalContext.current

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    audioBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    // ดึงชื่อไฟล์
                    val cursor = context.contentResolver.query(it, null, null, null, null)
                    selectedFileName = cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) c.getString(nameIndex) else null
                        } else null
                    } ?: "audio file"
                }
            } catch (_: Exception) {
                selectedFileName = null
                audioBase64 = null
            }
        }
    }

    HudPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = AriaOrange, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "ส่งไฟล์เสียง",
                color = AriaTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isSending) {
                CircularProgressIndicator(color = AriaOrange, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("กำลังส่ง...", color = AriaTextMuted, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // User selector
        var userExpanded by remember { mutableStateOf(false) }
        val selectedName = selectedUserId?.let { userNameMap[it] ?: it.take(8) + "…" } ?: "เลือก User เป้าหมาย"
        ExposedDropdownMenuBox(expanded = userExpanded, onExpandedChange = { userExpanded = it }) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text("ส่งเสียงไปยัง User", color = AriaTextMuted, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaOrange,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            ExposedDropdownMenu(expanded = userExpanded, onDismissRequest = { userExpanded = false }) {
                onlineUsers.forEach { userId ->
                    val name = userNameMap[userId] ?: userId.take(8) + "…"
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                color = if (userId == selectedUserId) AriaOrange else AriaTextPrimary,
                                fontSize = 13.sp
                            )
                        },
                        onClick = { selectedUserId = userId; userExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Volume slider
        VolumeSlider(volume = volume, onVolumeChange = { volume = it })

        Spacer(modifier = Modifier.height(8.dp))

        // File picker + Send
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pick file button
            FilledTonalButton(
                onClick = { audioPickerLauncher.launch("audio/*") },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AriaOrange.copy(alpha = 0.2f),
                    contentColor = AriaOrange
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedFileName ?: "เลือกไฟล์เสียง",
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Send button
            FilledTonalButton(
                onClick = {
                    val uid = selectedUserId
                    val b64 = audioBase64
                    if (uid != null && b64 != null) {
                        onSendAudio(uid, b64, volume)
                        selectedFileName = null
                        audioBase64 = null
                    }
                },
                enabled = selectedUserId != null && audioBase64 != null && !isSending,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AriaOrange.copy(alpha = 0.2f),
                    contentColor = AriaOrange
                )
            ) {
                Icon(Icons.Filled.Send, "ส่ง", modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Audio Preset ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioPresetCard(
    presets: List<AudioPresetItem>,
    isLoading: Boolean,
    isSending: Boolean,
    onlineUsers: List<String>,
    userNameMap: Map<String, String>,
    onSendPreset: (userId: String, filename: String, loop: Boolean, volume: Float) -> Unit,
    onStopAudio: (userId: String) -> Unit,
    onRefreshPresets: () -> Unit,
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedPreset by remember { mutableStateOf<AudioPresetItem?>(null) }
    var loopEnabled by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(1.0f) }

    HudPanel {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = AriaCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Preset เสียง",
                color = AriaTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(color = AriaCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            if (isSending) {
                Spacer(modifier = Modifier.width(6.dp))
                CircularProgressIndicator(color = AriaOrange, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("กำลังส่ง...", color = AriaTextMuted, fontSize = 11.sp)
            }
            IconButton(onClick = onRefreshPresets, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.Refresh, "รีเฟรช", tint = AriaTextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // User selector
        var userExpanded by remember { mutableStateOf(false) }
        val selectedName = selectedUserId?.let { userNameMap[it] ?: it.take(8) + "…" } ?: "เลือก User เป้าหมาย"
        ExposedDropdownMenuBox(expanded = userExpanded, onExpandedChange = { userExpanded = it }) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text("ส่ง Preset ไปยัง User", color = AriaTextMuted, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaCyan,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            ExposedDropdownMenu(expanded = userExpanded, onDismissRequest = { userExpanded = false }) {
                onlineUsers.forEach { userId ->
                    val name = userNameMap[userId] ?: userId.take(8) + "…"
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                color = if (userId == selectedUserId) AriaCyan else AriaTextPrimary,
                                fontSize = 13.sp
                            )
                        },
                        onClick = { selectedUserId = userId; userExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset selector
        var presetExpanded by remember { mutableStateOf(false) }
        val presetLabel = selectedPreset?.let { "${it.name} (${formatFileSize(it.size)})" } ?: "เลือกไฟล์เสียง Preset"
        ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = { presetExpanded = it }) {
            OutlinedTextField(
                value = presetLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("ไฟล์เสียง Preset", color = AriaTextMuted, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriaOrange,
                    unfocusedBorderColor = AriaBorder,
                    focusedTextColor = AriaTextPrimary,
                    unfocusedTextColor = AriaTextPrimary,
                )
            )
            ExposedDropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false }) {
                if (presets.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("ไม่มีไฟล์ Preset", color = AriaTextMuted, fontSize = 13.sp) },
                        onClick = { presetExpanded = false }
                    )
                } else {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        preset.name,
                                        color = if (preset == selectedPreset) AriaOrange else AriaTextPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        formatFileSize(preset.size),
                                        color = AriaTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = { selectedPreset = preset; presetExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Volume slider
        VolumeSlider(volume = volume, onVolumeChange = { volume = it })

        Spacer(modifier = Modifier.height(8.dp))

        // Loop toggle + action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Loop toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = if (loopEnabled) AriaOrange else AriaTextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Loop",
                    color = if (loopEnabled) AriaOrange else AriaTextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = loopEnabled,
                    onCheckedChange = { loopEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AriaOrange,
                        checkedTrackColor = AriaOrange.copy(alpha = 0.3f),
                        uncheckedThumbColor = AriaTextMuted,
                        uncheckedTrackColor = AriaBorder,
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            // Send button
            FilledTonalButton(
                onClick = {
                    val uid = selectedUserId
                    val preset = selectedPreset
                    if (uid != null && preset != null) {
                        onSendPreset(uid, preset.name, loopEnabled, volume)
                    }
                },
                enabled = selectedUserId != null && selectedPreset != null && !isSending,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AriaCyan.copy(alpha = 0.2f),
                    contentColor = AriaCyan
                )
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ส่ง", fontSize = 12.sp)
            }

            // Stop button
            FilledTonalButton(
                onClick = { selectedUserId?.let { onStopAudio(it) } },
                enabled = selectedUserId != null,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AriaRed.copy(alpha = 0.2f),
                    contentColor = AriaRed
                )
            ) {
                Icon(Icons.Filled.Stop, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("หยุด", fontSize = 12.sp)
            }
        }
    }
}

/** Format file size to human readable (KB/MB) */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
    }
}

// ─── Volume Slider (reusable) ─────────────────────────────────────────────────

@Composable
private fun VolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = AriaTextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Volume", color = AriaTextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AriaOrange,
                activeTrackColor = AriaOrange,
                inactiveTrackColor = AriaBorder,
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(volume * 100).toInt()}%",
            color = AriaOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Device Status ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceStatusCard(
    screenOn: Boolean?,
    screenLocked: Boolean?,
    onlineUsers: List<String>,
    userNameMap: Map<String, String>,
    onRefreshStatus: (String) -> Unit,
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    GlowCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Smartphone,
                    contentDescription = null,
                    tint = AriaCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("อุปกรณ์", color = AriaTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User selector for device status
            var expanded by remember { mutableStateOf(false) }
            val selectedName = selectedUserId?.let { userNameMap[it] ?: it.take(8) + "…" } ?: "เลือก User"
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ตรวจสอบอุปกรณ์ของ", color = AriaTextMuted, fontSize = 11.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AriaCyan,
                        unfocusedBorderColor = AriaBorder,
                        focusedTextColor = AriaTextPrimary,
                        unfocusedTextColor = AriaTextPrimary,
                    )
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    onlineUsers.forEach { userId ->
                        val name = userNameMap[userId] ?: userId.take(8) + "…"
                        DropdownMenuItem(
                            text = { Text(name, fontSize = 13.sp) },
                            onClick = { selectedUserId = userId; expanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("หน้าจอ: ", color = AriaTextSecondary, fontSize = 13.sp)
                        when (screenOn) {
                            null -> Text("ยังไม่ได้ตรวจ", color = AriaTextMuted, fontSize = 13.sp)
                            true -> Text("เปิดอยู่", color = AriaGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            false -> Text("ดับ", color = AriaRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ล็อค: ", color = AriaTextSecondary, fontSize = 13.sp)
                        when (screenLocked) {
                            null -> Text("ยังไม่ได้ตรวจ", color = AriaTextMuted, fontSize = 13.sp)
                            true -> Text("ล็อคอยู่", color = AriaOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            false -> Text("ปลดล็อค", color = AriaGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                FilledTonalButton(
                    onClick = { selectedUserId?.let { onRefreshStatus(it) } },
                    enabled = selectedUserId != null,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AriaCyan.copy(alpha = 0.2f),
                        contentColor = AriaCyan
                    )
                ) {
                    Text("ตรวจสอบ", fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Waveform Visualizer ──────────────────────────────────────────────────────

@Composable
private fun WaveformVisualizer(amplitudes: List<Float>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AriaBgDark, RoundedCornerShape(8.dp))
            .border(1.dp, AriaCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / amplitudes.size.coerceAtLeast(1)
            val centerY = size.height / 2f

            amplitudes.forEachIndexed { index, amplitude ->
                val barHeight = (amplitude * size.height * 0.8f).coerceAtLeast(2f)
                val x = index * barWidth + barWidth / 2f
                val alpha = 0.4f + amplitude * 0.6f

                drawLine(
                    color = AriaCyan.copy(alpha = alpha),
                    start = Offset(x, centerY - barHeight / 2f),
                    end = Offset(x, centerY + barHeight / 2f),
                    strokeWidth = (barWidth * 0.6f).coerceAtLeast(2f)
                )
            }
        }
    }
}

// ─── PiP Video ────────────────────────────────────────────────────────────────

@Composable
private fun VideoPiP(bitmap: Bitmap) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, AriaOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Camera feed",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(AriaRed, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
