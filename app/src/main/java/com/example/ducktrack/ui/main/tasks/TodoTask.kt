package com.example.ducktrack.ui.main.tasks

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName // <-- NHỚ IMPORT CÁI NÀY

data class TodoTask(
    @DocumentId
    val id: String = "",

    val description: String = "",

    // Ánh xạ trường 'completed' trên Firebase vào biến 'isCompleted'
    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var isCompleted: Boolean = false,

    // Ánh xạ trường 'pinned' trên Firebase vào biến 'isPinned'
    @get:PropertyName("pinned")
    @set:PropertyName("pinned")
    var isPinned: Boolean = false,

    val date: Long = 0L
)