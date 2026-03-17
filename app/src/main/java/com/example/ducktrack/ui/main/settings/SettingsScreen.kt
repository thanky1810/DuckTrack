package com.example.ducktrack.ui.main.settings

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.R
import com.example.ducktrack.ui.approot.AuthViewModelFactory
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.UserInfo
import com.example.ducktrack.ui.theme.AppColors
import com.example.ducktrack.utils.PermissionHelper

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToExportHistory: () -> Unit,

    // Dùng hàm findActivity() an toàn thay vì ép kiểu
    settingsViewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current.findActivity()
            ?: throw IllegalStateException("Không tìm thấy Activity")
    ),

    onNavigateToAbout: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val context = LocalContext.current
    val primaryColor = AppColors.TextGreen

    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var linkedProviders by remember { mutableStateOf(authViewModel.getLinkedProviders()) }

    val isVibration by settingsViewModel.isVibration.collectAsState()
    val isKeepScreenOn by settingsViewModel.isKeepScreenOn.collectAsState()
    val isChristmas by settingsViewModel.isChristmasTheme.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val selectedTitle = remember(userInfo?.selectedAchievementId) {
        val id = userInfo?.selectedAchievementId
        if (id != null) {
            AchievementList.list.find { it.id == id }?.title
        } else null
    }

    LaunchedEffect(Unit) {
        authViewModel.loadUserInfo { info -> userInfo = info }
        linkedProviders = authViewModel.getLinkedProviders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Cài đặt", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))

        // 1. THẺ USER
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToProfile() },
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                    val base64Str = userInfo?.photoBase64
                    val bitmap = remember(base64Str) { try { val b = Base64.decode(base64Str, Base64.DEFAULT); BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap() } catch (e: Exception) { null } }
                    if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, primaryColor, CircleShape), contentScale = ContentScale.Crop)
                    else Image(painter = painterResource(R.drawable.duck_waiting), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, primaryColor, CircleShape), contentScale = ContentScale.Crop)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (selectedTitle != null) {
                        Surface(
                            color = Color(0xFFFFF9C4),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBC02D)),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFF57F17), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(text = selectedTitle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                            }
                        }
                    }
                    Text(text = userInfo?.name ?: "Đang tải...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "Xem hồ sơ chi tiết & thống kê", fontSize = 12.sp, color = primaryColor)
                }
                Icon(Icons.Default.ChevronRight, null, tint = primaryColor)
            }
        }

        // 2. TRẢI NGHIỆM
        Text("Trải nghiệm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(top = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {

                // NÚT THEME GIÁNG SINH
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Chế độ Giáng sinh 🎄", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.Black)
                        }
                        Text("Giao diện đỏ, nhạc vui nhộn & tuyết rơi", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isChristmas,
                        onCheckedChange = { settingsViewModel.toggleChristmasTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFD32F2F) // Màu đỏ khi bật
                        )
                    )
                }

                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                SettingSwitchRow("Rung khi hết giờ", "Rung điện thoại khi hoàn thành phiên", isVibration) { settingsViewModel.setVibration(it) }
                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                SettingSwitchRow("Giữ màn hình sáng", "Không tắt màn hình khi đang tập trung", isKeepScreenOn) { settingsViewModel.setKeepScreenOn(it) }
            }
        }

        // 3. DỮ LIỆU & ỨNG DỤNG
        Text("Dữ liệu & Ứng dụng", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(top = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {

                // [CHÈN THÊM ĐOẠN NÀY] - Nút mở màn hình AI Chat
                SettingActionRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Trợ lý AI - Giáo Sư Vịt",
                    subtitle = "Phân tích thói quen và trò chuyện"
                ) { onNavigateToAiChat() }

                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                SettingActionRow(
                    icon = Icons.Default.Download,
                    title = "Xuất dữ liệu & Lịch sử",
                    subtitle = "Tải về và quản lý các file báo cáo CSV"
                ) { onNavigateToExportHistory() }

                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                SettingActionRow(
                    icon = Icons.Default.EmojiEvents,
                    title = "Thành tựu",
                    subtitle = "Xem danh hiệu bạn đã đạt được"
                ) { onNavigateToAchievements() }

                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                SettingActionRow(
                    icon = Icons.Default.Info,
                    title = "Về chúng tôi",
                    subtitle = "Thông tin nhóm phát triển"
                ) { onNavigateToAbout() }
            }
        }

        // 4. LIÊN KẾT TÀI KHOẢN
        Text("Liên kết tài khoản", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(top = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                AccountLinkRow(R.drawable.ic_google, "Google", linkedProviders.contains("google.com"),
                    onLink = { Toast.makeText(context, "Đăng nhập Google trước", Toast.LENGTH_SHORT).show() },
                    onUnlink = { authViewModel.unlinkProvider("google.com", { linkedProviders = authViewModel.getLinkedProviders() }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                AccountLinkRow(R.drawable.ic_github, "GitHub", linkedProviders.contains("github.com"),
                    onLink = { val activity = context.findActivity(); if(activity!=null) authViewModel.linkWithGithub(activity, { linkedProviders = authViewModel.getLinkedProviders() }, {Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}) },
                    onUnlink = { authViewModel.unlinkProvider("github.com", { linkedProviders = authViewModel.getLinkedProviders() }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
            }
        }

        Divider(color = Color(0xFFEEEEEE))
        MonitoringControlSection(settingsViewModel)
        Divider(color = Color(0xFFEEEEEE))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.ExitToApp, null); Spacer(Modifier.width(8.dp)); Text("Đăng xuất", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Xóa tài khoản vĩnh viễn", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa tài khoản?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = { Text("Hành động này sẽ xóa vĩnh viễn toàn bộ dữ liệu (Nhiệm vụ, Cây trồng, Thành tựu) của bạn trên máy chủ. Bạn không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount(
                            onSuccess = {
                                Toast.makeText(context, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show()
                                onLogout()
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Xóa vĩnh viễn", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// --- CÁC HÀM COMPOSABLE BỊ THIẾU Ở BƯỚC TRƯỚC ---

@Composable
fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.Black)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.ButtonGreen))
    }
}

@Composable
fun SettingActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Icon(icon, null, tint = AppColors.TextGreen, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.Black)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}

@Composable
fun AccountLinkRow(iconRes: Int, providerName: String, isLinked: Boolean, onLink: () -> Unit, onUnlink: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = providerName, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = Color.Black)
        if (isLinked) TextButton(onClick = onUnlink) { Text("Đã kết nối", color = AppColors.TextGreen, fontWeight = FontWeight.Bold) }
        else Button(onClick = onLink, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(36.dp)) { Text("Kết nối", fontSize = 12.sp) }
    }
}

@Composable
private fun MonitoringControlSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val isMonitoring by viewModel.isMonitoringEnabled.collectAsState()
    var hasOverlayPermission by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    LaunchedEffect(Unit) { hasOverlayPermission = PermissionHelper.hasOverlayPermission(context) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Giám sát thời gian sử dụng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.Black)
            if (!hasOverlayPermission) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ Cần quyền hiển thị overlay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)); Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    PermissionHelper.requestOverlayPermission(activity)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) { Text("Cấp quyền ngay", fontSize = 12.sp) }
                    }
                }
            }
            SettingSwitchRow("Kích hoạt giám sát", if (isMonitoring) "Đang hoạt động" else "Tắt", isMonitoring) { viewModel.setMonitoringEnabled(it) }
        }
    }
}

// Hàm Extension để tìm Activity từ Context an toàn
fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}