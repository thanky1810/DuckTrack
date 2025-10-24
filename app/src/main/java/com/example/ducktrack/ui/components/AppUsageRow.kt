package com.example.ducktrack.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.utils.msToReadable
import kotlin.collections.forEach
import kotlin.let

@Composable
fun AppUsageRow(
    usage: AppUsage,
    limitMinutes: Int?,
    onSetLimit: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(usage.label) },
        supportingContent = {
            val used = msToReadable(usage.totalForegroundMs)
            val limitText = limitMinutes?.let { " • Giới hạn: ${it}m" } ?: ""
            Text("Đã sử dụng: $used$limitText")
        },
        trailingContent = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                listOf(15, 30, 45, 60, 90, 120).forEach { m ->
                    DropdownMenuItem(
                        text = { Text("Giới hạn ${m} phút") },
                        onClick = { showMenu = false; onSetLimit(m) }
                    )
                }
            }
        }
    )
    Divider()
}
