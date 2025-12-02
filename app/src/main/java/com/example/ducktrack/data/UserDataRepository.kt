package com.example.ducktrack.data

import com.example.ducktrack.ui.main.garden.SeedType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UserDataRepository(private val userDao: UserDao) {

    // 1. Lấy điểm
    val userPoints: Flow<Int> = userDao.getUserProfile().map { it?.points ?: 0 }

    // 2. Lấy cây đã mở khóa
    val unlockedSeeds: Flow<Set<SeedType>> = userDao.getUnlockedSeeds().map { list ->
        list.mapNotNull { SeedType.fromId(it.seedId) }.toSet()
    }

    // 3. Lấy cây đã trồng
    val grownTrees: Flow<List<SeedType>> = userDao.getGrownTrees().map { list ->
        list.take(12).mapNotNull { SeedType.fromId(it.seedId) }
    }

    /**
     * Thêm điểm (SỬA LẠI: Dùng SQL update trực tiếp)
     */
    suspend fun addPoints(amount: Int) {
        withContext(Dispatchers.IO) {
            // Gọi lệnh SQL cộng dồn, không cần lấy điểm cũ ra tính toán nữa
            // Đảm bảo 50 + 50 luôn bằng 100
            userDao.increasePoints(amount)
        }
    }

    /**
     * Mua cây
     */
    suspend fun unlockSeed(seed: SeedType): Boolean {
        return withContext(Dispatchers.IO) {
            val currentPoints = userPoints.firstOrNull() ?: 0
            if (currentPoints >= seed.cost) {
                // Trừ điểm thì vẫn dùng cách cũ (set lại giá trị mới)
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
     * Thêm cây vào vườn
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