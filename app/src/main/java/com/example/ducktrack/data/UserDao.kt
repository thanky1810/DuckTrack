package com.example.ducktrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // --- User Profile (Điểm) ---
    @Upsert // (Insert hoặc Update nếu đã tồn tại)
    suspend fun upsertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?> // Dùng Flow để tự động cập nhật

    // --- Unlocked Seeds (Cây đã mở khóa) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Bỏ qua nếu đã có
    suspend fun insertUnlockedSeed(seed: UnlockedSeed)

    @Query("SELECT * FROM unlocked_seeds")
    fun getUnlockedSeeds(): Flow<List<UnlockedSeed>>

    // --- Grown Trees (Cây đã trồng) ---
    @Insert
    suspend fun insertGrownTree(tree: GrownTree)

    @Query("SELECT * FROM grown_trees ORDER BY id ASC") // Sắp xếp để giữ đúng thứ tự
    fun getGrownTrees(): Flow<List<GrownTree>>

    @Query("SELECT COUNT(*) FROM grown_trees")
    suspend fun getGrownTreeCount(): Int // Để check giới hạn 12 cây
}