package com.example.ducktrack.data

import android.util.Log
import com.example.ducktrack.data.model.GrownTree
import com.example.ducktrack.ui.main.garden.SeedType
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

class UserDataRepository(private val userDao: UserDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- PHẦN 1: DỮ LIỆU TỪ ROOM (LOCAL) ---
    val userPoints: Flow<Int> = userDao.getUserProfile().map { it?.points ?: 0 }

    val unlockedSeeds: Flow<Set<SeedType>> = userDao.getUnlockedSeeds().map { list ->
        list.mapNotNull { SeedType.fromId(it.seedId) }.toSet()
    }

    // --- PHẦN 2: DỮ LIỆU TỪ FIRESTORE (CLOUD) ---
    fun getGrownTreesStream(): Flow<List<GrownTree>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val collection = firestore.collection("users").document(uid).collection("garden")

        // Lấy cây, sắp xếp mới nhất lên đầu
        val listener = collection
            .orderBy("plantedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Repo", "Lỗi lấy cây: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val trees = snapshot.toObjects(GrownTree::class.java)
                    trySend(trees)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- PHẦN 3: THAY ĐỔI DỮ LIỆU ---

    suspend fun addPoints(amount: Int) {
        withContext(Dispatchers.IO) {
            userDao.increasePoints(amount)
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
            } else {
                false
            }
        }
    }

    // CẬP NHẬT HÀM NÀY: Thêm tham số config
    suspend fun addGrownTreeToCloud(seed: SeedType, configString: String) {
        val uid = auth.currentUser?.uid ?: return

        val newTree = GrownTree(
            seedId = seed.id,
            plantedAt = System.currentTimeMillis(),
            config = configString // Lưu chuỗi cấu hình
        )

        try {
            firestore.collection("users")
                .document(uid)
                .collection("garden")
                .add(newTree)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- PHẦN 4: ĐỒNG BỘ & KHÔI PHỤC ---
    private suspend fun syncUserProfileToCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val points = userPoints.firstOrNull() ?: 0
            val seeds = unlockedSeeds.firstOrNull()?.map { it.id } ?: emptyList()
            val data = hashMapOf("points" to points, "unlockedSeeds" to seeds)
            firestore.collection("users").document(uid).set(data, SetOptions.merge())
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
                    for (seedId in seedsList) {
                        userDao.insertUnlockedSeed(UnlockedSeed(seedId))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}