package com.example.ducktrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("UPDATE user_profile SET points = points + :amount WHERE id = 1")
    suspend fun increasePoints(amount: Int)

    @Query("UPDATE user_profile SET points = MAX(0, points - :amount) WHERE id = 1")
    suspend fun decreasePoints(amount: Int)

    // --- SEED & TREE ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedSeed(seed: UnlockedSeed)

    @Query("SELECT * FROM unlocked_seeds")
    fun getUnlockedSeeds(): Flow<List<UnlockedSeed>>

    @Insert
    suspend fun insertGrownTree(tree: GrownTree)

    @Query("SELECT * FROM grown_trees ORDER BY id ASC")
    fun getGrownTrees(): Flow<List<GrownTree>>

    // [THÊM MỚI] Hàm lấy danh sách cây (One-shot) cho AI đọc
    @Query("SELECT * FROM grown_trees")
    suspend fun getAllGrownTreesList(): List<GrownTree>

    @Query("SELECT COUNT(*) FROM grown_trees")
    suspend fun getGrownTreeCount(): Int

    @Query("DELETE FROM grown_trees")
    suspend fun deleteAllGrownTrees()

    // --- TASK ---
    @Query("SELECT * FROM tasks WHERE date >= :start AND date <= :end ORDER BY isPinned DESC, isCompleted ASC, id DESC")
    fun getTasksForDate(start: Long, end: Long): Flow<List<TaskEntity>>

    // [THÊM MỚI] Hàm lấy TOÀN BỘ task (One-shot) cho AI đọc
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteTasksByIds(ids: List<Int>)

    @Query("UPDATE tasks SET isPinned = :isPinned WHERE id IN (:ids)")
    suspend fun updateTasksPinStatus(ids: List<Int>, isPinned: Boolean)

    // --- XÓA DỮ LIỆU ---
    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    @Query("DELETE FROM unlocked_seeds")
    suspend fun clearUnlockedSeeds()

    @Query("DELETE FROM grown_trees")
    suspend fun clearGrownTrees()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Transaction
    suspend fun clearAllData() {
        clearUserProfile()
        clearUnlockedSeeds()
        clearGrownTrees()
        clearTasks()
    }
}