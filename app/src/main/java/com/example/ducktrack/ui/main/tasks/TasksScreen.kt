
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel()
) {
    // --- Định nghĩa màu sắc ---
    val screenBgColor = Color(0xFFFFFFFF)
    val titleColor = Color(0xFF62B26A)
    val selectionColor = Color(0xFFD0E3FF)
    val taskTextColor = Color(0xFF424242)
    val pinColor = Color(0xFF62B26A)

    // --- State (Lấy từ ViewModel) ---
    val tasks by viewModel.tasks.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()
    val newTaskText by viewModel.newTaskText.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()

    val isInSelectionMode = selectedTaskIds.isNotEmpty()
    val sortedTasks = tasks

    Scaffold(
        containerColor = screenBgColor,
    ) { innerPadding ->

        // Hiển thị Dialog Sửa nếu taskToEdit không null
        taskToEdit?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = viewModel::onCancelEdit,
                onConfirm = { newText ->
                    viewModel.onConfirmEdit(newText)
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.duck_celebrate),
                contentDescription = "Con Vịt Nền",
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
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Danh Sách Nhiệm Vụ Cần Làm Trong Hôm Nay ",
                    color = titleColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 0.dp)
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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = sortedTasks,
                        key = { it.id }
                    ) { task ->
                        val isSelected = task.id in selectedTaskIds

                        TaskItem(
                            // Thêm modifier này để tạo animation
                            modifier = Modifier.animateItem(),

                            // Các tham số còn lại
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