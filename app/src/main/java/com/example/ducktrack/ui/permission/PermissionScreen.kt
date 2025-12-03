package com.example.ducktrack.ui.permission

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
import com.example.ducktrack.utils.hasUsageAccess
import com.example.ducktrack.ui.theme.AppColors // Import màu mới

@Composable
fun PermissionScreen(
    onGoToSettings: () -> Unit,
    onCancel: () -> Unit,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Logic kiểm tra quyền khi quay lại (Giữ nguyên)
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
                .background(MaterialTheme.colorScheme.background) // Nền trắng
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            // Logo và hình nền bong bóng
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo",
                    modifier = Modifier.size(180.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            TitleText() // Gọi hàm TitleText đã sửa
            Spacer(modifier = Modifier.height(24.dp))

            PermissionInfoCard(text = "Thống kê thời gian bạn đã dùng điện thoại và từng ứng dụng.")
            Spacer(modifier = Modifier.height(16.dp))
            PermissionInfoCard(text = "Cho phép đặt giới hạn và cảnh báo khi bạn vượt quá thời gian đã cài.")
            Spacer(modifier = Modifier.height(40.dp))

            // Nút "Đi tới Cài đặt và Cấp quyền" (Màu xanh)
            Button(
                onClick = onGoToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Đi tới Cài đặt và Cấp quyền",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp // Giảm cỡ chữ 1 chút
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút "Hủy" (Màu xám)
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGray, contentColor = Color.Black),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Hủy",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.weight(1.5f))
        }
    }
}

// Sửa lại TitleText
@Composable
private fun TitleText() {
    Text(
        text = buildAnnotatedString {
            append("Ứng dụng ")
            withStyle(style = SpanStyle(color = AppColors.TextGreen, fontWeight = FontWeight.Bold)) {
                append("DUCKTRACK")
            }
            append(" cần quyền\nTruy cập dữ liệu sử dụng để:")
        },
        textAlign = TextAlign.Center,
        fontSize = 20.sp,
        color = Color.Black, // Màu chữ đen
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal
    )
}

// Sửa lại PermissionInfoCard
@Composable
private fun PermissionInfoCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)), // Màu xanh lá rất nhạt
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = AppColors.TextGray, // Màu chữ xám
            fontSize = 18.sp,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Normal
        )
    }
}