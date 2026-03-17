package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
        task.isCompleted -> Color(0xFFE6F8E8)
        else -> Color(0xFFFFF6F6)
    }

    // --- CẬP NHẬT TÊN THẺ VÀ MÀU SẮC (ĐỦ 4 LOẠI) ---
    val (tagLabel, tagColor) = when {
        task.isImportant && task.isUrgent -> "Quan trọng & Khẩn cấp" to Color(0xFFD32F2F) // Đỏ
        task.isImportant && !task.isUrgent -> "Quan trọng & Không khẩn cấp" to Color(0xFF1976D2) // Xanh dương
        !task.isImportant && task.isUrgent -> "Không quan trọng & Khẩn cấp" to Color(0xFFF57C00) // Cam

        // --- SỬA Ở ĐÂY: Thêm trường hợp thứ 4 ---
        else -> "Không quan trọng & Không khẩn cấp" to Color(0xFF388E3C) // Xanh lá (hoặc Xám: Color.Gray)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(finalBgColor)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongPress() }
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = null, // Để Row xử lý click
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF62B26A),
                uncheckedColor = Color(0xFFCCCCCC),
                disabledCheckedColor = Color(0xFF62B26A),
                disabledUncheckedColor = Color(0xFFCCCCCC),
                checkmarkColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.description,
                color = textColor,
                textDecoration = textDecoration,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif
            )

            // Hiển thị thẻ (Chỉ hiện khi chưa hoàn thành để đỡ rối, hoặc bỏ điều kiện !isCompleted nếu muốn hiện luôn)
            if (!task.isCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = tagColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        tagColor.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = tagLabel,
                        color = tagColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!task.isCompleted && !isSelected) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Sửa",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (task.isPinned) {
            IconButton(onClick = onPinClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Ghim",
                    tint = pinColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}