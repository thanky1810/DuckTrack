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

    // --- CẬP NHẬT TÊN THẺ HIỂN THỊ ---
    val (tagLabel, tagColor) = when {
        task.isImportant && task.isUrgent -> "Quan trọng & Khẩn cấp" to Color(0xFFD32F2F)
        task.isImportant && !task.isUrgent -> "Quan trọng & Không khẩn cấp" to Color(0xFF1976D2)
        !task.isImportant && task.isUrgent -> "Không quan trọng & Khẩn cấp" to Color(0xFFF57C00)
        else -> null to null // Nhóm 4 (Xám) không cần hiện thẻ cho đỡ rối
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.1f))
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.description,
                color = textColor,
                textDecoration = textDecoration,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif
            )

            // Hiển thị thẻ
            if (tagLabel != null && tagColor != null && !task.isCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = tagColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = tagLabel,
                        color = tagColor,
                        fontSize = 9.sp, // Chữ nhỏ lại vì tên dài
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!task.isCompleted && !isSelected) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Sửa", tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
            }
        }
        if (task.isPinned) {
            IconButton(onClick = onPinClick, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Filled.PushPin, contentDescription = "Ghim", tint = pinColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}