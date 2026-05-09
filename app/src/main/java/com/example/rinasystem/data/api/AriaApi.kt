package com.example.rinasystem.data.api

import com.example.rinasystem.data.model.AppVersionResponse
import com.example.rinasystem.data.model.AudioPresetDetailResponse
import com.example.rinasystem.data.model.AudioPresetsResponse
import com.example.rinasystem.data.model.CharacterCreateRequest
import com.example.rinasystem.data.model.CharacterItem
import com.example.rinasystem.data.model.CharacterListResponse
import com.example.rinasystem.data.model.TtsMessagesResponse
import com.example.rinasystem.data.model.TtsModelsResponse
import com.example.rinasystem.data.model.TtsSpeakersResponse
import com.example.rinasystem.data.model.DashboardResponse
import com.example.rinasystem.data.model.LoginRequest
import com.example.rinasystem.data.model.TestAccountResponse
import com.example.rinasystem.data.model.TestLoginRequest
import com.example.rinasystem.data.model.MessageResponse
import com.example.rinasystem.data.model.RefreshRequest
import com.example.rinasystem.data.model.RegisterRequest
import com.example.rinasystem.data.model.StreamSessionResponse
import com.example.rinasystem.data.model.TokenResponse
import com.example.rinasystem.data.model.UpdateFCMTokenRequest
import com.example.rinasystem.data.model.UpdateRoleRequest
import com.example.rinasystem.data.model.UpdateStatusRequest
import com.example.rinasystem.data.model.UpdateUserRequest
import com.example.rinasystem.data.model.UserListResponse
import com.example.rinasystem.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AriaApi {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<TokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @GET("api/auth/test-accounts")
    suspend fun getTestAccounts(): Response<List<TestAccountResponse>>

    @POST("api/auth/test-login")
    suspend fun testLogin(@Body body: TestLoginRequest): Response<TokenResponse>

    // User
    @GET("api/users/me")
    suspend fun getMe(): Response<UserResponse>

    @PUT("api/users/me")
    suspend fun updateMe(@Body body: UpdateUserRequest): Response<UserResponse>

    @PUT("api/users/me/fcm-token")
    suspend fun updateFCMToken(@Body body: UpdateFCMTokenRequest): Response<MessageResponse>

    @GET("api/users/me/messages")
    suspend fun getMyMessages(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<TtsMessagesResponse>

    // Admin
    @GET("api/admin/users")
    suspend fun getUsers(): Response<UserListResponse>

    @GET("api/admin/users/{user_id}")
    suspend fun getUser(@Path("user_id") userId: String): Response<UserResponse>

    @PUT("api/admin/users/{user_id}/status")
    suspend fun updateUserStatus(
        @Path("user_id") userId: String,
        @Body body: UpdateStatusRequest
    ): Response<MessageResponse>

    @PUT("api/admin/users/{user_id}/role")
    suspend fun updateUserRole(
        @Path("user_id") userId: String,
        @Body body: UpdateRoleRequest
    ): Response<UserResponse>

    @GET("api/admin/users/{user_id}/streams")
    suspend fun getUserStreams(@Path("user_id") userId: String): Response<List<StreamSessionResponse>>

    @GET("api/admin/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @DELETE("api/admin/messages/clear")
    suspend fun clearMessages(): Response<MessageResponse>

    // TTS Models & Speakers
    @GET("api/admin/tts/models")
    suspend fun getTtsModels(): Response<TtsModelsResponse>

    @GET("api/admin/tts/speakers")
    suspend fun getTtsSpeakers(@Query("model_id") modelId: Int): Response<TtsSpeakersResponse>

    // Audio Presets
    @GET("api/admin/audio-presets")
    suspend fun getAudioPresets(): Response<AudioPresetsResponse>

    @GET("api/admin/audio-presets/{filename}")
    suspend fun getAudioPreset(@Path("filename") filename: String): Response<AudioPresetDetailResponse>

    // App Update
    @GET("api/app/version")
    suspend fun getLatestVersion(): Response<AppVersionResponse>

    // Characters (User: trigger, Admin: manage)
    @GET("api/users/characters")
    suspend fun getUserCharacters(): Response<CharacterListResponse>

    @GET("api/admin/characters")
    suspend fun getAdminCharacters(): Response<CharacterListResponse>

    @POST("api/admin/characters")
    suspend fun createCharacter(@Body body: CharacterCreateRequest): Response<CharacterItem>

    @PUT("api/admin/characters/{character_id}")
    suspend fun updateCharacter(
        @Path("character_id") characterId: String,
        @Body body: CharacterCreateRequest,
    ): Response<CharacterItem>

    @DELETE("api/admin/characters/{character_id}")
    suspend fun deleteCharacter(@Path("character_id") characterId: String): Response<MessageResponse>
}
