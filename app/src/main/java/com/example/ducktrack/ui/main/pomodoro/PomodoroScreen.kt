package com.example.ducktrack.ui.main.pomodoro

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
import com.example.ducktrack.utils.formatTime
// Import hàm findActivity từ file Extensions.kt (hoặc file utils khác nếu bạn đã tạo)
// Nếu báo đỏ dòng này, hãy chắc chắn bạn đã tạo file utils/Extensions.kt như hướng dẫn trước
import com.example.ducktrack.utils.findActivity
import kotlinx.coroutines.launch

@Composable
fun PomodoroScreen(
    context: Context = LocalContext.current.applicationContext,
    viewModel: PomodoroViewModel,
    onStartFocus: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val duckScale = remember { Animatable(1f) }
    val plantScale = remember { Animatable(1f) }

    val currentContext = LocalContext.current
    // Sử dụng findActivity từ import
    val activity = remember(currentContext) { currentContext.findActivity() }

    DisposableEffect(uiState.isKeepScreenOn, uiState.isTimerRunning) {
        if (uiState.isKeepScreenOn && uiState.isTimerRunning) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(uiState.pomodoroState) {
        scope.launch { duckScale.animateTo(1.2f, tween(150)); duckScale.animateTo(1f, tween(150)) }
        scope.launch { plantScale.animateTo(1.2f, tween(150)); plantScale.animateTo(1f, tween(150)) }
    }

    val focusMin = uiState.focusDurationMillis / 60000
    val breakMin = uiState.breakDurationMillis / 60000
    val sessions = uiState.sessionsBeforeLongBreak
    val longBreakMin = uiState.longBreakDurationMillis / 60000
    val configDisplayString = "$focusMin / $breakMin / $sessions / $longBreakMin"

    // Sử dụng Quadruple để destructure dữ liệu
    val (duckImageRes, plantImageRes, statusText, timerCardText) = when (uiState.pomodoroState) {
        PomodoroState.Ready -> Quadruple(R.drawable.duck_waiting, R.drawable.plant_chit, "Đang chờ...", "Sẵn sàng gieo hạt")
        PomodoroState.Running -> Quadruple(R.drawable.duck_watering, R.drawable.plant_sendling, "Đang tập trung...", "Đang tập trung...")
        PomodoroState.Break -> Quadruple(R.drawable.duck_waiting, R.drawable.plant_chit, "Đang chờ...", "Đến giờ nghỉ ngơi rồi!")
        PomodoroState.Finished -> Quadruple(R.drawable.duck_happy, R.drawable.plant_grown, "Thu hoạch thôi!", "Hoàn thành! Thu hoạch ngay.")
        PomodoroState.Failed -> Quadruple(R.drawable.duck_crying, R.drawable.plant_dead, "Thất bại", "Đã dừng lại.")
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Chế Độ Tập Trung 🌿", color = primaryColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // CARD 1
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val sessionDisplay = if (uiState.pomodoroState == PomodoroState.Break) "Nghỉ giải lao" else "Tập trung / Nghỉ ngắn / Số phiên / Nghỉ dài"
                    Text(text = sessionDisplay, color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = configDisplayString, color = textColor, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(timerCardText, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(onClick = { viewModel.onSettingsClick() }, shape = RoundedCornerShape(12.dp)) { Text("Cấu hình"); Icon(Icons.Default.ArrowDropDown, null) }

                        val (mainButtonColor, mainButtonText) = when (uiState.pomodoroState) {
                            PomodoroState.Running -> Pair(Color(0xFFD9534F), "Đang chạy...")
                            PomodoroState.Break -> Pair(Color(0xFFF5A623), "Nghỉ ngơi")
                            else -> Pair(Color(0xFFF5A623), "Bắt đầu")
                        }

                        Button(
                            onClick = {
                                when (uiState.pomodoroState) {
                                    PomodoroState.Ready, PomodoroState.Finished, PomodoroState.Failed -> { viewModel.startNewSession(); onStartFocus() }
                                    PomodoroState.Running, PomodoroState.Break -> onStartFocus()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = mainButtonColor),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(mainButtonText, color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 2 (Seeds)
            Text("🌱 Lựa chọn hạt giống", color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardColor), border = BorderStroke(1.dp, cardBorder)) {
                LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(uiState.availableSeeds) { seed ->
                        SeedChoiceCard(label = seed.displayName, subtitle = if (seed.cost == 0) "Mặc định" else null, image = painterResource(id = seed.selectionIcon), isSelected = uiState.selectedSeed == seed, onClick = { viewModel.onSeedSelected(seed) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 3 (Status)
            Text("💎 Trồng cây", color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardColor), border = BorderStroke(1.dp, cardBorder)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hạt giống: ${uiState.selectedSeed.displayName}", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(180.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) {
                        Image(painter = painterResource(id = duckImageRes), contentDescription = null, modifier = Modifier.height(100.dp).graphicsLayer(scaleX = duckScale.value, scaleY = duckScale.value), contentScale = ContentScale.Fit)
                        Image(painter = painterResource(id = plantImageRes), contentDescription = null, modifier = Modifier.height(140.dp).graphicsLayer(scaleX = plantScale.value, scaleY = plantScale.value), contentScale = ContentScale.Fit)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.pomodoroState == PomodoroState.Finished) {
                        Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFFFC107), disabledContentColor = Color.White), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Đã thu hoạch +50 ⭐", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    } else {
                        Text(statusText, color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }

        // Dialogs
        if (uiState.showSettingsDialog) {
            TimeSettingsDialog(
                initialFocusMinutes = (uiState.focusDurationMillis / 60000).toString(),
                initialBreakMinutes = (uiState.breakDurationMillis / 60000).toString(),
                initialLongBreakMinutes = (uiState.longBreakDurationMillis / 60000).toString(),
                initialSessions = uiState.sessionsBeforeLongBreak.toString(),
                onDismiss = { viewModel.onDismissSettingsDialog() },
                onSettingsApplied = { f, b, l, s -> viewModel.onSettingsApplied(f, b, l, s) }
            )
        }
        if (uiState.showFailedDialog) FailedDialog(onDismiss = { viewModel.onDismissFailedDialog() })
        if (uiState.showHarvestDialog) HarvestDialog(onDismiss = { viewModel.onDismissHarvestDialog() })
    }
}

// --- CLASS QUADRUPLE (THÊM VÀO ĐÂY ĐỂ TRÁNH LỖI UNRESOLVED REFERENCE) ---
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// --- ĐÃ XÓA HÀM findActivity Ở ĐÂY ĐỂ TRÁNH TRÙNG LẶP ---