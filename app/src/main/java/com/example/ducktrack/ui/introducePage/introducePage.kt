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
import com.example.ducktrack.ui.theme.AppColors // Import màu từ theme

// --- ĐỊNH NGHĨA FONT FAMILY TÙY CHỈNH ---
val JostFontFamily = FontFamily(
    Font(R.font.jost_extrabold, FontWeight.ExtraBold)
)
// ------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun introduceScreen(
    onGoLogin: () -> Unit = {}
    // ĐÃ XÓA onForgotPassword và onCreateAccount
) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundWhite)
                .padding(inner)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo và hình nền bong bóng
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.1f),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo",
                    modifier = Modifier.size(250.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- CHỮ (Giữ nguyên style) ---
            val textStyleWithShadow = TextStyle(
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                color = AppColors.TextGreen,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp,
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

            Spacer(Modifier.height(40.dp))

            // Nút "Đăng nhập"
            Button(
                onClick = onGoLogin,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.ButtonGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 40.dp)
            ) {
                Text(
                    "Đăng nhập",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}