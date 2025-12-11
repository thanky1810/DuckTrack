// FILE: UserPreferences.kt
package com.example.ducktrack.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey // [SỬA] Thêm dòng này
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Khởi tạo DataStore (Giữ nguyên)
val Context.userPrefsDataStore by preferencesDataStore("user_settings")

class UserPreferences(private val context: Context) {

    private object Keys {
        val VIBRATION = booleanPreferencesKey("vibration")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DUCK_NAME = stringPreferencesKey("duck_name") // [SỬA] Định nghĩa key tên Vịt ở đây cho chuẩn
    }

    // [SỬA] Dùng context.userPrefsDataStore thay vì dataStore khơi khơi
    val duckName: Flow<String> = context.userPrefsDataStore.data.map { preferences ->
        preferences[Keys.DUCK_NAME] ?: "Giáo Sư Vịt"
    }

    val isVibrationEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map {
        it[Keys.VIBRATION] ?: true
    }

    val isKeepScreenOn: Flow<Boolean> = context.userPrefsDataStore.data.map {
        it[Keys.KEEP_SCREEN_ON] ?: false
    }

    // [THÊM MỚI] Hàm lưu tên Vịt (Cần cái này để chức năng đổi tên hoạt động)
    suspend fun setDuckName(name: String) {
        context.userPrefsDataStore.edit { preferences ->
            preferences[Keys.DUCK_NAME] = name
        }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }
}