// FILE: ui/main/settings/ExportHistoryScreen.kt
package com.example.ducktrack.ui.main.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.ui.AppRoot.AuthViewModelFactory
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.theme.AppColors
import com.example.ducktrack.utils.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportHistoryScreen(
    onBack: () -> Unit,
    exportViewModel: ExportViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(LocalContext.current.applicationContext))
) {
    val context = LocalContext.current
    val historyList by exportViewModel.historyList.collectAsState()
    val latestItem by exportViewModel.latestItem.collectAsState()

    val isLoading by exportViewModel.isLoading.collectAsState()
    val isExporting by authViewModel.isUploading.collectAsState()

    // Hàm thực hiện xuất file mới
    val performExport = {
        authViewModel.exportData(context,
            onSuccess = { path ->
                Toast.makeText(context, "Xuất thành công!", Toast.LENGTH_SHORT).show()
                exportViewModel.onExportSuccess(context, path)
            },
            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
        )
    }

    LaunchedEffect(Unit) {
        exportViewModel.loadHistory(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử xuất dữ liệu", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { performExport() },
                containerColor = AppColors.ButtonGreen,
                contentColor = Color.White
            ) {
                if (isExporting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Icon(Icons.Default.Add, "New")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- THẺ BẢN XUẤT GẦN NHẤT ---
            if (latestItem != null) {
                LatestExportCard(
                    item = latestItem!!,
                    onShare = { exportViewModel.shareFile(context, latestItem!!) },
                    onReExport = { performExport() }
                )
            }

            // --- DANH SÁCH CHI TIẾT ---
            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có lịch sử", color = Color.Gray)
                }
            } else {
                Text(
                    "Danh sách chi tiết:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList) { item ->
                        HistoryItemRow(
                            item = item,
                            onShare = { exportViewModel.shareFile(context, item) }
                        )
                    }
                }
            }
        }
    }
}

// UI: Card hiển thị file mới nhất
@Composable
fun LatestExportCard(item: HistoryItem, onShare: () -> Unit, onReExport: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFF1976D2))
                Spacer(Modifier.width(8.dp))
                Text("BẢN XUẤT GẦN NHẤT", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))

            Text(item.fileName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Đường dẫn: ...${item.filePath.takeLast(30)}", fontSize = 12.sp, color = Color.Gray)

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Nút Chia sẻ
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Chia sẻ")
                }

                // Nút Tải lại (Tạo mới)
                Button(
                    onClick = onReExport,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tải lại")
                }
            }
        }
    }
}

// UI: Một dòng trong lịch sử
@Composable
fun HistoryItemRow(item: HistoryItem, onShare: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val prettyPath = item.filePath.replace("/storage/emulated/0", "Bộ nhớ trong")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.fileName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(dateFormat.format(Date(item.dateModified)), fontSize = 11.sp, color = Color.Gray)
                    Text(" • ${item.fileSize}", fontSize = 11.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(2.dp))
                Text(prettyPath, fontSize = 10.sp, color = Color.LightGray, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, null, tint = AppColors.TextGreen)
            }
        }
    }
}