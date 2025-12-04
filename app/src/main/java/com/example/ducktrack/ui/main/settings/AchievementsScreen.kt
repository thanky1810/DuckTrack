package com.example.ducktrack.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.ui.theme.AppColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CheckCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AchievementsViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )

    val unlockedIds by viewModel.unlockedIds.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Gom nhóm thành tựu theo Category
    val groupedAchievements = AchievementList.list.groupBy { it.category }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thành tựu (${unlockedIds.size}/${AchievementList.list.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.ButtonGreen)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                // Duyệt qua từng nhóm để hiển thị Header + Items
                groupedAchievements.forEach { (category, achievements) ->
                    // 1. Header (Chiếm trọn 2 cột)
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = category.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextGreen,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    // 2. Các item trong nhóm
                    items(AchievementList.list) { achievement ->
                        val isUnlocked = unlockedIds.contains(achievement.id)
                        val isSelected = selectedId == achievement.id // Check xem có đang chọn không

                        AchievementItem(
                            achievement = achievement,
                            isUnlocked = isUnlocked,
                            isSelected = isSelected, // Truyền vào
                            onSelect = {
                                if (isUnlocked) viewModel.selectAchievement(achievement.id)
                            }
                        )
                    }
                }

                // Padding dưới cùng
                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun AchievementItem(
    achievement: Achievement,
    isUnlocked: Boolean,
    isSelected: Boolean, // Tham số mới
    onSelect: () -> Unit )
    {
    // Hiệu ứng đặc biệt cho cái MASTER
        val isMaster = achievement.category == AchievementCategory.MASTER

        // Màu nền & Border
        val cardColor = if (isMaster && isUnlocked) Color(0xFFFFD700)
        else if (isUnlocked) Color.White
        else Color(0xFFE0E0E0)

        // Border xanh nếu đang chọn
        val borderModifier = if (isSelected) Modifier.border(2.dp, AppColors.ButtonGreen, RoundedCornerShape(16.dp)) else Modifier

        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(if (isUnlocked) 4.dp else 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isMaster) 220.dp else 200.dp) // Tăng chiều cao xíu để chứa nút
                .then(borderModifier)
                .clickable(enabled = isUnlocked) { onSelect() } // Bấm vào để chọn
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // --- HEADER: ICON + TRẠNG THÁI CHỌN ---
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                    // Icon chính giữa
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(if(isMaster) 70.dp else 50.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMaster && isUnlocked) Color.White.copy(alpha = 0.5f)
                                else if (isUnlocked) Color(0xFFFFF9C4)
                                else Color(0xFFBDBDBD)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) achievement.icon else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isMaster && isUnlocked) Color(0xFFD84315)
                            else if (isUnlocked) Color(0xFFFBC02D)
                            else Color.White,
                            modifier = Modifier.size(if(isMaster) 40.dp else 28.dp)
                        )
                    }

                    // Dấu tích xanh nếu đang chọn (Góc trên phải)
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = AppColors.ButtonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = if(isMaster) 16.sp else 13.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    maxLines = 2,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = achievement.description,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = if (isUnlocked) Color.DarkGray else Color.Gray,
                    lineHeight = 12.sp,
                    maxLines = 3,
                    minLines = 2
                )

                // Text trạng thái
                Spacer(modifier = Modifier.height(8.dp))
                if (isSelected) {
                    Text("Đang sử dụng", fontSize = 10.sp, color = AppColors.ButtonGreen, fontWeight = FontWeight.Bold)
                } else if (isUnlocked) {
                    Text("Chạm để dùng", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
}