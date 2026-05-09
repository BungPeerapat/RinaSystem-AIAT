package com.example.rinasystem.ui.screens.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rinasystem.data.model.UserResponse
import com.example.rinasystem.ui.components.AriaTextField
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.components.UserStatusBadge
import com.example.rinasystem.ui.theme.AriaBorder
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaGreen
import com.example.rinasystem.ui.theme.AriaRed
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary

@Composable
fun ProfileScreen(
    user: UserResponse?,
    onLogout: () -> Unit,
    onUpdateProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(user?.displayName) { mutableStateOf(user?.displayName ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Card
        GlowCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = AriaCyan,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isEditing) {
                    AriaTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = "ชื่อที่แสดง",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        IconButton(onClick = {
                            if (editName.isNotBlank()) {
                                onUpdateProfile(editName.trim())
                                isEditing = false
                            }
                        }) {
                            Icon(Icons.Filled.Check, "บันทึก", tint = AriaGreen)
                        }
                        IconButton(onClick = {
                            editName = user?.displayName ?: ""
                            isEditing = false
                        }) {
                            Icon(Icons.Filled.Close, "ยกเลิก", tint = AriaRed)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user?.displayName ?: "User",
                            color = AriaTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "แก้ไขชื่อ",
                            tint = AriaCyan,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { isEditing = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                UserStatusBadge(status = user?.status ?: "active")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        GlowCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ข้อมูลบัญชี",
                    color = AriaCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileInfoRow(
                    icon = Icons.Filled.Email,
                    label = "อีเมล",
                    value = user?.email ?: "-"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = AriaBorder
                )

                ProfileInfoRow(
                    icon = Icons.Filled.Shield,
                    label = "บทบาท",
                    value = when (user?.role) {
                        "admin" -> "ผู้ดูแลระบบ"
                        else -> "ผู้ใช้งาน"
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = AriaBorder
                )

                ProfileInfoRow(
                    icon = Icons.Filled.AccountCircle,
                    label = "สมัครเมื่อ",
                    value = user?.createdAt?.take(10) ?: "-"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AriaRed),
            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(AriaRed.copy(alpha = 0.5f))
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ออกจากระบบ",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AriaCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, color = AriaTextMuted, fontSize = 12.sp)
            Text(text = value, color = AriaTextPrimary, fontSize = 14.sp)
        }
    }
}
