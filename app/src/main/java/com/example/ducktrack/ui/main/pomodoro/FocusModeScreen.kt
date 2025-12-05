package com.example.ducktrack.ui.main.pomodoro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack // Icon nốt nhạc
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ducktrack.R
import com.example.ducktrack.utils.formatTime

@Composable
fun FocusModeScreen(
    viewModel: PomodoroViewModel,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmExitDialog by remember { mutableStateOf(false) }

    // State cho Dropdown Menu nhạc
    var isSoundMenuExpanded by remember { mutableStateOf(false) }

    BackHandler {
        showConfirmExitDialog = true
    }

    LaunchedEffect(uiState.promodoroState) {
        if (uiState.promodoroState == PromodoroState.Finished) {
            onExit()
        }
    }

    val isBreak = uiState.promodoroState == PromodoroState.Break
    val currentSessionDisplay = uiState.currentSessionCount + 1
    val targetSession = uiState.sessionsBeforeLongBreak

    val statusText = if (isBreak) {
        "ĐANG NGHỈ NGƠI"
    } else {
        "PHIÊN $currentSessionDisplay / $targetSession"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // --- NÚT CHỌN NHẠC (Góc trên phải) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
        ) {
            Button(
                onClick = { isSoundMenuExpanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0F2F1), // Màu xanh nhạt
                    contentColor = Color(0xFF00695C)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Audiotrack, contentDescription = "Chọn nhạc", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                // Hiển thị tên bài hát đang chọn (nếu không phải OFF thì hiện tên ngắn gọn)
                val label = if (uiState.selectedSound == BackgroundSound.OFF) "Nhạc nền" else uiState.selectedSound.displayName.substringBefore(" ")
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = isSoundMenuExpanded,
                onDismissRequest = { isSoundMenuExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                BackgroundSound.values().forEach { sound ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sound.displayName,
                                    fontWeight = if (sound == uiState.selectedSound) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sound == uiState.selectedSound) Color(0xFF2E7D32) else Color.Black
                                )
                                if (sound == uiState.selectedSound) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            viewModel.onSoundSelected(sound)
                            isSoundMenuExpanded = false
                        }
                    )
                }
            }
        }

        // --- NỘI DUNG CHÍNH (Giữ nguyên) ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = if (isBreak) Color(0xFFE0F7FA) else Color(0xFFF1F8E9),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isBreak) Color(0xFF0097A7) else Color(0xFF388E3C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = uiState.remainingTimeMillis.formatTime(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(id = if (isBreak) R.drawable.duck_waiting else R.drawable.duck_watering),
                contentDescription = null,
                modifier = Modifier.size(250.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(60.dp))

            // Hàng nút điều khiển
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(
                    onClick = { showConfirmExitDialog = true },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEEEEEE))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Button(
                    onClick = { viewModel.onMainButtonClick() },
                    modifier = Modifier.height(60.dp).width(160.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF212121),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                }
            }
        }
    }

    if (showConfirmExitDialog) {
        ExitConfirmDialog(
            onConfirmExit = {
                viewModel.stopTimer(isFailed = true)
                showConfirmExitDialog = false
                onExit()
            },
            onCancel = { showConfirmExitDialog = false }
        )
    }
}

// ExitConfirmDialog giữ nguyên
@Composable
fun ExitConfirmDialog(
    onConfirmExit: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bạn muốn dừng phiên này?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Thời gian đã tập trung sẽ không được tính điểm.",
                    fontSize = 14.sp,
                    color = Color.Red,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onConfirmExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Kết thúc", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Tiếp tục", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}