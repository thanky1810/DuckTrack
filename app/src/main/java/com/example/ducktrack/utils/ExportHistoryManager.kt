// FILE: utils/ExportHistoryManager.kt
package com.example.ducktrack.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Bỏ field "exists"
data class HistoryItem(
    val fileName: String,
    val filePath: String,
    val dateModified: Long,
    val fileSize: String
)

object ExportHistoryManager {
    private const val PREF_NAME = "ducktrack_export_history"
    private const val KEY_HISTORY = "history_list"

    fun addFileToHistory(context: Context, item: HistoryItem) {
        val list = getHistory(context).toMutableList()
        list.add(0, item)
        if (list.size > 50) list.removeAt(list.lastIndex)
        saveList(context, list)
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, "[]")
        val list = mutableListOf<HistoryItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HistoryItem(
                        fileName = obj.getString("fileName"),
                        filePath = obj.getString("filePath"),
                        dateModified = obj.getLong("dateModified"),
                        fileSize = obj.getString("fileSize")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveList(context: Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("fileName", item.fileName)
            obj.put("filePath", item.filePath)
            obj.put("dateModified", item.dateModified)
            obj.put("fileSize", item.fileSize)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }
}