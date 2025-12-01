package com.example.ducktrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Bảng màu tối (Sẽ không dùng tới, nhưng cứ để đây để tránh lỗi reference) ---
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// --- Bảng màu sáng (Sẽ luôn dùng bảng này) ---
private val LightColorScheme = lightColorScheme(
    primary = AppColors.ButtonGreen, // Dùng màu xanh của app làm màu chính
    secondary = AppColors.TextGreen,
    tertiary = Pink40,
    background = Color.White,        // Nền mặc định luôn là trắng
    surface = Color.White,           // Nền các card/surface luôn là trắng
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,      // Chữ trên nền trắng là đen
    onSurface = Color.Black,

    )

@Composable
fun DuckTrackTheme(
    // 1. ÉP BUỘC darkTheme luôn là false (không quan tâm hệ thống)
    darkTheme: Boolean = false,

    // 2. Tắt Dynamic Color (để tránh màu bị đổi theo hình nền điện thoại Android 12+)
    // Bạn có thể để true nếu muốn, nhưng để false sẽ giữ đúng màu thiết kế của bạn hơn.
    dynamicColor: Boolean = false,

    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Nếu có dynamic color, ép buộc dùng bản Light
            dynamicLightColorScheme(context)
        }
        // Luôn rơi vào trường hợp này hoặc dynamicLight vì darkTheme = false
        else -> LightColorScheme
    }

    // 3. Logic xử lý thanh trạng thái (Status Bar) để icon luôn hiển thị rõ trên nền trắng
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Đặt màu status bar trùng với màu primary hoặc màu trắng tùy bạn
            // Ở đây tôi để trong suốt hoặc trắng để icon đen hiện lên
            window.statusBarColor = Color.Transparent.toArgb()

            // Ép buộc icon trên status bar là màu tối (đen) để nổi trên nền trắng
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}