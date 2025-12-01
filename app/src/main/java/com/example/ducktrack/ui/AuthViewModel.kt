package com.example.ducktrack.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserInfo(
    val name: String,
    val email: String,
    val provider: String
)

class AuthViewModel(context: Context) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("DuckTrackPrefs", Context.MODE_PRIVATE)

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val providerKey = "LOGIN_PROVIDER"
    private val authKey = "IS_LOGGED_IN"

    private val initialAuthState: Boolean
        get() {
            val savedState = prefs.getBoolean(authKey, false)
            val currentUser = firebaseAuth.currentUser
            return if (currentUser != null) savedState else false
        }

    private val _isAuthenticated = MutableStateFlow(initialAuthState)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        if (!initialAuthState) {
            prefs.edit { putBoolean(authKey, false) }
        }
    }

    // --- LẤY THÔNG TIN USER ---
    fun getCurrentUserInfo(): UserInfo? {
        val user = firebaseAuth.currentUser
        val savedProvider = prefs.getString(providerKey, "Không rõ") ?: "Không rõ"

        return if (user != null) {
            UserInfo(
                name = user.displayName ?: user.email ?: "Người dùng",
                email = user.email ?: "Không có email",
                provider = savedProvider
            )
        } else {
            if (initialAuthState) UserInfo("Đang tải...", "...", savedProvider) else null
        }
    }

    // --- CẬP NHẬT TÊN ---
    fun updateUserName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) onSuccess()
                    else onError(task.exception?.message ?: "Lỗi cập nhật tên")
                }
        } else {
            onError("Chưa đăng nhập")
        }
    }

    // --- ĐĂNG NHẬP GOOGLE ---
    suspend fun signInWithGoogleToken(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            _isAuthenticated.update { true }
            prefs.edit {
                putBoolean(authKey, true)
                putString(providerKey, "Google")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logout()
        }
    }

    // --- ĐĂNG NHẬP GITHUB ---
    fun signInWithGithub(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("allow_signup", "true")
        provider.scopes = listOf("user:email")

        val pendingResultTask = firebaseAuth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener {
                    _isAuthenticated.update { true }
                    prefs.edit {
                        putBoolean(authKey, true)
                        putString(providerKey, "GitHub")
                    }
                    onSuccess()
                }
                .addOnFailureListener { onError("Lỗi phiên cũ: ${it.message}") }
        } else {
            firebaseAuth
                .startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener {
                    _isAuthenticated.update { true }
                    prefs.edit {
                        putBoolean(authKey, true)
                        putString(providerKey, "GitHub")
                    }
                    onSuccess()
                }
                .addOnFailureListener { onError(it.message ?: "Đăng nhập GitHub thất bại") }
        }
    }

    // --- ĐĂNG XUẤT ---
    fun logout() {
        viewModelScope.launch {
            firebaseAuth.signOut()
            _isAuthenticated.update { false }
            prefs.edit {
                putBoolean(authKey, false)
                remove(providerKey)
            }
        }
    }

    // =========================================================
    //              TÍNH NĂNG LIÊN KẾT TÀI KHOẢN
    // =========================================================

    /**
     * Lấy danh sách các provider đang liên kết (vd: ["google.com", "github.com"])
     */
    fun getLinkedProviders(): List<String> {
        val user = firebaseAuth.currentUser
        // providerData chứa danh sách các phương thức đăng nhập của user này
        return user?.providerData?.map { it.providerId } ?: emptyList()
    }

    /**
     * Liên kết tài khoản hiện tại với GitHub
     */
    fun linkWithGithub(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            onError("Chưa đăng nhập")
            return
        }

        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("allow_signup", "true")
        provider.scopes = listOf("user:email")

        user.startActivityForLinkWithProvider(activity, provider.build())
            .addOnSuccessListener {
                onSuccess() // Liên kết thành công
            }
            .addOnFailureListener { e ->
                // Lỗi: credential-already-in-use nghĩa là GitHub này đã được dùng cho tài khoản khác
                onError(e.message ?: "Liên kết thất bại (Có thể tài khoản GitHub này đã được sử dụng?)")
            }
    }

    /**
     * Hủy liên kết (Unlink)
     */
    fun unlinkProvider(providerId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) return

        if (user.providerData.size <= 1) {
            onError("Bạn phải giữ lại ít nhất một phương thức đăng nhập!")
            return
        }

        user.unlink(providerId)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Lỗi hủy liên kết") }
    }
}