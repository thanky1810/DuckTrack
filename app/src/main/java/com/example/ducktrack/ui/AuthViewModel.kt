package com.example.ducktrack.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.content.edit

// Data class cho thông tin người dùng
data class UserInfo(val name: String, val email: String)

class AuthViewModel(context: Context) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("DuckTrackPrefs", Context.MODE_PRIVATE)

    // Khởi tạo Firebase Auth instance
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val initialAuthState = prefs.getBoolean("IS_LOGGED_IN", false)

    private val _isAuthenticated = MutableStateFlow(initialAuthState)
    //val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    //private val userCredentials = mutableMapOf<String, String>() // Giữ lại cho login thường

    /**
     * HÀM QUAN TRỌNG: Lấy thông tin người dùng từ Firebase Auth
     */
    fun getCurrentUserInfo(): UserInfo? {
        val user = firebaseAuth.currentUser // Lấy user từ instance của ViewModel
        return if (user != null) {
            UserInfo(
                name = user.displayName ?: user.email ?: "Người dùng",
                email = user.email ?: "Không có email"
            )
        } else {
            null
        }
    }

    // ... (Hàm signUp và login thường giữ nguyên) ...


//    fun login(username: String, pass: String): Boolean {
//        if (userCredentials[username] == pass) {
//            viewModelScope.launch {
//                _isAuthenticated.update { true }
//                prefs.edit().putBoolean("IS_LOGGED_IN", true).apply()
//            }
//            return true
//        }
//        return false
//    }

    /**
     * SỬA LỖI: Xử lý đăng nhập bằng Google ID Token
     * Hàm này sẽ thực hiện xác thực với Firebase
     */
    suspend fun signInWithGoogleToken(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            // Thực hiện đăng nhập Firebase bằng credential
            firebaseAuth.signInWithCredential(credential).await()

            // Nếu thành công, cập nhật trạng thái
            _isAuthenticated.update { true }
            prefs.edit { putBoolean("IS_LOGGED_IN", true) }

        } catch (e: Exception) {
            // Xử lý lỗi nếu xác thực Firebase thất bại
            e.printStackTrace()
            _isAuthenticated.update { false }
            prefs.edit { putBoolean("IS_LOGGED_IN", false) }
        }
    }

    /**
     * Xử lý đăng xuất
     */
    fun logout() {
        viewModelScope.launch {
            firebaseAuth.signOut() // Đăng xuất khỏi Firebase
            _isAuthenticated.update { false }
            prefs.edit { putBoolean("IS_LOGGED_IN", false) }
        }
    }
}