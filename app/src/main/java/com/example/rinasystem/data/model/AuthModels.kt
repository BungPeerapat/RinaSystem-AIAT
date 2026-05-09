package com.example.rinasystem.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("display_name") val displayName: String
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    val role: String,
    val status: String,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class ErrorResponse(
    val detail: String? = null,
    @SerialName("error_code") val errorCode: String? = null
)

@Serializable
data class TestAccountResponse(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    val role: String
)

@Serializable
data class TestLoginRequest(
    @SerialName("user_id") val userId: String
)
