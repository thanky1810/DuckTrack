// FILE: ui/main/settings/AboutUsScreen.kt
package com.example.ducktrack.ui.main.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
import com.example.ducktrack.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Về chúng tôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. LOGO & VERSION ---
            // Thay R.drawable.duck_waiting bằng logo app của bạn (hoặc R.mipmap.ic_launcher)
            Image(
                painter = painterResource(id = R.drawable.duck_waiting),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AppColors.ButtonGreen.copy(alpha = 0.1f))
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DuckTrack",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextGreen
            )

            Text(
                text = "Phiên bản: 1.0.0 (Beta)",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- 2. GIỚI THIỆU ---
            InfoCard(
                title = "Mục tiêu",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "Sản phẩm được xây dựng để phục vụ báo cáo cuối học phần môn Lập trình thiết bị di động. DuckTrack giúp bạn cai nghiện điện thoại và tập trung hơn vào công việc.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. TEAM MEMBERS ---
            InfoCard(
                title = "Nhóm phát triển",
                icon = Icons.Default.Code
            ) {
                MemberRow("Thân Văn Ký", "Leader / Developer")
                HorizontalDivider(
                    color = Color(0xFFEEEEEE),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                MemberRow("Vũ Trí Dũng", "Developer")
                HorizontalDivider(
                    color = Color(0xFFEEEEEE),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                MemberRow("Nguyễn Thị Hương Giang", "Developer")
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- 4. FOOTER ---
            Text(
                text = "© 2025 DuckTrack Team.\nAll rights reserved.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }
    }
}

// Composable phụ trợ: Card thông tin
@Composable
fun InfoCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = AppColors.TextGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// Composable phụ trợ: Dòng thành viên
@Composable
fun MemberRow(name: String, role: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFE0F2F1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                null,
                tint = AppColors.TextGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
            Text(role, fontSize = 12.sp, color = Color.Gray)
        }
    }
}