package com.example.ducktrack.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Bo tròn nhỏ (cho các nút bé, text field)
    small = RoundedCornerShape(8.dp),

    // Bo tròn vừa (cho Card, Dialog) - Chuẩn đẹp là 16dp
    medium = RoundedCornerShape(16.dp),

    // Bo tròn lớn (cho BottomSheet hoặc khung to)
    large = RoundedCornerShape(24.dp),

    // Bo tròn cực đại (nhưng cố định dp để không bị thành hình trứng/viên thuốc)
    // Giảm từ 50 xuống 28.dp để nó chỉ bo góc thôi, không tròn vo.
    extraLarge = RoundedCornerShape(28.dp)
)