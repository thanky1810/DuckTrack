package com.example.ducktrack.data.model

data class AppUsage(
    val packageName: String,
    val label: String,
    val totalForegroundMs: Long
)
