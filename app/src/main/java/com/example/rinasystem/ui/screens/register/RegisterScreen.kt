package com.example.rinasystem.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rinasystem.ui.components.AriaButton
import com.example.rinasystem.ui.components.AriaTextField
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaRed
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: (role: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isLoggedIn, state.user) {
        if (state.isLoggedIn && state.user != null) {
            onRegisterSuccess(state.user!!.role)
        }
    }

    val errorMessage = localError ?: state.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AriaBgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "ARIA",
            color = AriaCyan,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp
        )
        Text(
            text = "AI Interection",
            color = AriaTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        GlowCard {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "สมัครสมาชิก",
                    color = AriaCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                AriaTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        localError = null
                        viewModel.clearError()
                    },
                    label = "ชื่อที่แสดง",
                    modifier = Modifier.fillMaxWidth()
                )

                AriaTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null
                        viewModel.clearError()
                    },
                    label = "อีเมล",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth()
                )

                AriaTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                        viewModel.clearError()
                    },
                    label = "รหัสผ่าน",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AriaTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                        viewModel.clearError()
                    },
                    label = "ยืนยันรหัสผ่าน",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = AriaRed,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AriaButton(
                    text = "สมัครสมาชิก",
                    isLoading = state.isLoading,
                    onClick = {
                        when {
                            displayName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                                localError = "กรุณากรอกข้อมูลให้ครบ"
                            !email.contains("@") ->
                                localError = "รูปแบบอีเมลไม่ถูกต้อง"
                            password.length < 6 ->
                                localError = "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"
                            password != confirmPassword ->
                                localError = "รหัสผ่านไม่ตรงกัน"
                            else -> viewModel.register(email.trim(), password, displayName.trim())
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(
                text = "มีบัญชีแล้ว? เข้าสู่ระบบ",
                color = AriaCyan,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
