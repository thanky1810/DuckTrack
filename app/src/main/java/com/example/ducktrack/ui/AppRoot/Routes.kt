package com.example.ducktrack.ui.AppRoot

object Routes {
    // Luồng chưa xác thực
    const val Home = "introducePage"
    const val Login = "login"
    const val SignUp = "signup"

    // Luồng đã xác thực
    const val Permission = "permission"
    const val Main = "main"

    // Các tab trong màn hình chính
    const val Dashboard = "dashboard"
    const val Tasks = "tasks"
    const val Pomodoro = "pomodoro"
    const val Garden = "garden"
    const val Settings = "settings"
    const val ForgotPassword = "forgotpassword"
}
