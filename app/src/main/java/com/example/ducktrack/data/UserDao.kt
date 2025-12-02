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
    @Upsert
    suspend fun upsertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    // --- THÊM MỚI: Hàm cộng điểm trực tiếp bằng SQL (Chính xác tuyệt đối) ---
    @Query("UPDATE user_profile SET points = points + :amount WHERE id = 1")
    suspend fun increasePoints(amount: Int)

    // --- Unlocked Seeds (Cây đã mở khóa) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedSeed(seed: UnlockedSeed)

    @Query("SELECT * FROM unlocked_seeds")
    fun getUnlockedSeeds(): Flow<List<UnlockedSeed>>

    // --- Grown Trees (Cây đã trồng) ---
    @Insert
    suspend fun insertGrownTree(tree: GrownTree)

    @Query("SELECT * FROM grown_trees ORDER BY id ASC")
    fun getGrownTrees(): Flow<List<GrownTree>>

    @Query("SELECT COUNT(*) FROM grown_trees")
    suspend fun getGrownTreeCount(): Int

    @Query("DELETE FROM grown_trees")
    suspend fun deleteAllGrownTrees()
}