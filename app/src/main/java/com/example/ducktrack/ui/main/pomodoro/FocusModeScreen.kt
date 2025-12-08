package com.example.ducktrack.ui.main.pomodoro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ducktrack.R
import com.example.ducktrack.utils.formatTime
import com.example.ducktrack.utils.findActivity // Import hàm tiện ích
import kotlinx.coroutines.launch

@Composable
fun FocusModeScreen(
    viewModel: PomodoroViewModel,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val duckScale = remember { Animatable(1f) }
    val plantScale = remember { Animatable(1f) }

    val currentContext = LocalContext.current
    val activity = remember(currentContext) { currentContext.findActivity() }

    var showConfirmExitDialog by remember { mutableStateOf(false) }
    var isSoundMenuExpanded by remember { mutableStateOf(false) }

    // --- LẮNG NGHE SỰ KIỆN THOÁT APP (BẤM HOME) ---
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Nếu Timer đang chạy mà thoát ra -> Phạt
                if (uiState.isTimerRunning && (uiState.pomodoroState == PomodoroState.Running || uiState.pomodoroState == PomodoroState.Break)) {
                    viewModel.cancelSession(isHomeExit = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Giữ màn hình sáng
    DisposableEffect(uiState.isKeepScreenOn, uiState.isTimerRunning) {
        if (uiState.isKeepScreenOn && uiState.isTimerRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Nút Back
    BackHandler {
        if (uiState.pomodoroState == PomodoroState.Finished || uiState.pomodoroState == PomodoroState.Failed) {
            onExit()
        } else {
            if (uiState.isTimerRunning) viewModel.onMainButtonClick()
            showConfirmExitDialog = true
        }
    }

    LaunchedEffect(uiState.pomodoroState) {
        scope.launch { duckScale.animateTo(1.2f, tween(150)); duckScale.animateTo(1f, tween(150)) }
        scope.launch { plantScale.animateTo(1.2f, tween(150)); plantScale.animateTo(1f, tween(150)) }
    }

    val isBreak = uiState.pomodoroState == PomodoroState.Break
    val currentSessionDisplay = uiState.currentSessionCount + 1
    val targetSession = uiState.sessionsBeforeLongBreak
    val statusText = if (isBreak) "ĐANG NGHỈ NGƠI" else "PHIÊN $currentSessionDisplay / $targetSession"

    val (duckImageRes, plantImageRes) = when (uiState.pomodoroState) {
        PomodoroState.Ready -> R.drawable.duck_waiting to R.drawable.plant_chit
        PomodoroState.Running -> R.drawable.duck_watering to R.drawable.plant_sendling
        PomodoroState.Break -> R.drawable.duck_waiting to R.drawable.plant_chit
        PomodoroState.Finished -> R.drawable.duck_happy to R.drawable.plant_grown
        PomodoroState.Failed -> R.drawable.duck_crying to R.drawable.plant_dead
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Nút chọn nhạc
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 20.dp)
        ) {
            Button(
                onClick = { isSoundMenuExpanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2F1), contentColor = Color(0xFF00695C)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Audiotrack, "Nhạc", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                val label = if (uiState.selectedSound == BackgroundSound.OFF) "Nhạc nền" else uiState.selectedSound.displayName.substringBefore(" ")
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = isSoundMenuExpanded,
                onDismissRequest = { isSoundMenuExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                BackgroundSound.values().forEach { sound ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sound.displayName, fontWeight = if (sound == uiState.selectedSound) FontWeight.Bold else FontWeight.Normal, color = if (sound == uiState.selectedSound) Color(0xFF2E7D32) else Color.Black)
                                if (sound == uiState.selectedSound) { Spacer(Modifier.width(8.dp)); Icon(Icons.Filled.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp)) }
                            }
                        },
                        onClick = { viewModel.onSoundSelected(sound); isSoundMenuExpanded = false }
                    )
                }
            }
        }

        // Nội dung chính
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = if (isBreak) Color(0xFFE0F7FA) else Color(0xFFF1F8E9),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(statusText, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isBreak) Color(0xFF0097A7) else Color(0xFF388E3C), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(uiState.remainingTimeMillis.formatTime(), fontSize = 80.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(id = duckImageRes),
                contentDescription = null,
                modifier = Modifier.size(250.dp).graphicsLayer(scaleX = duckScale.value, scaleY = duckScale.value),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(60.dp))

            // Chỉ hiện nút khi chưa kết thúc/thất bại
            if (uiState.pomodoroState != PomodoroState.Finished && uiState.pomodoroState != PomodoroState.Failed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (uiState.isTimerRunning) viewModel.onMainButtonClick()
                            showConfirmExitDialog = true
                        },
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFEEEEEE))
                    ) {
                        Icon(Icons.Filled.Stop, "Stop", tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                    }

                    Button(
                        onClick = { viewModel.onMainButtonClick() },
                        modifier = Modifier.height(60.dp).width(160.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121), contentColor = Color.White),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Icon(if (uiState.isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }

    // DIALOGS

    if (showConfirmExitDialog) {
        ExitConfirmDialog(
            onConfirmExit = {
                // 1. Trừ điểm
                viewModel.cancelSession(isHomeExit = false)
                // 2. Tắt dialog confirm
                showConfirmExitDialog = false
                // LƯU Ý: KHÔNG GỌI onExit() Ở ĐÂY
                // Để nó tự chuyển sang FailedDialog
            },
            onCancel = { showConfirmExitDialog = false }
        )
    }

    // Khi bị Failed (do thoát hoặc bấm hủy), dialog này sẽ hiện lên
    if (uiState.showFailedDialog) {
        FailedDialog(onDismiss = {
            viewModel.onDismissFailedDialog()
            // 3. CHỈ THOÁT KHI BẤM "ĐÃ HIỂU"
            onExit()
        })
    }

    if (uiState.showHarvestDialog) {
        HarvestDialog(onDismiss = {
            viewModel.onDismissHarvestDialog()
            onExit()
        })
    }
}

// ... (ExitConfirmDialog giữ nguyên)
@Composable
fun ExitConfirmDialog(onConfirmExit: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bạn muốn dừng phiên này?", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Cây sẽ chết và bạn sẽ bị TRỪ ĐIỂM tương ứng với số phiên chưa hoàn thành!", fontSize = 14.sp, color = Color.Red, textAlign = TextAlign.Center, lineHeight = 20.sp)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onConfirmExit, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Kết thúc & Chấp nhận phạt", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onCancel, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black), border = BorderStroke(1.dp, Color.Black), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Tiếp tục tập trung", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}