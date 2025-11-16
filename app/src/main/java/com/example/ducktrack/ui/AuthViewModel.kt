package com.example.ducktrack.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.content.edit

// SỬA LẠI: Thêm 'provider'
data class UserInfo(
    val name: String,
    val email: String,
    val provider: String // "Google", "Facebook", hoặc "Email"
)

class AuthViewModel(context: Context) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("DuckTrackPrefs", Context.MODE_PRIVATE)

    private val firebaseAuth = FirebaseAuth.getInstance()

    // Key để lưu phương thức đăng nhập
    private val providerKey = "LOGIN_PROVIDER"
    private val authKey = "IS_LOGGED_IN"

    private val initialAuthState = prefs.getBoolean(authKey, false)
    private val _isAuthenticated = MutableStateFlow(initialAuthState)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /**
     * HÀM QUAN TRỌNG: Lấy thông tin người dùng
     */
    fun getCurrentUserInfo(): UserInfo? {
        val user = firebaseAuth.currentUser
        // Đọc provider đã lưu từ SharedPreferences
        val providerName = prefs.getString(providerKey, "Không rõ") ?: "Không rõ"

        return if (user != null) {
            UserInfo(
                name = user.displayName ?: user.email ?: "Người dùng",
                email = user.email ?: "Không có email",
                provider = providerName // Trả về provider đã lưu
            )
        } else {
            // Ngay cả khi user null, nếu cờ IS_LOGGED_IN vẫn còn (trường hợp hiếm)
            // ta vẫn trả về provider (nếu có)
            if (initialAuthState) {
                UserInfo("Đang tải...", "...", providerName)
            } else {
                null
            }
        }
    }

    /**
     * SỬA LẠI: Xử lý đăng nhập bằng Google ID Token
     */
    suspend fun signInWithGoogleToken(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()

            _isAuthenticated.update { true }
            // Sửa: Lưu cả trạng thái và provider
            prefs.edit {
                putBoolean(authKey, true)
                putString(providerKey, "Google")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logout() // Đảm bảo logout nếu Firebase thất bại
        }
    }

    /**
     * THÊM MỚI: Hàm giả lập cho Đăng nhập Facebook
     */
    suspend fun signInWithFacebookToken() {
        // TODO: Thêm logic xác thực Facebook Firebase ở đây
        // Giả lập thành công:
        _isAuthenticated.update { true }
        prefs.edit {
            putBoolean(authKey, true)
            putString(providerKey, "Facebook")
        }
    }

    /**
     * SỬA LẠI: Xử lý đăng xuất
     */
    fun logout() {
        viewModelScope.launch {
            firebaseAuth.signOut()
            _isAuthenticated.update { false }
            // Sửa: Xóa cả trạng thái và provider
            prefs.edit {
                putBoolean(authKey, false)
                remove(providerKey)
            }
        }
    }
}