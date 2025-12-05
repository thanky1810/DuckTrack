package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskRow(
    newTaskText: String,
    onNewTaskTextChange: (String) -> Unit,
    onAddTask: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ô nhập liệu
        OutlinedTextField(
            value = newTaskText,
            onValueChange = onNewTaskTextChange,
            placeholder = { Text("Thêm nhiệm vụ mới ..") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF62B26A),
                unfocusedBorderColor = Color(0xFFEEEEEE),
                focusedContainerColor = Color(0xFFFFFFFF),
                unfocusedContainerColor = Color(0xFFFFFFFF),
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        // --- ĐÃ SỬA: Dùng Button thay vì IconButton để chắc chắn hiện nền xanh ---
        Button(
            onClick = onAddTask,
            modifier = Modifier.size(56.dp), // Kích thước bằng chiều cao mặc định của TextField
            shape = RoundedCornerShape(16.dp), // Bo góc giống TextField
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF62B26A), // Màu xanh
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp) // Bỏ padding để icon nằm giữa
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Thêm nhiệm vụ",
                modifier = Modifier.size(32.dp) // Icon to rõ
            )
        }
    }
}

@Composable
fun SelectionActionRow(
    selectedCount: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFEFEFEF),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.Black
                    )
                }
                Text(
                    text = "$selectedCount",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPin) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Ghim",
                        tint = Color(0xFF62B26A)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}