
package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    modifier: Modifier = Modifier, // Thêm tham số modifier
    task: Task,
    selectionColor: Color,
    textColor: Color,
    pinColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit // Thêm callback cho nút Sửa
) {
    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None

    val finalBgColor = when {
        isSelected -> selectionColor
        task.isCompleted -> Color(0xFFE6F8E8)
        else -> Color(0xFFFFF6F6)
    }

    Row(
        // Áp dụng modifier từ tham số
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
            .heightIn(min = 56.dp) // Dùng heightIn để tự dãn
            .padding(horizontal = 8.dp),
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

        Text(
            text = task.description,
            color = textColor,
            textDecoration = textDecoration,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Nút Sửa
        if (!task.isCompleted && !isSelected) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Sửa nhiệm vụ",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Nút Ghim
        if (task.isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Đã ghim",
                tint = pinColor,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onPinClick() }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}