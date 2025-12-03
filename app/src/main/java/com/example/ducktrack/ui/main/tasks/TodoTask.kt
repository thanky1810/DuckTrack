// FILE: TodoTask.kt
package com.example.ducktrack.ui.main.tasks

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

// Đổi tên từ Task -> TodoTask để tránh trùng với Firebase Task
data class TodoTask(
    @DocumentId
    @get:Exclude
    val id: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val date: Long = 0L
)