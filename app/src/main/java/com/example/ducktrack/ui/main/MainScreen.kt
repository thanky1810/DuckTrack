package com.example.ducktrack.ui.main

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ducktrack.ui.AppRoot.Routes
import com.example.ducktrack.ui.components.AppUsageRow
import com.example.ducktrack.ui.components.PieChart
import com.example.ducktrack.ui.main.garden.GardenScreen
import com.example.ducktrack.ui.main.pomodoro.PomodoroScreen
import com.example.ducktrack.ui.main.settings.SettingsScreen
import com.example.ducktrack.ui.main.tasks.TasksScreen
import com.example.ducktrack.utils.msToReadable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    hasPermission: Boolean,
    onLogout: () -> Unit,

) {



    val bottomNavController = rememberNavController()
    var currentPageTitle by remember { mutableStateOf("Trang chủ") }

    // Ghi chú hiển thị ở header (ví dụ: "Hôm nay: 2h35m")
    var headerNote by remember { mutableStateOf<String?>(null) }

    // Bắt route hiện tại để biết đang ở tab nào
    val currentBackStack by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val topTitle = remember(currentPageTitle, headerNote, currentRoute) {
        // Header giữ “Trang chủ”, phần tổng thời gian hiển thị trong nội dung theo mockup
        currentPageTitle
    }


    Scaffold(
        topBar = {
            MainTopAppBar(
                title = topTitle,
                starCount = 1
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = bottomNavController,
                onTabSelected = { title ->
                    currentPageTitle = title
                    if (bottomNavController.currentDestination?.route != Routes.Dashboard) {
                        headerNote = null
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = bottomNavController,
                startDestination = Routes.Dashboard,
                modifier = Modifier.fillMaxSize()
            ) {
                // Dashboard = UI theo mockup + logic xin quyền
                composable(Routes.Dashboard) {
                    DashboardScreen(
                        hasPermission = hasPermission,
                        onGoToPermission = { mainNavController.navigate(Routes.Permission) },
                        onHeaderNoteChange = { note -> headerNote = note }
                    )
                }

                composable(Routes.Tasks) {
                    LaunchedEffect(Unit) { headerNote = null }
                    TasksScreen()
                }
                composable(Routes.Pomodoro) {
                    LaunchedEffect(Unit) { headerNote = null }
                    PomodoroScreen()
                }
                composable(Routes.Garden) {
                    LaunchedEffect(Unit) { headerNote = null }
                    GardenScreen()
                }
                composable(Routes.Settings) {
                    LaunchedEffect(Unit) { headerNote = null }
                    SettingsScreen(onLogout = onLogout)
                }
            }
        }
    }
}

/**
 * DashboardScreen:
 * - Nếu CHƯA có quyền -> màn xin quyền (theo mockup không có app bar riêng)
 * - Nếu ĐÃ có quyền -> hiển thị layout giống ảnh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    hasPermission: Boolean,
    onGoToPermission: () -> Unit,
    onHeaderNoteChange: (String?) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    if (!hasPermission) {
        LaunchedEffect(Unit) { onHeaderNoteChange(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("DuckTrack cần quyền Usage Access để thống kê thời gian sử dụng ứng dụng của bạn.")
            Spacer(Modifier.height(20.dp))
            Button(onClick = onGoToPermission) { Text("Cấp quyền ngay") }
        }
        return
    }

    // Đã có quyền
    LaunchedEffect(Unit) { vm.load() }

    val totalReadable = remember(vm.totalMs) { msToReadable(vm.totalMs) }
    LaunchedEffect(totalReadable) { onHeaderNoteChange("Hôm nay: $totalReadable") }

    val total = vm.totalMs.toFloat().coerceAtLeast(1f)
    val slices = vm.usages.take(6).map { it.label to (it.totalForegroundMs / total) }

    // ---- Layout giống ảnh ----
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header: Chip vàng "Thời gian sử dụng thiết bị" + số giờ lớn màu xanh
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Pill(
                    text = "Thời gian sử dụng thiết bị",
                    bg = Color(0xFFE0C378),
                    fg = Color(0xFF3A2A11),
                    horizontalPadding = 14.dp,
                    verticalPadding = 6.dp,
                    radius = 14.dp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = totalReadable,
                    color = Color(0xFF2E7D32),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Pie chart
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PieChart(
                    slices = slices,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(240.dp)
                )
            }
        }

        // Date pager: nút trái / pill ngày / nút phải
//        item {
//            DayPagerRow()
//        }

        // Danh sách app usage (AppUsageRow bạn đã có)
        items(vm.usages, key = { it.packageName }) { u ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                AppUsageRow(
                    usage = u,
                    limitMinutes = vm.limits[u.packageName],
                    appIcon = vm.iconFor(u),         // ← truyền icon
                    onSetLimit = { minutes -> vm.setLimit(u.packageName, minutes) }
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* ====================== Helpers (trong file này) ====================== */

@Composable
private fun Pill(
    text: String,
    bg: Color,
    fg: Color,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 4.dp,
    radius: Dp = 12.dp
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayPagerRow(
    modifier: Modifier = Modifier
) {
    val locale = Locale("vi", "VN")
    // ví dụ "Th 4, 1 tháng 10"
    val label = remember {
        val today = LocalDate.now()
        val dow = today.dayOfWeek.value % 7 + 1 // 1..7
        val dowText = "Th $dow"
        val dateText = today.format(DateTimeFormatter.ofPattern("d 'tháng' M", locale))
        "$dowText, $dateText"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(icon = Icons.Filled.ChevronLeft, onClick = { /* TODO: sang ngày trước */ })
        Spacer(Modifier.width(10.dp))
        Pill(
            text = label,
            bg = Color(0xFFE0C378),
            fg = Color(0xFF3A2A11),
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            radius = 12.dp
        )
        Spacer(Modifier.width(10.dp))
        RoundIconButton(icon = Icons.Filled.ChevronRight, onClick = { /* TODO: sang ngày sau */ })
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFFEEE8D6),
        shape = CircleShape,
        shadowElevation = 1.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = null, tint = Color(0xFF3A2A11))
        }
    }
}
