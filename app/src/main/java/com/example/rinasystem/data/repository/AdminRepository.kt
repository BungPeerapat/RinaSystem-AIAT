package com.example.rinasystem.data.repository

import com.example.rinasystem.data.api.AriaApi
import com.example.rinasystem.data.model.AudioPresetDetailResponse
import com.example.rinasystem.data.model.AudioPresetItem
import com.example.rinasystem.data.model.CharacterCreateRequest
import com.example.rinasystem.data.model.CharacterItem
import com.example.rinasystem.data.model.TtsModelItem
import com.example.rinasystem.data.model.TtsSpeakerItem
import com.example.rinasystem.data.model.DashboardResponse
import com.example.rinasystem.data.model.ErrorResponse
import com.example.rinasystem.data.model.StreamSessionResponse
import com.example.rinasystem.data.model.UpdateRoleRequest
import com.example.rinasystem.data.model.UpdateStatusRequest
import com.example.rinasystem.data.model.UserListResponse
import com.example.rinasystem.data.model.UserResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: AriaApi,
    private val json: Json
) {
    suspend fun getUsers(): ApiResult<UserListResponse> {
        return try {
            val response = api.getUsers()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun getUser(userId: String): ApiResult<UserResponse> {
        return try {
            val response = api.getUser(userId)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun updateUserStatus(userId: String, status: String): ApiResult<String> {
        return try {
            val response = api.updateUserStatus(userId, UpdateStatusRequest(status))
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.message)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun updateUserRole(userId: String, role: String): ApiResult<UserResponse> {
        return try {
            val response = api.updateUserRole(userId, UpdateRoleRequest(role))
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun getUserStreams(userId: String): ApiResult<List<StreamSessionResponse>> {
        return try {
            val response = api.getUserStreams(userId)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun getDashboard(): ApiResult<DashboardResponse> {
        return try {
            val response = api.getDashboard()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun clearMessages(): ApiResult<String> {
        return try {
            val response = api.clearMessages()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.message)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun getAudioPresets(): ApiResult<List<AudioPresetItem>> {
        return try {
            val response = api.getAudioPresets()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.presets)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun getAudioPreset(filename: String): ApiResult<AudioPresetDetailResponse> {
        return try {
            val response = api.getAudioPreset(filename)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun getTtsModels(): ApiResult<List<TtsModelItem>> {
        return try {
            val response = api.getTtsModels()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.models)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถโหลดรายชื่อ TTS models ได้")
        }
    }

    suspend fun getTtsSpeakers(modelId: Int): ApiResult<List<TtsSpeakerItem>> {
        return try {
            val response = api.getTtsSpeakers(modelId)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.speakers)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถโหลดรายชื่อ speakers ได้")
        }
    }

    suspend fun getCharacters(): ApiResult<List<CharacterItem>> {
        return try {
            val response = api.getAdminCharacters()
            if (response.isSuccessful) ApiResult.Success(response.body()!!.characters)
            else parseError(response.errorBody()?.string())
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถโหลด Character ได้")
        }
    }

    suspend fun createCharacter(name: String, modelId: Int, speakerId: Int, emoji: String, sortOrder: Int): ApiResult<CharacterItem> {
        return try {
            val response = api.createCharacter(CharacterCreateRequest(name, modelId, speakerId, emoji, sortOrder))
            if (response.isSuccessful) ApiResult.Success(response.body()!!)
            else parseError(response.errorBody()?.string())
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถสร้าง Character ได้")
        }
    }

    suspend fun updateCharacter(id: String, name: String, modelId: Int, speakerId: Int, emoji: String, sortOrder: Int): ApiResult<CharacterItem> {
        return try {
            val response = api.updateCharacter(id, CharacterCreateRequest(name, modelId, speakerId, emoji, sortOrder))
            if (response.isSuccessful) ApiResult.Success(response.body()!!)
            else parseError(response.errorBody()?.string())
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถอัพเดต Character ได้")
        }
    }

    suspend fun deleteCharacter(id: String): ApiResult<Unit> {
        return try {
            val response = api.deleteCharacter(id)
            if (response.isSuccessful) ApiResult.Success(Unit)
            else parseError(response.errorBody()?.string())
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถลบ Character ได้")
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
