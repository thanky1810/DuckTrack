
package com.example.ducktrack.ui.main.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF62B26A)
                )
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF888888)
                )
            ) {
                Text("Hủy")
            }
        }
    )
}