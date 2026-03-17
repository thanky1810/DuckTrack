package com.example.ducktrack.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPrefsDataStore by preferencesDataStore("user_settings")

class UserPreferences(private val context: Context) {

    private object Keys {
        val VIBRATION = booleanPreferencesKey("vibration")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DUCK_NAME = stringPreferencesKey("duck_name")
        val CHRISTMAS_THEME = booleanPreferencesKey("christmas_theme") // KEY MỚI
    }

    val duckName: Flow<String> = context.userPrefsDataStore.data.map {
        it[Keys.DUCK_NAME] ?: "Giáo Sư Vịt"
    }

    val isVibrationEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map {
        it[Keys.VIBRATION] ?: true
    }

    val isKeepScreenOn: Flow<Boolean> = context.userPrefsDataStore.data.map {
        it[Keys.KEEP_SCREEN_ON] ?: false
    }

    // FLOW MỚI
    val isChristmasTheme: Flow<Boolean> = context.userPrefsDataStore.data.map {
        it[Keys.CHRISTMAS_THEME] ?: false
    }

    suspend fun setVibration(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    // HÀM MỚI
    suspend fun setChristmasTheme(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.CHRISTMAS_THEME] = enabled }
    }
}