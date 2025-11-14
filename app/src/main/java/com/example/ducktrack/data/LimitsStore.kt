package com.example.ducktrack.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

// Extension DataStore phải ở top-level (không đặt trong class)
val Context.limitsDataStore by preferencesDataStore("limits_store")

class LimitsStore(private val ctx: Context) {

    private object Keys {
        val LIMITS = stringPreferencesKey("limits_json")
        // Dòng mới để lưu trạng thái bật/tắt
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
    }

    // Ghi: map<packageName, minutes>
    suspend fun setLimit(pkg: String, minutes: Int) {
        ctx.limitsDataStore.edit { pref ->
            val current = pref[Keys.LIMITS] ?: "{}"
            val map = JSONObject(current)
            map.put(pkg, minutes)
            pref[Keys.LIMITS] = map.toString()
        }
    }

    // Xóa giới hạn
    suspend fun removeLimit(pkg: String) {
        ctx.limitsDataStore.edit { pref ->
            val current = pref[Keys.LIMITS] ?: "{}"
            val map = JSONObject(current)
            map.remove(pkg)
            pref[Keys.LIMITS] = map.toString()
        }
    }


    // Lấy tất cả giới hạn
    suspend fun getAll(): Map<String, Int> {
        val obj = ctx.limitsDataStore.data
            .map { pref -> JSONObject(pref[Keys.LIMITS] ?: "{}") }
            .first()
        return obj.keys().asSequence().associateWith { obj.getInt(it) }
    }

    // =======================================================
    // PHẦN MỚI: Thêm 2 hàm để lưu trạng thái bật/tắt
    // =======================================================

    /**
     * Lấy trạng thái giám sát (dùng Flow để lắng nghe thay đổi)
     */
    val isMonitoringEnabled: Flow<Boolean> = ctx.limitsDataStore.data
        .map { pref ->
            pref[Keys.MONITORING_ENABLED] ?: false // Mặc định là 'tắt'
        }

    /**
     * Lưu trạng thái giám sát
     */
    suspend fun setMonitoringEnabled(enabled: Boolean) {
        ctx.limitsDataStore.edit { pref ->
            pref[Keys.MONITORING_ENABLED] = enabled
        }
    }
}