// FILE: AuthViewModel.kt
package com.example.ducktrack.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.ui.main.garden.SeedType
import com.example.ducktrack.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.ducktrack.utils.CsvExporter
import java.io.File

data class UserInfo(
    val name: String,
    val email: String,
    val provider: String,
    val photoBase64: String?,
    val duckName: String = "Vịt con",
    val createdAt: Long = 0L,
    val treeCounts: Map<String, Int> = emptyMap()
)

class AuthViewModel(context: Context) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("DuckTrackPrefs", Context.MODE_PRIVATE)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val providerKey = "LOGIN_PROVIDER"
    private val authKey = "IS_LOGGED_IN"
    private val appContext = context.applicationContext

    private val initialAuthState: Boolean get() = firebaseAuth.currentUser != null
    private val _isAuthenticated = MutableStateFlow(initialAuthState)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private var _cachedBase64Avatar: String? = null

    init {
        if (!initialAuthState) { prefs.edit { putBoolean(authKey, false) } }
    }

    // --- LOAD USER INFO ---
    fun loadUserInfo(onLoaded: (UserInfo) -> Unit) {
        val user = firebaseAuth.currentUser
        val savedProvider = prefs.getString(providerKey, "Không rõ") ?: "Không rõ"

        if (user != null) {
            viewModelScope.launch {
                val name = user.displayName ?: user.email ?: "Người dùng"
                val email = user.email ?: "Không có email"
                val createdAt = user.metadata?.creationTimestamp ?: System.currentTimeMillis()

                var avatarStr = _cachedBase64Avatar
                var duckName = "Vịt con"
                val treeCounts = mutableMapOf<String, Int>()

                try {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    if (doc.exists()) {
                        avatarStr = doc.getString("avatarBase64")
                        _cachedBase64Avatar = avatarStr
                        doc.getString("duckName")?.let { duckName = it }
                    }

                    val gardenSnapshot = firestore.collection("users").document(user.uid)
                        .collection("garden").get().await()

                    for (treeDoc in gardenSnapshot.documents) {
                        val seedId = treeDoc.getString("seedId") ?: SeedType.NORMAL.id
                        val currentCount = treeCounts[seedId] ?: 0
                        treeCounts[seedId] = currentCount + 1
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onLoaded(UserInfo(name, email, savedProvider, avatarStr, duckName, createdAt, treeCounts))
            }
        }
    }

    fun getCurrentUserInfo(): UserInfo? {
        val user = firebaseAuth.currentUser
        val savedProvider = prefs.getString(providerKey, "Không rõ") ?: "Không rõ"
        return if (user != null) {
            UserInfo(
                name = user.displayName ?: "Người dùng",
                email = user.email ?: "...",
                provider = savedProvider,
                photoBase64 = _cachedBase64Avatar
            )
        } else null
    }

    // --- UPDATE AVATAR ---
    fun updateAvatar(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val base64 = withContext(Dispatchers.IO) { ImageUtils.uriToBase64(appContext, uri) }
                if (base64 == null) throw Exception("Lỗi xử lý ảnh")

                firestore.collection("users").document(user.uid)
                    .set(mapOf("avatarBase64" to base64), SetOptions.merge())
                    .await()

                _cachedBase64Avatar = base64
                _isUploading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isUploading.value = false
                onError(e.message ?: "Lỗi upload")
            }
        }
    }

    // --- UPDATE NAME ---
    fun updateUserName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        val updates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
        user.updateProfile(updates).addOnCompleteListener {
            if(it.isSuccessful) onSuccess() else onError(it.exception?.message ?: "Lỗi")
        }
    }

    // --- UPDATE DUCK NAME ---
    fun updateDuckName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(user.uid)
                    .set(mapOf("duckName" to newName), SetOptions.merge())
                    .await()
                onSuccess()
            } catch (ex: Exception) {
                onError(ex.message ?: "Lỗi")
            }
        }
    }

    // --- AUTH METHODS ---
    suspend fun signInWithGoogleToken(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            _isAuthenticated.update { true }
            prefs.edit { putBoolean(authKey, true); putString(providerKey, "Google") }
        } catch (e: Exception) { logout() }
    }

    fun signInWithGithub(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("allow_signup", "true")
        provider.scopes = listOf("user:email")

        val pending = firebaseAuth.pendingAuthResult
        if (pending != null) {
            pending.addOnSuccessListener {
                _isAuthenticated.update { true }
                prefs.edit { putBoolean(authKey, true); putString(providerKey, "GitHub") }
                onSuccess()
            }.addOnFailureListener { onError(it.message ?: "") }
        } else {
            firebaseAuth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener {
                    _isAuthenticated.update { true }
                    prefs.edit { putBoolean(authKey, true); putString(providerKey, "GitHub") }
                    onSuccess()
                }.addOnFailureListener { onError(it.message ?: "") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            firebaseAuth.signOut()
            _isAuthenticated.update { false }
            _cachedBase64Avatar = null
            prefs.edit { putBoolean(authKey, false); remove(providerKey) }
        }
    }

    fun getLinkedProviders() = firebaseAuth.currentUser?.providerData?.map { it.providerId } ?: emptyList()

    fun linkWithGithub(act: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("allow_signup", "true")
        provider.scopes = listOf("user:email")

        firebaseAuth.currentUser?.startActivityForLinkWithProvider(act, provider.build())
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { onError(it.message ?: "") }
    }

    fun unlinkProvider(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if((firebaseAuth.currentUser?.providerData?.size ?: 0) <= 1) {
            onError("Giữ lại ít nhất 1 phương thức!")
        } else {
            firebaseAuth.currentUser?.unlink(id)
                ?.addOnSuccessListener { onSuccess() }
                ?.addOnFailureListener { onError(it.message ?: "") }
        }
    }

    fun exportData(context: Context, onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true // Tận dụng biến loading này để hiện xoay xoay nếu muốn
            try {
                // 1. Đảm bảo lấy data mới nhất từ Firestore trước khi xuất
                val user = firebaseAuth.currentUser
                if (user == null) {
                    onError("Chưa đăng nhập!")
                    _isUploading.value = false
                    return@launch
                }

                // Load lại info (tái sử dụng logic của loadUserInfo nhưng dạng suspend để đợi)
                // Hoặc đơn giản là dùng data đang cache nếu chấp nhận độ trễ
                // Ở đây mình gọi loadUserInfo và export trong callback cho chắc ăn
                loadUserInfo { latestInfo ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val file = CsvExporter.exportUserData(context, latestInfo)
                        withContext(Dispatchers.Main) {
                            _isUploading.value = false
                            if (file != null) {
                                onSuccess(file)
                            } else {
                                onError("Lỗi khi tạo file CSV")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _isUploading.value = false
                onError(e.message ?: "Lỗi không xác định")
            }
        }
    }
}