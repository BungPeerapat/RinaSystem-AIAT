package com.example.rinasystem.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rinasystem.BuildConfig
import com.example.rinasystem.data.model.AppVersionResponse
import com.example.rinasystem.data.repository.ApiResult
import com.example.rinasystem.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UpdateState(
    val isChecking: Boolean = false,
    val hasChecked: Boolean = false,
    val updateAvailable: Boolean = false,
    val versionInfo: AppVersionResponse? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val error: String? = null,
    val dismissed: Boolean = false,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var downloadedApk: File? = null

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isChecking = true, error = null)
            when (val result = updateRepository.checkForUpdate()) {
                is ApiResult.Success -> {
                    val hasUpdate = updateRepository.isUpdateAvailable(result.data)
                    Log.i("UpdateVM", "Server: code=${result.data.versionCode}, App: code=${BuildConfig.VERSION_CODE}, hasUpdate=$hasUpdate")
                    _state.value = _state.value.copy(
                        isChecking = false,
                        hasChecked = true,
                        updateAvailable = hasUpdate,
                        versionInfo = if (hasUpdate) result.data else null,
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isChecking = false,
                        hasChecked = true,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun downloadAndInstall() {
        val info = _state.value.versionInfo ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isDownloading = true,
                downloadProgress = 0,
                error = null,
            )
            val apkFile = updateRepository.downloadApk(info.downloadUrl) { progress ->
                _state.value = _state.value.copy(downloadProgress = progress)
            }
            if (apkFile != null) {
                downloadedApk = apkFile
                _state.value = _state.value.copy(isDownloading = false, dismissed = true)
                updateRepository.installApk(apkFile)
            } else {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    error = "ดาวน์โหลดไม่สำเร็จ กรุณาลองใหม่",
                )
            }
        }
    }

    fun dismissUpdate() {
        _state.value = _state.value.copy(dismissed = true)
    }
}
