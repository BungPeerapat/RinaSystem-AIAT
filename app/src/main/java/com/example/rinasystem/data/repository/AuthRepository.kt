package com.example.rinasystem.data.repository

import com.example.rinasystem.data.api.AriaApi
import com.example.rinasystem.data.local.TokenManager
import com.example.rinasystem.data.model.LoginRequest
import com.example.rinasystem.data.model.RefreshRequest
import com.example.rinasystem.data.model.RegisterRequest
import com.example.rinasystem.data.model.TestAccountResponse
import com.example.rinasystem.data.model.TestLoginRequest
import com.example.rinasystem.data.model.TokenResponse
import com.example.rinasystem.data.model.UserResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: String? = null) : ApiResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: AriaApi,
    private val tokenManager: TokenManager,
    private val json: Json
) {
    suspend fun login(email: String, password: String): ApiResult<TokenResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                tokenManager.saveUserRole(body.user.role)
                ApiResult.Success(body)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun register(email: String, password: String, displayName: String): ApiResult<TokenResponse> {
        return try {
            val response = api.register(RegisterRequest(email, password, displayName))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                tokenManager.saveUserRole(body.user.role)
                ApiResult.Success(body)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun refresh(): ApiResult<TokenResponse> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return ApiResult.Error("ไม่พบ Refresh Token")
            val response = api.refresh(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                tokenManager.saveUserRole(body.user.role)
                ApiResult.Success(body)
            } else {
                tokenManager.clearAll()
                ApiResult.Error("Session หมดอายุ กรุณาเข้าสู่ระบบใหม่")
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        return try {
            api.logout()
            tokenManager.clearAll()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            tokenManager.clearAll()
            ApiResult.Success(Unit)
        }
    }

    suspend fun getTestAccounts(): ApiResult<List<TestAccountResponse>> {
        return try {
            val response = api.getTestAccounts()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun testLogin(userId: String): ApiResult<TokenResponse> {
        return try {
            val response = api.testLogin(TestLoginRequest(userId))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                tokenManager.saveUserRole(body.user.role)
                ApiResult.Success(body)
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null
    }

    suspend fun getMe(): ApiResult<UserResponse> {
        return try {
            val response = api.getMe()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else if (response.code() == 401) {
                val refreshResult = refresh()
                if (refreshResult is ApiResult.Success) {
                    val retryResponse = api.getMe()
                    if (retryResponse.isSuccessful) {
                        ApiResult.Success(retryResponse.body()!!)
                    } else {
                        parseError(retryResponse.errorBody()?.string())
                    }
                } else {
                    ApiResult.Error("Session หมดอายุ กรุณาเข้าสู่ระบบใหม่")
                }
            } else {
                parseError(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    private fun parseError(errorBody: String?): ApiResult.Error {
        if (errorBody == null) return ApiResult.Error("เกิดข้อผิดพลาด")
        return try {
            val error = json.decodeFromString<com.example.rinasystem.data.model.ErrorResponse>(errorBody)
            ApiResult.Error(error.detail ?: "เกิดข้อผิดพลาด", error.errorCode)
        } catch (e: Exception) {
            ApiResult.Error("เกิดข้อผิดพลาด")
        }
    }
}
