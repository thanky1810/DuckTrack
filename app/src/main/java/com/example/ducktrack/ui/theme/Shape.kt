package com.example.ducktrack.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Bo tròn nhỏ (Button nhỏ, Text field)
    small = RoundedCornerShape(16.dp),
    // Bo tròn vừa (Card, Dialog)
    medium = RoundedCornerShape(24.dp),
    // Bo tròn lớn (BottomSheet, Container lớn)
    large = RoundedCornerShape(32.dp),
    // Bo tròn cực đại (Pill, Circle button)
    extraLarge = RoundedCornerShape(50)
)