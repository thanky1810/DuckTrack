package com.example.ducktrack.ui.main.promodoro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onExit: () -> Unit // Callback để thoát về màn hình chính
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmExitDialog by remember { mutableStateOf(false) }

    // Chặn nút Back của điện thoại để tránh thoát nhầm
    BackHandler {
        showConfirmExitDialog = true
    }

    // Nếu trạng thái quay về Ready hoặc Finished -> Tự động thoát
    LaunchedEffect(uiState.pomodoroState) {
        if (uiState.pomodoroState == PomodoroState.Ready ||
            uiState.pomodoroState == PomodoroState.Finished) {
            onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Nền trắng
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Nút icon góc trên (nếu cần)
            // ...

            Spacer(Modifier.height(40.dp))

            // Đồng hồ đếm ngược (To đùng)
            Text(
                text = uiState.remainingTimeMillis.formatTime(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(Modifier.height(16.dp))

            // Hình con vịt (Duck)
            Image(
                painter = painterResource(id = R.drawable.duck_watering), // Hoặc ảnh duck_eating tùy logic
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
                // Nút Dừng hẳn (Vuông đỏ nhỏ)
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
                        tint = Color(0xFFD32F2F), // Màu đỏ
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Nút Tạm dừng / Tiếp tục (To đen dài)
                Button(
                    onClick = { viewModel.onMainButtonClick() }, // Toggle Pause/Resume
                    modifier = Modifier
                        .height(60.dp)
                        .width(160.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF212121), // Màu đen
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

    // Dialog xác nhận thoát (Giống hình image_a54c5d.png)
    if (showConfirmExitDialog) {
        ExitConfirmDialog(
            onConfirmExit = {
                viewModel.stopTimer(isFailed = true) // Gọi logic dừng (Failed)
                showConfirmExitDialog = false
                onExit()
            },
            onCancel = { showConfirmExitDialog = false }
        )
    }
}

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
                    text = "Thời gian tập trung dưới 5 phút, có tiếp tục không?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Phiên tập trung dưới 5 phút sẽ không được ghi lại",
                    fontSize = 14.sp,
                    color = Color.Red,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // Nút Kết thúc (Đen)
                Button(
                    onClick = onConfirmExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Kết thúc", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                // Nút Tiếp tục (Viền đen, nền trắng)
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Tiếp tục tập trung", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}