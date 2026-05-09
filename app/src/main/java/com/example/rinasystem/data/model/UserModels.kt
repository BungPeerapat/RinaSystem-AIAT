package com.example.rinasystem.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    @SerialName("display_name") val displayName: String
)

@Serializable
data class UpdateFCMTokenRequest(
    @SerialName("fcm_token") val fcmToken: String
)
