package com.example.ducktrack.ui.main.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R // Đảm bảo bạn đã import R

@Composable
fun DashboardScreen(
    hasPermission: Boolean, // <-- Nhận trạng thái
    onGoToPermission: () -> Unit // <-- Nhận hành động
) {
    if (hasPermission) {
        // --- GIAO DIỆN KHI ĐÃ CÓ QUYỀN ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Trang chủ (Đã có quyền)")
            // TODO: Hiển thị giao diện dashboard thật ở đây
        }
    } else {
        // --- GIAO DIỆN KHI CHƯA CÓ QUYỀN (GIỮ NGUYÊN) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7)) // Màu nền xám nhạt
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hình ảnh con vịt
            Image(
                painter = painterResource(id = R.drawable.logo), // Giả sử dùng logo
                contentDescription = "Yêu cầu quyền",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dòng text 1
            Text(
                text = "Cần quyền truy cập",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dòng text 2
            Text(
                text = "Vui lòng cấp quyền truy cập dữ liệu sử dụng để DuckTrack có thể theo dõi và thống kê thời gian dùng ứng dụng của bạn.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nút bấm
            Button(
                onClick = onGoToPermission, // <-- Gọi lambda
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3BA15D) // Màu xanh lá
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Cấp quyền ngay",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}