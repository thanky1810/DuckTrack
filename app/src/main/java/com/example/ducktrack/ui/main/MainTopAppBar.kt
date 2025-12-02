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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    starCount: Int,
    dateText: String // Tham số mới để nhận chuỗi ngày tháng từ MainScreen
) {
    // Màu sắc cố định theo thiết kế xanh lá
    val green       = Color(0xFF62B26A)
    val textOnGreen = Color(0xFFFAFDF9)
    val pillGold    = Color(0xFFE0C378)
    val textOnPill  = Color(0xFF3A2A11)

    val shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape)
            .clip(shape),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(green)
        ) {
            TopAppBar(
                modifier = Modifier.height(75.dp),
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
                            text = dateText, // Hiển thị ngày được truyền vào
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
                    containerColor = Color.Transparent,
                    titleContentColor = textOnGreen,
                    actionIconContentColor = textOnGreen
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    }
}