package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    modifier: Modifier = Modifier,
    task: TodoTask,
    selectionColor: Color,
    textColor: Color,
    pinColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
    val finalBgColor = when {
        isSelected -> selectionColor
        task.isCompleted -> Color(0xFFE6F8E8) // Màu xanh nhạt khi xong
        else -> Color(0xFFFFF6F6) // Màu mặc định
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.1f))
            .clip(RoundedCornerShape(16.dp))
            .background(finalBgColor)
            // --- QUAN TRỌNG: Sự kiện bấm nằm ở đây (cho cả dòng) ---
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongPress() }
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- SỬA LỖI Ở ĐÂY ---
        Checkbox(
            checked = task.isCompleted,
            // Đặt onCheckedChange = null để vô hiệu hóa cảm ứng của riêng ô Checkbox.
            // Khi đó, sự kiện bấm sẽ "xuyên qua" ô này và được Row ở trên bắt lấy.
            // -> Bấm vào ô vuông hay bấm vào dòng đều ăn như nhau.
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF62B26A),
                uncheckedColor = Color(0xFFCCCCCC),
                disabledCheckedColor = Color(0xFF62B26A),
                disabledUncheckedColor = Color(0xFFCCCCCC),
                checkmarkColor = Color.White
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = task.description,
            color = textColor,
            textDecoration = textDecoration,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Nút Sửa (Chỉ hiện khi chưa xong và chưa chọn)
        if (!task.isCompleted && !isSelected) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Sửa", tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
            }
        }

        // Nút Ghim (Chỉ hiện khi đã ghim)
        if (task.isPinned) {
            IconButton(onClick = onPinClick, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Filled.PushPin, contentDescription = "Ghim", tint = pinColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}