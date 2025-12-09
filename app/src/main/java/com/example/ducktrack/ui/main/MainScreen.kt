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
import com.example.ducktrack.MyApplication
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.ui.AppRoot.Routes
import com.example.ducktrack.ui.components.AppUsageRow
import com.example.ducktrack.ui.components.OverlayPermissionDialog
import com.example.ducktrack.ui.components.PieChart
import com.example.ducktrack.ui.main.garden.GardenScreen
import com.example.ducktrack.ui.main.pomodoro.PomodoroScreen
import com.example.ducktrack.ui.main.pomodoro.PomodoroViewModel
import com.example.ducktrack.ui.main.settings.SettingsScreen
import com.example.ducktrack.ui.main.tasks.TasksScreen
import com.example.ducktrack.ui.theme.AppColors
import com.example.ducktrack.utils.PermissionHelper
import com.example.ducktrack.utils.msToReadable
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    hasPermission: Boolean,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    pomodoroViewModel: PomodoroViewModel,
    onNavigateToFocus: () -> Unit,
    onNavigateToExportHistory: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAiChat: () -> Unit
) {
    val bottomNavController = rememberNavController()
    var currentPageTitle by remember { mutableStateOf("Trang chủ") }
    var headerNote by remember { mutableStateOf<String?>(null) }

    val currentBackStack by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val application = LocalContext.current.applicationContext as MyApplication
    val starCount by application.repository.userPoints.collectAsState(initial = 0)

    val selectedDateMs = homeViewModel.selectedDateMs
    val dateText = remember(selectedDateMs) {
        val date = Date(selectedDateMs)
        val today = Date()
        val fmt = java.text.SimpleDateFormat("dd 'tháng' MM", Locale("vi", "VN"))
        val fmtCheck = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)

        val isToday = fmtCheck.format(date) == fmtCheck.format(today)
        if (isToday) "Hôm nay, ${fmt.format(date)}" else fmt.format(date)
    }

    // Xin quyền Thông báo (Android 13+)
    val currentContext = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = currentContext as? android.app.Activity
        if (activity != null && !PermissionHelper.hasNotificationPermission(currentContext)) {
            PermissionHelper.requestNotificationPermission(activity)
        }
    }

    val topTitle = currentPageTitle

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = topTitle,
                starCount = starCount,
                dateText = dateText
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
        },
        containerColor = AppColors.BackgroundWhite
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = bottomNavController,
                startDestination = Routes.Dashboard,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. DASHBOARD
                composable(Routes.Dashboard) {
                    DashboardScreen(
                        hasPermission = hasPermission,
                        onGoToPermission = { mainNavController.navigate(Routes.Permission) },
                        onHeaderNoteChange = { note -> headerNote = note },
                        vm = homeViewModel,
                        onNavigateToStatistics = onNavigateToStatistics
                    )
                }

                // 2. TASKS
                composable(Routes.Tasks) {
                    LaunchedEffect(Unit) { headerNote = null }
                    TasksScreen()
                }

                // 3. POMODORO
                composable(Routes.Pomodoro) {
                    LaunchedEffect(Unit) { headerNote = null }
                    key(Routes.Pomodoro + currentRoute) {
                        PomodoroScreen(
                            viewModel = pomodoroViewModel,
                            onStartFocus = onNavigateToFocus
                        )
                    }
                }

                // 4. GARDEN
                composable(Routes.Garden) {
                    LaunchedEffect(Unit) { headerNote = null }
                    key(Routes.Garden + currentRoute) {
                        GardenScreen(
                            onNavigateToPomodoro = {
                                bottomNavController.navigate(Routes.Pomodoro) {
                                    popUpTo(Routes.Dashboard)
                                    launchSingleTop = true
                                    restoreState = false
                                }
                                currentPageTitle = "Pomodoro"
                            }
                        )
                    }
                }

                // 5. SETTINGS
                composable(Routes.Settings) {
                    LaunchedEffect(Unit) { headerNote = null }
                    SettingsScreen(
                        onLogout = onLogout,
                        onNavigateToProfile = { mainNavController.navigate(Routes.UserProfile) },
                        onNavigateToExportHistory = onNavigateToExportHistory,
                        onNavigateToAbout = onNavigateToAbout,
                        onNavigateToAchievements = onNavigateToAchievements,
                        onNavigateToAiChat = onNavigateToAiChat
                    )
                }
            }
        }
    }
}

/**
 * Màn hình Dashboard: Hiển thị Biểu đồ, Tổng thời gian, Danh sách App
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    hasPermission: Boolean,
    onGoToPermission: () -> Unit,
    onHeaderNoteChange: (String?) -> Unit,
    onNavigateToStatistics: () -> Unit,
    vm: HomeViewModel
) {
    val context = LocalContext.current

    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }

    var editingApp by remember { mutableStateOf<AppUsage?>(null) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var editingCurrentLimitMinutes by remember { mutableStateOf<Int?>(null) }

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
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppColors.ButtonGreen
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = "DuckTrack cần quyền Usage Access để thống kê thời gian sử dụng ứng dụng của bạn.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onGoToPermission,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen)
            ) {
                Text("Cấp quyền ngay")
            }
        }
        return
    }

    LaunchedEffect(vm.selectedDateMs) {
        vm.load()
    }

    val totalReadable = remember(vm.totalMs) { msToReadable(vm.totalMs) }
    LaunchedEffect(totalReadable) { onHeaderNoteChange("Hôm nay: $totalReadable") }

    val total = vm.totalMs.toFloat().coerceAtLeast(1f)
    val slices = vm.usages.take(6).map { it.label to (it.totalForegroundMs / total) }

    if (showOverlayPermissionDialog) {
        OverlayPermissionDialog(
            onDismiss = { showOverlayPermissionDialog = false },
            onGoToSettings = {
                showOverlayPermissionDialog = false
                PermissionHelper.requestOverlayPermission(context as android.app.Activity)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- PHẦN THỐNG KÊ TỔNG QUAN (TRÊN) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            DayPagerRow(
                currentDateText = vm.getDateText(),
                onPrev = { vm.previousDay() },
                onNext = { vm.nextDay() },
                isNextEnabled = !vm.isToday(vm.selectedDateMs)
            )

            Spacer(Modifier.height(8.dp))

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
                    color = AppColors.TextGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

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

            // Nút Thống kê chi tiết
            Button(
                onClick = onNavigateToStatistics,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE3F2FD),
                    contentColor = Color(0xFF1976D2)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Xem thống kê chi tiết 📊")
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- PHẦN DANH SÁCH APP (DƯỚI) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundWhite),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = vm.usages,
                key = { _, u -> u.iconPackage ?: u.packageName }
            ) { index, u ->
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
                        { vm.removeLimit(limitKey) }
                    } else null
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showLimitDialog && editingApp != null && editingKey != null) {
        TimeLimitDialog(
            initialMinutes = editingCurrentLimitMinutes,
            onDismiss = { showLimitDialog = false },
            onConfirm = { minutes ->
                if (!hasOverlayPermission.value) {
                    showOverlayPermissionDialog = true
                }
                val app = editingApp!!
                val key = editingKey!!
                vm.setLimit(
                    pkg = key,
                    minutes = minutes,
                    baselineMs = app.totalForegroundMs
                )
                showLimitDialog = false
            }
        )
    }
}

// --- CÁC COMPOSABLE CON ---

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
        Text(
            text = text,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DayPagerRow(
    currentDateText: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            RoundIconButton(
                icon = Icons.Filled.ChevronLeft,
                onClick = onPrev
            )
        }

        Box(
            modifier = Modifier.weight(2f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentDateText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextGreen
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isNextEnabled) {
                RoundIconButton(
                    icon = Icons.Filled.ChevronRight,
                    onClick = onNext
                )
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
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
        shadowElevation = 1.dp,
        modifier = Modifier.size(40.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3A2A11)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeLimitDialog(
    initialMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hourText by remember { mutableStateOf("0") }
    var minuteText by remember { mutableStateOf("0") }

    LaunchedEffect(initialMinutes) {
        val total = initialMinutes ?: 0
        hourText = (total / 60).toString()
        minuteText = (total % 60).toString()
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
                        onValueChange = {
                            if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                hourText = it
                            }
                        },
                        label = { Text("Giờ") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = {
                            if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                minuteText = it
                            }
                        },
                        label = { Text("Phút") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Giới hạn: $preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (valid) AppColors.TextGreen else Color.Red
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = clampedHour * 60 + clampedMinute
                    if (total > 0) onConfirm(total)
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.TextGray)
            ) {
                Text("Hủy")
            }
        }
    )
}