package com.example.ducktrack.ui.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    starCount: Int
) {
    // ====== TÙY CHỈNH MÀU Ở ĐÂY ======
    val green       = Color(0xFF62B26A)   // nền topbar
    val textOnGreen = Color(0xFFFAFDF9)   // chữ trên nền xanh
    val pillGold    = Color(0xFFE0C378)   // pill ⭐
    val textOnPill  = Color(0xFF3A2A11)

    val shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    // Định dạng ngày kiểu: "Hôm nay, 2 tháng 10"
    val dateStr = "Hôm nay, " + LocalDate.now().format(
        DateTimeFormatter.ofPattern("d 'tháng' M", Locale("vi", "VN"))
    )

    // Surface để bo góc đáy + đổ bóng
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape) // độ bóng
            .clip(shape),
        color = Color.Transparent
    ) {
        // Nền xanh cho toàn vùng top bar
        Box(
            modifier = Modifier
                .background(green)
        ) {
            TopAppBar(
                modifier = Modifier
                    .height(75.dp),

                title = {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = textOnGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dateStr,
                            color = Color.Black,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                },
                actions = {

                    Surface(

                        color = pillGold,
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .padding(end = 16.dp, top = 12.dp)
                            .offset(y = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)

                        ) {
                            Text(
                                text = starCount.toString(),
                                color = textOnPill,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Số sao",
                                tint = textOnPill,
                                modifier = Modifier.size(18.dp)
                            )


                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,       // để nền do Box quyết định
                    titleContentColor = textOnGreen,
                    actionIconContentColor = textOnGreen
                ),
                // Không thêm inset mặc định để giữ layout gọn
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    }
}
