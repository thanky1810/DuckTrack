package com.example.ducktrack.ui.AppRoot

object Routes {
    // Luồng bắt buộc khi khởi động
    const val Permission = "permission"
    const val Home = "introducePage"
    const val Login = "login"

    // Luồng đã xác thực
    const val Main = "main"

    // Các tab trong màn hình chính (Dùng bởi MainScreen)
    const val Dashboard = "dashboard"
    const val Tasks = "tasks"
    const val Pomodoro = "pomodoro"
    const val Garden = "garden"
    const val Settings = "settings"

    // --- THÊM MỚI: Màn hình tập trung Full-screen ---
    const val FocusMode = "focus_mode"
}