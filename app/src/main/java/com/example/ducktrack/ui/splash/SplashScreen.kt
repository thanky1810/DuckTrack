package com.example.ducktrack.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
import com.example.ducktrack.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    isDataReady: Boolean, onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    // Biến cờ để đánh dấu animation đã xong chưa
    var animationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f, animationSpec = tween(durationMillis = 1200)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f, animationSpec = tween(durationMillis = 1200)
            )
        }

        delay(2000)

        animationFinished = true
    }

    // Logic kiểm tra chuyển màn hình
    LaunchedEffect(isDataReady, animationFinished) {
        if (isDataReady && animationFinished) {
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // --- Cụm Logo và Tên App ở chính giữa ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(BiasAlignment(0f, -0.3f))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_duck_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(260.dp) // Tăng kích thước logo
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .clip(CircleShape) // Bo tròn icon cho mềm mại
            )

            Spacer(modifier = Modifier.height(14.dp)) // Tăng khoảng cách

            Text(
                text = "DUCKTRACK",
                fontSize = 34.sp, // Chữ to hơn
                fontWeight = FontWeight.ExtraBold, // Đậm hơn
                color = AppColors.ButtonGreen,
                modifier = Modifier.alpha(alpha.value),
                letterSpacing = 2.sp // Thêm khoảng cách giữa các chữ cái
            )
        }

        // --- Slogan ở dưới cùng ---
        Text(
            text = "Plant your Forest\nGrow your focus",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextGreen.copy(alpha = 0.4f), // Màu xanh nhạt hơn logo
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp) // Cách đáy màn hình một đoạn
                .alpha(alpha.value)
        )
    }
}