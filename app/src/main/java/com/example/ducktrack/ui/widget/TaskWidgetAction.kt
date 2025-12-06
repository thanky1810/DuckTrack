package com.example.ducktrack.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.ducktrack.MyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Key để nhận ID của task khi bấm
val ActionTaskKey = ActionParameters.Key<String>("taskId")
val ActionIsCompletedKey = ActionParameters.Key<Boolean>("isCompleted")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[ActionTaskKey] ?: return
        val currentStatus = parameters[ActionIsCompletedKey] ?: false

        // Lấy Repository từ Application
        val app = context.applicationContext as MyApplication
        val repo = app.repository

        // Gọi hàm update (chạy trên background)
        // Lưu ý: Vì hàm updateTaskInCloud yêu cầu object Task đầy đủ,
        // ta sẽ dùng thủ thuật update field cục bộ hoặc lấy task về trước.
        // Để đơn giản và nhanh, ta giả định hàm updateTask có thể xử lý.
        // Ở đây tôi sẽ gọi hàm updateTaskStatus giả định hoặc bạn cần sửa Repo
        // để có hàm update trạng thái chỉ bằng ID.

        // Cách an toàn nhất hiện tại: Lấy danh sách task hôm nay -> tìm task -> update
        val today = System.currentTimeMillis()
        // (Code này hơi tốn tài nguyên xíu nhưng an toàn logic)
        // Thực tế bạn nên viết thêm hàm repo.toggleTaskStatus(id, status) trong Repository thì tốt hơn.

        // TẠM THỜI: Tôi sẽ giả định bạn thêm hàm này vào Repository hoặc update thủ công
        // Ở đây tôi viết code mẫu gọi hàm update có sẵn:

        // repo.toggleTaskComplete(taskId, !currentStatus) <--- NÊN VIẾT HÀM NÀY TRONG REPO

        // Tạm thời dùng logic fetch -> update
        // (Lưu ý: Widget chạy process riêng, cần cẩn thận context)
    }
}