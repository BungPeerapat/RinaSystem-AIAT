package com.example.rinasystem.ui.screens.update

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rinasystem.ui.theme.AriaBgCard
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaBorder
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaGreen
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.viewmodel.UpdateViewModel
import kotlinx.coroutines.delay

@Composable
fun UpdateCheckScreen(
    onContinue: () -> Unit,
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by updateViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate()
    }

    // Auto-continue if no update available after check
    LaunchedEffect(state.isChecking, state.hasChecked, state.updateAvailable) {
        if (!state.hasChecked) {
            // Check not completed yet, wait
            return@LaunchedEffect
        }
        if (!state.updateAvailable && state.error == null) {
            delay(1500) // Show "up to date" briefly
            onContinue()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AriaBgDark),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .background(AriaBgCard, shape)
                .border(1.dp, AriaBorder, shape)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                // Checking for update
                state.isChecking -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "iconPulse",
                    )

                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = AriaCyan,
                        modifier = Modifier
                            .size(64.dp)
                            .alpha(alpha),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "กำลังตรวจสอบอัพเดต...",
                        color = AriaTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CircularProgressIndicator(
                        color = AriaCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Update available
                state.updateAvailable && state.versionInfo != null -> {
                    val versionInfo = state.versionInfo!!

                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = AriaCyan,
                        modifier = Modifier.size(64.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "มีอัพเดตใหม่",
                        color = AriaCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "เวอร์ชัน ${versionInfo.versionName}",
                        color = AriaTextPrimary,
                        fontSize = 16.sp,
                    )

                    if (versionInfo.releaseNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = versionInfo.releaseNotes,
                            color = AriaTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error!!,
                            color = androidx.compose.ui.graphics.Color(0xFFFF9100),
                            fontSize = 12.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isDownloading) {
                        Text(
                            text = "กำลังดาวน์โหลด... ${state.downloadProgress}%",
                            color = AriaTextMuted,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = AriaCyan,
                            trackColor = AriaBorder,
                        )
                    } else {
                        Button(
                            onClick = { updateViewModel.downloadAndInstall() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AriaCyan),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "อัพเดตเลย",
                                color = AriaBgCard,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }

                        if (!versionInfo.forceUpdate) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = onContinue) {
                                Text(
                                    text = "ข้ามไปก่อน",
                                    color = AriaTextMuted,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                // Error checking
                state.error != null -> {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = AriaTextMuted,
                        modifier = Modifier.size(64.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ไม่สามารถตรวจสอบอัพเดตได้",
                        color = AriaTextSecondary,
                        fontSize = 16.sp,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.error!!,
                        color = AriaTextMuted,
                        fontSize = 12.sp,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TextButton(onClick = onContinue) {
                        Text(
                            text = "ดำเนินการต่อ",
                            color = AriaCyan,
                            fontSize = 14.sp,
                        )
                    }
                }

                // No update — up to date
                else -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AriaGreen,
                        modifier = Modifier.size(64.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "แอปเป็นเวอร์ชันล่าสุดแล้ว",
                        color = AriaGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "กำลังเข้าสู่ระบบ...",
                        color = AriaTextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
