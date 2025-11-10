package com.example.ducktrack.ui.main.promodoro

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.MyApplication
import com.example.ducktrack.R
import com.example.ducktrack.ui.main.ViewModelFactory
import com.example.ducktrack.utils.*
import kotlinx.coroutines.launch

@Composable
fun PomodoroScreen(
    // Thay vì viewModel() mặc định:
    // viewModel: PomodoroViewModel = viewModel()
    // Chúng ta dùng Factory:
    context: Context = LocalContext.current.applicationContext,
    viewModel: PomodoroViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as MyApplication)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- Logic cho UI Animation ---
    val scope = rememberCoroutineScope()
    val duckScale = remember { Animatable(1f) }
    val plantScale = remember { Animatable(1f) }

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

    // --- Tính toán các giá trị hiển thị ---
    val timerCardText = when (uiState.pomodoroState) {
        PomodoroState.Ready -> "Bạn đã sẵn sàng để trồng cây chưa?"
        PomodoroState.Running -> "Cây đang lớn.. Không được mở app 🚫"
        PomodoroState.Break -> "Thư giãn một chút nhé!"
        PomodoroState.Finished -> "Hoàn thành! Hãy thu hoạch cây của bạn."
        PomodoroState.Failed -> "Bạn đã dừng lại. Cây đã héo mất..."
    }

    val timeToShowMillis = when (uiState.pomodoroState) {
        PomodoroState.Finished -> 0L
        PomodoroState.Ready, PomodoroState.Failed -> uiState.focusDurationMillis
        PomodoroState.Running -> uiState.remainingTimeMillis
        PomodoroState.Break -> uiState.remainingTimeMillis
    }
    // Các file drawable:
    // duck_waiting, plant_chit (nảy mầm)
    // duck_watering, plant_sendling (cây non)
    // duck_happy, plant_grown (cây lớn)
    // duck_crying, plant_dead (cây héo)
    val (duckImageRes, plantImageRes, statusText) = when (uiState.pomodoroState) {
        PomodoroState.Ready, PomodoroState.Break -> Triple(R.drawable.duck_waiting, R.drawable.plant_chit, "Đang chờ sẵn sàng...")
        PomodoroState.Running -> Triple(R.drawable.duck_watering, R.drawable.plant_sendling, "Đang chờ tập trung...")
        PomodoroState.Finished -> Triple(R.drawable.duck_happy, R.drawable.plant_grown, "Thu hoạch thôi!")
        PomodoroState.Failed -> Triple(R.drawable.duck_crying, R.drawable.plant_dead, "Ôi không...!!")
    }

    // --- Giao diện (UI) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor) // Nền trắng để cuộn
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp), // Padding cho nội dung
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // --- Card 1 (Timer) ---
            Text(
                text = "Chế Độ Tập Trung và Trồng cây 🌿",
                color = darkGreenText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                    Text("Thời Gian Hiện Tại", color = tealColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(timeToShowMillis.formatTime(), color = darkGreenText, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                    Text(timerCardText, color = grayText, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onSettingsClick() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("${uiState.focusDurationMillis / 60000}m/${uiState.breakDurationMillis / 60000}m")
                            Icon(Icons.Default.ArrowDropDown, "Cài đặt thời gian")
                        }

                        val (mainButtonColor, mainButtonText) = when (uiState.pomodoroState) {
                            PomodoroState.Running -> Pair(redButton, "Dừng lại")
                            PomodoroState.Break -> Pair(yellowButton, "Nghỉ ngơi")
                            else -> Pair(yellowButton, "Sẵn sàng")
                        }

                        Button(
                            onClick = { viewModel.onMainButtonClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = mainButtonColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(mainButtonText, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Card 2 (Lựa chọn hạt giống) ---
            Text(
                text = "🌱 Lựa chọn hạt giống",
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
                // Render động dựa trên các cây đã mở khóa
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
                            onClick = { viewModel.onSeedSelected(seed) },
                            modifier = Modifier.size(width = 100.dp, height = 120.dp),
                            imageHeight = 50.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Text Hướng dẫn ---
            Text(
                text = buildAnnotatedString {
                    append("Thời gian là ")
                    withStyle(style = SpanStyle(color = textGreen, fontWeight = FontWeight.Bold)) { append("hạt giống") }
                    append(", kỷ luật là ")
                    withStyle(style = SpanStyle(color = textGreen, fontWeight = FontWeight.Bold)) { append("nước tưới") }
                    append(". Bắt đầu phiên Pomodoro để gieo ")
                    withStyle(style = SpanStyle(color = textYellow, fontWeight = FontWeight.Bold)) { append("thói quen") }
                    append(" và gặt hái ")
                    withStyle(style = SpanStyle(color = textYellow, fontWeight = FontWeight.Bold)) { append("thành công") }
                    append(" ngay hôm nay ! 🌤️")
                },
                color = grayText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Card 3 (Trồng cây) ---
            Text(
                text = "💎 Trồng cây và thu hoạch",
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
                    Text("Hạt giống được chọn : ${uiState.selectedSeed.displayName}", color = grayText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Image(
                            painter = painterResource(id = duckImageRes),
                            contentDescription = "Trạng thái Vịt",
                            modifier = Modifier
                                .height(110.dp)
                                .graphicsLayer(scaleX = duckScale.value, scaleY = duckScale.value),
                            contentScale = ContentScale.Fit
                        )
                        Image(
                            painter = painterResource(id = plantImageRes),
                            contentDescription = "Trạng thái Cây",
                            modifier = Modifier
                                .height(160.dp)
                                .graphicsLayer(scaleX = plantScale.value, scaleY = plantScale.value),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.pomodoroState == PomodoroState.Finished) {
                        Button(
                            onClick = { viewModel.onHarvestClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = yellowButtonHarvest),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Thu hoạch ngay +50 🌟", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(statusText, color = darkGreenText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp)) // Thêm đệm dưới
        }

        // --- 9. Các Dialog ---
        if (uiState.showSettingsDialog) {
            TimeSettingsDialog(
                initialFocusMinutes = (uiState.focusDurationMillis / 60000).toString(),
                initialBreakMinutes = (uiState.breakDurationMillis / 60000).toString(),
                onDismiss = { viewModel.onDismissSettingsDialog() },
                onSettingsApplied = { newFocus, newBreak ->
                    viewModel.onSettingsApplied(newFocus, newBreak)
                }
            )
        }

        if (uiState.showFailedDialog) {
            FailedDialog(
                onDismiss = { viewModel.onDismissFailedDialog() }
            )
        }

        if (uiState.showHarvestDialog) {
            HarvestDialog(
                onDismiss = { viewModel.onDismissHarvestDialog() }
            )
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun PomodoroScreenPreview() {
    Surface(color = Color.White) {
        PomodoroScreen()
    }
}