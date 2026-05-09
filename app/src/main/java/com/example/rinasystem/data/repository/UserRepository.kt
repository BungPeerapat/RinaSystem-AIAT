package com.example.rinasystem.data.repository

import com.example.rinasystem.data.api.AriaApi
import com.example.rinasystem.data.model.CharacterItem
import com.example.rinasystem.data.model.ErrorResponse
import com.example.rinasystem.data.model.TtsMessageItem
import com.example.rinasystem.data.model.UpdateFCMTokenRequest
import com.example.rinasystem.data.model.UpdateUserRequest
import com.example.rinasystem.data.model.UserResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: AriaApi,
    private val json: Json
) {
    suspend fun getMe(): ApiResult<UserResponse> {
        return try {
            val response = api.getMe()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun updateProfile(displayName: String): ApiResult<UserResponse> {
        return try {
            val response = api.updateMe(UpdateUserRequest(displayName))
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun updateFCMToken(token: String): ApiResult<Unit> {
        return try {
            val response = api.updateFCMToken(UpdateFCMTokenRequest(token))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun getMyMessages(): ApiResult<List<TtsMessageItem>> {
        return try {
            val response = api.getMyMessages()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.messages)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถโหลดข้อความได้")
        }
    }

    suspend fun getCharacters(): ApiResult<List<CharacterItem>> {
        return try {
            val response = api.getUserCharacters()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.characters)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถโหลด Character ได้")
        }
    }

    private fun parseError(errorBody: String?): ApiResult.Error {
        if (errorBody == null) return ApiResult.Error("เกิดข้อผิดพลาด")
        return try {
            val error = json.decodeFromString<ErrorResponse>(errorBody)
            ApiResult.Error(error.detail ?: "เกิดข้อผิดพลาด", error.errorCode)
        } catch (e: Exception) {
            ApiResult.Error("เกิดข้อผิดพลาด")
        }
    }
}
