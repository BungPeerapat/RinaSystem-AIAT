package com.example.rinasystem.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rinasystem.data.model.TtsMessageItem
import com.example.rinasystem.ui.theme.AriaBgCard
import com.example.rinasystem.ui.theme.AriaBorder
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaOrange
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.viewmodel.MessageViewModel

@Composable
fun UserMessagesScreen(
    modifier: Modifier = Modifier,
    messageViewModel: MessageViewModel = hiltViewModel(),
) {
    val state by messageViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        messageViewModel.loadMessages()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ข้อความเสียง TTS",
                color = AriaCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = AriaCyan,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            IconButton(
                onClick = { messageViewModel.loadMessages() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "รีเฟรช",
                    tint = AriaCyan,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = AriaOrange,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        if (state.messages.isEmpty() && !state.isLoading) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = AriaCyan.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ยังไม่มีข้อความเสียง",
                        color = AriaTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ข้อความ TTS จาก Admin จะแสดงที่นี่",
                        color = AriaTextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageCard(message)
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: TtsMessageItem) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AriaBgCard, shape)
            .border(1.dp, AriaBorder, shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (message.isBroadcast) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = "Broadcast",
                    tint = AriaOrange,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("ส่งถึงทุกคน", color = AriaOrange, fontSize = 11.sp)
            } else {
                Text("ส่งถึงคุณ", color = AriaCyan, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTimestamp(message.createdAt),
                color = AriaTextMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message.content,
            color = AriaTextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

private fun formatTimestamp(isoString: String): String {
    return try {
        // ISO 8601: "2026-03-13T10:30:00+00:00" → "13/03 10:30"
        val parts = isoString.split("T")
        if (parts.size < 2) return isoString
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")
        if (dateParts.size >= 3 && timeParts.size >= 2) {
            "${dateParts[2]}/${dateParts[1]} ${timeParts[0]}:${timeParts[1]}"
        } else {
            isoString
        }
    } catch (_: Exception) {
        isoString
    }
}
