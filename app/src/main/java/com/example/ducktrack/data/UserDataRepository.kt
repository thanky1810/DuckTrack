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
    suspend fun addTaskToCloud(description: String, dateMs: Long) {
        val uid = auth.currentUser?.uid ?: return
        val newTask = TodoTask(
            description = description,
            isCompleted = false,
            isPinned = false,
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
            val snapshot = firestore.collection("users").document(uid)
                .collection("tasks")
                .get()
                .await()

            val total = snapshot.size()
            // Đếm số document có trường 'completed' là true
            val completed = snapshot.documents.count { it.getBoolean("completed") == true }
            val pending = total - completed

            return TaskStats(total, completed, pending)
        } catch (e: Exception) {
            e.printStackTrace()
            return TaskStats()
        }
    }
}