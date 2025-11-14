package com.example.ducktrack.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.utils.msToReadable

@Composable
fun AppUsageRow(
    usage: AppUsage,
    limitMinutes: Int?,
    appIcon: Drawable?,
    chartColor: Color, // Màu từ chart
    onClickSetLimit: () -> Unit,
    onClickRemoveLimit: (() -> Unit)? = null
) {
    val (showMenu, setShowMenu) = remember { mutableStateOf(false) }

    // Surface với nền màu từ chart (opacity nhẹ)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = chartColor.copy(alpha = 0.12f), // Nền màu nhạt 12% opacity
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar tròn chứa icon app
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                appIcon?.let {
                    val bmp = it.toBitmap(width = 64, height = 64, config = null)
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usage.label,
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F)
                )
                val used = msToReadable(usage.totalForegroundMs)
                val limitText = limitMinutes?.let {
                    val h = it / 60
                    val m = it % 60
                    val formatted = String.format("%02d:%02d", h, m)
                    " • Giới hạn: $formatted"
                } ?: ""
                Text(
                    text = "Đã sử dụng: $used$limitText",
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B)
                )
            }

            // Pill tổng phút bên phải
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 0.dp
            ) {
                Text(
                    text = msToReadable(usage.totalForegroundMs),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    color = Color(0xFF333333)
                )
            }

            IconButton(onClick = { setShowMenu(true) }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = Color(0xFF666666)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { setShowMenu(false) }
            ) {
                // Đặt / chỉnh giới hạn
                DropdownMenuItem(
                    text = {
                        Text(
                            if (limitMinutes != null)
                                "Chỉnh giới hạn thời gian"
                            else
                                "Đặt giới hạn thời gian"
                        )
                    },
                    onClick = {
                        setShowMenu(false)
                        onClickSetLimit()
                    }
                )

                // Xóa giới hạn (chỉ hiện nếu đã có limit)
                if (limitMinutes != null && onClickRemoveLimit != null) {
                    DropdownMenuItem(
                        text = { Text("Xóa giới hạn") },
                        onClick = {
                            setShowMenu(false)
                            onClickRemoveLimit()
                        }
                    )
                }
            }
        }
    }
}
