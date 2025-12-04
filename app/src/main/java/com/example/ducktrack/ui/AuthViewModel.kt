package com.example.ducktrack.ui

import android.app.Activity
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MyApplication
import com.example.ducktrack.ui.main.garden.SeedType
import com.example.ducktrack.ui.main.tasks.TaskStats // Import Model TaskStats
import com.example.ducktrack.utils.AppUsageCsvItem
import com.example.ducktrack.utils.CsvExporter
import com.example.ducktrack.utils.ImageUtils
import com.example.ducktrack.utils.TreeHistoryCsvItem
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
import java.util.Calendar

// Data Model chứa thông tin người dùng
data class UserInfo(
    val name: String,
    val email: String,
    val provider: String,
    val photoBase64: String?,
    val duckName: String = "Vịt con",
    val createdAt: Long = 0L,
    val treeCounts: Map<String, Int> = emptyMap(),
    val selectedAchievementId: String? = null
)

class AuthViewModel(context: Context) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("DuckTrackPrefs", Context.MODE_PRIVATE)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // --- THÊM: Repository để lấy dữ liệu Task ---
    private val repository = (context.applicationContext as MyApplication).repository

    // Key lưu trong SharedPreferences
    private val providerKey = "LOGIN_PROVIDER"
    private val authKey = "IS_LOGGED_IN"

    // Context ứng dụng
    private val appContext = context.applicationContext

    // StateFlow quản lý trạng thái
    private val initialAuthState: Boolean get() = firebaseAuth.currentUser != null
    private val _isAuthenticated = MutableStateFlow(initialAuthState)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    // --- THÊM: StateFlow chứa thống kê Task ---
    private val _taskStats = MutableStateFlow(TaskStats())
    val taskStats: StateFlow<TaskStats> = _taskStats.asStateFlow()

    private var _cachedBase64Avatar: String? = null

    init {
        if (!initialAuthState) {
            prefs.edit { putBoolean(authKey, false) }
        }
    }

    // =========================================================================
    //                           QUẢN LÝ THÔNG TIN USER
    // =========================================================================

    fun loadUserInfo(onLoaded: (UserInfo) -> Unit) {
        val user = firebaseAuth.currentUser
        val savedProvider = prefs.getString(providerKey, "Không rõ") ?: "Không rõ"

        if (user != null) {
            viewModelScope.launch {
                val name = user.displayName ?: user.email ?: "Người dùng"
                val email = user.email ?: "Không có email"
                val createdAt = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
                var selectedAchieveId: String? = null

                var avatarStr = _cachedBase64Avatar
                var duckName = "Vịt con"
                val treeCounts = mutableMapOf<String, Int>()

                // 1. Lấy thông tin User & Vườn cây từ Firestore
                try {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    if (doc.exists()) {
                        avatarStr = doc.getString("avatarBase64")
                        _cachedBase64Avatar = avatarStr
                        doc.getString("duckName")?.let { duckName = it }
                        selectedAchieveId = doc.getString("selectedAchievementId")
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

                // 2. --- THÊM MỚI: Lấy thống kê Nhiệm vụ từ Repository ---
                // (Repository đã được cập nhật để gọi Firestore đếm task)
                val stats = repository.getTaskStats()
                _taskStats.value = stats

                onLoaded(UserInfo(name, email, savedProvider, avatarStr, duckName, createdAt, treeCounts, selectedAchieveId))
            }
        }
    }

    // ... (Giữ nguyên getCurrentUserInfo, updateAvatar, updateUserName, updateDuckName...)
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

    fun updateAvatar(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val base64 = withContext(Dispatchers.IO) { ImageUtils.uriToBase64(appContext, uri) }
                if (base64 == null) throw Exception("Lỗi xử lý ảnh")
                firestore.collection("users").document(user.uid).set(mapOf("avatarBase64" to base64), SetOptions.merge()).await()
                _cachedBase64Avatar = base64; _isUploading.value = false; onSuccess()
            } catch (e: Exception) { _isUploading.value = false; onError(e.message ?: "Lỗi upload") }
        }
    }

    fun updateUserName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        firebaseAuth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(newName).build())
            ?.addOnCompleteListener { if(it.isSuccessful) onSuccess() else onError(it.exception?.message ?: "Lỗi") }
    }

    fun updateDuckName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        viewModelScope.launch { try { firestore.collection("users").document(user.uid).set(mapOf("duckName" to newName), SetOptions.merge()).await(); onSuccess() } catch (ex: Exception) { onError(ex.message ?: "Lỗi") } }
    }

    suspend fun signInWithGoogleToken(idToken: String) {
        try { firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await(); _isAuthenticated.update { true }; prefs.edit { putBoolean(authKey, true); putString(providerKey, "Google") } } catch (e: Exception) { logout() }
    }

    fun signInWithGithub(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val provider = OAuthProvider.newBuilder("github.com"); provider.addCustomParameter("allow_signup", "true"); provider.scopes = listOf("user:email")
        val pending = firebaseAuth.pendingAuthResult
        if (pending != null) { pending.addOnSuccessListener { _isAuthenticated.update { true }; prefs.edit { putBoolean(authKey, true); putString(providerKey, "GitHub") }; onSuccess() }.addOnFailureListener { onError(it.message ?: "") } }
        else { firebaseAuth.startActivityForSignInWithProvider(activity, provider.build()).addOnSuccessListener { _isAuthenticated.update { true }; prefs.edit { putBoolean(authKey, true); putString(providerKey, "GitHub") }; onSuccess() }.addOnFailureListener { onError(it.message ?: "") } }
    }

    fun logout() { viewModelScope.launch { firebaseAuth.signOut(); _isAuthenticated.update { false }; _cachedBase64Avatar = null; prefs.edit { putBoolean(authKey, false); remove(providerKey) } } }
    fun getLinkedProviders() = firebaseAuth.currentUser?.providerData?.map { it.providerId } ?: emptyList()
    fun linkWithGithub(act: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) { firebaseAuth.currentUser?.startActivityForLinkWithProvider(act, OAuthProvider.newBuilder("github.com").build())?.addOnSuccessListener { onSuccess() }?.addOnFailureListener { onError(it.message ?: "") } }
    fun unlinkProvider(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) { if((firebaseAuth.currentUser?.providerData?.size ?: 0) <= 1) onError("Giữ lại ít nhất 1 phương thức!") else firebaseAuth.currentUser?.unlink(id)?.addOnSuccessListener { onSuccess() }?.addOnFailureListener { onError(it.message ?: "") } }

    // =========================================================================
    //               LOGIC XUẤT DỮ LIỆU (EXPORT CSV) - ĐÃ CẬP NHẬT
    // =========================================================================

    fun exportData(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val user = firebaseAuth.currentUser
                if (user == null) {
                    _isUploading.value = false
                    onError("Chưa đăng nhập!")
                    return@launch
                }

                // 1. Lấy dữ liệu User
                val userInfoDeferred = withContext(Dispatchers.IO) {
                    val savedProvider = prefs.getString("LOGIN_PROVIDER", "Không rõ") ?: "Không rõ"
                    val name = user.displayName ?: user.email ?: "Người dùng"
                    val email = user.email ?: "Không có email"
                    val createdAt = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
                    var duckName = "Vịt con"
                    try {
                        val doc = firestore.collection("users").document(user.uid).get().await()
                        if (doc.exists()) doc.getString("duckName")?.let { duckName = it }
                    } catch (e: Exception) { e.printStackTrace() }
                    UserInfo(name, email, savedProvider, null, duckName, createdAt, emptyMap())
                }

                // 2. Lấy dữ liệu thống kê khác
                val weeklyTrees = getWeeklyTreeHistory(user.uid)
                val monthlyApps = getTopAppsUsage(context)

                // --- THÊM: Lấy thống kê Task mới nhất để ghi vào file ---
                val currentTaskStats = repository.getTaskStats()

                // 3. Lưu file (Truyền thêm taskStats)
                withContext(Dispatchers.IO) {
                    // Cập nhật CsvExporter.saveToDownloads nhận thêm tham số taskStats
                    val resultMessage = CsvExporter.saveToDownloads(
                        context,
                        userInfoDeferred,
                        weeklyTrees,
                        monthlyApps,
                        currentTaskStats // <-- Truyền vào đây
                    )

                    withContext(Dispatchers.Main) {
                        _isUploading.value = false
                        if (resultMessage != null) {
                            val realPath = resultMessage.replace("Đã lưu tại: ", "")
                            onSuccess(realPath)
                        } else {
                            onError("Lỗi khi lưu file.")
                        }
                    }
                }
            } catch (e: Exception) {
                _isUploading.value = false
                onError(e.message ?: "Lỗi export")
            }
        }
    }

    // --- HELPER 1: Lấy cây trồng từ Thứ 2 -> Chủ nhật tuần này ---
    private suspend fun getWeeklyTreeHistory(uid: String): List<TreeHistoryCsvItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TreeHistoryCsvItem>()
        try {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfWeek = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 7)
            val endOfWeek = cal.timeInMillis

            val snapshot = firestore.collection("users").document(uid)
                .collection("garden")
                .whereGreaterThanOrEqualTo("plantedAt", startOfWeek)
                .whereLessThan("plantedAt", endOfWeek)
                .get().await()

            for (doc in snapshot.documents) {
                val seedId = doc.getString("seedId") ?: ""
                val treeNameVN = when (seedId) {
                    "normal" -> "Cây thường"
                    "pine" -> "Cây thông"
                    "red_leaf", "redleaf" -> "Cây lá đỏ"
                    "sakura" -> "Hoa anh đào"
                    else -> "Cây khác ($seedId)"
                }
                val timestamp = doc.getLong("plantedAt") ?: 0L
                val configStr = doc.getString("config") ?: "N/A"
                val status = doc.getString("status") ?: "Hoàn thành"

                list.add(TreeHistoryCsvItem(treeNameVN, timestamp, configStr, status))
            }
            list.sortByDescending { it.timestamp }
        } catch (e: Exception) { e.printStackTrace() }
        list
    }

    // --- HELPER 2: Lấy Top 10 App (30 ngày) ---
    private suspend fun getTopAppsUsage(context: Context): List<AppUsageCsvItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AppUsageCsvItem>()
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val packageManager = context.packageManager

            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -30)
            val startTime = cal.timeInMillis

            val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

            if (statsMap.isNotEmpty()) {
                val sortedStats = statsMap.values
                    .filter { it.totalTimeInForeground > 0 }
                    .sortedByDescending { it.totalTimeInForeground }

                var count = 0
                for (usageStats in sortedStats) {
                    if (count >= 10) break
                    val packageName = usageStats.packageName
                    var appName = packageName
                    try {
                        val ai = packageManager.getApplicationInfo(packageName, 0)
                        appName = packageManager.getApplicationLabel(ai).toString()
                    } catch (e: PackageManager.NameNotFoundException) { }

                    list.add(AppUsageCsvItem(appName, usageStats.totalTimeInForeground))
                    count++
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        list
    }
}