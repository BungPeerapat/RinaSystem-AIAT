package com.example.rinasystem.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterItem(
    val id: String,
    val name: String,
    @SerialName("model_id") val modelId: Int = 0,
    @SerialName("speaker_id") val speakerId: Int = 0,
    val emoji: String = "🎭",
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class CharacterListResponse(
    val characters: List<CharacterItem>,
    val total: Int,
)

@Serializable
data class CharacterCreateRequest(
    val name: String,
    @SerialName("model_id") val modelId: Int = 0,
    @SerialName("speaker_id") val speakerId: Int = 0,
    val emoji: String = "🎭",
    @SerialName("sort_order") val sortOrder: Int = 0,
)
