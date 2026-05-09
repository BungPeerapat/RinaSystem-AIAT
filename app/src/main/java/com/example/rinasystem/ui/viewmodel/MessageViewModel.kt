package com.example.rinasystem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rinasystem.data.model.TtsMessageItem
import com.example.rinasystem.data.repository.ApiResult
import com.example.rinasystem.data.repository.UserRepository
import com.example.rinasystem.service.TtsNewMessageEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageState(
    val isLoading: Boolean = false,
    val messages: List<TtsMessageItem> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MessageState())
    val state: StateFlow<MessageState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            TtsNewMessageEvent.event.collect {
                loadMessages()
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = userRepository.getMyMessages()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        messages = result.data,
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message,
                    )
                }
            }
        }
    }
}
