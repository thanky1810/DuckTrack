package com.example.ducktrack.ui.main.tasks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Đặt giá trị mặc định cho id
    val description: String,
    val isCompleted: Boolean,
    val isPinned: Boolean
)