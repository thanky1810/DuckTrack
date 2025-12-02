package com.example.ducktrack.data

import android.util.Log
import com.example.ducktrack.ui.main.garden.SeedType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserDataRepository(private val userDao: UserDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 1. Lấy điểm (Local)
    val userPoints: Flow<Int> = userDao.getUserProfile().map { it?.points ?: 0 }

    // 2. Lấy hạt giống (Local)
    val unlockedSeeds: Flow<Set<SeedType>> = userDao.getUnlockedSeeds().map { list ->
        list.mapNotNull { SeedType.fromId(it.seedId) }.toSet()
    }

    // 3. Lấy cây đã trồng (Local)
    val grownTrees: Flow<List<SeedType>> = userDao.getGrownTrees().map { list ->
        list.take(12).mapNotNull { SeedType.fromId(it.seedId) }
    }

    // --- CÁC HÀM THAY ĐỔI DỮ LIỆU (Local + Cloud) ---

    suspend fun addPoints(amount: Int) {
        withContext(Dispatchers.IO) {
            // 1. Cập nhật Local (SQL)
            userDao.increasePoints(amount)
            // 2. Cập nhật Cloud
            syncPointsToCloud()
        }
    }

    suspend fun unlockSeed(seed: SeedType): Boolean {
        return withContext(Dispatchers.IO) {
            val currentPoints = userPoints.firstOrNull() ?: 0
            if (currentPoints >= seed.cost) {
                // 1. Trừ điểm Local
                val newPoints = currentPoints - seed.cost
                userDao.upsertUserProfile(UserProfile(id = 1, points = newPoints))
                // 2. Thêm hạt giống Local
                userDao.insertUnlockedSeed(UnlockedSeed(seed.id))

                // 3. Đồng bộ lên Cloud
                syncPointsToCloud() // Cập nhật điểm mới
                syncUnlockedSeedsToCloud() // Cập nhật danh sách hạt
                true
            } else {
                false
            }
        }
    }

    suspend fun addGrownTree(seed: SeedType) {
        withContext(Dispatchers.IO) {
            val count = userDao.getGrownTreeCount()
            if (count < 12) {
                // 1. Trồng cây Local
                userDao.insertGrownTree(GrownTree(seedId = seed.id))
                // 2. Đồng bộ lên Cloud
                syncGrownTreesToCloud()
            }
        }
    }

    // --- CÁC HÀM ĐỒNG BỘ LÊN CLOUD (Private Helper) ---

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun syncPointsToCloud() {
        val uid = getCurrentUserId() ?: return
        // Lấy điểm mới nhất từ Flow (dùng first để lấy giá trị hiện tại)
        // Lưu ý: Coroutine này chạy bất đồng bộ
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val points = userPoints.firstOrNull() ?: 0
            val data = hashMapOf("points" to points)
            firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
        }
    }

    private fun syncUnlockedSeedsToCloud() {
        val uid = getCurrentUserId() ?: return
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val seeds = unlockedSeeds.firstOrNull()?.map { it.id } ?: emptyList()
            val data = hashMapOf("unlockedSeeds" to seeds)
            firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
        }
    }

    private fun syncGrownTreesToCloud() {
        val uid = getCurrentUserId() ?: return
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val trees = grownTrees.firstOrNull()?.map { it.id } ?: emptyList()
            val data = hashMapOf("grownTrees" to trees)
            firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
        }
    }

    // --- HÀM QUAN TRỌNG: KHÔI PHỤC DỮ LIỆU TỪ CLOUD (Dùng khi Login) ---
    suspend fun restoreDataFromCloud() {
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                // 1. Tải dữ liệu từ Firestore
                val document = firestore.collection("users").document(uid).get().await()
                if (document.exists()) {
                    // 2. Lấy dữ liệu ra
                    val points = document.getLong("points")?.toInt() ?: 0
                    val seedsList = document.get("unlockedSeeds") as? List<String> ?: emptyList()
                    val treesList = document.get("grownTrees") as? List<String> ?: emptyList()

                    // 3. Ghi đè vào Room Local
                    // - Điểm
                    userDao.upsertUserProfile(UserProfile(id = 1, points = points))

                    // - Hạt giống (Cần check trùng trong Dao nhưng ở đây cứ insert)
                    for (seedId in seedsList) {
                        userDao.insertUnlockedSeed(UnlockedSeed(seedId))
                    }

                    // - Cây đã trồng (Xóa cũ đi trồng lại cho chắc, hoặc trồng tiếp)
                    // Ở đây ta trồng tiếp những cây chưa có, hoặc đơn giản là loop insert
                    // Vì GrownTree ID tự tăng nên ta cứ insert list từ cloud về
                    // (Lưu ý: Cách này có thể bị duplicate nếu logic không chặt,
                    // nhưng với cấu trúc đơn giản hiện tại thì tạm ổn cho việc restore khi cài lại app)

                    // Để an toàn khi "Cài lại app" (Database trống), ta chỉ cần insert.
                    // Nhưng nếu "Đăng nhập lại trên máy cũ" (Database còn), ta nên xóa sạch cũ trước?
                    // -> Để đơn giản: Ta xóa sạch bảng cây trồng cũ và nạp lại từ Cloud
                    userDao.deleteAllGrownTrees() // (Cần thêm hàm này vào UserDao)
                    for (treeId in treesList) {
                        userDao.insertGrownTree(GrownTree(seedId = treeId))
                    }
                }
            } catch (e: Exception) {
                Log.e("UserDataRepository", "Error restoring data", e)
            }
        }
    }
}