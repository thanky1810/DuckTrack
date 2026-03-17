package com.example.ducktrack.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- 1. ĐỊNH NGHĨA DATA CLASS MÀU SẮC ---
data class DuckTrackColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val buttonColor: Color,
    val secondaryText: Color = Color.Gray
)

// --- 2. CÁC BẢNG MÀU (PALETTE) ---
val DefaultPalette = DuckTrackColors(
    primary = AppColors.TextGreen,
    background = AppColors.BackgroundWhite,
    surface = AppColors.SurfaceColor,
    onSurface = Color.Black,
    buttonColor = AppColors.ButtonGreen
)

val ChristmasPalette = DuckTrackColors(
    primary = Color(0xFFD32F2F),       // Đỏ
    background = Color(0xFFFFF8E1),    // Kem
    surface = Color(0xFFE0F7FA),       // Xanh băng
    onSurface = Color(0xFF004D40),     // Xanh thông
    buttonColor = Color(0xFFC62828),   // Đỏ đậm
    secondaryText = Color(0xFF5D4037)
)

val LocalDuckColors = staticCompositionLocalOf { DefaultPalette }

// --- 3. THEME CHÍNH ---
@Composable
fun DuckTrackTheme(
    isChristmas: Boolean = false,
    content: @Composable () -> Unit
) {
    val currentPalette = if (isChristmas) ChristmasPalette else DefaultPalette

    val colorScheme = lightColorScheme(
        primary = currentPalette.buttonColor,
        onPrimary = Color.White,
        secondary = currentPalette.primary,
        onSecondary = Color.White,
        background = currentPalette.background,
        onBackground = currentPalette.onSurface,
        surface = currentPalette.surface,
        onSurface = currentPalette.onSurface,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = currentPalette.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalDuckColors provides currentPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}