package com.example.ducktrack.ui.main.garden

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.MyApplication
import com.example.ducktrack.ui.main.ViewModelFactory
import com.example.ducktrack.ui.main.pomodoro.NotEnoughPointsDialog
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun GardenScreen(
    onNavigateToPomodoro: () -> Unit,
    context: Context = LocalContext.current.applicationContext,
    viewModel: GardenViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as MyApplication)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTree by remember { mutableStateOf<GrownTreeUI?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
    ) {
        // ... (Tiêu đề, Navigator, Grid, Store giữ nguyên như cũ) ...
        // (Để code gọn, tôi paste lại phần chính bên dưới)
        Text(
            text = "Vườn cây 🌳",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        GardenDateNavigator(
            dateText = uiState.dateText,
            isNextEnabled = !uiState.isToday,
            onPrev = { viewModel.previousDay() },
            onNext = { viewModel.nextDay() })
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.treesForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { EmptyGardenState(isToday = uiState.isToday, onPlantNow = onNavigateToPomodoro) }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(uiState.treesForSelectedDate) { tree ->
                        GrownTreeItem(
                            tree = tree,
                            onClick = { selectedTree = tree })
                    }
                    item { PlantMoreButton(onClick = onNavigateToPomodoro) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Cửa hàng hạt giống", fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
        Spacer(Modifier.height(8.dp))
        SeedStoreCard(
            storeItems = uiState.storeItems,
            onItemClick = { storeItem -> if (!storeItem.isUnlocked) viewModel.onUnlockSeed(storeItem.seedType) },
            modifier = Modifier.height(250.dp)
        )
    }

    if (selectedTree != null) {
        TreeDetailDialog(
            tree = selectedTree!!,
            onDismiss = { }
        )
    }
    if (uiState.showNotEnoughPointsDialog) {
        NotEnoughPointsDialog(onDismiss = { viewModel.onDismissNotEnoughPointsDialog() })
    }
}

@Composable
fun TreeDetailDialog(tree: GrownTreeUI, onDismiss: () -> Unit) {
    val timeFormat = SimpleDateFormat("hh:mm:ss aa - dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
    val dateString = timeFormat.format(tree.plantedAt)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        "Chi tiết cây trồng",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(24.dp)
                    ) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                }
                Spacer(Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = tree.seedType.grownIcon),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(Modifier.height(24.dp))

                DetailRow(label = "Loại cây:", value = tree.seedType.displayName)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE)
                )

                // --- HIỂN THỊ THÔNG TIN MỚI ---
                DetailRow(label = "Bộ phiên:", value = "Bộ thứ ${tree.setIndex}")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE)
                )

                val totalSessionsInSet = try {
                    tree.config.split("/")[2]
                } catch (_: Exception) {
                    "?"
                }
                DetailRow(
                    label = "Phiên trong bộ:",
                    value = "${tree.sessionIndex} / $totalSessionsInSet"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE)
                )
                // -----------------------------

                DetailRow(label = "Thời gian trồng:", value = dateString)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE)
                )
                DetailRow(label = "Thông số (F/S/N/L):", value = tree.config)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE)
                )
                DetailRow(label = "Kết quả:", value = "Hoàn thành", valueColor = Color(0xFF2E7D32))
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Đóng") }
            }
        }
    }
}

// ... (Các hàm DetailRow, GrownTreeItem, GardenDateNavigator... giữ nguyên) ...
@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp); Text(
        value,
        color = valueColor,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
    }
}

@Composable
fun GrownTreeItem(tree: GrownTreeUI, onClick: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEDC8)),
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = tree.seedType.grownIcon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GardenDateNavigator(
    dateText: String,
    isNextEnabled: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                Icons.Filled.ChevronLeft,
                null,
                tint = Color(0xFF33691E)
            )
        }; Text(
        text = dateText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF33691E)
    ); IconButton(onClick = onNext, enabled = isNextEnabled) {
        Icon(
            Icons.Filled.ChevronRight,
            null,
            tint = if (isNextEnabled) Color(0xFF33691E) else Color.Transparent
        )
    }
    }
}

@Composable
fun PlantMoreButton(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
        modifier = Modifier.aspectRatio(1f),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("➕", fontSize = 24.sp); Text(
                "Trồng thêm",
                fontSize = 12.sp,
                color = Color(0xFFFBC02D)
            )
            }
        }
    }
}

@Composable
fun EmptyGardenState(isToday: Boolean, onPlantNow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val text = if (isToday) "Chưa có cây nào hôm nay 🌱" else "Không có cây nào hôm đó 🍂"; Text(
        text,
        color = Color.Gray
    ); Spacer(Modifier.height(8.dp)); Button(
        onClick = onPlantNow,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
    ) { Text("Bắt đầu tập trung ngay") }
    }
}