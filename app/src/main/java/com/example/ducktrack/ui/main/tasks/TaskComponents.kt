package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 1. Ô NHẬP LIỆU (TaskInput) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInput(
    text: String,
    onTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    focusManager: FocusManager
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ô nhập text
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Thêm nhiệm vụ mới...", fontSize = 14.sp) },
            modifier = Modifier
                .weight(1f)
                .background(Color.White, RoundedCornerShape(24.dp)), // Nền trắng để nổi trên ảnh
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF62B26A),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onAddClick()
                focusManager.clearFocus()
            })
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Nút Add
        IconButton(
            onClick = {
                onAddClick()
                focusManager.clearFocus()
            },
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFF62B26A), RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Thêm",
                tint = Color.White
            )
        }
    }
}

// --- 2. HÀNG NÚT KHI CHỌN TASK (TaskActionRows) ---
// (Đây thực chất là wrapper cho SelectionActionRow bạn đã có, nhưng đặt tên cho khớp với code TasksScreen)
@Composable
fun TaskActionRows(
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF212121), // Màu đen hoặc xám đậm cho nổi
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Nút xóa
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFFF5252))
            }

            // Đường kẻ dọc
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray))

            // Nút ghim
            IconButton(onClick = onPin) {
                Icon(Icons.Default.PushPin, contentDescription = "Ghim", tint = Color(0xFF62B26A))
            }
        }
    }
}