package com.example.ducktrack.ui.main.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AchievementsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MyApplication).repository
    // Cần thêm Firestore để lấy selectedId (hoặc truyền từ AuthVM, nhưng gọi trực tiếp cho nhanh gọn ở màn này)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _unlockedIds = MutableStateFlow<List<String>>(emptyList())
    val unlockedIds: StateFlow<List<String>> = _unlockedIds.asStateFlow()

    // State lưu ID đang được chọn
    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAchievements()
    }

    fun loadAchievements() {
        viewModelScope.launch {
            _isLoading.value = true
            // 1. Sync & Get Unlocked
            val ids = repository.checkAndSyncAchievements()
            _unlockedIds.value = ids

            // 2. Get Selected ID
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val doc = firestore.collection("users").document(uid).get().await()
                    _selectedId.value = doc.getString("selectedAchievementId")
                } catch (e: Exception) { e.printStackTrace() }
            }

            _isLoading.value = false
        }
    }

    // Hàm chọn thành tựu
    fun selectAchievement(id: String) {
        viewModelScope.launch {
            _selectedId.value = id // Update UI ngay lập tức cho mượt
            repository.selectAchievement(id) // Lưu xuống DB
        }
    }
}