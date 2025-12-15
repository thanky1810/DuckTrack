package com.example.ducktrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.example.ducktrack.R
import kotlinx.coroutines.isActive

data class Snowflake(
    var x: Float,
    var y: Float,
    var scale: Float,
    var speed: Float,
    val bitmap: ImageBitmap
)

@Composable
fun SnowfallEffect() {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp.value * 2.5f
    val screenWidth = configuration.screenWidthDp.dp.value * 2.5f

    // [SỬA LỖI Ở ĐÂY]
    // Load ảnh TRƯỚC, bên ngoài remember. Vì imageResource là hàm Composable.
    val img1 = ImageBitmap.imageResource(R.drawable.snow_flower1)
    val img2 = ImageBitmap.imageResource(R.drawable.snow_flower2)
    val img3 = ImageBitmap.imageResource(R.drawable.snow_flower3)

    // Sau đó mới đưa vào list
    val snowflakeImages = remember(img1, img2, img3) {
        listOf(img1, img2, img3)
    }

    // Tạo danh sách bông tuyết
    val snowflakes = remember {
        List(50) { // Giảm số lượng xuống 50 để đỡ lag vì vẽ ảnh nặng hơn vẽ chấm
            Snowflake(
                x = (Math.random() * screenWidth).toFloat(),
                y = (Math.random() * screenHeight * -1f).toFloat(),
                scale = (Math.random() * 0.15f + 0.05f).toFloat(), // Chỉnh scale nhỏ lại (0.05 đến 0.2) cho vừa màn hình
                speed = (Math.random() * 3f + 2f).toFloat(),
                bitmap = snowflakeImages.random()
            )
        }
    }

    var time by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameTime ->
                time = frameTime
                snowflakes.forEach { flake ->
                    flake.y += flake.speed
                    // Rơi xuống đáy thì reset
                    if (flake.y > screenHeight + 100) {
                        flake.y = -100f
                        flake.x = (Math.random() * screenWidth).toFloat()
                        // Reset lại tốc độ và kích thước cho tự nhiên
                        flake.speed = (Math.random() * 3f + 2f).toFloat()
                    }
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val trigger = time

        snowflakes.forEach { flake ->
            scale(scale = flake.scale, pivot = Offset(flake.x, flake.y)) {
                val topLeftX = flake.x - (flake.bitmap.width / 2f)
                val topLeftY = flake.y - (flake.bitmap.height / 2f)

                drawImage(
                    image = flake.bitmap,
                    topLeft = Offset(topLeftX, topLeftY),
                    alpha = 0.9f
                )
            }
        }
    }
}