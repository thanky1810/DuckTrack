// FILE: TasksScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.ducktrack.ui.main.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.R
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel()
) {
    val screenBgColor = Color(0xFFFFFFFF)
    val titleColor = Color(0xFF62B26A)
    val selectionColor = Color(0xFFD0E3FF)
    val taskTextColor = Color(0xFF424242)
    val pinColor = Color(0xFF62B26A)

    val tasks by viewModel.tasks.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()
    val newTaskText by viewModel.newTaskText.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()
    val dateText by viewModel.dateText.collectAsState()
    val isToday by viewModel.isToday.collectAsState()

    val isInSelectionMode = selectedTaskIds.isNotEmpty()

    Scaffold(
        containerColor = screenBgColor,
    ) { innerPadding ->

        taskToEdit?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = viewModel::onCancelEdit,
                onConfirm = { newText -> viewModel.onConfirmEdit(newText) }
            )
        }

        // --- THAY ĐỔI CHÍNH Ở ĐÂY ---
        // Sử dụng Column để xếp chồng theo chiều dọc, không đè lên nhau
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // PHẦN 1: Nội dung chính (Tiêu đề, Điều hướng, Danh sách)
            // Sử dụng weight(1f) để chiếm không gian phía trên
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Danh Sách Nhiệm Vụ",
                    color = titleColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                TaskDateNavigator(
                    dateText = dateText,
                    onPrev = { viewModel.previousDay() },
                    onNext = { viewModel.nextDay() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    if (isInSelectionMode) {
                        SelectionActionRow(
                            selectedCount = selectedTaskIds.size,
                            onClose = viewModel::onCloseSelectionMode,
                            onDelete = viewModel::onDeleteSelected,
                            onPin = viewModel::onPinSelected
                        )
                    } else {
                        AddTaskRow(
                            newTaskText = newTaskText,
                            onNewTaskTextChange = viewModel::onNewTaskTextChange,
                            onAddTask = viewModel::onAddTask
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = if(isToday) "Hôm nay chưa có nhiệm vụ nào" else "Ngày này không có nhiệm vụ",
                            color = Color.Gray,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        // Giảm padding bottom vì không còn bị ảnh che nữa
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = tasks,
                            key = { it.id }
                        ) { task ->
                            val isSelected = task.id in selectedTaskIds
                            TaskItem(
                                modifier = Modifier.animateItem(),
                                task = task,
                                isSelected = isSelected,
                                onClick = { viewModel.onTaskClick(task) },
                                onLongPress = { viewModel.onTaskLongPress(task) },
                                onPinClick = { viewModel.onUnpinClick(task) },
                                onEditClick = { viewModel.onEditClick(task) },
                                selectionColor = selectionColor,
                                textColor = taskTextColor,
                                pinColor = pinColor
                            )
                        }
                    }
                }
            }

            // PHẦN 2: Hình con vịt nằm cố định ở dưới cùng
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp), // Thêm chút khoảng cách
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.duck_celebrate),
                    contentDescription = "Con Vịt Nền",
                    // Có thể điều chỉnh kích thước ở đây nếu muốn
                    modifier = Modifier.size(160.dp)
                )
            }
        }
    }
}

// (Giữ nguyên Composable TaskDateNavigator bên dưới)
@Composable
fun TaskDateNavigator(
    dateText: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, null, tint = Color(0xFF33691E))
        }
        Text(
            text = dateText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF33691E)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF33691E))
        }
    }
}