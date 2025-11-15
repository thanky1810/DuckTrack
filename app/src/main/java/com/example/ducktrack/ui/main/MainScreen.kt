package com.example.ducktrack.ui.main

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.ui.AppRoot.Routes
import com.example.ducktrack.ui.components.AppUsageRow
import com.example.ducktrack.ui.components.OverlayPermissionDialog
import com.example.ducktrack.ui.components.PieChart
import com.example.ducktrack.ui.main.garden.GardenScreen
import com.example.ducktrack.ui.main.promodoro.PomodoroScreen
import com.example.ducktrack.ui.main.settings.SettingsScreen
import com.example.ducktrack.ui.main.tasks.TasksScreen
import com.example.ducktrack.ui.theme.AppColors
import com.example.ducktrack.utils.PermissionHelper
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
        // Header giữ "Trang chủ", phần tổng thời gian hiển thị trong nội dung theo mockup
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
                    // Thêm key với route để force recomposition
                    key(Routes.Pomodoro + currentRoute) {
                        PomodoroScreen()
                    }
                }
                composable(Routes.Garden) {
                    LaunchedEffect(Unit) { headerNote = null }
                    // Thêm key với route để force recomposition
                    key(Routes.Garden + currentRoute) {
                        GardenScreen(
                            onNavigateToPomodoro = {
                                bottomNavController.navigate(Routes.Pomodoro) {
                                    // Pop tất cả để reset navigation stack
                                    popUpTo(Routes.Dashboard) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                                currentPageTitle = "Pomodoro"
                            }
                        )
                    }
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
 * - Nếu ĐÃ có quyền -> hiển thị layout: Header + Chart cố định, danh sách cuộn
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    hasPermission: Boolean,
    onGoToPermission: () -> Unit,
    onHeaderNoteChange: (String?) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current

    // State cho dialog nhắc quyền overlay
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    // Dialog chọn giới hạn thời gian
    var showLimitDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<AppUsage?>(null) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var editingCurrentLimitMinutes by remember { mutableStateOf<Int?>(null) }

    // Kiểm tra quyền overlay
    val hasOverlayPermission = remember {
        mutableStateOf(PermissionHelper.hasOverlayPermission(context))
    }

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

    // Đã có quyền Usage Stats
    LaunchedEffect(Unit) { vm.load() }

    val totalReadable = remember(vm.totalMs) { msToReadable(vm.totalMs) }
    LaunchedEffect(totalReadable) { onHeaderNoteChange("Hôm nay: $totalReadable") }

    val total = vm.totalMs.toFloat().coerceAtLeast(1f)
    val slices = vm.usages.take(6).map { it.label to (it.totalForegroundMs / total) }

    // Dialog nhắc quyền overlay
    if (showOverlayPermissionDialog) {
        OverlayPermissionDialog(
            onDismiss = { showOverlayPermissionDialog = false },
            onGoToSettings = {
                showOverlayPermissionDialog = false
                PermissionHelper.requestOverlayPermission(context as android.app.Activity)
            }
        )
    }

    // ---- Layout: Header + Chart cố định ở trên, danh sách cuộn được ----
    Column(modifier = Modifier.fillMaxSize()) {
        // Phần header + chart CỐ ĐỊNH (không cuộn)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {
            // Header: Chip vàng + số giờ lớn
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
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

            // Pie chart
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PieChart(
                    slices = slices,
                    modifier = Modifier.size(220.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Phần danh sách app - CUỘN ĐƯỢC
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(vm.usages, key = { _, u -> u.iconPackage ?: u.packageName }) { index, u ->
                // Key dùng để lưu limit: ưu tiên packageName thật
                val limitKey = u.iconPackage ?: u.packageName
                val limitMinutes = vm.limits[limitKey]

                AppUsageRow(
                    usage = u,
                    limitMinutes = limitMinutes,
                    appIcon = vm.iconFor(u),
                    chartColor = AppColors.getColorForIndex(index),
                    onClickSetLimit = {
                        editingApp = u
                        editingKey = limitKey
                        editingCurrentLimitMinutes = limitMinutes
                        showLimitDialog = true
                    },
                    onClickRemoveLimit = if (limitMinutes != null) {
                        {
                            vm.removeLimit(limitKey)
                        }
                    } else null
                )
            }

            // Spacer cuối để tránh bị che bởi bottom bar
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // Dialog chọn thời gian 00:00 - 23:59
    if (showLimitDialog && editingApp != null && editingKey != null) {
        TimeLimitDialog(
            initialMinutes = editingCurrentLimitMinutes,
            onDismiss = { showLimitDialog = false },
            onConfirm = { minutes ->
                // Kiểm tra quyền overlay khi set limit
                if (!hasOverlayPermission.value) {
                    showOverlayPermissionDialog = true
                }
                val app = editingApp!!
                val key = editingKey!!

                vm.setLimit(
                    pkg = key,
                    minutes = minutes,
                    baselineMs = app.totalForegroundMs // tổng ms đã dùng tới lúc set
                )
                showLimitDialog = false
            }
        )
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
    val locale = Locale.forLanguageTag("vi-VN")
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

/**
 * Dialog cho phép user chọn giới hạn thời gian 00:00 - 23:59
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeLimitDialog(
    initialMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hourText by remember { mutableStateOf("0") }
    var minuteText by remember { mutableStateOf("0") }

    // Khi mở dialog, map minutes -> giờ/phút
    LaunchedEffect(initialMinutes) {
        val total = initialMinutes ?: 0
        val h = total / 60
        val m = total % 60
        hourText = h.toString()
        minuteText = m.toString()
    }

    val hourInt = hourText.toIntOrNull() ?: 0
    val minuteInt = minuteText.toIntOrNull() ?: 0
    val clampedHour = hourInt.coerceIn(0, 23)
    val clampedMinute = minuteInt.coerceIn(0, 59)
    val valid = clampedHour != 0 || clampedMinute != 0
    val preview = String.format("%02d:%02d", clampedHour, clampedMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đặt giới hạn thời gian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Chọn thời gian sử dụng cho ứng dụng (00:00 - 23:59).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                hourText = value
                            }
                        },
                        label = { Text("Giờ (0-23)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                minuteText = value
                            }
                        },
                        label = { Text("Phút (0-59)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Giới hạn: $preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (valid) Color(0xFF2E7D32) else Color.Red
                )
                if (!valid) {
                    Text(
                        text = "Thời gian phải lớn hơn 0 phút.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalMinutes = clampedHour * 60 + clampedMinute
                    if (totalMinutes > 0) {
                        onConfirm(totalMinutes)
                    }
                },
                enabled = valid
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
