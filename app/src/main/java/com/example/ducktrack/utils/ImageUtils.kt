// FILE: ImageUtils.kt
package com.example.ducktrack.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    fun uriToBase64(context: Context, uri: Uri): String? {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // 1. Giảm kích thước ảnh xuống (Max 300x300 để nhẹ)
            val scaledBitmap = getResizedBitmap(originalBitmap, 300)

            // 2. Nén thành JPEG và chuyển sang Byte Array
            val outputStream = ByteArrayOutputStream()
            // Chất lượng 60% là đủ cho avatar điện thoại
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()

            // 3. Chuyển thành chuỗi Base64
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Hàm phụ trợ để resize ảnh giữ nguyên tỷ lệ
    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }
}