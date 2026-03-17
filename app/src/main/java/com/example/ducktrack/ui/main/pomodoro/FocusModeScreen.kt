package com.example.ducktrack.ui.main.pomodoro

import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ducktrack.R
import com.example.ducktrack.utils.findActivity
import com.example.ducktrack.utils.formatTime
import kotlinx.coroutines.delay
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

    // --- SỬA LỖI: LẮNG NGHE SỰ KIỆN THOÁT APP (THÔNG MINH HƠN) ---
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Lấy PowerManager để kiểm tra màn hình có đang sáng không
    val powerManager =
        remember { currentContext.getSystemService(Context.POWER_SERVICE) as PowerManager }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Kiểm tra xem màn hình có đang sáng không?
                // isInteractive = true (Màn hình sáng) -> Nghĩa là user bấm Home hoặc đa nhiệm để thoát -> PHẠT
                // isInteractive = false (Màn hình tắt) -> Nghĩa là user khóa máy -> KHÔNG PHẠT
                val isScreenOn = powerManager.isInteractive

                if (isScreenOn && uiState.isTimerRunning && (uiState.pomodoroState == PomodoroState.Running || uiState.pomodoroState == PomodoroState.Break)) {
                    // Chỉ phạt khi màn hình sáng mà app bị dừng (thoát ra ngoài)
                    viewModel.cancelSession(isHomeExit = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // -------------------------------------------------------------

    // Giữ màn hình sáng (Nếu bật trong cài đặt)
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

    // Xử lý nút Back (Phần cứng)
    BackHandler {
        if (uiState.pomodoroState == PomodoroState.Finished || uiState.pomodoroState == PomodoroState.Failed) {
            onExit()
        } else {
            if (uiState.isTimerRunning) viewModel.onMainButtonClick() // Tạm dừng trước
            showConfirmExitDialog = true
        }
    }

    LaunchedEffect(uiState.pomodoroState) {
        // Animation
        scope.launch { duckScale.animateTo(1.2f, tween(150)); duckScale.animateTo(1f, tween(150)) }
        scope.launch {
            plantScale.animateTo(1.2f, tween(150)); plantScale.animateTo(
            1f,
            tween(150)
        )
        }

        // Tự động thoát sau 2s khi hoàn thành (nếu muốn)
        if (uiState.pomodoroState == PomodoroState.Finished) {
            delay(2000)
            viewModel.onDismissHarvestDialog()
            onExit()
        }
    }

    val isBreak = uiState.pomodoroState == PomodoroState.Break
    val currentSessionDisplay = uiState.currentSessionCount + 1
    val targetSession = uiState.sessionsBeforeLongBreak

    val statusText = when {
        uiState.pomodoroState == PomodoroState.Finished -> "HOÀN THÀNH TẤT CẢ!"
        isBreak -> "ĐANG NGHỈ NGƠI"
        else -> "PHIÊN $currentSessionDisplay / $targetSession"
    }

    val (duckImageRes) = when (uiState.pomodoroState) {
        PomodoroState.Ready -> R.drawable.duck_waiting to R.drawable.plant_chit
        PomodoroState.Running -> R.drawable.duck_watering to R.drawable.plant_sendling
        PomodoroState.Break -> R.drawable.duck_waiting to R.drawable.plant_chit
        PomodoroState.Finished -> R.drawable.duck_happy to R.drawable.plant_grown
        PomodoroState.Failed -> R.drawable.duck_crying to R.drawable.plant_dead
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Nút chọn nhạc
        if (uiState.pomodoroState != PomodoroState.Finished && uiState.pomodoroState != PomodoroState.Failed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp)
            ) {
                Button(
                    onClick = { isSoundMenuExpanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0F2F1),
                        contentColor = Color(0xFF00695C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Audiotrack, "Nhạc", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    val label =
                        if (uiState.selectedSound == BackgroundSound.OFF) "Nhạc nền" else uiState.selectedSound.displayName.substringBefore(
                            " "
                        )
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(
                    expanded = isSoundMenuExpanded,
                    onDismissRequest = { isSoundMenuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    BackgroundSound.entries.forEach { sound ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        sound.displayName,
                                        fontWeight = if (sound == uiState.selectedSound) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sound == uiState.selectedSound) Color(0xFF2E7D32) else Color.Black
                                    )
                                    if (sound == uiState.selectedSound) {
                                        Spacer(Modifier.width(8.dp)); Icon(
                                            Icons.Filled.Check,
                                            null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.onSoundSelected(sound); isSoundMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Nội dung chính
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = if (isBreak) Color(0xFFE0F7FA) else if (uiState.pomodoroState == PomodoroState.Finished) Color(
                    0xFFFFD700
                ) else Color(0xFFF1F8E9),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(
                    statusText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isBreak) Color(0xFF0097A7) else if (uiState.pomodoroState == PomodoroState.Finished) Color.Black else Color(
                        0xFF388E3C
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                uiState.remainingTimeMillis.formatTime(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(id = duckImageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(250.dp)
                    .graphicsLayer(scaleX = duckScale.value, scaleY = duckScale.value),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(60.dp))

            // Chỉ hiện nút khi đang chạy hoặc break
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
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEEEEEE))
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            "Stop",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.onMainButtonClick() },
                        modifier = Modifier
                            .height(60.dp)
                            .width(160.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF212121),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Icon(
                            if (uiState.isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showConfirmExitDialog) {
        ExitConfirmDialog(
            onConfirmExit = {
                viewModel.cancelSession(isHomeExit = false)
            },
            onCancel = {}
        )
    }

    if (uiState.showFailedDialog) FailedDialog(onDismiss = { viewModel.onDismissFailedDialog(); onExit() })
    if (uiState.showHarvestDialog) HarvestDialog(onDismiss = { viewModel.onDismissHarvestDialog(); onExit() })
}

@Composable
fun ExitConfirmDialog(onConfirmExit: () -> Unit, onCancel: () -> Unit) {
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
                    "Bạn muốn dừng phiên này?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cây sẽ chết và bạn sẽ bị TRỪ ĐIỂM tương ứng với số phiên chưa hoàn thành!",
                    fontSize = 14.sp,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onConfirmExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        "Kết thúc & Chấp nhận phạt",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color.Black),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Tiếp tục tập trung", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}