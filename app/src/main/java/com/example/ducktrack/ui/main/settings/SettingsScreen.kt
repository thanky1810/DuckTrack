package com.example.ducktrack.ui.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.utils.PermissionHelper
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.AppRoot.AuthViewModelFactory

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val userInfo = authViewModel.getCurrentUserInfo()
    val primaryColor = Color(0xFF62B26A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cài đặt",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // --- THẺ THÔNG TIN NGƯỜI DÙNG (ĐÃ SỬA) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = primaryColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Tên người dùng
                Text(
                    text = "Tên người dùng:",
                    fontSize = 14.sp,
                    color = primaryColor
                )
                Text(
                    text = userInfo?.name ?: "Khách",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(Modifier.height(12.dp))

                // Email
                Text(
                    text = "Email:",
                    fontSize = 14.sp,
                    color = primaryColor
                )
                Text(
                    text = userInfo?.email ?: "Vui lòng đăng nhập",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                // --- THÊM MỚI: PHƯƠNG THỨC ĐĂNG NHẬP ---
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Phương thức đăng nhập:",
                    fontSize = 14.sp,
                    color = primaryColor
                )
                Text(
                    text = userInfo?.provider ?: "Không rõ", // Lấy từ UserInfo
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.SemiBold
                )
                // --- KẾT THÚC PHẦN THÊM MỚI ---
            }
        }
        // --- KẾT THÚC THẺ ---

        Divider()

        // Phần giám sát thời gian (Giữ nguyên)
        MonitoringControlSection(settingsViewModel)

        Divider()

        // Nút đăng xuất (Giữ nguyên)
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F)
            )
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Đăng xuất", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Section để bật/tắt monitoring (Giữ nguyên)
 */
@Composable
private fun MonitoringControlSection(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val isMonitoring by viewModel.isMonitoringEnabled.collectAsState()

    var hasOverlayPermission by remember {
        mutableStateOf(PermissionHelper.hasOverlayPermission(context))
    }

    LaunchedEffect(Unit) {
        hasOverlayPermission = PermissionHelper.hasOverlayPermission(context)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Giám sát thời gian sử dụng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "Kích hoạt để nhận cảnh báo khi vượt giới hạn thời gian",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (!hasOverlayPermission) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "⚠️ Cần quyền hiển thị overlay",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Để hiển thị cảnh báo khi vượt giới hạn",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                PermissionHelper.requestOverlayPermission(
                                    context as android.app.Activity
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Text("Cấp quyền ngay")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Kích hoạt giám sát",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (isMonitoring) "Đang hoạt động" else "Tắt",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMonitoring) Color(0xFF2E7D32) else Color.Gray
                    )
                }

                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { enabled ->
                        viewModel.setMonitoringEnabled(enabled)
                    },
                    enabled = hasOverlayPermission
                )
            }

            if (isMonitoring) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "✓ Service đang chạy trong background",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}