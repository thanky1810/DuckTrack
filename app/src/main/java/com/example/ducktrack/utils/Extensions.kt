package com.example.ducktrack.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

// Hàm mở rộng dùng chung cho toàn bộ App
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}