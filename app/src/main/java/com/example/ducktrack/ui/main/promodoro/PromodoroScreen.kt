package com.example.ducktrack.ui.main.pomodoro
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ducktrack.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// --- 1. Định nghĩa các trạng thái của Pomodoro ---
enum class PomodoroState {
    Ready,
    Running,
    Finished,
    Failed
}

// --- 2. Hàm helper định dạng thời gian ---
fun Long.formatTime(): String {
    val minutes = this / 1000 / 60
    val seconds = (this / 1000) % 60
    return "%02d:%02d".format(minutes, seconds)
}


@Composable
fun PomodoroScreen() {

    // --- 3. Định nghĩa State (Trạng thái) ---
    var pomodoroState by remember { mutableStateOf(PomodoroState.Ready) }
    var focusDurationMillis by remember { mutableStateOf(25 * 60 * 1000L) }
    var breakDurationMillis by remember { mutableStateOf(5 * 60 * 1000L) }
    var remainingTimeMillis by remember { mutableStateOf(focusDurationMillis) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedSeed by remember { mutableStateOf("Cây thường") }

    var showFailedDialog by remember { mutableStateOf(false) }
    var showHarvestDialog by remember { mutableStateOf(false) }


    // --- 4. Định nghĩa Màu sắc ---
    val screenBgColor = Color.White
    val cardBgColor = Color(0xFFFFFFFF)
    val mainGreen = Color(0xFF62B26A)
    val darkGreenText = Color(0xFF3A593F)
    val yellowButton = Color(0xFFF5A623)
    val yellowButtonHarvest = Color(0xFFE0C378)
    val redButton = Color(0xFFD9534F)
    val grayText = Color(0xFF5A5A5A)
    val tealColor = Color(0xFF009688)

    val card1BorderColor = Color(0xFFB0E0F2)
    val card2and3BorderColor = Color(0xFFA5D6A7)

    val textGreen = Color(0xFF08780C)
    val textYellow = Color(0xFFE2970E)

    val dialogBgRed = Color(0xFFF8D7DA)
    val dialogTextRed = Color(0xFF721C24)


    // --- 5. Logic Timer ---
    LaunchedEffect(key1 = isTimerRunning, key2 = remainingTimeMillis) {
        if (isTimerRunning && remainingTimeMillis > 0) {

            while (isActive && remainingTimeMillis > 0) {
                delay(1000L)
                remainingTimeMillis -= 1000L
            }

            if (remainingTimeMillis <= 0) {
                isTimerRunning = false
                pomodoroState = PomodoroState.Finished
            }
        }
    }

    LaunchedEffect(pomodoroState) {
        if (pomodoroState == PomodoroState.Failed) {
            showFailedDialog = true
        }
    }


    // --- 6. Xác định UI dựa trên State ---
    val timerCardText = when (pomodoroState) {
        PomodoroState.Ready -> "Bạn đã sẵn sàng để trồng cây chưa?"
        PomodoroState.Running -> "Cây đang lớn.. Không được mở app 🚫"
        PomodoroState.Finished -> "Hoàn thành! Hãy thu hoạch cây của bạn."
        PomodoroState.Failed -> "Bạn đã dừng lại. Cây đã héo mất..."
    }

    val timeToShowMillis = when (pomodoroState) {
        PomodoroState.Finished -> 0L
        PomodoroState.Ready, PomodoroState.Failed -> focusDurationMillis
        PomodoroState.Running -> remainingTimeMillis
    }

    val (duckImageRes, plantImageRes, statusText) = when (pomodoroState) {
        PomodoroState.Ready -> Triple(R.drawable.duck_waiting, R.drawable.plant_chit, "Đang chờ sẵn sàng...")
        PomodoroState.Running -> Triple(R.drawable.duck_watering, R.drawable.plant_sendling, "Đang chờ tập trung...")
        PomodoroState.Finished -> Triple(R.drawable.duck_happy, R.drawable.plant_grown, "Thu hoạch thôi!")
        PomodoroState.Failed -> Triple(R.drawable.duck_crying, R.drawable.plant_dead, "Ôi không...!!")
    }

    // --- 7. Logic Hiệu ứng "Pop" ---
    val scope = rememberCoroutineScope()
    val duckScale = remember { Animatable(1f) }
    val plantScale = remember { Animatable(1f) }

    LaunchedEffect(pomodoroState) {
        scope.launch {
            duckScale.animateTo(1.2f, tween(150))
            duckScale.animateTo(1f, tween(150))
        }
        scope.launch {
            plantScale.animateTo(1.2f, tween(150))
            plantScale.animateTo(1f, tween(150))
        }
    }


    // --- 8. Giao diện (UI)  ---
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

            // --- Card 1  ---
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
                            onClick = { showSettingsDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("${focusDurationMillis / 60000}m/${breakDurationMillis / 60000}m")
                            Icon(Icons.Default.ArrowDropDown, "Cài đặt thời gian")
                        }

                        val mainButtonColor = if (pomodoroState == PomodoroState.Running) redButton else yellowButton
                        val mainButtonText = if (pomodoroState == PomodoroState.Running) "Dừng lại" else "Sẵn sàng"

                        Button(
                            onClick = {
                                if (pomodoroState == PomodoroState.Running) {
                                    pomodoroState = PomodoroState.Failed
                                    isTimerRunning = false
                                    remainingTimeMillis = focusDurationMillis
                                } else {
                                    pomodoroState = PomodoroState.Running
                                    remainingTimeMillis = focusDurationMillis
                                    isTimerRunning = true
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

            // --- Card 2 ---
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
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SeedChoiceCard(
                            label = "Cây thường",
                            subtitle = "Mặc định",
                            image = painterResource(id = R.drawable.tree_normal),
                            isSelected = selectedSeed == "Cây thường",
                            onClick = { selectedSeed = "Cây thường" },
                            modifier = Modifier.size(width = 100.dp, height = 120.dp),
                            imageHeight = 50.dp
                        )
                    }

                    item {
                        SeedChoiceCard(
                            label = "Cây thông",
                            subtitle = null,
                            image = painterResource(id = R.drawable.tree_pine),
                            isSelected = selectedSeed == "Cây thông",
                            onClick = { selectedSeed = "Cây thông" },
                            modifier = Modifier.size(width = 100.dp, height = 120.dp),
                            imageHeight = 50.dp
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Text Hướng dẫn --
            Text(
                text = buildAnnotatedString {
                    append("Thời gian là ")
                    withStyle(style = SpanStyle(color = textGreen, fontWeight = FontWeight.Bold)) {
                        append("hạt giống")
                    }
                    append(", kỷ luật là ")
                    withStyle(style = SpanStyle(color = textGreen, fontWeight = FontWeight.Bold)) {
                        append("nước tưới")
                    }
                    append(". Bắt đầu phiên Pomodoro để gieo ")
                    withStyle(style = SpanStyle(color = textYellow, fontWeight = FontWeight.Bold)) {
                        append("thói quen")
                    }
                    append(" và gặt hái ")
                    withStyle(style = SpanStyle(color = textYellow, fontWeight = FontWeight.Bold)) {
                        append("thành công")
                    }
                    append(" ngay hôm nay ! 🌤️")
                },
                color = grayText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Card 3 ---
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
                    Text("Hạt giống được chọn : $selectedSeed", color = grayText, fontSize = 14.sp)
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

                    if (pomodoroState == PomodoroState.Finished) {
                        Button(
                            onClick = {
                                showHarvestDialog = true
                            },
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

            Spacer(modifier = Modifier.height(60.dp))
        }

        // --- 9. Dialog Cài đặt  ---
        if (showSettingsDialog) {
            TimeSettingsDialog(
                initialFocusMinutes = (focusDurationMillis / 60000).toString(),
                initialBreakMinutes = (breakDurationMillis / 60000).toString(),
                onDismiss = { showSettingsDialog = false },
                onSettingsApplied = { newFocus, newBreak ->
                    focusDurationMillis = newFocus * 60 * 1000L
                    breakDurationMillis = newBreak * 60 * 1000L
                    if (pomodoroState == PomodoroState.Ready || pomodoroState == PomodoroState.Failed) {
                        remainingTimeMillis = focusDurationMillis
                    }
                    showSettingsDialog = false
                }
            )
        }

        // --- 10. Dialog Thông báo ---
        if (showFailedDialog) {
            AlertDialog(
                onDismissRequest = { showFailedDialog = false },
                containerColor = dialogBgRed,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        "Ôi không cây đã chết mất rồi !!",
                        color = dialogTextRed,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        "Vì bạn đã mở ứng dụng hoặc dừng đột xuất",
                        color = dialogTextRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showFailedDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = redButton)
                    ) {
                        Text("Đã hiểu", color = Color.White)
                    }
                }
            )
        }

        if (showHarvestDialog) {
            AlertDialog(
                onDismissRequest = {
                    showHarvestDialog = false
                    pomodoroState = PomodoroState.Ready
                    remainingTimeMillis = focusDurationMillis
                },
                containerColor = dialogBgRed,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        "Đã thu hoạch cây :3",
                        color = dialogTextRed,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        "Cây sẽ được trồng trên mảnh đất của bạn",
                        color = dialogTextRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showHarvestDialog = false
                            pomodoroState = PomodoroState.Ready
                            remainingTimeMillis = focusDurationMillis
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = mainGreen)
                    ) {
                        Text("Tuyệt vời!", color = Color.White)
                    }
                }
            )
        }
    }
}


/**
 * Composable cho một lựa chọn hạt giống
 */
@Composable
fun SeedChoiceCard(
    label: String,
    subtitle: String?,
    image: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(width = 130.dp, height = 150.dp),
    imageHeight: Dp = 80.dp
) {
    val borderColor = if (isSelected) Color(0xFF62B26A) else Color.Transparent
    val bgColor = if (isSelected) Color(0xFFF0FFF1) else Color.White

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = image,
                contentDescription = label,
                modifier = Modifier
                    .height(imageHeight)
                    .padding(top = 8.dp),
                contentScale = ContentScale.Fit
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                if (subtitle != null) {
                    Text(text = subtitle, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

/**
 * Composable cho Dialog Cài đặt thời gian
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSettingsDialog(
    initialFocusMinutes: String,
    initialBreakMinutes: String,
    onDismiss: () -> Unit,
    onSettingsApplied: (focus: Long, breakTime: Long) -> Unit
) {
    var focusMinutes by remember { mutableStateOf(initialFocusMinutes) }
    var breakMinutes by remember { mutableStateOf(initialBreakMinutes) }

    val mainGreen = Color(0xFF62B26A)
    val darkGreenText = Color(0xFF3A593F)
    val yellowButton = Color(0xFFF5A623)
    val lightGrayButton = Color(0xFFEBF0E7)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFDFCF8),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cài đặt thời gian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = mainGreen)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Thời gian tập trung", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = focusMinutes,
                                onValueChange = { focusMinutes = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.width(80.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = darkGreenText),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Thời gian nghỉ ngơi", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = breakMinutes,
                                onValueChange = { breakMinutes = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.width(80.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = darkGreenText),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val presets = listOf("25/5", "30/5", "45/10")
                        presets.forEach { preset ->
                            val (focus, breakT) = preset.split("/").map { it }

                            val isSelected = (focusMinutes == focus && breakMinutes == breakT)
                            val buttonBgColor = if (isSelected) mainGreen else lightGrayButton
                            val buttonTextColor = if (isSelected) Color.White else darkGreenText

                            Button(
                                onClick = {
                                    focusMinutes = focus
                                    breakMinutes = breakT
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonBgColor,
                                    contentColor = buttonTextColor
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(preset)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val newFocus = focusMinutes.toLongOrNull() ?: 25L
                            val newBreak = breakMinutes.toLongOrNull() ?: 5L
                            onSettingsApplied(newFocus, newBreak)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = yellowButton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Cài đặt", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}


// --- 11. Preview (Xem trước) ---
@Preview(showBackground = true)
@Composable
fun PomodoroScreenPreview() {
    Surface(color = Color.White) {
        PomodoroScreen()
    }
}