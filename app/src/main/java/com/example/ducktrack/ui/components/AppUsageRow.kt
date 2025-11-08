package com.example.ducktrack.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
    onSetLimit: (Int) -> Unit
) {
    val (showMenu, setShowMenu) = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // avatar tròn chứa icon app
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F2F2)),
            contentAlignment = Alignment.Center
        ) {
            appIcon?.let {
                val bmp = it.toBitmap(width = 64, height = 64, config = null)
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(usage.label, fontSize = 16.sp, color = Color(0xFF1F1F1F))
            val used = msToReadable(usage.totalForegroundMs)
            val limitText = limitMinutes?.let { " • Giới hạn: ${it}m" } ?: ""
            Text("Đã sử dụng: $used$limitText", fontSize = 12.sp, color = Color(0xFF6B6B6B))
        }

        // pill tổng phút bên phải
        Surface(
            color = Color(0xFFEFEFEF),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 0.dp
        ) {
            Text(
                text = msToReadable(usage.totalForegroundMs),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = Color(0xFF333333)
            )
        }

        IconButton(onClick = { setShowMenu(true) }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { setShowMenu(false) }) {
            listOf(15, 30, 45, 60, 90, 120).forEach { m ->
                DropdownMenuItem(
                    text = { Text("Giới hạn ${m} phút") },
                    onClick = { setShowMenu(false); onSetLimit(m) }
                )
            }
        }
    }
    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
}
