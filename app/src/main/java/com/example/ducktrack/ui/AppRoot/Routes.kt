// FILE: ui/AppRoot/Routes.kt (Hoặc đường dẫn cũ của bạn)
package com.example.ducktrack.ui.AppRoot

object Routes {
    // --- Các màn hình chính (Top Level) ---
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Login = "login"
    const val Permission = "permission"
    const val Main = "main"
    const val FocusMode = "focus_mode"
    const val UserProfile = "user_profile"

    // Màn hình mới thêm
    const val ExportHistory = "export_history"

    // --- Các màn hình con trong Bottom Navigation ---
    const val Dashboard = "dashboard"
    const val Tasks = "tasks"
    const val Pomodoro = "pomodoro"
    const val Garden = "garden"
    const val Settings = "settings"
    const val AboutUs = "about_us"
}