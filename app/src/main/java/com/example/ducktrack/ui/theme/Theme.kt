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

// Dark Theme dùng màu mặc định (Purple80...) từ file Color.kt
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Light Theme dùng màu custom từ AppColors.kt
private val LightColorScheme = lightColorScheme(
    primary = AppColors.ButtonGreen,
    secondary = AppColors.TextGreen,
    tertiary = Pink40,
    background = AppColors.BackgroundWhite,
    surface = AppColors.SurfaceColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun DuckTrackTheme(
    darkTheme: Boolean = false, // Ép buộc Light Mode
    dynamicColor: Boolean = false, // Tắt Dynamic Color
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Đặt status bar cùng màu primary
            window.statusBarColor = colorScheme.primary.toArgb()
            // Icon status bar màu sáng (trắng) vì nền xanh đậm
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes, // Đảm bảo bạn đã có file Shape.kt
        content = content
    )
}