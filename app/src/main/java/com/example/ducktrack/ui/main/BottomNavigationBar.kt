package com.example.ducktrack.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ducktrack.ui.AppRoot.Routes

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("Trang chủ", Icons.Default.Home, Routes.Dashboard),
        BottomNavItem("Nhiệm vụ", Icons.Default.DateRange, Routes.Tasks),
        BottomNavItem("Pomodoro", Icons.Default.AddCircle, Routes.Pomodoro),
        BottomNavItem("Vườn cây", Icons.Default.Star, Routes.Garden),
        BottomNavItem("Cài đặt", Icons.Default.Person, Routes.Settings),
    )

    NavigationBar(
        // Ghi đè window insets mặc định để ngăn nó thêm khoảng đệm thừa ở dưới cùng
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    onTabSelected(item.label) // Cập nhật tiêu đề trang
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

