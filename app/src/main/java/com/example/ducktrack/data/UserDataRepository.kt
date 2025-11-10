package com.example.ducktrack.data

import com.example.ducktrack.ui.main.garden.SeedType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository (giờ là 1 class) nhận UserDao để giao tiếp với Room.
 */
class UserDataRepository(private val userDao: UserDao) {

    // 1. Lấy điểm
    // Chuyển Flow<UserProfile?> -> Flow<Int> (điểm)
    val userPoints: Flow<Int> = userDao.getUserProfile().map { it?.points ?: 0 }

    // 2. Lấy cây đã mở khóa
    // Chuyển Flow<List<UnlockedSeed>> -> Flow<Set<SeedType>>
    val unlockedSeeds: Flow<Set<SeedType>> = userDao.getUnlockedSeeds().map { list ->
        list.mapNotNull { SeedType.fromId(it.seedId) }.toSet()
    }

    // 3. Lấy cây đã trồng
    // Chuyển Flow<List<GrownTree>> -> Flow<List<SeedType>>
    val grownTrees: Flow<List<SeedType>> = userDao.getGrownTrees().map { list ->
        // Giới hạn 12 cây và chuyển đổi
        list.take(12).mapNotNull { SeedType.fromId(it.seedId) }
    }

    /**
     * Thêm điểm (giờ là hàm suspend)
     */
    suspend fun addPoints(amount: Int) {
        withContext(Dispatchers.IO) {
            // Lấy điểm hiện tại (dùng .firstOrNull() để lấy giá trị 1 lần)
            val currentPoints = userPoints.firstOrNull() ?: 0
            val newPoints = currentPoints + amount
            userDao.upsertUserProfile(UserProfile(id = 1, points = newPoints))
        }
    }

    /**
     * Mua cây (giờ là hàm suspend)
     */
    suspend fun unlockSeed(seed: SeedType): Boolean {
        return withContext(Dispatchers.IO) {
            val currentPoints = userPoints.firstOrNull() ?: 0
            if (currentPoints >= seed.cost) {
                // Trừ điểm
                val newPoints = currentPoints - seed.cost
                userDao.upsertUserProfile(UserProfile(id = 1, points = newPoints))

                // Thêm cây
                userDao.insertUnlockedSeed(UnlockedSeed(seed.id))
                true
            } else {
                false
            }
        }
    }

    /**
     * Thêm cây vào vườn (giờ là hàm suspend)
     */
    suspend fun addGrownTree(seed: SeedType) {
        withContext(Dispatchers.IO) {
            val count = userDao.getGrownTreeCount()
            if (count < 12) {
                userDao.insertGrownTree(GrownTree(seedId = seed.id))
            }
        }
    }
}