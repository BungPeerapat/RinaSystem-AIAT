package com.example.rinasystem.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rinasystem.BuildConfig
import com.example.rinasystem.ui.components.AriaButton
import com.example.rinasystem.ui.components.AriaTextField
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaGreen
import com.example.rinasystem.ui.theme.AriaRed
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.components.UpdateDialog
import com.example.rinasystem.ui.viewmodel.SettingsViewModel
import com.example.rinasystem.ui.viewmodel.TestResult
import com.example.rinasystem.ui.viewmodel.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val updateState by updateViewModel.state.collectAsState()
    val context = LocalContext.current
    val installedVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "ARIA v${pInfo.versionName} (build ${pInfo.longVersionCode})"
        } catch (_: Exception) {
            "ARIA v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ตั้งค่าเซิร์ฟเวอร์",
                        color = AriaCyan,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "กลับ",
                            tint = AriaCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriaBgDark)
            )
        },
        containerColor = AriaBgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Info
            GlowCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = AriaCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "การเชื่อมต่อ Backend",
                            color = AriaTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "กรอก IP ของเครื่องที่รัน ARIA Server",
                            color = AriaTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // IP Input
            GlowCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Server IP Address",
                        color = AriaCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    AriaTextField(
                        value = state.serverIp,
                        onValueChange = { viewModel.updateIp(it) },
                        label = "IP Address (เช่น 192.168.1.37)",
                        keyboardType = KeyboardType.Uri,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Server Port",
                        color = AriaCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    AriaTextField(
                        value = state.serverPort,
                        onValueChange = { viewModel.updatePort(it) },
                        label = "Port (เช่น 8000)",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Current URL preview
                    Text(
                        text = "URL: http://${state.serverIp.ifBlank { "..." }}:${state.serverPort.ifBlank { "..." }}",
                        color = AriaTextMuted,
                        fontSize = 12.sp
                    )

                    // Reset to default button
                    AriaButton(
                        text = "ค่าเริ่มต้น",
                        onClick = { viewModel.resetToDefault() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Connection Button
            AriaButton(
                text = if (state.isTesting) "กำลังทดสอบ..." else "ทดสอบการเชื่อมต่อ",
                isLoading = state.isTesting,
                onClick = { viewModel.testConnection() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Test Result
            if (state.testResult != null) {
                GlowCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (val result = state.testResult) {
                            is TestResult.Success -> {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = AriaGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = result.message,
                                    color = AriaGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is TestResult.Error -> {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = AriaRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = result.message,
                                    color = AriaRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            null -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            AriaButton(
                text = if (state.isSaved) "บันทึกแล้ว!" else "บันทึกการตั้งค่า",
                onClick = { viewModel.saveConfig() }
            )

            if (state.isSaved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "การตั้งค่าจะมีผลทันที ไม่ต้องรีสตาร์ทแอป",
                    color = AriaGreen,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Check Update Button
            AriaButton(
                text = when {
                    updateState.isChecking -> "กำลังตรวจสอบ..."
                    updateState.updateAvailable && !updateState.dismissed -> "พบอัพเดต v${updateState.versionInfo?.versionName}"
                    else -> "ตรวจสอบอัพเดต"
                },
                isLoading = updateState.isChecking,
                onClick = { updateViewModel.checkForUpdate() },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Update Dialog
            if (updateState.updateAvailable && !updateState.dismissed) {
                UpdateDialog(
                    state = updateState,
                    onUpdate = { updateViewModel.downloadAndInstall() },
                    onDismiss = { updateViewModel.dismissUpdate() },
                )
            }

            // App Version
            Text(
                text = installedVersion,
                color = AriaTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
