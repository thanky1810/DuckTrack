package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R

// Data class (không đổi)
data class Task(
    val id: Int,
    val description: String,
    val isCompleted: Boolean
)

@Composable
fun TasksScreen() {
    // --- Định nghĩa màu sắc (không đổi) ---
    val screenBgColor = Color(0xFFFFFFFF)
    val titleColor = Color(0xFF62B26A)
    val inputBgColor = Color(0xFFFFFFFF)
    val inputBorderColor = Color(0xFFE0E0E0)
    val buttonColor = Color(0xFF62B26A)
    val taskPendingBg = Color(0xFFFFF6F6)
    val taskCompletedBg = Color(0xFFE6F8E8)
    val taskTextColor = Color(0xFF424242)


    // --- State (không đổi) ---
    var newTaskText by remember { mutableStateOf("") }
    val tasks = remember {
        mutableStateListOf(
            Task(1, "Hoàn thành bài tập Tiếng Anh", false),
            Task(2, "Đọc 1 quyển sách", false),
            Task(3, "Dọn dẹp nhà cửa", true),
        )
    }

    // --- Giao diện (UI) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {

        // --- THAY ĐỔI 1: Điều chỉnh kích thước Image và loại bỏ alpha ---
        Image(
            painter = painterResource(id = R.drawable.duck_celebrate),
            contentDescription = "Con vịt nền",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(200.dp)
                .padding(bottom = 0.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Danh Sách Nhiệm Vụ Cần Làm Trong Hôm Nay ✅",
                color = titleColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text("Thêm nhiệm vụ mới ..") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = buttonColor,
                        unfocusedBorderColor = inputBorderColor,
                        focusedContainerColor = inputBgColor,
                        unfocusedContainerColor = inputBgColor,
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            tasks.add(
                                Task(
                                    id = (tasks.maxOfOrNull { it.id } ?: 0) + 1,
                                    description = newTaskText,
                                    isCompleted = false
                                )
                            )
                            newTaskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(buttonColor, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm nhiệm vụ",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onCheckedChange = { isChecked ->
                            val index = tasks.indexOf(task)
                            if (index != -1) {
                                tasks[index] = task.copy(isCompleted = isChecked)
                            }
                        },
                        backgroundColor = if (task.isCompleted) taskCompletedBg else taskPendingBg,
                        textColor = taskTextColor
                    )
                }
            }
        }
    }
}

/**
 * Một Composable riêng cho từng Hàng nhiệm vụ (Task Item)
 * (Không thay đổi)
 */
@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    backgroundColor: Color,
    textColor: Color
) {
    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF62B26A),
                uncheckedColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = task.description,
            color = textColor,
            textDecoration = textDecoration,
            fontSize = 16.sp
        )
    }
}

// --- Preview (Xem trước) ---
@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    Surface(color = Color(0xFFFDFCF8)) {
        TasksScreen()
    }
}