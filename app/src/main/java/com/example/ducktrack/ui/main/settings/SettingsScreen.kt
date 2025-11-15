package com.example.ducktrack.ui.main.settings

// Các import không cần nữa đã bị xóa (Intent, Build, UsageMonitorService)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.viewmodel.compose.viewModel // <-- Import mới
import com.example.ducktrack.utils.PermissionHelper

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    // Khởi tạo ViewModel
    viewModel: SettingsViewModel = viewModel()
) {
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

        // Phần giám sát thời gian (MỚI)
        // Truyền ViewModel vào
        MonitoringControlSection(viewModel)

        Divider()

        // Nút đăng xuất
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
            Text("Đăng xuất")
        }
    }
}

/**
 * Section để bật/tắt monitoring và xin quyền overlay
 */
@Composable
private fun MonitoringControlSection(
    // Nhận ViewModel làm tham số
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current

    // Lấy trạng thái giám sát TỪ VIEWMODEL (đã kết nối DataStore)
    val isMonitoring by viewModel.isMonitoringEnabled.collectAsState()

    // DÒNG NÀY ĐÃ BỊ XÓA:
    // var isMonitoring by remember { mutableStateOf(false) }

    var hasOverlayPermission by remember {
        mutableStateOf(PermissionHelper.hasOverlayPermission(context))
    }

    // Check lại quyền khi quay lại app
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

            // Kiểm tra quyền overlay (Giữ nguyên)
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

            // Nút bật/tắt monitoring
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
                    checked = isMonitoring, // Lấy trạng thái từ ViewModel
                    onCheckedChange = { enabled ->
                        // Khi gạt nút, gọi ViewModel để xử lý
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

// 2 HÀM startMonitoringService và stopMonitoringService ĐÃ BỊ XÓA KHỎI ĐÂY
// (Vì chúng đã được chuyển vào SettingsViewModel)