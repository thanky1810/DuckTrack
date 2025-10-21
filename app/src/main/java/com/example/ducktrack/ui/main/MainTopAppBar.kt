package com.example.ducktrack.ui.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val currentDate = LocalDate.now()
    // Sử dụng "vi" locale để đảm bảo hiển thị đúng tiếng Việt
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("vi"))
    val formattedDate = currentDate.format(formatter)

    TopAppBar(
        modifier = Modifier.height(90.dp),
        title = {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    maxLines = 1
                )
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp, top = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Số sao",
                    tint = Color(0xFFFFC107), // Màu vàng cho ngôi sao
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = starCount.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        ),
        // Ghi đè window insets mặc định để ngăn nó thêm khoảng đệm thừa ở trên cùng
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

