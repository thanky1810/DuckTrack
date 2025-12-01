package com.example.ducktrack.ui.introducePage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

val JostFontFamily = FontFamily(
    Font(R.font.jost_extrabold, FontWeight.ExtraBold)
)

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
            // --- PHẦN 1: LOGO & ẢNH NỀN (TỰ ĐỘNG CO GIÃN) ---
            // Sử dụng weight(1f) để nó chiếm toàn bộ không gian trống
            Box(
                modifier = Modifier
                    .weight(1f) // Chiếm phần lớn màn hình
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.9f), // Co lại chút để không sát lề
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo",
                    // Thay vì size cố định, ta dùng fillMaxHeight theo tỷ lệ
                    modifier = Modifier.fillMaxHeight(0.6f).aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }

            // --- PHẦN 2: NỘI DUNG CHỮ & NÚT ---
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp), // Cách đáy một chút
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val textStyleWithShadow = TextStyle(
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp, // Giảm nhẹ font để an toàn cho máy nhỏ
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

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onGoLogin,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.ButtonGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp) // Nút cao vừa phải
                ) {
                    Text(
                        "Bắt đầu",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}