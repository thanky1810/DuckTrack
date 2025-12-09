package com.example.ducktrack.ui.main.pomodoro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ducktrack.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSettingsDialog(
    initialFocusMinutes: String,
    initialBreakMinutes: String,
    initialLongBreakMinutes: String,
    initialSessions: String,
    onDismiss: () -> Unit,
    onSettingsApplied: (focus: Long, breakTime: Long, longBreak: Long, sessions: Int) -> Unit
) {
    var focusMinutes by remember { mutableStateOf(initialFocusMinutes) }
    var breakMinutes by remember { mutableStateOf(initialBreakMinutes) }
    var longBreakMinutes by remember { mutableStateOf(initialLongBreakMinutes) }
    var sessionsInput by remember { mutableStateOf(initialSessions) }

    // State lưu thông báo lỗi
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    Text("Cài đặt Pomodoro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = mainGreen)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Hàng 1
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Tập trung", fontSize = 12.sp, color = grayText)
                            OutlinedTextField(
                                value = focusMinutes,
                                // Chỉ cho nhập số
                                onValueChange = { if (it.all { char -> char.isDigit() }) focusMinutes = it },
                                modifier = Modifier.fillMaxWidth(0.9f),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                singleLine = true
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Nghỉ ngắn", fontSize = 12.sp, color = grayText)
                            OutlinedTextField(
                                value = breakMinutes,
                                onValueChange = { if (it.all { char -> char.isDigit() }) breakMinutes = it },
                                modifier = Modifier.fillMaxWidth(0.9f),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Hàng 2
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Số phiên", fontSize = 12.sp, color = grayText)
                            OutlinedTextField(
                                value = sessionsInput,
                                onValueChange = { if (it.all { char -> char.isDigit() }) sessionsInput = it },
                                modifier = Modifier.fillMaxWidth(0.9f),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                singleLine = true
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Nghỉ dài", fontSize = 12.sp, color = grayText)
                            OutlinedTextField(
                                value = longBreakMinutes,
                                onValueChange = { if (it.all { char -> char.isDigit() }) longBreakMinutes = it },
                                modifier = Modifier.fillMaxWidth(0.9f),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                singleLine = true
                            )
                        }
                    }

                    // HIỂN THỊ LỖI (NẾU CÓ)
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // PRESETS
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        val presets = listOf(
                            Triple("25/5/4/15", "25", "5"),
                            Triple("30/5/4/15", "30", "5"),
                            Triple("45/10/4/20", "45", "10")
                        )
                        presets.forEach { (fullStr, focusStr, breakStr) ->
                            val parts = fullStr.split("/")
                            val f = parts[0]; val b = parts[1]; val s = parts[2]; val l = parts[3]
                            val isSelected = (focusMinutes == f && breakMinutes == b && sessionsInput == s && longBreakMinutes == l)
                            val buttonBgColor = if (isSelected) mainGreen else lightGrayButton
                            val buttonTextColor = if (isSelected) Color.White else darkGreenText

                            Button(
                                onClick = {
                                    focusMinutes = f; breakMinutes = b; sessionsInput = s; longBreakMinutes = l
                                    errorMessage = null // Xóa lỗi khi chọn preset
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonBgColor, contentColor = buttonTextColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(fullStr, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            // --- KIỂM TRA ĐẦU VÀO (VALIDATION) ---
                            if (focusMinutes.isBlank() || breakMinutes.isBlank() || sessionsInput.isBlank() || longBreakMinutes.isBlank()) {
                                errorMessage = "Vui lòng nhập đầy đủ thông tin!"
                                return@Button
                            }

                            val newFocus = focusMinutes.toLongOrNull() ?: 0L
                            val newBreak = breakMinutes.toLongOrNull() ?: 0L
                            val newLongBreak = longBreakMinutes.toLongOrNull() ?: 0L
                            val newSessions = sessionsInput.toIntOrNull() ?: 0

                            if (newFocus <= 0 || newBreak <= 0 || newLongBreak <= 0 || newSessions <= 0) {
                                errorMessage = "Thời gian và số phiên phải lớn hơn 0!"
                                return@Button
                            }

                            // Nếu hợp lệ thì lưu và đóng
                            onSettingsApplied(newFocus, newBreak, newLongBreak, newSessions)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = yellowButton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Lưu cấu hình", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng", tint = Color.Gray)
                }
            }
        }
    }
}

// ... (Giữ nguyên FailedDialog, HarvestDialog, NotEnoughPointsDialog bên dưới)
@Composable
fun FailedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = dialogBgRed, shape = RoundedCornerShape(16.dp),
        title = { Text("Ôi không cây đã chết mất rồi !!", color = dialogTextRed, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = { Text("Vì bạn đã mở ứng dụng hoặc dừng đột xuất", color = dialogTextRed, textAlign = TextAlign.Center) },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = redButton)) { Text("Đã hiểu", color = Color.White) } }
    )
}
@Composable
fun HarvestDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = dialogBgRed, shape = RoundedCornerShape(16.dp),
        title = { Text("Đã thu hoạch cây :3", color = dialogTextRed, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = { Text("Cây sẽ được trồng trên mảnh đất của bạn", color = dialogTextRed, textAlign = TextAlign.Center) },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = mainGreen)) { Text("Tuyệt vời!", color = Color.White) } }
    )
}
@Composable
fun NotEnoughPointsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(16.dp),
        title = { Text("Không đủ điểm 😥", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = { Text("Bạn không đủ điểm sao 🌟 để đổi vật phẩm này.", textAlign = TextAlign.Center) },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = mainGreen)) { Text("Đã hiểu", color = Color.White) } }
    )
}