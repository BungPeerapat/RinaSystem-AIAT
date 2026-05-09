package com.example.rinasystem.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rinasystem.ui.components.AriaButton
import com.example.rinasystem.ui.components.AriaTextField
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.theme.AriaBgCard
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaBgSurface
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaOrange
import com.example.rinasystem.ui.theme.AriaRed
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (role: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var showTestAccountSheet by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(state.isLoggedIn, state.user) {
        if (state.isLoggedIn && state.user != null) {
            onLoginSuccess(state.user!!.role)
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
                    text = "เข้าสู่ระบบ",
                    color = AriaCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
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

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = AriaRed,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AriaButton(
                    text = "เข้าสู่ระบบ",
                    isLoading = state.isLoading,
                    onClick = {
                        when {
                            email.isBlank() || password.isBlank() ->
                                localError = "กรุณากรอกข้อมูลให้ครบ"
                            !email.contains("@") ->
                                localError = "รูปแบบอีเมลไม่ถูกต้อง"
                            password.length < 6 ->
                                localError = "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"
                            else -> viewModel.login(email.trim(), password)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Account Button
        OutlinedButton(
            onClick = {
                showTestAccountSheet = true
                viewModel.loadTestAccounts()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SolidColor(AriaOrange)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AriaOrange)
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "เข้าสู่ระบบบัญชีทดสอบ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text(
                text = "ยังไม่มีบัญชี? สมัครสมาชิก",
                color = AriaCyan,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        IconButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "ตั้งค่าเซิร์ฟเวอร์",
                tint = AriaTextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "ตั้งค่าเซิร์ฟเวอร์",
            color = AriaTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Bottom Sheet — Test Account List
    if (showTestAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTestAccountSheet = false },
            sheetState = sheetState,
            containerColor = AriaBgCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "เลือกบัญชีทดสอบ",
                    color = AriaCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "กดเลือกบัญชีเพื่อเข้าสู่ระบบอัตโนมัติ",
                    color = AriaTextMuted,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    state.isLoadingTestAccounts -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AriaCyan,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    state.testAccounts.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ไม่พบบัญชีทดสอบ",
                                color = AriaTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        state.testAccounts.forEachIndexed { index, account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showTestAccountSheet = false
                                        viewModel.testLogin(account.id)
                                    }
                                    .background(AriaBgSurface, RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = if (account.role == "admin") AriaOrange else AriaCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.displayName,
                                        color = AriaTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = account.email,
                                        color = AriaTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = account.role.uppercase(),
                                    color = if (account.role == "admin") AriaOrange else AriaCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            if (account.role == "admin")
                                                AriaOrange.copy(alpha = 0.15f)
                                            else
                                                AriaCyan.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (index < state.testAccounts.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
