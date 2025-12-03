// FILE: UserPreferences.kt
package com.example.ducktrack.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPrefsDataStore by preferencesDataStore("user_settings")

class UserPreferences(private val context: Context) {

    private object Keys {
        // Đã xóa DARK_MODE key
        val VIBRATION = booleanPreferencesKey("vibration")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val isVibrationEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map { it[Keys.VIBRATION] ?: true }
    val isKeepScreenOn: Flow<Boolean> = context.userPrefsDataStore.data.map { it[Keys.KEEP_SCREEN_ON] ?: false }

    suspend fun setVibration(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }
}