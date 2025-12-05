package com.example.ducktrack.ui.main.tasks

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class TodoTask(
    @DocumentId
    val id: String = "",

    val description: String = "",

    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var isCompleted: Boolean = false,

    @get:PropertyName("pinned")
    @set:PropertyName("pinned")
    var isPinned: Boolean = false,

    // --- THÊM MỚI ---
    @get:PropertyName("important")
    @set:PropertyName("important")
    var isImportant: Boolean = false,

    @get:PropertyName("urgent")
    @set:PropertyName("urgent")
    var isUrgent: Boolean = false,
    // ----------------

    val date: Long = 0L
)