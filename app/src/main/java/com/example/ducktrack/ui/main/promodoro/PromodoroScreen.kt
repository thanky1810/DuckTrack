package com.example.ducktrack.ui.main.promodoro

import android.content.Context
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ducktrack.utils.* // Đảm bảo import đúng các file utils màu sắc của bạn
import kotlinx.coroutines.launch

@Composable
fun PomodoroScreen(
    context: Context = LocalContext.current.applicationContext,
    viewModel: PomodoroViewModel,
    onStartFocus: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- Animation ---
    val scope = rememberCoroutineScope()
    val duckScale = remember { Animatable(1f) }
    val plantScale = remember { Animatable(1f) }

    // Chạy hiệu ứng nảy khi trạng thái thay đổi
    LaunchedEffect(uiState.pomodoroState) {
        scope.launch {
            duckScale.animateTo(1.2f, tween(150))
            duckScale.animateTo(1f, tween(150))
        }
        scope.launch {
            plantScale.animateTo(1.2f, tween(150))
            plantScale.animateTo(1f, tween(150))
        }
    }

    // --- Tính toán hiển thị ---
    val focusMin = uiState.focusDurationMillis / 60000
    val breakMin = uiState.breakDurationMillis / 60000
    val sessions = uiState.sessionsBeforeLongBreak
    val longBreakMin = uiState.longBreakDurationMillis / 60000

    val configDisplayString = "$focusMin / $breakMin / $sessions / $longBreakMin"

    // Text & Image Resources theo trạng thái
    val (duckImageRes, plantImageRes, statusText, timerCardText) = when (uiState.pomodoroState) {
        PomodoroState.Ready -> Quadruple(R.drawable.duck_waiting, R.drawable.plant_chit, "Đang chờ...", "Sẵn sàng gieo hạt")
        PomodoroState.Running -> Quadruple(R.drawable.duck_watering, R.drawable.plant_sendling, "Đang tập trung...", "Đang tập trung...")
        PomodoroState.Break -> Quadruple(R.drawable.duck_waiting, R.drawable.plant_chit, "Đang chờ...", "Đến giờ nghỉ ngơi rồi!")
        PomodoroState.Finished -> Quadruple(R.drawable.duck_happy, R.drawable.plant_grown, "Thu hoạch thôi!", "Hoàn thành! Thu hoạch ngay.")
        PomodoroState.Failed -> Quadruple(R.drawable.duck_crying, R.drawable.plant_dead, "Thất bại", "Đã dừng lại.")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // --- HEADER TITLE ---
            Text(
                text = "Chế Độ Tập Trung 🌿",
                color = darkGreenText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            // --- CARD 1: THÔNG TIN & ĐIỀU KHIỂN ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(2.dp, card1BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hiển thị Phiên hiện tại
                    val sessionDisplay = if (uiState.pomodoroState == PomodoroState.Break)
                        "Nghỉ giải lao"
                    else
                        "Phiên ${uiState.currentSessionCount + 1} / $sessions"

                    Text(sessionDisplay, color = tealColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = configDisplayString,
                        color = darkGreenText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(timerCardText, color = grayText, fontSize = 14.sp, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Button Row ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Nút Cấu hình
                        OutlinedButton(
                            onClick = { viewModel.onSettingsClick() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cấu hình")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }

                        // Logic màu nút và text
                        val (mainButtonColor, mainButtonText) = when (uiState.pomodoroState) {
                            PomodoroState.Running -> Pair(redButton, "Đang chạy...")
                            PomodoroState.Break -> Pair(yellowButton, "Nghỉ ngơi")
                            else -> Pair(yellowButton, "Bắt đầu")
                        }

                        // Nút Bắt đầu / Tiếp tục
                        Button(
                            onClick = {
                                when (uiState.pomodoroState) {
                                    PomodoroState.Ready,
                                    PomodoroState.Finished,
                                    PomodoroState.Failed -> {
                                        viewModel.startNewSession() // Reset và chạy mới
                                        onStartFocus() // Chuyển sang màn hình Timer
                                    }
                                    PomodoroState.Running,
                                    PomodoroState.Break -> {
                                        onStartFocus() // Chỉ chuyển màn hình
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = mainButtonColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(mainButtonText, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CARD 2: CHỌN HẠT GIỐNG ---
            Text(
                "🌱 Lựa chọn hạt giống",
                color = darkGreenText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(2.dp, card2and3BorderColor)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.availableSeeds) { seed ->
                        SeedChoiceCard(
                            label = seed.displayName,
                            subtitle = if (seed.cost == 0) "Mặc định" else null,
                            image = painterResource(id = seed.selectionIcon),
                            isSelected = uiState.selectedSeed == seed,
                            onClick = { viewModel.onSeedSelected(seed) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CARD 3: TRẠNG THÁI CÂY TRỒNG ---
            Text(
                "💎 Trồng cây",
                color = darkGreenText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(2.dp, card2and3BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hạt giống: ${uiState.selectedSeed.displayName}", color = grayText, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hình ảnh Vịt và Cây
                    Row(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Image(
                            painter = painterResource(id = duckImageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .height(100.dp)
                                .graphicsLayer(scaleX = duckScale.value, scaleY = duckScale.value),
                            contentScale = ContentScale.Fit
                        )
                        Image(
                            painter = painterResource(id = plantImageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .height(140.dp)
                                .graphicsLayer(scaleX = plantScale.value, scaleY = plantScale.value),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Trạng thái Hoàn thành / Thu hoạch ---
                    if (uiState.pomodoroState == PomodoroState.Finished) {
                        Button(
                            onClick = { /* Button chỉ để hiển thị trạng thái */ },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFFFFC107), // Màu vàng Gold
                                disabledContentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Đã thu hoạch +50 ⭐", // Thêm icon sao cho sinh động
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(statusText, color = darkGreenText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }

        // --- Dialogs ---
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

// Helper class để return nhiều giá trị trong 'when' expression
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


