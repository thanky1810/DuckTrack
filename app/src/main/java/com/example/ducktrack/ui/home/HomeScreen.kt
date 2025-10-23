package com.example.ducktrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ducktrack.ui.components.AppUsageRow
import com.example.ducktrack.ui.components.PieChart
import com.example.ducktrack.utils.msToReadable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel) {
    LaunchedEffect(Unit) { vm.load() }

    val total = vm.totalMs.toFloat().coerceAtLeast(1f)
    val slices = vm.usages.take(6).map { it.label to (it.totalForegroundMs / total) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Trang chủ") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Trang chủ") }
                )
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Thời gian sử dụng hôm nay")
                    Text(msToReadable(vm.totalMs))
                    PieChart(
                        slices = slices,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(220.dp)
                    )
                }
            }
            items(vm.usages, key = { it.packageName }) { u ->
                AppUsageRow(
                    usage = u,
                    limitMinutes = vm.limits[u.packageName],
                    onSetLimit = { minutes -> vm.setLimit(u.packageName, minutes) }
                )
            }
        }
    }
}
