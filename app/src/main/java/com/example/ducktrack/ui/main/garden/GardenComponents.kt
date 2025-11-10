package com.example.ducktrack.ui.main.garden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

/**
 * Card Cửa hàng
 */
@Composable
fun SeedStoreCard(
    storeItems: List<StoreItem>,
    onItemClick: (StoreItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFA5D6A7)) // Màu viền
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Render các item theo chiều dọc
            storeItems.forEach { item ->
                SeedStoreItem(
                    item = item,
                    onClick = { onItemClick(item) }
                )
                // Thêm khoảng cách giữa các item
                if (item != storeItems.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Một item trong Cửa hàng
 */
@Composable
fun SeedStoreItem(
    item: StoreItem,
    onClick: () -> Unit
) {
    // Dùng Surface để tạo card lồng bên trong
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        // Màu nền xám nếu đã mở khóa
        color = if (item.isUnlocked) Color(0xFFF0F0F0) else Color.White,
        // Viền mờ nếu chưa mở khóa
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
                modifier = Modifier.size(50.dp) // Ảnh to hơn
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Tên cây và giá/trạng thái
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

/**
 * Card Mảnh đất (Đã bỏ Title bên trong)
 */
@Composable
fun GardenPlotsCard(
    plots: List<SeedType?>,
    onPlantNowClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFA5D6A7)) // Màu viền
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Các cây bạn đã trồng thành công sẽ được sắp xếp trên mảnh đất này",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Grid 3 cột
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.heightIn(max = 400.dp), // Set chiều cao tối đa
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(plots) { seed ->
                    GardenPlotItem(seed = seed, onPlantNowClick = onPlantNowClick)
                }
            }
        }
    }
}

/**
 * Một ô đất trong Mảnh đất (Không đổi)
 */
@Composable
fun GardenPlotItem(
    seed: SeedType?,
    onPlantNowClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EFE6)),
        modifier = Modifier.aspectRatio(1f) // Để nó vuông
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (seed != null) {
                // Đã có cây
                Image(
                    painter = painterResource(id = seed.grownIcon),
                    contentDescription = seed.displayName,
                    modifier = Modifier.fillMaxSize(0.8f)
                )
            } else {
                // Ô trống
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