package com.example.ducktrack.ui.main.garden

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.MyApplication
import com.example.ducktrack.ui.main.ViewModelFactory
import com.example.ducktrack.ui.main.promodoro.NotEnoughPointsDialog
import androidx.compose.ui.*
import androidx.compose.foundation.*
import com.example.ducktrack.R

@Composable
fun GardenScreen(
    // (onNavigateToPomodoro được truyền từ NavHost chung)
    onNavigateToPomodoro: () -> Unit,
    // Thay vì viewModel() mặc định:
    // viewModel: GardenViewModel = viewModel()
    // Chúng ta dùng Factory:
    context: Context = LocalContext.current.applicationContext,
    viewModel: GardenViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as MyApplication)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp) // Padding cho nội dung
    ) {

        // 1. Tiêu đề "Cửa Hàng Hạt Giống"
        Text(
            text = "Cửa Hàng Hạt Giống 🌿",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A593F), // Màu xanh đậm
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Card Cửa hàng
        SeedStoreCard(
            storeItems = uiState.storeItems,
            onItemClick = { storeItem ->
                if (!storeItem.isUnlocked) {
                    viewModel.onUnlockSeed(storeItem.seedType)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Tiêu đề "Mảnh đất của tôi"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center, // Căn giữa
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Mảnh đất của tôi",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3A593F)
            )
            Image(
                painter = painterResource(id = R.drawable.duck_farming),
                contentDescription = "Duck Icon",
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        // 4. Card Mảnh đất
        GardenPlotsCard(
            plots = uiState.gardenPlots,
            onPlantNowClick = onNavigateToPomodoro
        )
    }
    if (uiState.showNotEnoughPointsDialog) {
        NotEnoughPointsDialog(
            onDismiss = { viewModel.onDismissNotEnoughPointsDialog() }
        )
    }
}