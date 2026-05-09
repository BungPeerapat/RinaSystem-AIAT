package com.example.rinasystem.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserListResponse(
    val users: List<UserResponse>,
    val total: Int,
    @SerialName("online_count") val onlineCount: Int
)

@Serializable
data class UpdateStatusRequest(
    val status: String
)

@Serializable
data class DashboardResponse(
    @SerialName("total_users") val totalUsers: Int,
    @SerialName("online_count") val onlineCount: Int,
    @SerialName("total_streams") val totalStreams: Int,
    @SerialName("total_messages") val totalMessages: Int
)

@Serializable
data class UpdateRoleRequest(
    val role: String
)

@Serializable
data class StreamSessionResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("stream_type") val streamType: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = false
)

@Serializable
data class AudioPresetItem(
    val name: String,
    val size: Long,
    val extension: String,
)

@Serializable
data class AudioPresetsResponse(
    val presets: List<AudioPresetItem>,
)

@Serializable
data class AudioPresetDetailResponse(
    val name: String,
    @SerialName("audio_base64") val audioBase64: String,
    val size: Long,
)

// TTS Models & Speakers

@Serializable
data class TtsModelItem(
    val id: Int,
    val name: String,
    @SerialName("num_speakers") val numSpeakers: Int = 0,
)

@Serializable
data class TtsModelsResponse(
    val models: List<TtsModelItem>,
)

@Serializable
data class TtsSpeakerItem(
    val id: Int,
    val name: String,
)

@Serializable
data class TtsSpeakersResponse(
    val speakers: List<TtsSpeakerItem>,
    @SerialName("model_id") val modelId: Int,
)

// TTS Messages

@Serializable
data class TtsMessageItem(
    val id: String,
    val content: String,
    val status: String,
    @SerialName("is_broadcast") val isBroadcast: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TtsMessagesResponse(
    val messages: List<TtsMessageItem>,
)
