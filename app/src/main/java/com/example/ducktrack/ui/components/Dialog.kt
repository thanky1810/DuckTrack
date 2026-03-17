package com.example.ducktrack.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Dialog nhắc nhở user cấp quyền overlay khi họ set limit lần đầu
 * Hiển thị trong MainScreen/DashboardScreen
 */
@Composable
fun OverlayPermissionDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("⚠️", style = MaterialTheme.typography.headlineLarge)
        },
        title = {
            Text(
                "Cần quyền hiển thị",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Để hiển thị cảnh báo khi vượt giới hạn thời gian, " +
                        "DuckTrack cần quyền hiển thị overlay.\n\n" +
                        "Vui lòng vào Cài đặt để cấp quyền."
            )
        },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text("Đi đến Cài đặt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau")
            }
        }
    )
}