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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension DataStore phải ở top-level (không đặt trong class)
val Context.limitsDataStore by preferencesDataStore("limits_store")

data class BaselineInfo(
    val day: String,       // yyyy-MM-dd
    val baselineMs: Long   // tổng thời gian đã dùng tới lúc set limit (ms)
)

class LimitsStore(private val ctx: Context) {

    private object Keys {
        val LIMITS = stringPreferencesKey("limits_json")
        // trạng thái bật/tắt monitoring
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        // baseline cho từng app
        val BASELINES = stringPreferencesKey("limits_baselines_json")
    }

    // ---------------------- GIỚI HẠN ----------------------

    /**
     * Ghi: map<packageName, minutes>
     * Dùng cho các trường hợp chỉ thay đổi phút (vd: +15 phút),
     * KHÔNG đụng tới baseline.
     */
    suspend fun setLimit(pkg: String, minutes: Int) {
        ctx.limitsDataStore.edit { pref ->
            val current = pref[Keys.LIMITS] ?: "{}"
            val map = JSONObject(current)
            map.put(pkg, minutes)
            pref[Keys.LIMITS] = map.toString()
        }
    }

    /**
     * Ghi limit + đồng thời lưu baseline:
     *  - ngày hiện tại
     *  - tổng ms đã dùng tới thời điểm set (baselineMs)
     *
     * Dùng khi user SET / ĐỔI limit từ UI.
     */
    suspend fun setLimitWithBaseline(pkg: String, minutes: Int, baselineMs: Long) {
        ctx.limitsDataStore.edit { pref ->
            // 1. Lưu giới hạn phút
            val limitsJson = JSONObject(pref[Keys.LIMITS] ?: "{}")
            limitsJson.put(pkg, minutes)
            pref[Keys.LIMITS] = limitsJson.toString()

            // 2. Lưu baseline
            val baselineJson = JSONObject(pref[Keys.BASELINES] ?: "{}")
            val dayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val entry = JSONObject().apply {
                put("day", dayStr)
                put("baselineMs", baselineMs)
            }
            baselineJson.put(pkg, entry)
            pref[Keys.BASELINES] = baselineJson.toString()
        }
    }

    // Xóa giới hạn + xóa luôn baseline của app đó
    suspend fun removeLimit(pkg: String) {
        ctx.limitsDataStore.edit { pref ->
            // Xóa minute limit
            run {
                val current = pref[Keys.LIMITS] ?: "{}"
                val map = JSONObject(current)
                map.remove(pkg)
                pref[Keys.LIMITS] = map.toString()
            }

            // Xóa baseline
            run {
                val currentBase = pref[Keys.BASELINES] ?: "{}"
                val baseMap = JSONObject(currentBase)
                baseMap.remove(pkg)
                pref[Keys.BASELINES] = baseMap.toString()
            }
        }
    }

    // Lấy tất cả giới hạn: Map<packageName, minutes>
    suspend fun getAll(): Map<String, Int> {
        val obj = ctx.limitsDataStore.data
            .map { pref -> JSONObject(pref[Keys.LIMITS] ?: "{}") }
            .first()
        return obj.keys().asSequence().associateWith { obj.getInt(it) }
    }

    // Lấy toàn bộ baseline: Map<packageName, BaselineInfo>
    suspend fun getAllBaselines(): Map<String, BaselineInfo> {
        val obj = ctx.limitsDataStore.data
            .map { pref -> JSONObject(pref[Keys.BASELINES] ?: "{}") }
            .first()

        return obj.keys().asSequence().mapNotNull { key ->
            val v = obj.optJSONObject(key) ?: return@mapNotNull null
            val day = v.optString("day", null) ?: return@mapNotNull null
            val baselineMs = v.optLong("baselineMs", -1L)
            if (baselineMs < 0L) return@mapNotNull null
            key to BaselineInfo(day = day, baselineMs = baselineMs)
        }.toMap()
    }

    // ----------------- TRẠNG THÁI GIÁM SÁT -----------------

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
