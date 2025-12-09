package com.example.ducktrack.ui.main.garden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ... (SeedStoreCard giữ nguyên) ...
@Composable
fun SeedStoreCard(
    storeItems: List<StoreItem>,
    onItemClick: (StoreItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFA5D6A7))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(storeItems) { item ->
                SeedStoreItem(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

// --- SỬA CHỖ NÀY: SeedStoreItem ---
@Composable
fun SeedStoreItem(
    item: StoreItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (item.isUnlocked) Color(0xFFF0F0F0) else Color.White,
        border = BorderStroke(1.dp, if (item.isUnlocked) Color.Transparent else Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = item.seedType.storeIcon),
                contentDescription = item.seedType.displayName,
                // SỬA: Tăng kích thước từ 50.dp lên 70.dp
                modifier = Modifier.size(70.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.seedType.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.isUnlocked) {
                        "Đã sở hữu"
                    } else if (item.seedType.cost == 0) {
                        "Free"
                    } else {
                        "Đổi ngay ${item.seedType.cost} 🌟"
                    },
                    color = if (item.isUnlocked) Color.Gray else Color(0xFFE2970E),
                    fontWeight = if (item.isUnlocked) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
// ----------------------------------

// --- SỬA CHỖ NÀY: GardenPlotItem ---
@Composable
fun GardenPlotItem(
    seed: SeedType?,
    onPlantNowClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EFE6)),
        modifier = Modifier.aspectRatio(1f)
    ) {
        // Giảm padding để cây to hơn
        Box(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (seed != null) {
                Image(
                    painter = painterResource(id = seed.grownIcon),
                    contentDescription = seed.displayName,
                    // SỬA: fillMaxSize() để to hết cỡ
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Button(
                    onClick = onPlantNowClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0C378)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Trồng ngay", fontSize = 11.sp, color = Color.Black)
                }
            }
        }
    }
}
// ----------------------------------