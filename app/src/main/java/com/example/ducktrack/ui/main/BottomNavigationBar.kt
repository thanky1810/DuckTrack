package com.example.ducktrack.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ducktrack.R
import com.example.ducktrack.ui.approot.Routes
// [QUAN TRỌNG] Import LocalDuckColors
import com.example.ducktrack.ui.theme.LocalDuckColors

data class BottomNavItem(
    val label: String,
    val iconRes: Int,
    val route: String
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
    onTabSelected: (String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // [QUAN TRỌNG] Lấy màu Theme
    val currentThemeColor = LocalDuckColors.current.buttonColor

    val leftItems = listOf(
        BottomNavItem("Trang chủ", R.drawable.ic_house, Routes.Dashboard),
        BottomNavItem("Nhiệm vụ", R.drawable.ic_note, Routes.Tasks)
    )
    val rightItems = listOf(
        BottomNavItem("Vườn cây", R.drawable.ic_plant, Routes.Garden),
        BottomNavItem("Cài đặt", R.drawable.ic_setting, Routes.Settings)
    )

    val barHeight = 80.dp
    val fabSize = 64.dp
    val fabOffset = 30.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight + fabOffset),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. THANH BAR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            color = currentThemeColor, // [ĐÃ SỬA] Dùng màu động
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    leftItems.forEach { item ->
                        CustomBottomNavItem(
                            item = item,
                            isSelected = currentRoute == item.route,
                            onClick = {
                                onTabSelected(item.label)
                                navigateTo(navController, item.route)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(80.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rightItems.forEach { item ->
                        CustomBottomNavItem(
                            item = item,
                            isSelected = currentRoute == item.route,
                            onClick = {
                                onTabSelected(item.label)
                                navigateTo(navController, item.route)
                            }
                        )
                    }
                }
            }
        }

        // 2. NÚT TRÒN TO (FAB)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 10.dp)
                .size(fabSize)
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onTabSelected("Pomodoro")
                    navigateTo(navController, Routes.Pomodoro)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Pomodoro",
                tint = currentThemeColor, // [ĐÃ SỬA] Icon cũng đổi màu theo theme
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

fun navigateTo(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun CustomBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .width(60.dp)
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.label,
            modifier = Modifier.size(32.dp),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            softWrap = false
        )
    }
}