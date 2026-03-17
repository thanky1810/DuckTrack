package com.example.ducktrack.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.ducktrack.MyApplication

// Định nghĩa các Key để nhận dữ liệu từ Widget
val ActionTaskKey = ActionParameters.Key<String>("taskId")
val ActionIsCompletedKey = ActionParameters.Key<Boolean>("isCompleted")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 1. Lấy ID và trạng thái hiện tại từ tham số truyền vào
        val taskId = parameters[ActionTaskKey] ?: return
        val isCompleted = parameters[ActionIsCompletedKey] ?: false

        // 2. Lấy Repository từ Application
        val app = context.applicationContext as MyApplication
        val repo = app.repository

        // 3. Gọi hàm update lên Firestore
        repo.toggleTaskStatus(taskId, isCompleted)

        // 4. Cập nhật lại Widget ngay lập tức để người dùng thấy phản hồi
        DuckWidget().update(context, glanceId)
    }
}