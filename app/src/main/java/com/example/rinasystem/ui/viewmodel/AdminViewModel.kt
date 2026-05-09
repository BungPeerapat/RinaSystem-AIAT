package com.example.rinasystem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rinasystem.data.model.DashboardResponse
import com.example.rinasystem.data.model.StreamSessionResponse
import com.example.rinasystem.data.model.UserResponse
import com.example.rinasystem.data.repository.AdminRepository
import com.example.rinasystem.data.repository.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminState(
    val isLoading: Boolean = false,
    val users: List<UserResponse> = emptyList(),
    val totalUsers: Int = 0,
    val onlineCount: Int = 0,
    val dashboard: DashboardResponse? = null,
    val error: String? = null,
    val successMessage: String? = null,
    // User detail
    val selectedUser: UserResponse? = null,
    val selectedUserStreams: List<StreamSessionResponse> = emptyList(),
    val isDetailLoading: Boolean = false,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = adminRepository.getUsers()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        users = result.data.users,
                        totalUsers = result.data.total,
                        onlineCount = result.data.onlineCount
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

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = adminRepository.getDashboard()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        dashboard = result.data
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

    fun updateUserStatus(userId: String, status: String) {
        viewModelScope.launch {
            when (val result = adminRepository.updateUserStatus(userId, status)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(successMessage = result.data)
                    loadUsers()
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(error = result.message)
                }
            }
        }
    }

    fun updateUserRole(userId: String, role: String) {
        viewModelScope.launch {
            when (val result = adminRepository.updateUserRole(userId, role)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        successMessage = "เปลี่ยน Role เป็น $role สำเร็จ",
                        selectedUser = result.data
                    )
                    loadUsers()
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(error = result.message)
                }
            }
        }
    }

    fun loadUserDetail(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDetailLoading = true)
            val userResult = adminRepository.getUser(userId)
            val streamsResult = adminRepository.getUserStreams(userId)

            _state.value = _state.value.copy(
                isDetailLoading = false,
                selectedUser = when (userResult) {
                    is ApiResult.Success -> userResult.data
                    is ApiResult.Error -> { _state.value = _state.value.copy(error = userResult.message); null }
                },
                selectedUserStreams = when (streamsResult) {
                    is ApiResult.Success -> streamsResult.data
                    is ApiResult.Error -> emptyList()
                }
            )
        }
    }

    fun clearSelectedUser() {
        _state.value = _state.value.copy(
            selectedUser = null,
            selectedUserStreams = emptyList()
        )
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}
