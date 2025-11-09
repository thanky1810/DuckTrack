package com.example.ducktrack.ui.main.promodoro

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

/**
 * Dialog thông báo Thất bại
 */
@Composable
fun FailedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = redButton)
            ) {
                Text("Đã hiểu", color = Color.White)
            }
        }
    )
}

/**
 * Dialog thông báo Thu hoạch
 */
@Composable
fun HarvestDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = mainGreen)
            ) {
                Text("Tuyệt vời!", color = Color.White)
            }
        }
    )
}