package com.example.ducktrack.ui.main.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.R
import com.example.ducktrack.ui.AppRoot.AuthViewModelFactory
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.theme.AppColors
import com.example.ducktrack.utils.PermissionHelper

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val context = LocalContext.current
    val primaryColor = AppColors.TextGreen

    // State lưu thông tin user
    var userInfo by remember { mutableStateOf(authViewModel.getCurrentUserInfo()) }

    // State cho Dialog đổi tên
    var showRenameDialog by remember { mutableStateOf(false) }

    // State danh sách các provider đã liên kết
    var linkedProviders by remember { mutableStateOf(authViewModel.getLinkedProviders()) }

    // Hàm refresh lại dữ liệu
    fun refreshData() {
        userInfo = authViewModel.getCurrentUserInfo()
        linkedProviders = authViewModel.getLinkedProviders()
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

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

        // --- 1. THÔNG TIN NGƯỜI DÙNG ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Tên người dùng
                Text(text = "Tên người dùng:", fontSize = 14.sp, color = primaryColor)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = userInfo?.name ?: "Khách",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Đổi tên", tint = primaryColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Email
                Text(text = "Email:", fontSize = 14.sp, color = primaryColor)
                Text(text = userInfo?.email ?: "...", fontSize = 16.sp, color = Color.DarkGray)

                // --- (ĐÃ THÊM LẠI) PHƯƠNG THỨC ĐĂNG NHẬP HIỆN TẠI ---
                Spacer(Modifier.height(12.dp))
                Text(text = "Đăng nhập hiện tại:", fontSize = 14.sp, color = primaryColor)
                Text(
                    text = userInfo?.provider ?: "Không rõ",
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- 2. LIÊN KẾT TÀI KHOẢN ---
        Text(
            text = "Liên kết tài khoản",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Dòng Google
                AccountLinkRow(
                    iconRes = R.drawable.ic_google,
                    providerName = "Google",
                    isLinked = linkedProviders.contains("google.com"),
                    onLink = { Toast.makeText(context, "Vui lòng đăng nhập Google trước", Toast.LENGTH_SHORT).show() },
                    onUnlink = {
                        authViewModel.unlinkProvider("google.com",
                            onSuccess = { refreshData() },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                // Dòng GitHub
                AccountLinkRow(
                    iconRes = R.drawable.ic_github,
                    providerName = "GitHub",
                    isLinked = linkedProviders.contains("github.com"),
                    onLink = {
                        val activity = context as? Activity
                        if (activity != null) {
                            authViewModel.linkWithGithub(activity,
                                onSuccess = {
                                    Toast.makeText(context, "Đã liên kết GitHub!", Toast.LENGTH_SHORT).show()
                                    refreshData()
                                },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            )
                        }
                    },
                    onUnlink = {
                        authViewModel.unlinkProvider("github.com",
                            onSuccess = { refreshData() },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                )
            }
        }

        Divider(color = Color(0xFFEEEEEE))

        // --- 3. GIÁM SÁT THỜI GIAN ---
        MonitoringControlSection(settingsViewModel)

        Divider(color = Color(0xFFEEEEEE))

        // --- 4. ĐĂNG XUẤT ---
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Đăng xuất", fontWeight = FontWeight.Bold)
        }
    }

    // Dialog Đổi tên
    if (showRenameDialog) {
        ChangeNameDialog(
            currentName = userInfo?.name ?: "",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                authViewModel.updateUserName(newName,
                    onSuccess = {
                        Toast.makeText(context, "Đổi tên thành công!", Toast.LENGTH_SHORT).show()
                        refreshData()
                        showRenameDialog = false
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }
}

/**
 * UI Component hiển thị dòng liên kết
 */
@Composable
fun AccountLinkRow(
    iconRes: Int,
    providerName: String,
    isLinked: Boolean,
    onLink: () -> Unit,
    onUnlink: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))

        Text(
            text = providerName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (isLinked) {
            TextButton(onClick = onUnlink) {
                Text("Đã kết nối", color = AppColors.TextGreen, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onLink,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Kết nối", fontSize = 12.sp)
            }
        }
    }
}

// Dialog Đổi tên
@Composable
fun ChangeNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textState by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Đổi tên người dùng") },
        text = {
            Column {
                OutlinedTextField(
                    value = textState, onValueChange = { textState = it },
                    label = { Text("Tên mới") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (textState.isNotBlank()) onConfirm(textState) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen)
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        },
        containerColor = Color.White, shape = RoundedCornerShape(16.dp)
    )
}

// Section Giám sát
@Composable
private fun MonitoringControlSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val isMonitoring by viewModel.isMonitoringEnabled.collectAsState()
    var hasOverlayPermission by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }

    LaunchedEffect(Unit) { hasOverlayPermission = PermissionHelper.hasOverlayPermission(context) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Giám sát thời gian sử dụng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!hasOverlayPermission) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ Cần quyền hiển thị overlay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        Spacer(Modifier.height(4.dp))
                        Text("Để hiển thị cảnh báo khi vượt giới hạn thời gian", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { PermissionHelper.requestOverlayPermission(context as Activity) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), modifier = Modifier.height(36.dp)
                        ) { Text("Cấp quyền ngay", fontSize = 12.sp) }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kích hoạt giám sát", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(if (isMonitoring) "Đang hoạt động" else "Tắt", style = MaterialTheme.typography.bodySmall, color = if (isMonitoring) AppColors.TextGreen else Color.Gray)
                }
                Switch(
                    checked = isMonitoring, onCheckedChange = { viewModel.setMonitoringEnabled(it) },
                    enabled = hasOverlayPermission,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.ButtonGreen)
                )
            }
        }
    }
}