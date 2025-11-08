package com.example.ducktrack.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.ui.AppRoot.AuthViewModelFactory
import com.example.ducktrack.ui.AuthViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context.applicationContext)
    )

    // Lỗi xảy ra ở dòng này nếu hàm trong ViewModel bị thiếu
    val userInfo = authViewModel.getCurrentUserInfo()
    val primaryColor = Color(0xFF62B26A) // Màu xanh lá cây

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // --- KHU VỰC THÔNG TIN NGƯỜI DÙNG ---
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
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- NÚT ĐĂNG XUẤT ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Đẩy nút xuống dưới cùng
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onLogout,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Đăng xuất", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}