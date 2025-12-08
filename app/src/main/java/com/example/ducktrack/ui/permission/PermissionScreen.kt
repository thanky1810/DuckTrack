package com.example.ducktrack.ui.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ducktrack.R
import com.example.ducktrack.ui.theme.AppColors
import com.example.ducktrack.utils.hasUsageAccess

@Composable
fun PermissionScreen(
    onGoToSettings: () -> Unit,
    onCancel: () -> Unit,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // --- 1. LOGIC XIN QUYỀN THÔNG BÁO TỰ ĐỘNG (Android 13+) ---
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Quyền thông báo là phụ, cho hay không cũng không chặn flow chính
            // Nên không cần xử lý gì đặc biệt ở đây
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPerm) {
                // Tự động bung popup xin quyền ngay khi vào màn hình
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // -----------------------------------------------------------

    // Logic kiểm tra quyền Usage Access khi quay lại (Giữ nguyên)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasUsageAccess(context)) {
                    onPermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ... (Phần giao diện UI bên dưới giữ nguyên y hệt file cũ) ...
            // Tôi sẽ paste lại đoạn UI để bạn dễ copy

            Spacer(Modifier.weight(1f))

            // Logo và hình nền bong bóng
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo", modifier = Modifier.size(180.dp), contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            TitleText()
            Spacer(modifier = Modifier.height(24.dp))

            PermissionInfoCard(text = "Thống kê thời gian bạn đã dùng điện thoại và từng ứng dụng.")
            Spacer(modifier = Modifier.height(16.dp))
            PermissionInfoCard(text = "Cho phép đặt giới hạn và cảnh báo khi bạn vượt quá thời gian đã cài.")
            Spacer(modifier = Modifier.height(40.dp))

            // Nút "Đi tới Cài đặt và Cấp quyền" (Usage Access)
            Button(
                onClick = onGoToSettings,
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Đi tới Cài đặt và Cấp quyền", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút "Hủy"
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGray, contentColor = Color.Black),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Hủy", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1.5f))
        }
    }
}

// ... (Các hàm TitleText và PermissionInfoCard giữ nguyên) ...
@Composable
private fun TitleText() {
    Text(
        text = buildAnnotatedString {
            append("Ứng dụng ")
            withStyle(style = SpanStyle(color = AppColors.TextGreen, fontWeight = FontWeight.Bold)) { append("DUCKTRACK") }
            append(" cần quyền\nTruy cập dữ liệu sử dụng để:")
        },
        textAlign = TextAlign.Center, fontSize = 20.sp, color = Color.Black, lineHeight = 28.sp, fontWeight = FontWeight.Normal
    )
}

@Composable
private fun PermissionInfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, modifier = Modifier.padding(16.dp), color = AppColors.TextGray, fontSize = 18.sp, textAlign = TextAlign.Start, fontWeight = FontWeight.Normal)
    }
}