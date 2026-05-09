package com.example.rinasystem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rinasystem.data.model.TestAccountResponse
import com.example.rinasystem.data.model.UserResponse
import com.example.rinasystem.data.repository.ApiResult
import com.example.rinasystem.data.repository.AuthRepository
import com.example.rinasystem.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: UserResponse? = null,
    val error: String? = null,
    val isCheckingAuth: Boolean = true,
    val testAccounts: List<TestAccountResponse> = emptyList(),
    val isLoadingTestAccounts: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkAuth()
    }

    fun checkAuth() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingAuth = true)
            if (authRepository.isLoggedIn()) {
                when (val result = authRepository.getMe()) {
                    is ApiResult.Success -> {
                        _state.value = _state.value.copy(
                            isLoggedIn = true,
                            user = result.data,
                            isCheckingAuth = false
                        )
                    }
                    is ApiResult.Error -> {
                        _state.value = _state.value.copy(
                            isLoggedIn = false,
                            isCheckingAuth = false
                        )
                    }
                }
            } else {
                _state.value = _state.value.copy(
                    isLoggedIn = false,
                    isCheckingAuth = false
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = result.data.user
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.register(email, password, displayName)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = result.data.user
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = AuthState(isCheckingAuth = false)
        }
    }

    fun loadTestAccounts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingTestAccounts = true, error = null)
            when (val result = authRepository.getTestAccounts()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoadingTestAccounts = false,
                        testAccounts = result.data
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingTestAccounts = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun testLogin(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.testLogin(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = result.data.user
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun updateProfile(displayName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = userRepository.updateProfile(displayName)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        user = result.data
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
