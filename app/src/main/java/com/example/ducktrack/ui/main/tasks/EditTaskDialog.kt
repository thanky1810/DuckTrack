// FILE: EditTaskDialog.kt
package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: TodoTask, // Đổi Task -> TodoTask
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newText by remember(task) { mutableStateOf(task.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = { Text("Sửa nhiệm vụ") },
        text = {
            Column {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nội dung nhiệm vụ") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF62B26A),
                        focusedLabelColor = Color(0xFF62B26A)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF62B26A))
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF888888))
            ) { Text("Hủy") }
        }
    )
}