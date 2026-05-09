package com.example.rinasystem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rinasystem.data.local.ServerConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val serverIp: String = ServerConfigManager.DEFAULT_IP,
    val serverPort: String = ServerConfigManager.DEFAULT_PORT.toString(),
    val isTesting: Boolean = false,
    val testResult: TestResult? = null,
    val isSaved: Boolean = false
)

sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Error(val message: String) : TestResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverConfigManager: ServerConfigManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val ip = serverConfigManager.getServerIp()
            val port = serverConfigManager.getServerPort()
            _state.value = _state.value.copy(
                serverIp = ip,
                serverPort = port.toString()
            )
        }
    }

    fun updateIp(ip: String) {
        _state.value = _state.value.copy(serverIp = ip, testResult = null, isSaved = false)
    }

    fun updatePort(port: String) {
        _state.value = _state.value.copy(serverPort = port, testResult = null, isSaved = false)
    }

    fun testConnection() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _state.value = _state.value.copy(isTesting = true, testResult = null)

            val ip = _state.value.serverIp.trim()
            val portStr = _state.value.serverPort.trim()
            val port = portStr.toIntOrNull()

            if (ip.isBlank() || port == null || port !in 1..65535) {
                _state.value = _state.value.copy(
                    isTesting = false,
                    testResult = TestResult.Error("IP หรือ Port ไม่ถูกต้อง")
                )
                return@launch
            }

            try {
                val url = java.net.URL("http://$ip:$port/api/health")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                connection.disconnect()

                if (responseCode == 200) {
                    _state.value = _state.value.copy(
                        isTesting = false,
                        testResult = TestResult.Success("เชื่อมต่อสำเร็จ! ($ip:$port)")
                    )
                } else {
                    _state.value = _state.value.copy(
                        isTesting = false,
                        testResult = TestResult.Error("Server ตอบกลับ HTTP $responseCode")
                    )
                }
            } catch (e: java.net.ConnectException) {
                _state.value = _state.value.copy(
                    isTesting = false,
                    testResult = TestResult.Error("ไม่สามารถเชื่อมต่อ $ip:$portStr ได้\nตรวจสอบว่า Server รันอยู่และ IP ถูกต้อง")
                )
            } catch (e: java.net.SocketTimeoutException) {
                _state.value = _state.value.copy(
                    isTesting = false,
                    testResult = TestResult.Error("หมดเวลาเชื่อมต่อ (Timeout)\nตรวจสอบว่ามือถือและ PC อยู่ WiFi เดียวกัน")
                )
            } catch (e: java.io.IOException) {
                _state.value = _state.value.copy(
                    isTesting = false,
                    testResult = TestResult.Error("ไม่สามารถเชื่อมต่อได้\nตรวจสอบ IP และ Port อีกครั้ง")
                )
            } catch (e: Exception) {
                val errorName = e.javaClass.simpleName
                val errorMsg = e.message ?: "ไม่ทราบสาเหตุ"
                _state.value = _state.value.copy(
                    isTesting = false,
                    testResult = TestResult.Error("$errorName: $errorMsg")
                )
            }
        }
    }

    fun resetToDefault() {
        _state.value = _state.value.copy(
            serverIp = ServerConfigManager.DEFAULT_IP,
            serverPort = ServerConfigManager.DEFAULT_PORT.toString(),
            testResult = null,
            isSaved = false,
        )
    }

    fun saveConfig() {
        viewModelScope.launch {
            val ip = _state.value.serverIp.trim()
            val port = _state.value.serverPort.trim().toIntOrNull() ?: ServerConfigManager.DEFAULT_PORT
            serverConfigManager.saveConfig(ip, port)
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
