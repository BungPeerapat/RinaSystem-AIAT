package com.example.rinasystem.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.rinasystem.data.model.CharacterItem
import com.example.rinasystem.service.StreamingService
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.components.HudPanel
import com.example.rinasystem.ui.components.StatusIndicator
import com.example.rinasystem.ui.screens.messages.UserMessagesScreen
import com.example.rinasystem.ui.screens.profile.ProfileScreen
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaBgSurface
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaGreen
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.IconButton
import com.example.rinasystem.service.AriaLogBuffer
import androidx.compose.material.icons.filled.Refresh
import com.example.rinasystem.service.ForceUpdateEvent
import com.example.rinasystem.ui.components.UpdateDialog
import com.example.rinasystem.ui.viewmodel.AuthViewModel
import com.example.rinasystem.ui.viewmodel.UpdateViewModel
import com.example.rinasystem.ui.viewmodel.UserViewModel

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("หน้าหลัก", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("ข้อความ", Icons.AutoMirrored.Filled.Message, Icons.AutoMirrored.Outlined.Message),
    BottomNavItem("โปรไฟล์", Icons.Filled.Person, Icons.Outlined.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCharacterSheet by remember { mutableStateOf(false) }
    val authState by authViewModel.state.collectAsState()
    val userState by userViewModel.state.collectAsState()
    val updateState by updateViewModel.state.collectAsState()
    val context = LocalContext.current

    // Request mic + camera permissions then start service
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        StreamingService.start(context)
    }

    LaunchedEffect(Unit) {
        userViewModel.loadProfile()
        userViewModel.loadCharacters()
        val permList = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permList.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(permList.toTypedArray())

        // Reload profile หลัง WebSocket น่าจะต่อเสร็จแล้ว เพื่ออัพเดตสถานะ online
        kotlinx.coroutines.delay(3000)
        userViewModel.loadProfile()
    }


    // รับ ForceUpdateEvent จาก Admin ผ่าน WebSocket
    LaunchedEffect(Unit) {
        ForceUpdateEvent.event.collect {
            updateViewModel.checkForUpdate()
        }
    }

    // Update Dialog (เมื่อ Admin สั่ง Force Update)
    if (updateState.updateAvailable && !updateState.dismissed) {
        UpdateDialog(
            state = updateState,
            onUpdate = { updateViewModel.downloadAndInstall() },
            onDismiss = { updateViewModel.dismissUpdate() },
        )
    }

    val user = userState.user ?: authState.user

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "ARIA Dashboard"
                            1 -> "ข้อความเสียง"
                            else -> "โปรไฟล์"
                        },
                        color = AriaCyan,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriaBgDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AriaBgSurface) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AriaCyan,
                            selectedTextColor = AriaCyan,
                            unselectedIconColor = AriaTextMuted,
                            unselectedTextColor = AriaTextMuted,
                            indicatorColor = AriaCyan.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (userState.characters.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCharacterSheet = true },
                    containerColor = AriaCyan,
                    contentColor = AriaBgDark,
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = "กระตุ้น Character")
                }
            }
        },
        containerColor = AriaBgDark
    ) { innerPadding ->
        // Character Trigger Bottom Sheet
        if (showCharacterSheet) {
            CharacterTriggerSheet(
                characters = userState.characters,
                isLoading = userState.isLoadingCharacters,
                triggerSuccess = userState.triggerSuccess,
                onTrigger = { char ->
                    userViewModel.triggerCharacter(char)
                    showCharacterSheet = false
                },
                onDismiss = { showCharacterSheet = false },
            )
        }
        when (selectedTab) {
            0 -> UserHomeTab(
                userName = user?.displayName ?: "User",
                isOnline = user?.isOnline ?: false,
                onRefresh = { userViewModel.loadProfile() },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> UserMessagesScreen(modifier = Modifier.padding(innerPadding))
            2 -> ProfileScreen(
                user = user,
                onLogout = {
                    authViewModel.logout()
                    onLogout()
                },
                onUpdateProfile = { displayName ->
                    authViewModel.updateProfile(displayName)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun UserHomeTab(
    userName: String,
    isOnline: Boolean,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Welcome card
        GlowCard {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "สวัสดี, $userName",
                        color = AriaTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusIndicator(isOnline = isOnline)
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "รีเฟรชสถานะ",
                        tint = AriaCyan,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Panel
        HudPanel {
            Text(
                text = "สถานะระบบ",
                color = AriaCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusCard(title = "การเชื่อมต่อ", value = "เชื่อมต่อแล้ว", isActive = true)
                StatusCard(title = "ไมโครโฟน", value = "พร้อมใช้งาน", isActive = true)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusCard(title = "กล้อง", value = "พร้อมใช้งาน", isActive = true)
                StatusCard(title = "TTS", value = "พร้อมรับข้อความ", isActive = true)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info card
        GlowCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ข้อมูลระบบ",
                    color = AriaCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ระบบพร้อมแล้ว",
                    color = AriaTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "กำลังรอรับข้อมูล",
                    color = AriaTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debug Log Panel
        DebugLogPanel()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DebugLogPanel() {
    val logLines by AriaLogBuffer.lines.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(logLines.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    HudPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Debug Log",
                color = AriaCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${logLines.size} lines",
                    color = AriaTextMuted,
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ARIA Log", logLines.joinToString("\n")))
                        Toast.makeText(context, "Copied log", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy Log",
                        tint = AriaCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(AriaBgDark, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (logLines.isEmpty()) {
                    Text(
                        text = "ยังไม่มี log...",
                        color = AriaTextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    for (line in logLines) {
                        Text(
                            text = line,
                            color = if ("ERROR" in line) com.example.rinasystem.ui.theme.AriaRed
                                    else AriaTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterTriggerSheet(
    characters: List<CharacterItem>,
    isLoading: Boolean,
    triggerSuccess: Boolean,
    onTrigger: (CharacterItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.example.rinasystem.ui.theme.AriaBgSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "กระตุ้น Character",
                color = AriaCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "เลือก Character ที่ต้องการแจ้งเตือน Admin",
                color = AriaTextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AriaCyan, modifier = Modifier.size(32.dp))
                }
            } else if (characters.isEmpty()) {
                Text("ยังไม่มี Character — Admin ยังไม่ได้ตั้งค่า", color = AriaTextMuted, fontSize = 13.sp)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(characters) { char ->
                        Button(
                            onClick = { onTrigger(char) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AriaCyan.copy(alpha = 0.15f),
                                contentColor = AriaCyan,
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(char.emoji, fontSize = 24.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    char.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            if (triggerSuccess) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "✅ ส่งการแจ้งเตือนแล้ว!",
                    color = AriaGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    isActive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (isActive) com.example.rinasystem.ui.theme.AriaGreen
                    else AriaTextMuted,
                    androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, color = AriaTextSecondary, fontSize = 11.sp)
        Text(text = value, color = AriaTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
