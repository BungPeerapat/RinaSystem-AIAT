package com.example.rinasystem.ui.screens.admin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.rinasystem.data.model.StreamSessionResponse
import com.example.rinasystem.data.model.UserResponse
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.components.StatusIndicator
import com.example.rinasystem.ui.components.UserStatusBadge
import com.example.rinasystem.ui.theme.AriaBgCard
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaBorder
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaGreen
import com.example.rinasystem.ui.theme.AriaRed
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.theme.AriaYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    user: UserResponse,
    streams: List<StreamSessionResponse>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onStatusChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("รายละเอียดผู้ใช้", color = AriaCyan, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "กลับ", tint = AriaCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriaBgDark)
            )
        },
        containerColor = AriaBgDark,
        modifier = modifier
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AriaCyan)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Header
            GlowCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = AriaCyan,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user.displayName,
                        color = AriaTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = user.email, color = AriaTextMuted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserStatusBadge(status = user.status)
                        StatusIndicator(isOnline = user.isOnline, dotSize = 8.dp)
                        Text(
                            text = if (user.isOnline) "ออนไลน์" else "ออฟไลน์",
                            color = if (user.isOnline) AriaGreen else AriaTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            GlowCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ข้อมูลบัญชี", color = AriaCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(label = "ID", value = user.id)
                    HorizontalDivider(color = AriaBorder, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "อีเมล", value = user.email)
                    HorizontalDivider(color = AriaBorder, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "สมัครเมื่อ", value = user.createdAt?.take(10) ?: "-")
                    HorizontalDivider(color = AriaBorder, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "อัปเดตล่าสุด", value = user.updatedAt?.take(10) ?: "-")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Role Management
            GlowCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("บทบาท", color = AriaCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    var roleExpanded by remember { mutableStateOf(false) }
                    val currentRole = user.role

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AriaBgCard, RoundedCornerShape(8.dp))
                                .clickable { roleExpanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentRole == "admin") "Admin (ผู้ดูแล)" else "User (ผู้ใช้)",
                                color = AriaTextPrimary,
                                fontSize = 14.sp
                            )
                            Text("เปลี่ยน", color = AriaCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        DropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Admin (ผู้ดูแล)") },
                                onClick = {
                                    roleExpanded = false
                                    if (currentRole != "admin") onRoleChange("admin")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("User (ผู้ใช้)") },
                                onClick = {
                                    roleExpanded = false
                                    if (currentRole != "user") onRoleChange("user")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Actions
            GlowCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("จัดการสถานะ", color = AriaCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (user.status != "active") {
                            ActionChip(
                                text = "เปิดใช้งาน",
                                icon = Icons.Filled.CheckCircle,
                                color = AriaGreen,
                                onClick = { onStatusChange("active") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (user.status != "suspended") {
                            ActionChip(
                                text = "ระงับ",
                                icon = Icons.Filled.Pause,
                                color = AriaYellow,
                                onClick = { onStatusChange("suspended") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (user.status != "blocked") {
                            ActionChip(
                                text = "บล็อก",
                                icon = Icons.Filled.Block,
                                color = AriaRed,
                                onClick = { onStatusChange("blocked") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stream History
            GlowCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "ประวัติ Stream (${streams.size})",
                        color = AriaCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (streams.isEmpty()) {
                        Text("ไม่มีประวัติ Stream", color = AriaTextMuted, fontSize = 14.sp)
                    } else {
                        streams.take(20).forEach { session ->
                            StreamSessionRow(session)
                            if (session != streams.last()) {
                                HorizontalDivider(color = AriaBorder, modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AriaTextMuted, fontSize = 13.sp)
        Text(text = value, color = AriaTextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun ActionChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StreamSessionRow(session: StreamSessionResponse) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = session.streamType.uppercase(),
                color = AriaCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = session.startedAt.take(16).replace("T", " "),
                color = AriaTextMuted,
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (session.isActive) {
                Text("LIVE", color = AriaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = session.endedAt?.take(16)?.replace("T", " ") ?: "-",
                    color = AriaTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
