package com.example.ducktrack.utils

fun msToReadable(ms: Long): String {
    val mins = ms / 60000
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
