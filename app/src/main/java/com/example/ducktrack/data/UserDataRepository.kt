package com.example.ducktrack.data

import android.util.Log
import com.example.ducktrack.data.model.GrownTree
import com.example.ducktrack.ui.main.garden.SeedType
import com.example.ducktrack.ui.main.tasks.TodoTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.ducktrack.ui.main.tasks.TaskStats
import com.example.ducktrack.ui.main.settings.AchievementList
import com.example.ducktrack.ui.main.settings.AchievementType
import com.google.firebase.firestore.FieldValue
import com.example.ducktrack.ui.main.tasks.QuadrantStat // Import mới

class UserDataRepository(private val userDao: UserDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ... (Giữ nguyên phần 1, 2, 3, 4: UserPoints, Garden, Sync...) ...
    val userPoints: Flow<Int> = userDao.getUserProfile().map { it?.points ?: 0 }
    val unlockedSeeds: Flow<Set<SeedType>> = userDao.getUnlockedSeeds().map { list -> list.mapNotNull { SeedType.fromId(it.seedId) }.toSet() }

    fun getGrownTreesStream(): Flow<List<GrownTree>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { close(); return@callbackFlow }
        val collection = firestore.collection("users").document(uid).collection("garden")
        val listener = collection.orderBy("plantedAt", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val trees = snapshot.documents.mapNotNull { doc -> doc.toObject(GrownTree::class.java)?.copy(id = doc.id) }
                trySend(trees)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun addPoints(amount: Int) { withContext(Dispatchers.IO) { userDao.increasePoints(amount); syncUserProfileToCloud() } }
    suspend fun unlockSeed(seed: SeedType): Boolean {
        return withContext(Dispatchers.IO) {
            val currentPoints = userPoints.firstOrNull() ?: 0
            if (currentPoints >= seed.cost) {
                val newPoints = currentPoints - seed.cost
                userDao.upsertUserProfile(UserProfile(id = 1, points = newPoints))
                userDao.insertUnlockedSeed(UnlockedSeed(seed.id))
                syncUserProfileToCloud()
                true
            } else false
        }
    }
    suspend fun addGrownTreeToCloud(seed: SeedType, configString: String) {
        val uid = auth.currentUser?.uid ?: return
        val newTree = GrownTree(seedId = seed.id, plantedAt = System.currentTimeMillis(), config = configString)
        try { firestore.collection("users").document(uid).collection("garden").add(newTree).await() } catch (e: Exception) { e.printStackTrace() }
    }
    private suspend fun syncUserProfileToCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val points = userPoints.firstOrNull() ?: 0
            val seeds = unlockedSeeds.firstOrNull()?.map { it.id } ?: emptyList()
            firestore.collection("users").document(uid).set(hashMapOf("points" to points, "unlockedSeeds" to seeds), SetOptions.merge())
        } catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun restoreDataFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        withContext(Dispatchers.IO) {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                if (document.exists()) {
                    val points = document.getLong("points")?.toInt() ?: 0
                    val seedsList = document.get("unlockedSeeds") as? List<String> ?: emptyList()
                    userDao.upsertUserProfile(UserProfile(id = 1, points = points))
                    for (seedId in seedsList) userDao.insertUnlockedSeed(UnlockedSeed(seedId))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- PHẦN 5: QUẢN LÝ TASK (ĐÃ SỬA DÙNG ROOM DATABASE) ---

    fun getTasksStream(dateMs: Long): Flow<List<TodoTask>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val startOfDay = getStartOfDay(dateMs)
        val endOfDay = getEndOfDay(dateMs)

        val collection = firestore.collection("users").document(uid).collection("tasks")

        // Lắng nghe thay đổi trên Cloud
        val listener = collection
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        // Convert document thành object TodoTask
                        doc.toObject(TodoTask::class.java)?.copy(id = doc.id)
                    }
                    // Sắp xếp: Ghim lên đầu -> Chưa xong -> Đã xong -> Mới nhất
                    val sortedTasks = tasks.sortedWith(compareByDescending<TodoTask> { it.isPinned }
                        .thenBy { it.isCompleted }
                        .thenByDescending { it.id }) // id Firestore là chuỗi ngẫu nhiên, sort tạm

                    trySend(sortedTasks)
                }
            }
        awaitClose { listener.remove() }
    }

    // 2. Thêm mới lên Cloud
    suspend fun addTaskToCloud(
        description: String,
        dateMs: Long,
        isImportant: Boolean = false, // Mặc định false
        isUrgent: Boolean = false     // Mặc định false
    ) {
        val uid = auth.currentUser?.uid ?: return
        val newTask = TodoTask(
            description = description,
            isCompleted = false,
            isPinned = false,
            // Gán giá trị mới
            isImportant = isImportant,
            isUrgent = isUrgent,
            date = dateMs
        )
        try {
            firestore.collection("users").document(uid).collection("tasks").add(newTask).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // 3. Cập nhật (Sửa tên, tick xong, ghim) lên Cloud
    suspend fun updateTaskInCloud(task: TodoTask) {
        val uid = auth.currentUser?.uid ?: return
        if (task.id.isEmpty()) return
        try {
            // Lưu ý: set() sẽ ghi đè, nên truyền object đầy đủ
            firestore.collection("users").document(uid).collection("tasks")
                .document(task.id).set(task).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // 4. Xóa nhiều task trên Cloud
    suspend fun deleteMultipleTasksCloud(taskIds: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        val batch = firestore.batch()
        val colRef = firestore.collection("users").document(uid).collection("tasks")

        taskIds.forEach { id ->
            if (id.isNotEmpty()) batch.delete(colRef.document(id))
        }
        try { batch.commit().await() } catch (e: Exception) { e.printStackTrace() }
    }

    // 5. Ghim nhiều task trên Cloud
    suspend fun pinMultipleTasksCloud(taskIds: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        val batch = firestore.batch()
        val colRef = firestore.collection("users").document(uid).collection("tasks")

        taskIds.forEach { id ->
            if (id.isNotEmpty()) batch.update(colRef.document(id), "isPinned", true)
        }
        try { batch.commit().await() } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun addTask(description: String, dateMs: Long) {
        withContext(Dispatchers.IO) {
            val newTask = TaskEntity(description = description, date = dateMs)
            userDao.insertTask(newTask)
        }
    }

    suspend fun updateTask(task: TodoTask) {
        withContext(Dispatchers.IO) {
            // Chuyển ngược UI -> Entity
            val entity = TaskEntity(
                id = task.id.toIntOrNull() ?: 0,
                description = task.description,
                isCompleted = task.isCompleted,
                isPinned = task.isPinned,
                date = task.date
            )
            userDao.updateTask(entity)
        }
    }

    suspend fun deleteMultipleTasks(taskIds: Set<String>) {
        withContext(Dispatchers.IO) {
            val ids = taskIds.mapNotNull { it.toIntOrNull() }
            if (ids.isNotEmpty()) userDao.deleteTasksByIds(ids)
        }
    }

    suspend fun pinMultipleTasks(taskIds: Set<String>) {
        withContext(Dispatchers.IO) {
            val ids = taskIds.mapNotNull { it.toIntOrNull() }
            if (ids.isNotEmpty()) userDao.updateTasksPinStatus(ids, true)
        }
    }

    // ... (Giữ nguyên Helper date)
    private fun getStartOfDay(time: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = time }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    private fun getEndOfDay(time: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = time }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23); cal.set(java.util.Calendar.MINUTE, 59); cal.set(java.util.Calendar.SECOND, 59); cal.set(java.util.Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    suspend fun getTaskStats(): TaskStats {
        val uid = auth.currentUser?.uid ?: return TaskStats()
        try {
            // Lấy toàn bộ task về để xử lý
            val snapshot = firestore.collection("users").document(uid)
                .collection("tasks")
                .get()
                .await()

            val allDocs = snapshot.documents

            // 1. Thống kê tổng (Cũ)
            val total = allDocs.size
            val completed = allDocs.count { it.getBoolean("completed") == true }
            val pending = total - completed

            // 2. Thống kê chi tiết 4 thẻ (Mới)
            // Định nghĩa 4 nhóm
            val q1Name = "Quan trọng & Khẩn cấp"
            val q2Name = "Quan trọng & Không khẩn cấp"
            val q3Name = "Không quan trọng & Khẩn cấp"
            val q4Name = "Không quan trọng & Không khẩn cấp"

            // Biến đếm tạm thời
            var q1T = 0; var q1C = 0
            var q2T = 0; var q2C = 0
            var q3T = 0; var q3C = 0
            var q4T = 0; var q4C = 0

            for (doc in allDocs) {
                val isComp = doc.getBoolean("completed") == true
                val isImp = doc.getBoolean("important") == true
                val isUrg = doc.getBoolean("urgent") == true

                if (isImp && isUrg) {           // Q1
                    q1T++
                    if (isComp) q1C++
                } else if (isImp && !isUrg) {   // Q2
                    q2T++
                    if (isComp) q2C++
                } else if (!isImp && isUrg) {   // Q3
                    q3T++
                    if (isComp) q3C++
                } else {                        // Q4
                    q4T++
                    if (isComp) q4C++
                }
            }

            val details = listOf(
                QuadrantStat(q1Name, q1T, q1C, q1T - q1C),
                QuadrantStat(q2Name, q2T, q2C, q2T - q2C),
                QuadrantStat(q3Name, q3T, q3C, q3T - q3C),
                QuadrantStat(q4Name, q4T, q4C, q4T - q4C)
            )

            return TaskStats(total, completed, pending, details)

        } catch (e: Exception) {
            e.printStackTrace()
            return TaskStats()
        }
    }

    // Hàm này sẽ tính toán lại toàn bộ chỉ số và cập nhật thành tựu mới lên Firestore
    suspend fun checkAndSyncAchievements(): List<String> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        try {
            // 1. Lấy danh sách đã unlock từ Firestore
            val userDoc = firestore.collection("users").document(uid).get().await()
            val currentUnlocked = (userDoc.get("unlockedAchievements") as? List<String> ?: emptyList()).toMutableSet()

            // 2. Lấy dữ liệu thống kê
            // - Task
            val taskStats = getTaskStats()
            val pinnedTasks = firestore.collection("users").document(uid).collection("tasks")
                .whereEqualTo("pinned", true).get().await().size()

            // - Garden
            val gardenSnapshot = firestore.collection("users").document(uid).collection("garden").get().await()
            val allTrees = gardenSnapshot.documents
            val totalTrees = allTrees.size
            val pineTrees = allTrees.count { it.getString("seedId") == "pine" }
            val redTrees = allTrees.count { it.getString("seedId") == "red_leaf" } + allTrees.count { it.getString("seedId") == "redleaf" }
            // - Profile info (check xem có khác default không)
            val duckName = userDoc.getString("duckName") ?: "Vịt con"
            val hasChangedDuckName = duckName != "Vịt con"
            val photoBase64 = userDoc.getString("avatarBase64")
            val hasAvatar = photoBase64 != null
            // (Tên user lấy từ Auth hoặc doc, tạm coi là luôn true nếu đã login)

            val newUnlocked = mutableListOf<String>()

            // 3. CHECK 30 THÀNH TỰU THƯỜNG (Loại bỏ master_complete ra để check sau)
            val normalAchievements = AchievementList.list.filter { it.id != "master_complete" }

            normalAchievements.forEach { achievement ->
                if (!currentUnlocked.contains(achievement.id)) {
                    val isReached = when (achievement.type) {
                        AchievementType.TASK_COMPLETED -> taskStats.completed >= achievement.target
                        AchievementType.TASK_PINNED -> pinnedTasks >= achievement.target
                        AchievementType.TREE_COUNT -> totalTrees >= achievement.target
                        AchievementType.PINE_LOVER -> pineTrees >= achievement.target
                        AchievementType.RED_LEAF_LOVER -> redTrees >= achievement.target
                        AchievementType.FOCUS_SESSION -> totalTrees >= achievement.target // Tạm tính 1 cây = 1 phiên
                        AchievementType.PROFILE_UPDATE -> {
                            when(achievement.id) {
                                "profile_duck" -> hasChangedDuckName
                                "profile_avatar" -> hasAvatar
                                "profile_name" -> true // Coi như luôn đạt
                                else -> false
                            }
                        }
                        // Check số lượng thành tựu đã đạt được (Recursive logic nhỏ)
                        AchievementType.MASTER_ALL -> currentUnlocked.size >= achievement.target
                        else -> false
                    }

                    if (isReached) {
                        newUnlocked.add(achievement.id)
                        currentUnlocked.add(achievement.id) // Add vào set tạm để tính cho master
                    }
                }
            }

            // 4. CHECK THÀNH TỰU CUỐI CÙNG (MASTER)
            val masterAchieve = AchievementList.list.find { it.id == "master_complete" }!!
            if (!currentUnlocked.contains(masterAchieve.id)) {
                // Đếm số thành tựu thường đã đạt
                val totalNormalUnlocked = currentUnlocked.count { id -> normalAchievements.any { it.id == id } }
                if (totalNormalUnlocked >= masterAchieve.target) {
                    newUnlocked.add(masterAchieve.id)
                    currentUnlocked.add(masterAchieve.id)
                }
            }

            // 5. Update Firestore nếu có cái mới
            if (newUnlocked.isNotEmpty()) {
                firestore.collection("users").document(uid)
                    .update("unlockedAchievements", FieldValue.arrayUnion(*newUnlocked.toTypedArray()))
                    .await()
            }

            return currentUnlocked.toList()

        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun selectAchievement(achievementId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            // Update field 'selectedAchievementId' trong document user
            firestore.collection("users").document(uid)
                .update("selectedAchievementId", achievementId)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}