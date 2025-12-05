package com.example.ducktrack.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.ui.components.ZoomableBarChart // Dùng cái này cho tất cả
import com.example.ducktrack.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: StatisticsViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )

    val last7Days by viewModel.last7DaysData.collectAsState()
    val allTime by viewModel.allTimeData.collectAsState()
    val topAppsChart by viewModel.topAppsChartData.collectAsState()
    val topAppsList by viewModel.topAppsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thống kê sử dụng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.ButtonGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // 1. BIỂU ĐỒ 7 NGÀY (CÓ ZOOM)
                val list7Days = last7Days.entries.toList().reversed().map {
                    AppUsageInfo(it.key, 0, String.format("%.1f giờ", it.value))
                }
                StatsCardWithZoomList(
                    title = "7 Ngày Gần Nhất",
                    subtitle = "Zoom để xem rõ - Tổng thời gian (Giờ)",
                    chartData = last7Days,
                    listData = list7Days,
                    barColor = Color(0xFF42A5F5)
                )

                // 2. BIỂU ĐỒ TOÀN BỘ (CÓ ZOOM)
                val listAllTime = allTime.entries.toList().reversed().map {
                    AppUsageInfo(it.key, 0, String.format("%.1f giờ", it.value))
                }
                StatsCardWithZoomList(
                    title = "Lịch sử theo Tháng",
                    subtitle = "Zoom để xem rõ - 12 tháng qua (Giờ)",
                    chartData = allTime,
                    listData = listAllTime,
                    barColor = Color(0xFFFFA726)
                )

                // 3. TOP 10 ỨNG DỤNG (CÓ ZOOM + TÊN ĐẦY ĐỦ)
                StatsCardWithZoomList(
                    title = "Top 10 Ứng Dụng",
                    subtitle = "Zoom để xem rõ tên ứng dụng",
                    chartData = topAppsChart,
                    listData = topAppsList,
                    barColor = Color(0xFFEC407A)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// --- CARD DÙNG CHUNG CHO TẤT CẢ (HỖ TRỢ ZOOM) ---
@Composable
fun StatsCardWithZoomList(
    title: String,
    subtitle: String,
    chartData: Map<String, Float>,
    listData: List<AppUsageInfo>,
    barColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(20.dp))

            if (chartData.isEmpty() || chartData.values.all { it == 0f }) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có dữ liệu", color = Color.LightGray)
                }
            } else {
                // --- VÙNG VẼ BIỂU ĐỒ ZOOM ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                ) {
                    // Gọi ZoomableBarChart từ file component
                    ZoomableBarChart(
                        data = chartData,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        barColor = barColor
                    )

                    // Chỉ dẫn
                    Text(
                        "↔ Vuốt/Kéo để Zoom",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                // --- DANH SÁCH CHI TIẾT ---
                Text("Chi tiết:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                listData.forEach { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = info.name,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = info.timeDisplay,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    Divider(color = Color(0xFFF5F5F5))
                }
            }
        }
    }
}