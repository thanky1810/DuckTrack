package com.example.ducktrack.ui.permission

// CÁC IMPORT MỚI ĐƯỢC THÊM
import android.app.AppOpsManager
import android.content.Context
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
// ---
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale // Đảm bảo đã import
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R

// --- Định nghĩa màu sắc dựa trên ảnh chụp màn hình ---
private val LightGrayBg = Color(0xFFF7F7F7)
private val LightGreenCard = Color(0xFFEBF5EE)
private val DarkGreen = Color(0xFF3BA15D)
private val MediumGrayButton = Color(0xFFBDBDBD)
private val DarkText = Color(0xFF333333)

/**
 * Màn hình xin cấp quyền Truy cập dữ liệu sử dụng.
 * (Đã cập nhật: Bỏ TopAppBar, dùng ContentScale.Crop, tự động kiểm tra quyền)
 */
@Composable
fun PermissionScreen(
    onGoToSettings: () -> Unit,
    onCancel: () -> Unit,
    onPermissionGranted: () -> Unit // <-- THAM SỐ MỚI ĐỂ ĐIỀU HƯỚNG
) {
    // --- LOGIC MỚI: TỰ ĐỘNG KIỂM TRA QUYỀN KHI QUAY LẠI MÀN HÌNH ---
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, context, onPermissionGranted) {
        // Hàm kiểm tra quyền
        fun checkPermission() {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            val hasPermission = (mode == AppOpsManager.MODE_ALLOWED)

            if (hasPermission) {
                // Nếu đã có quyền, gọi lambda để điều hướng
                onPermissionGranted()
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            // Kiểm tra quyền mỗi khi màn hình resume (quay lại)
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermission()
            }
        }
        // Thêm observer
        lifecycleOwner.lifecycle.addObserver(observer)
        // Gỡ observer khi composable bị hủy
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // --- KẾT THÚC LOGIC MỚI ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBg)
    ) {
        // Lớp 1: Ảnh nền trang trí (img.png)
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = null, // Ảnh trang trí
            modifier = Modifier.fillMaxSize(),
            // Dùng Crop để phóng to giữ tỉ lệ, không làm méo ảnh
            contentScale = ContentScale.Crop
        )

        // Lớp 2: Nội dung chính
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Cho phép cuộn
                // Thêm statusBarsPadding() để nội dung bắt đầu bên dưới thanh status bar
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Spacer này giờ sẽ tính từ dưới status bar
            Spacer(modifier = Modifier.height(16.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Ducktrack",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tiêu đề
            TitleText()

            Spacer(modifier = Modifier.height(24.dp))

            // Khung thông tin 1
            PermissionInfoCard(
                text = "Thống kê thời gian bạn đã dùng điện thoại và từng ứng dụng."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Khung thông tin 2
            PermissionInfoCard(
                text = "Cho phép đặt giới hạn và cảnh báo khi bạn vượt quá thời gian đã cài."
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nút màu xanh lá
            Button(
                onClick = onGoToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Đi tới Cài đặt và Cấp quyền",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nút màu xám
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediumGrayButton),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Hủy",
                    color = DarkText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // Padding cuối
        }
    }
}

/**
 * Composable cho phần văn bản tiêu đề (với chữ "DUCKTRACK" màu xanh).
 */
@Composable
private fun TitleText() {
    Text(
        text = buildAnnotatedString {
            append("Ứng dụng ")
            withStyle(style = SpanStyle(color = DarkGreen, fontWeight = FontWeight.Bold)) {
                append("DUCKTRACK")
            }
            append(" cần quyền\nTruy cập dữ liệu sử dụng để:")
        },
        textAlign = TextAlign.Center,
        fontSize = 18.sp,
        color = DarkText,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Composable cho các thẻ thông tin (màu xanh nhạt).
 */
@Composable
private fun PermissionInfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightGreenCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = DarkText,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}