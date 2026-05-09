package com.example.rinasystem.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rinasystem.data.model.UserResponse
import com.example.rinasystem.ui.components.GlowCard
import com.example.rinasystem.ui.components.HudPanel
import com.example.rinasystem.ui.components.StatusIndicator
import com.example.rinasystem.ui.components.UserStatusBadge
import com.example.rinasystem.ui.screens.profile.ProfileScreen
import com.example.rinasystem.ui.theme.AriaBgCard
import com.example.rinasystem.ui.theme.AriaBgDark
import com.example.rinasystem.ui.theme.AriaBgSurface
import com.example.rinasystem.ui.theme.AriaBorder
import com.example.rinasystem.ui.theme.AriaCyan
import com.example.rinasystem.ui.theme.AriaGreen
import com.example.rinasystem.ui.theme.AriaRed
import com.example.rinasystem.ui.theme.AriaTextMuted
import com.example.rinasystem.ui.theme.AriaTextPrimary
import com.example.rinasystem.ui.theme.AriaTextSecondary
import com.example.rinasystem.ui.theme.AriaYellow
import com.example.rinasystem.ui.viewmodel.AdminViewModel
import com.example.rinasystem.ui.viewmodel.AuthViewModel
import com.example.rinasystem.ui.viewmodel.StreamViewModel
import com.example.rinasystem.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.outlined.Stream

private data class DrawerItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val drawerItems = listOf(
    DrawerItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    DrawerItem("ผู้ใช้งาน", Icons.Filled.People, Icons.Outlined.People),
    DrawerItem("Streaming", Icons.Filled.Stream, Icons.Outlined.Stream),
    DrawerItem("โปรไฟล์", Icons.Filled.Person, Icons.Outlined.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    adminViewModel: AdminViewModel = hiltViewModel(),
    streamViewModel: StreamViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val authState by authViewModel.state.collectAsState()
    val adminState by adminViewModel.state.collectAsState()
    var showDetailUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        adminViewModel.loadDashboard()
        adminViewModel.loadUsers()
    }

    // User Detail overlay
    if (showDetailUserId != null) {
        val detailUser = adminState.selectedUser
        if (detailUser != null) {
            UserDetailScreen(
                user = detailUser,
                streams = adminState.selectedUserStreams,
                isLoading = adminState.isDetailLoading,
                onBack = {
                    showDetailUserId = null
                    adminViewModel.clearSelectedUser()
                },
                onStatusChange = { status ->
                    adminViewModel.updateUserStatus(detailUser.id, status)
                    adminViewModel.loadUserDetail(detailUser.id)
                },
                onRoleChange = { role ->
                    adminViewModel.updateUserRole(detailUser.id, role)
                }
            )
            return
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AriaBgSurface,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = AriaCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("ARIA Admin", color = AriaCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(authState.user?.email ?: "", color = AriaTextMuted, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = AriaBorder, modifier = Modifier.padding(vertical = 8.dp))

                drawerItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(item.label, color = AriaTextPrimary) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (selectedTab == index) AriaCyan else AriaTextMuted
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = AriaCyan.copy(alpha = 0.1f),
                            unselectedContainerColor = AriaBgSurface
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logout
                NavigationDrawerItem(
                    label = { Text("ออกจากระบบ", color = AriaRed) },
                    selected = false,
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = AriaRed
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = AriaBgSurface
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                0 -> "ARIA Admin"
                                1 -> "จัดการผู้ใช้งาน"
                                2 -> "Streaming Control"
                                else -> "โปรไฟล์"
                            },
                            color = AriaCyan,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Dashboard,
                                contentDescription = "เมนู",
                                tint = AriaCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AriaBgDark)
                )
            },
            containerColor = AriaBgDark
        ) { innerPadding ->
            when (selectedTab) {
                0 -> AdminHomeTab(
                    adminState = adminState,
                    onRefresh = {
                        adminViewModel.loadDashboard()
                        adminViewModel.loadUsers()
                    },
                    onForceUpdate = { streamViewModel.forceUpdateAllUsers() },
                    modifier = Modifier.padding(innerPadding)
                )
                1 -> AdminUsersTab(
                    adminState = adminState,
                    onStatusChange = { userId, status ->
                        adminViewModel.updateUserStatus(userId, status)
                    },
                    onUserClick = { userId ->
                        showDetailUserId = userId
                        adminViewModel.loadUserDetail(userId)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> StreamingControlScreen(
                    adminId = authState.user?.id ?: "",
                    modifier = Modifier.padding(innerPadding)
                )
                3 -> ProfileScreen(
                    user = authState.user,
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
}

@Composable
private fun AdminHomeTab(
    adminState: com.example.rinasystem.ui.viewmodel.AdminState,
    onRefresh: () -> Unit,
    onForceUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "ผู้ใช้ทั้งหมด",
                value = "${adminState.dashboard?.totalUsers ?: 0}",
                color = AriaCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "ออนไลน์",
                value = "${adminState.dashboard?.onlineCount ?: 0}",
                color = AriaGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Stream Sessions",
                value = "${adminState.dashboard?.totalStreams ?: 0}",
                color = AriaCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "TTS Messages",
                value = "${adminState.dashboard?.totalMessages ?: 0}",
                color = AriaCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Users
        HudPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ผู้ใช้งานล่าสุด",
                    color = AriaCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "รีเฟรชสถานะ",
                        tint = AriaCyan,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (adminState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AriaCyan, modifier = Modifier.size(32.dp))
                }
            } else {
                adminState.users.take(5).forEach { user ->
                    UserRow(user = user)
                    if (user != adminState.users.take(5).last()) {
                        HorizontalDivider(color = AriaBorder, modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Force Update Button
        GlowCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "จัดการอัพเดต",
                    color = AriaCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        onForceUpdate()
                        android.widget.Toast.makeText(context, "ส่งคำสั่งอัพเดตไปยัง User ทุกคนแล้ว", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AriaCyan,
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "สั่งอัพเดต User ทุกคน",
                        color = AriaBgDark,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    GlowCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = AriaTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun UserRow(user: UserResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AriaCyan.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.displayName.take(1).uppercase(),
                color = AriaCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, color = AriaTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = user.email, color = AriaTextMuted, fontSize = 12.sp)
        }
        UserStatusBadge(status = user.status)
    }
}

@Composable
private fun AdminUsersTab(
    adminState: com.example.rinasystem.ui.viewmodel.AdminState,
    onStatusChange: (String, String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (adminState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AriaCyan)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ทั้งหมด ${adminState.totalUsers} คน",
                    color = AriaTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "ออนไลน์ ${adminState.onlineCount} คน",
                    color = AriaGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(adminState.users) { user ->
            AdminUserCard(
                user = user,
                onStatusChange = { status -> onStatusChange(user.id, status) },
                onClick = { onUserClick(user.id) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AdminUserCard(
    user: UserResponse,
    onStatusChange: (String) -> Unit,
    onClick: () -> Unit
) {
    GlowCard(modifier = Modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AriaCyan.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        color = AriaCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName,
                        color = AriaTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = user.email, color = AriaTextMuted, fontSize = 13.sp)
                }
                StatusIndicator(isOnline = user.isOnline, dotSize = 8.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AriaBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("บทบาท", color = AriaTextMuted, fontSize = 11.sp)
                    Text(
                        text = if (user.role == "admin") "Admin" else "User",
                        color = AriaTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text("สถานะ", color = AriaTextMuted, fontSize = 11.sp)
                    UserStatusBadge(status = user.status)
                }

                // Quick actions
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (user.status == "active") {
                        Text(
                            text = "ระงับ",
                            color = AriaYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onStatusChange("suspended") }
                        )
                        Text(
                            text = "บล็อก",
                            color = AriaRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onStatusChange("blocked") }
                        )
                    } else if (user.status == "suspended") {
                        Text(
                            text = "เปิดใช้",
                            color = AriaGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onStatusChange("active") }
                        )
                        Text(
                            text = "บล็อก",
                            color = AriaRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onStatusChange("blocked") }
                        )
                    } else {
                        Text(
                            text = "เปิดใช้",
                            color = AriaGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onStatusChange("active") }
                        )
                    }
                }
            }
        }
    }
}
