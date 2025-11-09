
package com.example.ducktrack.ui.main.tasks

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_table ORDER BY isPinned DESC, isCompleted ASC, id ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM task_table WHERE id IN (:taskIds)")
    suspend fun deleteTasks(taskIds: Set<Int>)

    @Query("UPDATE task_table SET isPinned = 1 WHERE id IN (:taskIds)")
    suspend fun pinTasks(taskIds: Set<Int>)
}