package com.example.ducktrack.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel để quản lý trạng thái và logic xác thực người dùng.
 * Đã cập nhật để lưu trạng thái đăng nhập vào SharedPreferences.
 */
class AuthViewModel(context: Context) : ViewModel() { // <-- Sửa constructor

    // --- LOGIC MỚI: DÙNG SHARED PREFERENCES ---
    private val prefs: SharedPreferences =
        context.getSharedPreferences("DuckTrackPrefs", Context.MODE_PRIVATE)

    // Đọc trạng thái đăng nhập đã lưu khi ViewModel được khởi tạo
    private val initialAuthState = prefs.getBoolean("IS_LOGGED_IN", false)
    // ---

    // Giữ trạng thái xác thực (đã đăng nhập hay chưa)
    // Khởi tạo giá trị ban đầu từ SharedPreferences
    private val _isAuthenticated = MutableStateFlow(initialAuthState)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Lưu trữ thông tin người dùng đã đăng ký (giả lập database)
    private val userCredentials = mutableMapOf<String, String>()

    /**
     * Xử lý logic đăng ký.
     * @return true nếu đăng ký thành công, false nếu người dùng đã tồn tại.
     */
    fun signUp(username: String, email: String, pass: String): Boolean {
        if (userCredentials.containsKey(username)) {
            return false // Người dùng đã tồn tại
        }
        userCredentials[username] = pass
        return true
    }

    /**
     * Xử lý logic đăng nhập.
     * @return true nếu đăng nhập thành công, false nếu thất bại.
     */
    fun login(username: String, pass: String): Boolean {
        if (userCredentials[username] == pass) {
            viewModelScope.launch {
                _isAuthenticated.update { true }
                // --- LOGIC MỚI ---
                // Lưu trạng thái đăng nhập khi thành công
                prefs.edit().putBoolean("IS_LOGGED_IN", true).apply()
                // ---
            }
            return true
        }
        return false
    }

    /**
     * Xử lý logic đăng xuất.
     */
    fun logout() {
        viewModelScope.launch {
            _isAuthenticated.update { false }
            // --- LOGIC MỚI ---
            // Xóa trạng thái đăng nhập khi đăng xuất
            prefs.edit().putBoolean("IS_LOGGED_IN", false).apply()
            // ---
        }
    }
}