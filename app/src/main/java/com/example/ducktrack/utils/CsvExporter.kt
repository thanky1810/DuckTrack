// FILE: utils/CsvExporter.kt
package com.example.ducktrack.utils

import android.content.Context
import com.example.ducktrack.ui.UserInfo
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportUserData(context: Context, userInfo: UserInfo): File? {
        return try {
            // Tạo tên file dựa trên thời gian: ducktrack_export_20251204.csv
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ducktrack_export_$timeStamp.csv"

            // Lưu vào thư mục cache của app để dễ dàng chia sẻ mà không cần quyền Storage phức tạp
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            // --- Ghi nội dung CSV ---
            // 1. Header (Thêm BOM \uFEFF để Excel mở không bị lỗi font tiếng Việt)
            writer.append("\uFEFF")
            writer.append("Category,Key,Value\n")

            // 2. Thông tin User
            writer.append("Profile,Name,${userInfo.name}\n")
            writer.append("Profile,Email,${userInfo.email}\n")
            writer.append("Profile,Duck Name,${userInfo.duckName}\n")
            writer.append("Profile,Provider,${userInfo.provider}\n")

            val createdDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(userInfo.createdAt))
            writer.append("Profile,Created At,$createdDate\n")

            // 3. Thông tin Khu vườn (Thống kê cây)
            if (userInfo.treeCounts.isEmpty()) {
                writer.append("Garden,Status,Chưa trồng cây nào\n")
            } else {
                userInfo.treeCounts.forEach { (seedId, count) ->
                    // Bạn có thể map seedId sang tên cây đẹp hơn nếu muốn
                    writer.append("Garden,$seedId,$count\n")
                }
            }

            // 4. Tổng kết
            val totalTrees = userInfo.treeCounts.values.sum()
            writer.append("Garden,Total Trees,$totalTrees\n")

            writer.flush()
            writer.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}