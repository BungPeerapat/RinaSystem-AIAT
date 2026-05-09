package com.example.rinasystem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rinasystem.data.model.CharacterItem
import com.example.rinasystem.data.model.UserResponse
import com.example.rinasystem.data.repository.ApiResult
import com.example.rinasystem.data.repository.UserRepository
import com.example.rinasystem.data.ws.AriaWebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserState(
    val isLoading: Boolean = false,
    val user: UserResponse? = null,
    val error: String? = null,
    val updateSuccess: Boolean = false,
    val characters: List<CharacterItem> = emptyList(),
    val isLoadingCharacters: Boolean = false,
    val triggerSuccess: Boolean = false,
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val ariaWebSocket: AriaWebSocket,
) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = userRepository.getMe()) {
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

    fun updateProfile(displayName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, updateSuccess = false)
            when (val result = userRepository.updateProfile(displayName)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        user = result.data,
                        updateSuccess = true
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

    fun loadCharacters() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingCharacters = true)
            when (val result = userRepository.getCharacters()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    isLoadingCharacters = false, characters = result.data
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoadingCharacters = false, error = result.message
                )
            }
        }
    }

    fun triggerCharacter(character: CharacterItem) {
        val json = """{"type":"TRIGGER_CHARACTER","character_name":"${character.name}","model_id":${character.modelId},"speaker_id":${character.speakerId},"emoji":"${character.emoji}"}"""
        ariaWebSocket.sendStatusJson(json)
        _state.value = _state.value.copy(triggerSuccess = true)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _state.value = _state.value.copy(triggerSuccess = false)
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, updateSuccess = false)
    }
}
