package com.example.ducktrack.ui.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
// [QUAN TRỌNG] Import LocalDuckColors để lấy màu theo Theme
import com.example.ducktrack.ui.theme.LocalDuckColors

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    starCount: Int,
    dateText: String
) {
    val bottomRoundedShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

    // [QUAN TRỌNG] Lấy màu từ Theme hiện tại (Xanh hoặc Đỏ)
    val currentThemeColor = LocalDuckColors.current.buttonColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(elevation = 6.dp, shape = bottomRoundedShape),
        shape = bottomRoundedShape,
        color = currentThemeColor // [ĐÃ SỬA] Dùng màu động thay vì AppColors.ButtonGreen
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // 1. CỤM BÊN TRÁI: TÊN & NGÀY
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 2. CON VỊT (Ở GIỮA)
            Image(
                painter = painterResource(id = R.drawable.duck_topbar),
                contentDescription = "Duck Logo",
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(70.dp)
                    .offset(y = 4.dp),
                contentScale = ContentScale.Fit
            )

            // 3. CỤM BÊN PHẢI: ĐIỂM SỐ
            Surface(
                color = Color(0xFFE0C378), // Màu vàng khung điểm giữ nguyên (hoặc đổi nếu thích)
                shape = RoundedCornerShape(50),
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$starCount",
                        color = Color(0xFF3E2723),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}