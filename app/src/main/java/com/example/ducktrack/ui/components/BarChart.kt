package com.example.ducktrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.ducktrack.ui.theme.AppColors
import kotlin.math.max

// 1. BIỂU ĐỒ CÓ THỂ ZOOM (Dùng cho Top 10 App)
@Composable
fun ZoomableBarChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    barColor: Color = AppColors.ButtonGreen,
    labelColor: Color = Color.Gray
) {
    val textMeasurer = rememberTextMeasurer()

    // State Zoom & Scroll
    var scale by remember { mutableStateOf(1f) }
    var scrollOffset by remember { mutableStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Tính toán max value để scale chiều cao cột
    val maxVal = data.values.maxOrNull() ?: 0f
    val chartMax = if (maxVal == 0f) 1f else maxVal * 1.2f

    // Danh sách dữ liệu để dễ truy xuất theo index
    val dataList = remember(data) { data.toList() }

    Box(
        modifier = modifier
            .clipToBounds() // Quan trọng: Cắt phần vẽ lòi ra ngoài khung
            .onSizeChanged { canvasSize = it.toSize() }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // 1. Xử lý Zoom (Giới hạn 1x -> 5x)
                    val newScale = (scale * zoom).coerceIn(1f, 5f)

                    // Tính toán lại scroll để zoom vào tâm (tương đối) hoặc giữ vị trí
                    // Ở đây đơn giản hóa: Zoom thì giữ nguyên tỉ lệ scroll hiện tại
                    if (scale > 1) scrollOffset / (canvasSize.width * scale - canvasSize.width)

                    scale = newScale

                    // 2. Xử lý Scroll (Pan)
                    // Tổng chiều rộng ảo của biểu đồ khi zoom
                    val virtualWidth = canvasSize.width * scale
                    val maxScroll = max(0f, virtualWidth - canvasSize.width)

                    // Cộng dồn scroll và kẹp trong khoảng [0, maxScroll]
                    // Lưu ý: Pan dương là kéo sang phải -> nội dung trôi sang phải -> offset âm (logic vẽ thường dùng offset âm để dịch view)
                    // Nhưng ở đây ta dùng offset dương để đại diện cho "đã cuộn được bao nhiêu"
                    scrollOffset = (scrollOffset - pan.x).coerceIn(0f, maxScroll)
                }
            }
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp)) {
            val width = size.width
            val height = size.height

            // Tính toán kích thước cột dựa trên scale
            // Mặc định (scale=1): Cột chiếm 1 phần, khoảng trống 1 phần -> Tổng 2 phần * số cột
            val baseColumnSlotWidth = width / (dataList.size * 2f + 1) // +1 padding cuối
            val scaledSlotWidth = baseColumnSlotWidth * scale

            // Độ rộng cột thực tế (có thể giới hạn max để không quá to khi zoom)
            val barWidth = (scaledSlotWidth * 0.8f).coerceAtMost(150f)

            // Vị trí bắt đầu vẽ (trừ đi phần đã scroll)
            val startX = -scrollOffset + scaledSlotWidth / 2 // Padding trái ban đầu

            dataList.forEachIndexed { index, (label, value) ->
                // Tính tọa độ X của cột này
                val xPos = startX + (index * scaledSlotWidth * 2) // *2 vì 1 slot cột + 1 slot trống

                // CHỈ VẼ NẾU CỘT NẰM TRONG MÀN HÌNH (Optimization)
                if (xPos + barWidth > 0 && xPos < width) {
                    val barHeight = (value / chartMax) * height

                    // 1. Vẽ Cột
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x = xPos, y = height - barHeight),
                        size = Size(width = barWidth, height = barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // 2. Vẽ Nhãn (Tên App)
                    val textLayoutResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = labelColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = xPos + (barWidth / 2) - (textLayoutResult.size.width / 2),
                            y = height + 8.dp.toPx()
                        )
                    )

                    // 3. Vẽ Giá trị (Trên đỉnh)
                    if (value > 0.1f) {
                        val valueText = String.format("%.1f", value)
                        val valResult = textMeasurer.measure(
                            text = valueText,
                            style = TextStyle(fontSize = 9.sp, color = barColor)
                        )
                        drawText(
                            textLayoutResult = valResult,
                            topLeft = Offset(
                                x = xPos + (barWidth / 2) - (valResult.size.width / 2),
                                y = height - barHeight - 14.dp.toPx()
                            )
                        )
                    }
                }
            }
        }

        // Vẽ thanh cuộn (Scrollbar) đơn giản nếu đang zoom
        if (scale > 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val viewportWidth = size.width
                val contentWidth = viewportWidth * scale
                val scrollbarWidth = viewportWidth * (viewportWidth / contentWidth)
                val scrollbarX =
                    (scrollOffset / (contentWidth - viewportWidth)) * (viewportWidth - scrollbarWidth)

                // Vẽ thanh nhỏ ở đáy
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.5f),
                    topLeft = Offset(x = scrollbarX, y = size.height - 4.dp.toPx()),
                    size = Size(width = scrollbarWidth, height = 4.dp.toPx()),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}