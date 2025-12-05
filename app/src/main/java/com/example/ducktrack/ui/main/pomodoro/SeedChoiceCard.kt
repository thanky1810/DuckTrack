package com.example.ducktrack.ui.main.pomodoro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable cho một lựa chọn hạt giống
 */
@Composable
fun SeedChoiceCard(
    label: String,
    subtitle: String?,
    image: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(width = 130.dp, height = 150.dp),
    imageHeight: Dp = 80.dp
) {
    val borderColor = if (isSelected) Color(0xFF62B26A) else Color.Transparent
    val bgColor = if (isSelected) Color(0xFFF0FFF1) else Color.White

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = image,
                contentDescription = label,
                modifier = Modifier
                    .height(imageHeight)
                    .padding(top = 8.dp),
                contentScale = ContentScale.Fit
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                if (subtitle != null) {
                    Text(text = subtitle, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}