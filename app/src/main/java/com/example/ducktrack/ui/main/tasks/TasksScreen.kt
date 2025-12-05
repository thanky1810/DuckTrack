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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.R

@OptIn(ExperimentalMaterial3Api::class)
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

    // State chế độ xem
    val viewMode by viewModel.viewMode.collectAsState()

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(interactionSource = interactionSource, indication = null) {
                focusManager.clearFocus()
            }
            .padding(horizontal = 16.dp)
    ) {

        // 1. TIÊU ĐỀ + NÚT CHUYỂN ĐỔI
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            // Nút chuyển chế độ (Góc trái)
            IconButton(
                onClick = { viewModel.toggleViewMode() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = if (viewMode == TaskViewMode.LIST) Icons.Default.Dashboard else Icons.Default.List,
                    contentDescription = "Switch View",
                    tint = Color(0xFF62B26A)
                )
            }

            // Tiêu đề (Giữa)
            Text(
                text = "Danh Sách Nhiệm Vụ",
                color = Color(0xFF62B26A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. THANH ĐIỀU HƯỚNG NGÀY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp))
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
                color = Color(0xFF33691E)
            )
            IconButton(onClick = { viewModel.nextDay() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sau", tint = Color(0xFF33691E))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- NỘI DUNG THAY ĐỔI THEO CHẾ ĐỘ ---
        if (viewMode == TaskViewMode.LIST) {
            // === GIAO DIỆN CŨ (LIST) ===

            // 3. Ô NHẬP LIỆU (ĐÃ CẬP NHẬT)
            if (!selectionMode) {
                TaskInput(
                    text = newTaskText,
                    onTextChange = { viewModel.onNewTaskTextChange(it) },
                    // --- SỬA Ở ĐÂY: Nhận thêm imp và urg từ TaskInput ---
                    onAddClick = { imp, urg ->
                        viewModel.onAddTask(isImportant = imp, isUrgent = urg)
                    },
                    focusManager = focusManager
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. DANH SÁCH & VỊT
            Box(modifier = Modifier.weight(1f)) {
                if (tasks.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
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
                            modifier = Modifier.size(150.dp).padding(bottom = 32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
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

                // 5. MENU KHI CHỌN TASK
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
        } else {
            // === GIAO DIỆN MỚI (MA TRẬN EISENHOWER) ===
            EisenhowerMatrixView(tasks, viewModel, focusManager)
        }
    }
}

// --- CÁC COMPOSABLE HỖ TRỢ MA TRẬN ---

@Composable
fun EisenhowerMatrixView(
    tasks: List<TodoTask>,
    viewModel: TasksViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    // Dialog thêm nhanh
    var showAddDialog by remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) }
    var quickTaskText by remember { mutableStateOf("") }

    if (showAddDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = null; quickTaskText = "" },
            title = { Text("Thêm nhiệm vụ", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = quickTaskText,
                    onValueChange = { quickTaskText = it },
                    label = { Text("Nội dung") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (imp, urg) = showAddDialog!!
                        viewModel.onNewTaskTextChange(quickTaskText)
                        viewModel.onAddTask(isImportant = imp, isUrgent = urg)
                        showAddDialog = null
                        quickTaskText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF62B26A))
                ) { Text("Thêm") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = null }) { Text("Hủy") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        // Hàng trên
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            QuadrantBox(
                title = "Quan trọng & Khẩn cấp",
                color = Color(0xFFFFEBEE),
                headerColor = Color(0xFFD32F2F),
                tasks = tasks.filter { it.isImportant && it.isUrgent },
                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp),
                onAddTask = { showAddDialog = Pair(true, true) },
                viewModel = viewModel
            )
            QuadrantBox(
                title = "Quan trọng & Không khẩn cấp",
                color = Color(0xFFE3F2FD),
                headerColor = Color(0xFF1976D2),
                tasks = tasks.filter { it.isImportant && !it.isUrgent },
                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp),
                onAddTask = { showAddDialog = Pair(true, false) },
                viewModel = viewModel
            )
        }
        // Hàng dưới
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            QuadrantBox(
                title = "Không quan trọng & Khẩn cấp",
                color = Color(0xFFFFF3E0),
                headerColor = Color(0xFFF57C00),
                tasks = tasks.filter { !it.isImportant && it.isUrgent },
                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp),
                onAddTask = { showAddDialog = Pair(false, true) },
                viewModel = viewModel
            )
            QuadrantBox(
                title = "Không quan trọng & Không khẩn cấp",
                color = Color(0xFFF1F8E9),
                headerColor = Color(0xFF388E3C),
                tasks = tasks.filter { !it.isImportant && !it.isUrgent },
                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp),
                onAddTask = { showAddDialog = Pair(false, false) },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun QuadrantBox(
    title: String,
    color: Color,
    headerColor: Color,
    tasks: List<TodoTask>,
    modifier: Modifier,
    onAddTask: () -> Unit,
    viewModel: TasksViewModel
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- SỬA Ở ĐÂY: GIẢM CỠ CHỮ + CHO PHÉP XUỐNG DÒNG ---
                Text(
                    text = title,
                    fontSize = 11.sp, // Giảm từ 14.sp xuống 11.sp
                    fontWeight = FontWeight.Bold,
                    color = headerColor,
                    maxLines = 2, // Cho phép xuống dòng tối đa 2 dòng
                    lineHeight = 13.sp, // Chỉnh khoảng cách dòng cho đẹp
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f) // Chiếm không gian còn lại để không đẩy nút Add
                )

                IconButton(onClick = onAddTask, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = headerColor)
                }
            }
            Divider(color = headerColor.copy(alpha = 0.3f))
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Surface(
                        onClick = { viewModel.onTaskClick(task) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(if(task.isCompleted) headerColor else Color.Transparent, RoundedCornerShape(4.dp))
                                    .clickable { viewModel.onTaskClick(task) }
                                    .background(Color.Transparent, RoundedCornerShape(4.dp)) // border fix
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = task.description,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (task.isCompleted) Color.Gray else Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}