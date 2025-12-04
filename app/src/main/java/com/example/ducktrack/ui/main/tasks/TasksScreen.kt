package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.R

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel()
) {
    // --- STATE ---
    val tasks by viewModel.tasks.collectAsState()
    val dateText by viewModel.dateText.collectAsState()
    val isToday by viewModel.isToday.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()
    val newTaskText by viewModel.newTaskText.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()

    val selectionMode = selectedTaskIds.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    // --- DIALOG ---
    if (taskToEdit != null) {
        EditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { viewModel.onCancelEdit() },
            onConfirm = { viewModel.onConfirmEdit(it) }
        )
    }

    // --- UI CHÍNH ---
    // Sử dụng Column thay vì Scaffold để bỏ qua TopBar mặc định,
    // vì MainScreen đã có Header xanh rồi.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(interactionSource = interactionSource, indication = null) {
                focusManager.clearFocus()
            }
            .padding(horizontal = 16.dp) // Padding chung cho toàn màn hình
    ) {

        // 1. TIÊU ĐỀ "Danh Sách Nhiệm Vụ" (Màu xanh)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Danh Sách Nhiệm Vụ",
            color = Color(0xFF62B26A), // Màu xanh theo ảnh
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. THANH ĐIỀU HƯỚNG NGÀY (Nền xanh nhạt)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp)) // Nền xanh nhạt giống ảnh
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.previousDay() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trước", tint = Color(0xFF33691E))
            }
            Text(
                text = dateText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF33691E) // Chữ xanh đậm
            )
            IconButton(onClick = { viewModel.nextDay() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sau", tint = Color(0xFF33691E))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Ô NHẬP LIỆU (Nằm ngay dưới ngày, giống ảnh ban đầu)
        if (!selectionMode) {
            TaskInput(
                text = newTaskText,
                onTextChange = { viewModel.onNewTaskTextChange(it) },
                onAddClick = { viewModel.onAddTask() },
                focusManager = focusManager
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. DANH SÁCH & VỊT (Chiếm phần còn lại)
        Box(modifier = Modifier.weight(1f)) {
            if (tasks.isEmpty()) {
                // LOGIC: Nếu KHÔNG có task -> Hiện Vịt và Text
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom // Đẩy xuống đáy như ảnh
                ) {
                    Text(
                        text = if(isToday) "Hôm nay chưa có nhiệm vụ nào" else "Chưa có nhiệm vụ nào",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.duck_celebrate),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .padding(bottom = 32.dp)
                    )
                }
            } else {
                // LOGIC: Nếu CÓ task -> Hiện List, ẨN Vịt
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Padding để không bị che bởi nút menu nếu có
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            selectionColor = Color(0xFFE6F8E8),
                            textColor = if (task.isCompleted) Color.Gray else Color.Black,
                            pinColor = Color(0xFF62B26A),
                            isSelected = selectedTaskIds.contains(task.id),
                            onClick = { viewModel.onTaskClick(task) },
                            onLongPress = { viewModel.onTaskLongPress(task) },
                            onPinClick = { viewModel.onUnpinClick(task) },
                            onEditClick = { viewModel.onEditClick(task) }
                        )
                    }
                }
            }

            // 5. MENU KHI CHỌN TASK (Hiện đè ở dưới cùng)
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    TaskActionRows(
                        onDelete = { viewModel.onDeleteSelected() },
                        onPin = { viewModel.onPinSelected() }
                    )
                }
            }
        }
    }
}