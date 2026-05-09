package com.example.rinasystem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rinasystem.data.model.AppVersionResponse
import com.example.rinasystem.ui.theme.AriaBgCard
import com.example.rinasystem.ui.theme.AriaBorder
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaCyanDark
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.viewmodel.UpdateState

@Composable
fun UpdateDialog(
    state: UpdateState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val versionInfo = state.versionInfo ?: return

    Dialog(
        onDismissRequest = { if (!versionInfo.forceUpdate && !state.isDownloading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !versionInfo.forceUpdate && !state.isDownloading,
            dismissOnClickOutside = !versionInfo.forceUpdate && !state.isDownloading,
        ),
    ) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AriaBgCard, shape)
                .border(1.dp, AriaBorder, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "มีอัพเดตใหม่",
                color = AriaCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "เวอร์ชัน ${versionInfo.versionName}",
                color = AriaTextPrimary,
                fontSize = 15.sp,
            )

            if (versionInfo.releaseNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = versionInfo.releaseNotes,
                    color = AriaTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error,
                    color = androidx.compose.ui.graphics.Color(0xFFFF9100),
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    onClick = onUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AriaCyan),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = "อัพเดตเลย",
                        color = AriaBgCard,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (!versionInfo.forceUpdate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "ภายหลัง",
                            color = AriaTextMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
