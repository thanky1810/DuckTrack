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
import com.example.ducktrack.ui.main.tasks.QuadrantStat

class UserDataRepository(private val userDao: UserDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- PHẦN 1: DỮ LIỆU TỪ ROOM (LOCAL) ---
    val userPoints: Flow<Int> = userDao.getUserProfile().map { it?.points ?: 0 }
    val unlockedSeeds: Flow<Set<SeedType>> = userDao.getUnlockedSeeds().map { list -> list.mapNotNull { SeedType.fromId(it.seedId) }.toSet() }

    fun getGrownTreesStream(): Flow<List<GrownTree>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { close(); return@callbackFlow }
        val collection = firestore.collection("users").document(uid).collection("garden")
        val listener = collection.orderBy("plantedAt", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val trees = snapshot.documents.mapNotNull { doc ->
                    // toObject sẽ tự map các trường mới nếu tên khớp
                    doc.toObject(GrownTree::class.java)?.copy(id = doc.id)
                }
                trySend(trees)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun addPoints(amount: Int) { withContext(Dispatchers.IO) { userDao.increasePoints(amount); syncUserProfileToCloud() } }

    // --- MỚI: Hàm trừ điểm ---
    suspend fun deductPoints(amount: Int) {
        withContext(Dispatchers.IO) {
            userDao.decreasePoints(amount)
            syncUserProfileToCloud()
        }
    }

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
    suspend fun addGrownTreeToCloud(
        seed: SeedType,
        configString: String,
        sessionSetIndex: Int, // Mới
        sessionIndexInSet: Int // Mới
    ) {
        val uid = auth.currentUser?.uid ?: return
        val newTree = GrownTree(
            seedId = seed.id,
            plantedAt = System.currentTimeMillis(),
            config = configString,
            // Lưu thông tin phiên
            sessionSetIndex = sessionSetIndex,
            sessionIndexInSet = sessionIndexInSet
        )
        try {
            firestore.collection("users").document(uid).collection("garden").add(newTree).await()
        } catch (e: Exception) { e.printStackTrace() }
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

    // --- PHẦN 5: QUẢN LÝ TASK ---
    fun getTasksStream(dateMs: Long): Flow<List<TodoTask>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { close(); return@callbackFlow }
        val startOfDay = getStartOfDay(dateMs)
        val endOfDay = getEndOfDay(dateMs)
        val collection = firestore.collection("users").document(uid).collection("tasks")
        val listener = collection.whereGreaterThanOrEqualTo("date", startOfDay).whereLessThanOrEqualTo("date", endOfDay).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val tasks = snapshot.documents.mapNotNull { doc -> doc.toObject(TodoTask::class.java)?.copy(id = doc.id) }
                val sortedTasks = tasks.sortedWith(compareByDescending<TodoTask> { it.isPinned }.thenBy { it.isCompleted }.thenByDescending { it.id })
                trySend(sortedTasks)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun addTaskToCloud(description: String, dateMs: Long, isImportant: Boolean = false, isUrgent: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return
        val newTask = TodoTask(description = description, isCompleted = false, isPinned = false, isImportant = isImportant, isUrgent = isUrgent, date = dateMs)
        try { firestore.collection("users").document(uid).collection("tasks").add(newTask).await() } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun updateTaskInCloud(task: TodoTask) {
        val uid = auth.currentUser?.uid ?: return
        if (task.id.isEmpty()) return
        try { firestore.collection("users").document(uid).collection("tasks").document(task.id).set(task).await() } catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun deleteMultipleTasksCloud(taskIds: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        val batch = firestore.batch()
        val colRef = firestore.collection("users").document(uid).collection("tasks")
        taskIds.forEach { id -> if (id.isNotEmpty()) batch.delete(colRef.document(id)) }
        try { batch.commit().await() } catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun pinMultipleTasksCloud(taskIds: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        val batch = firestore.batch()
        val colRef = firestore.collection("users").document(uid).collection("tasks")
        taskIds.forEach { id -> if (id.isNotEmpty()) batch.update(colRef.document(id), "isPinned", true) }
        try { batch.commit().await() } catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun addTask(description: String, dateMs: Long) { withContext(Dispatchers.IO) { val newTask = TaskEntity(description = description, date = dateMs); userDao.insertTask(newTask) } }
    suspend fun updateTask(task: TodoTask) { withContext(Dispatchers.IO) { val entity = TaskEntity(id = task.id.toIntOrNull() ?: 0, description = task.description, isCompleted = task.isCompleted, isPinned = task.isPinned, date = task.date); userDao.updateTask(entity) } }
    suspend fun deleteMultipleTasks(taskIds: Set<String>) { withContext(Dispatchers.IO) { val ids = taskIds.mapNotNull { it.toIntOrNull() }; if (ids.isNotEmpty()) userDao.deleteTasksByIds(ids) } }
    suspend fun pinMultipleTasks(taskIds: Set<String>) { withContext(Dispatchers.IO) { val ids = taskIds.mapNotNull { it.toIntOrNull() }; if (ids.isNotEmpty()) userDao.updateTasksPinStatus(ids, true) } }

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
            val snapshot = firestore.collection("users").document(uid).collection("tasks").get().await()
            val allDocs = snapshot.documents
            val total = allDocs.size
            val completed = allDocs.count { it.getBoolean("completed") == true }
            val pending = total - completed
            val q1Name = "Quan trọng & Khẩn cấp"; val q2Name = "Quan trọng & Không khẩn cấp"; val q3Name = "Không quan trọng & Khẩn cấp"; val q4Name = "Không quan trọng & Không khẩn cấp"
            var q1T = 0; var q1C = 0; var q2T = 0; var q2C = 0; var q3T = 0; var q3C = 0; var q4T = 0; var q4C = 0
            for (doc in allDocs) {
                val isComp = doc.getBoolean("completed") == true
                val isImp = doc.getBoolean("important") == true
                val isUrg = doc.getBoolean("urgent") == true
                if (isImp && isUrg) { q1T++; if (isComp) q1C++ }
                else if (isImp && !isUrg) { q2T++; if (isComp) q2C++ }
                else if (!isImp && isUrg) { q3T++; if (isComp) q3C++ }
                else { q4T++; if (isComp) q4C++ }
            }
            val details = listOf(QuadrantStat(q1Name, q1T, q1C, q1T - q1C), QuadrantStat(q2Name, q2T, q2C, q2T - q2C), QuadrantStat(q3Name, q3T, q3C, q3T - q3C), QuadrantStat(q4Name, q4T, q4C, q4T - q4C))
            return TaskStats(total, completed, pending, details)
        } catch (e: Exception) { return TaskStats() }
    }

    suspend fun checkAndSyncAchievements(): List<String> { /* ... Giữ nguyên ... */ return emptyList() }
    suspend fun selectAchievement(achievementId: String) { /* ... Giữ nguyên ... */ }
    suspend fun clearLocalData() { withContext(Dispatchers.IO) { userDao.clearAllData(); val defaultProfile = UserProfile(id = 1, points = 0); userDao.upsertUserProfile(defaultProfile); val defaultSeed = UnlockedSeed(SeedType.NORMAL.id); userDao.insertUnlockedSeed(defaultSeed) } }
    suspend fun deleteAccountData() { withContext(Dispatchers.IO) { try { val userDoc = firestore.collection("users").document(auth.currentUser?.uid ?: return@withContext); userDoc.collection("garden").get().await().forEach { it.reference.delete() }; userDoc.collection("tasks").get().await().forEach { it.reference.delete() }; userDoc.delete().await(); clearLocalData() } catch (e: Exception) { e.printStackTrace() } } }
    suspend fun toggleTaskStatus(taskId: String, currentStatus: Boolean) { val uid = auth.currentUser?.uid ?: return; try { firestore.collection("users").document(uid).collection("tasks").document(taskId).update("completed", !currentStatus).await() } catch (e: Exception) { e.printStackTrace() } }
}