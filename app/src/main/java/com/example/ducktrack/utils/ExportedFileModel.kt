// FILE: utils/ExportedFileModel.kt
package com.example.ducktrack.utils

import android.net.Uri

data class ExportedFileModel(
    val fileName: String,
    val fileSize: String,
    val dateModified: Long,
    val uri: Uri, // Đường dẫn để chia sẻ
    val filePath: String // Đường dẫn thực để check file
)