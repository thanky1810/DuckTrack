package com.example.ducktrack.ui.introducePage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
import com.example.ducktrack.ui.theme.AppColors

val MainFontFamily = FontFamily(
    Font(R.font.montserrat_extrabold, FontWeight.ExtraBold)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun introduceScreen(
    onGoLogin: () -> Unit = {}
) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundWhite)
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- PHẦN 1: LOGO (Chiếm 45% màn hình) ---
            Box(
                modifier = Modifier
                    .weight(0.45f) // Giảm trọng số để chừa chỗ cho phần dưới đẩy lên
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter // Đẩy ảnh xuống sát mép dưới của vùng này
            ) {
                // Ảnh nền bong bóng
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.9f),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter
                )
                // Logo con vịt
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo",
                    modifier = Modifier
                        .fillMaxHeight(0.85f) // Tăng kích thước vịt lên chút cho cân đối
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter
                )
            }

            // Khoảng cách nhỏ giữa Ảnh và Chữ
            Spacer(Modifier.height(16.dp))

            // --- PHẦN 2: CHỮ & NÚT BẤM (Chiếm 55% màn hình) ---
            Column(
                modifier = Modifier
                    .weight(0.55f) // Chiếm phần còn lại
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Đẩy nội dung lên sát mép trên (gần ảnh)
            ) {
                val textStyleWithShadow = TextStyle(
                    fontFamily = MainFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = AppColors.TextGreen,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.2f),
                        offset = androidx.compose.ui.geometry.Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )

                Text(
                    text = "Plant your Forest,",
                    style = textStyleWithShadow,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Grow your Focus.",
                    style = textStyleWithShadow,
                    modifier = Modifier.fillMaxWidth()
                )

                // Khoảng cách giữa chữ và nút
                Spacer(Modifier.height(32.dp))

                // Nút Bắt đầu
                Button(
                    onClick = onGoLogin,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.ButtonGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(70.dp)
                ) {
                    Text(
                        "Bắt đầu",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MainFontFamily
                    )
                }
            }
        }
    }
}