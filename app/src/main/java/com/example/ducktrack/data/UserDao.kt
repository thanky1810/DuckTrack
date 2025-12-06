package com.example.ducktrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // ... (Giữ nguyên các hàm UserProfile và Garden cũ, ĐỪNG XÓA) ...
    @Upsert
    suspend fun upsertUserProfile(profile: UserProfile)
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>
    @Query("UPDATE user_profile SET points = points + :amount WHERE id = 1")
    suspend fun increasePoints(amount: Int)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedSeed(seed: UnlockedSeed)
    @Query("SELECT * FROM unlocked_seeds")
    fun getUnlockedSeeds(): Flow<List<UnlockedSeed>>
    @Insert
    suspend fun insertGrownTree(tree: GrownTree)
    @Query("SELECT * FROM grown_trees ORDER BY id ASC")
    fun getGrownTrees(): Flow<List<GrownTree>>
    @Query("SELECT COUNT(*) FROM grown_trees")
    suspend fun getGrownTreeCount(): Int
    @Query("DELETE FROM grown_trees")
    suspend fun deleteAllGrownTrees()

    // --- PHẦN THÊM MỚI CHO TASK ---
    @Query("SELECT * FROM tasks WHERE date >= :start AND date <= :end ORDER BY isPinned DESC, isCompleted ASC, id DESC")
    fun getTasksForDate(start: Long, end: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteTasksByIds(ids: List<Int>)

    @Query("UPDATE tasks SET isPinned = :isPinned WHERE id IN (:ids)")
    suspend fun updateTasksPinStatus(ids: List<Int>, isPinned: Boolean)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    @Query("DELETE FROM unlocked_seeds")
    suspend fun clearUnlockedSeeds()

    @Query("DELETE FROM grown_trees")
    suspend fun clearGrownTrees()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    // Hàm Transaction để xóa sạch tất cả cùng lúc
    @Transaction
    suspend fun clearAllData() {
        clearUserProfile()
        clearUnlockedSeeds()
        clearGrownTrees()
        clearTasks()
    }

}