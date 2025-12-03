// FILE: UserPreferences.kt
package com.example.ducktrack.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Tạo DataStore riêng cho cài đặt người dùng
val Context.userPrefsDataStore by preferencesDataStore("user_settings")

class UserPreferences(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode") // True: Tối, False: Sáng
        val VIBRATION = booleanPreferencesKey("vibration") // Rung
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on") // Giữ màn hình
    }

    // --- GETTERS ---
    val isDarkMode: Flow<Boolean> = context.userPrefsDataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val isVibrationEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map { it[Keys.VIBRATION] ?: true }
    val isKeepScreenOn: Flow<Boolean> = context.userPrefsDataStore.data.map { it[Keys.KEEP_SCREEN_ON] ?: false }

    // --- SETTERS ---
    suspend fun setDarkMode(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }
}