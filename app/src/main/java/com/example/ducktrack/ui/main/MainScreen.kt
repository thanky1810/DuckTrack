package com.example.ducktrack.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ducktrack.ui.AppRoot.Routes
import com.example.ducktrack.ui.main.dashboard.DashboardScreen
import com.example.ducktrack.ui.main.garden.GardenScreen
import com.example.ducktrack.ui.main.pomodoro.PomodoroScreen
import com.example.ducktrack.ui.main.settings.SettingsScreen
import com.example.ducktrack.ui.main.tasks.TasksScreen


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    hasPermission: Boolean, // <-- THAM SỐ MỚI
    onLogout: () -> Unit
) {
    val bottomNavController = rememberNavController()
    var currentPageTitle by remember { mutableStateOf("Trang chủ") }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = currentPageTitle,
                starCount = 0 // Tạm thời để là 0
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = bottomNavController,
                onTabSelected = { title ->
                    currentPageTitle = title
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
                // --- LOGIC CẬP NHẬT ---
                composable(Routes.Dashboard) {
                    DashboardScreen(
                        hasPermission = hasPermission, // <-- Truyền xuống
                        onGoToPermission = {
                            // Điều hướng tới màn hình xin quyền
                            mainNavController.navigate(Routes.Permission)
                        }
                    )
                }
                // --- KẾT THÚC CẬP NHẬT ---

                composable(Routes.Tasks) { TasksScreen() }
                composable(Routes.Pomodoro) { PomodoroScreen() }
                composable(Routes.Garden) { GardenScreen() }
                composable(Routes.Settings) {
                    SettingsScreen(onLogout = onLogout)
                }
            }
        }
    }
}