package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- CẬP NHẬT TÊN GỌI CHUẨN EISENHOWER ---
enum class EisenhowerType(val title: String, val color: Color, val isImp: Boolean, val isUrg: Boolean) {
    DO_NOW("Quan trọng & Khẩn cấp", Color(0xFFD32F2F), true, true),          // Đỏ
    SCHEDULE("Quan trọng & Không khẩn cấp", Color(0xFF1976D2), true, false), // Xanh dương
    DELEGATE("Không quan trọng & Khẩn cấp", Color(0xFFF57C00), false, true), // Cam
    DELETE("Không quan trọng & Không khẩn cấp", Color(0xFF757575), false, false) // Xám
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInput(
    text: String,
    onTextChange: (String) -> Unit,
    onAddClick: (Boolean, Boolean) -> Unit,
    focusManager: FocusManager
) {
    // Khởi tạo là null để bắt buộc người dùng phải chọn
    var selectedType by remember { mutableStateOf<EisenhowerType?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Nếu chưa chọn, hiển thị màu xám nhạt
    val flagColor = selectedType?.color ?: Color.Gray
    val isButtonEnabled = text.isNotBlank() && selectedType != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- NÚT CHỌN MỨC ĐỘ (CỜ) ---
        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(flagColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Chọn mức độ",
                    tint = flagColor
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                EisenhowerType.values().forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                type.title,
                                color = type.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp // Giảm font xíu vì tên dài
                            )
                        },
                        onClick = {
                            selectedType = type
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Flag, null, tint = type.color)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // --- Ô NHẬP LIỆU ---
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Thêm nhiệm vụ...", fontSize = 14.sp, color = Color.Gray) },
            modifier = Modifier
                .weight(1f)
                .background(Color.White, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = selectedType?.color?.copy(alpha = 0.5f) ?: Color(0xFFE0E0E0),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (isButtonEnabled) {
                    onAddClick(selectedType!!.isImp, selectedType!!.isUrg)
                    focusManager.clearFocus()
                    selectedType = null
                }
            })
        )

        Spacer(modifier = Modifier.width(8.dp))

        // --- NÚT THÊM ---
        Button(
            onClick = {
                if (selectedType != null) {
                    onAddClick(selectedType!!.isImp, selectedType!!.isUrg)
                    focusManager.clearFocus()
                    selectedType = null
                }
            },
            enabled = isButtonEnabled,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF62B26A),
                disabledContainerColor = Color(0xFFBDBDBD),
                contentColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Thêm",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ... (Phần TaskActionRows giữ nguyên)
@Composable
fun TaskActionRows(
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF212121),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFFF5252))
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray))
            IconButton(onClick = onPin) {
                Icon(Icons.Default.PushPin, contentDescription = "Ghim", tint = Color(0xFF62B26A))
            }
        }
    }
}