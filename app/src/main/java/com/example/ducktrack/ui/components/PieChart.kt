package com.example.ducktrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.ducktrack.ui.theme.AppColors
import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.collections.sumOf
import kotlin.let
import kotlin.math.max
import kotlin.to

@Composable
fun PieChart(
    slices: List<Pair<String, Float>>, // (label, percent 0f..1f)
    modifier: Modifier = Modifier
) {
    // Dùng palette chung từ AppColors
    val palette = AppColors.chartPalette

    // Chuẩn hoá tổng (nếu người gọi truyền tổng != 1f)
    val total = slices.sumOf { it.second.toDouble() }.toFloat().let { max(it, 0.0001f) }
    val normalized = slices.map { it.first to (it.second / total) }

    Canvas(modifier = modifier) {
        var startAngle = -90f // quay lên trên cho đẹp
        val fullSize = Size(size.width, size.height)

        normalized.forEachIndexed { index, entry ->
            val sweep = entry.second * 360f
            if (sweep > 0f) {
                drawArc(
                    color = palette[index % palette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    size = fullSize
                )
                startAngle += sweep
            }
        }
    }
}