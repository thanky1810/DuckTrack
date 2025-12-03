// FILE: SettingsScreen.kt
package com.example.ducktrack.ui.main.settings

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
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
import com.example.ducktrack.ui.AppRoot.AuthViewModelFactory
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.UserInfo
import com.example.ducktrack.ui.theme.AppColors
import com.example.ducktrack.utils.PermissionHelper

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit, // Callback chuyển trang
    settingsViewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val context = LocalContext.current
    val primaryColor = AppColors.TextGreen

    // Data State
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var linkedProviders by remember { mutableStateOf(authViewModel.getLinkedProviders()) }

    // Setting Toggles State
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val isVibration by settingsViewModel.isVibration.collectAsState()
    val isKeepScreenOn by settingsViewModel.isKeepScreenOn.collectAsState()

    // Dialog State
    var showAboutUsDialog by remember { mutableStateOf(false) }

    // Load data
    LaunchedEffect(Unit) {
        authViewModel.loadUserInfo { info -> userInfo = info }
        linkedProviders = authViewModel.getLinkedProviders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Cài đặt", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        // --- 1. THẺ THÔNG TIN USER (BẤM VÀO ĐỂ XEM PROFILE) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToProfile() }, // Bấm vào đây chuyển trang
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar (Chỉ hiển thị)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                    val base64Str = userInfo?.photoBase64
                    val bitmap = remember(base64Str) {
                        try {
                            val b = Base64.decode(base64Str, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap()
                        } catch (e: Exception) { null }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, primaryColor, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.duck_waiting), contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, primaryColor, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userInfo?.name ?: "Đang tải...",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black
                    )
                    Text(
                        text = "Xem hồ sơ chi tiết & thống kê",
                        fontSize = 12.sp, color = AppColors.TextGreen
                    )
                }

                Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextGreen)
            }
        }

        // --- 2. GIAO DIỆN & TRẢI NGHIỆM ---
        Text("Giao diện & Trải nghiệm", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingSwitchRow("Chế độ Tối (Dark Mode)", "Giao diện tối màu bảo vệ mắt", isDarkMode) { settingsViewModel.setDarkMode(it) }
                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                SettingSwitchRow("Rung khi hết giờ", "Rung điện thoại khi hoàn thành phiên", isVibration) { settingsViewModel.setVibration(it) }
                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                SettingSwitchRow("Giữ màn hình sáng", "Không tắt màn hình khi đang tập trung", isKeepScreenOn) { settingsViewModel.setKeepScreenOn(it) }
            }
        }

        // --- 3. DỮ LIỆU & ỨNG DỤNG ---
        Text("Dữ liệu & Ứng dụng", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingActionRow(Icons.Default.Download, "Xuất dữ liệu (Export)", "Tải về lịch sử cây trồng & task") {
                    Toast.makeText(context, "Tính năng đang phát triển!", Toast.LENGTH_SHORT).show()
                }
                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                SettingActionRow(Icons.Default.Info, "Về chúng tôi", "Thông tin nhóm phát triển") {
                    showAboutUsDialog = true
                }
            }
        }

        // --- 4. LIÊN KẾT TÀI KHOẢN ---
        Text("Liên kết tài khoản", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                AccountLinkRow(R.drawable.ic_google, "Google", linkedProviders.contains("google.com"),
                    onLink = { Toast.makeText(context, "Đăng nhập Google trước", Toast.LENGTH_SHORT).show() },
                    onUnlink = { authViewModel.unlinkProvider("google.com", { linkedProviders = authViewModel.getLinkedProviders() }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
                Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                AccountLinkRow(R.drawable.ic_github, "GitHub", linkedProviders.contains("github.com"),
                    onLink = { val act = context as? Activity; if(act!=null) authViewModel.linkWithGithub(act, { linkedProviders = authViewModel.getLinkedProviders() }, {Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}) },
                    onUnlink = { authViewModel.unlinkProvider("github.com", { linkedProviders = authViewModel.getLinkedProviders() }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
            }
        }

        Divider(color = Color(0xFFEEEEEE))
        MonitoringControlSection(settingsViewModel)
        Divider(color = Color(0xFFEEEEEE))

        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.ExitToApp, null); Spacer(Modifier.width(8.dp)); Text("Đăng xuất", fontWeight = FontWeight.Bold)
        }
    }

    // --- Dialog About Us ---
    if (showAboutUsDialog) {
        AlertDialog(
            onDismissRequest = { showAboutUsDialog = false },
            title = { Text("Về DuckTrack Team 🦆", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Phiên bản: 1.0.0 (Beta)")
                    Spacer(Modifier.height(8.dp))
                    Text("Nhóm phát triển:", fontWeight = FontWeight.Bold)
                    Text("- Thân Văn Ký (Leader/Dev)")
                    Text("- Vũ Trí Dũng (Dev)")
                    Text("- Nguyễn Thị Hương Giang (Dev)")
                    Spacer(Modifier.height(8.dp))
                    Text("Sản phẩm được xây dựng nên để phục vụ báo cáo cuối học phần môn lập trình thiết bị di động. " +
                            "Nên sản phẩm của chúng mình sẽ chưa được hoàn chỉnh toàn diện 100%.")
                    Spacer(Modifier.height(8.dp))
                    Text("Cảm ơn bạn đã sử dụng DuckTrack!")
                }
            },
            confirmButton = { TextButton(onClick = { showAboutUsDialog = false }) { Text("Đóng") } },
            containerColor = Color.White, shape = RoundedCornerShape(16.dp)
        )
    }
}

// --- CÁC COMPOSABLE PHỤ TRỢ (Để chung file này cho gọn) ---

@Composable
fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
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
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
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
        Text(text = providerName, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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
            Text("Giám sát thời gian sử dụng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!hasOverlayPermission) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ Cần quyền hiển thị overlay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)); Spacer(Modifier.height(8.dp))
                        Button(onClick = { PermissionHelper.requestOverlayPermission(context as Activity) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), modifier = Modifier.height(36.dp)) { Text("Cấp quyền ngay", fontSize = 12.sp) }
                    }
                }
            }
            SettingSwitchRow("Kích hoạt giám sát", if (isMonitoring) "Đang hoạt động" else "Tắt", isMonitoring) { viewModel.setMonitoringEnabled(it) }
        }
    }
}