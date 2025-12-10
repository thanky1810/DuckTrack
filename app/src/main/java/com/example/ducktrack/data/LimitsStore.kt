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

// Extension DataStore phải ở top-level
val Context.limitsDataStore by preferencesDataStore("limits_store")

data class BaselineInfo(
    val day: String,       // yyyy-MM-dd
    val baselineMs: Long   // tổng thời gian đã dùng tới lúc set limit (ms)
)

class LimitsStore(private val ctx: Context) {

    private object Keys {
        val LIMITS = stringPreferencesKey("limits_json")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val BASELINES = stringPreferencesKey("limits_baselines_json")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // --- THÊM MỚI: Cờ hiệu Chế độ Siêu Tập Trung ---
        val DEEP_FOCUS_MODE = booleanPreferencesKey("deep_focus_mode")
    }

    // ---------------------- GIỚI HẠN ----------------------
    suspend fun setLimit(pkg: String, minutes: Int) {
        ctx.limitsDataStore.edit { pref ->
            val current = pref[Keys.LIMITS] ?: "{}"
            val map = JSONObject(current)
            map.put(pkg, minutes)
            pref[Keys.LIMITS] = map.toString()
        }
    }

    suspend fun setLimitWithBaseline(pkg: String, minutes: Int, baselineMs: Long) {
        ctx.limitsDataStore.edit { pref ->
            val limitsJson = JSONObject(pref[Keys.LIMITS] ?: "{}")
            limitsJson.put(pkg, minutes)
            pref[Keys.LIMITS] = limitsJson.toString()

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

    suspend fun removeLimit(pkg: String) {
        ctx.limitsDataStore.edit { pref ->
            run {
                val current = pref[Keys.LIMITS] ?: "{}"
                val map = JSONObject(current)
                map.remove(pkg)
                pref[Keys.LIMITS] = map.toString()
            }
            run {
                val currentBase = pref[Keys.BASELINES] ?: "{}"
                val baseMap = JSONObject(currentBase)
                baseMap.remove(pkg)
                pref[Keys.BASELINES] = baseMap.toString()
            }
        }
    }

    suspend fun getAll(): Map<String, Int> {
        val obj = ctx.limitsDataStore.data
            .map { pref -> JSONObject(pref[Keys.LIMITS] ?: "{}") }
            .first()
        return obj.keys().asSequence().associateWith { obj.getInt(it) }
    }

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
    val isMonitoringEnabled: Flow<Boolean> = ctx.limitsDataStore.data
        .map { pref -> pref[Keys.MONITORING_ENABLED] ?: false }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        ctx.limitsDataStore.edit { pref ->
            pref[Keys.MONITORING_ENABLED] = enabled
        }
    }

    // --- ONBOARDING ---
    val onboardingCompleted: Flow<Boolean> = ctx.limitsDataStore.data
        .map { pref -> pref[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun saveOnboardingCompleted() {
        ctx.limitsDataStore.edit { pref ->
            pref[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    // --- MỚI: Hàm Get/Set Deep Focus ---
    val isDeepFocusEnabled: Flow<Boolean> = ctx.limitsDataStore.data
        .map { pref -> pref[Keys.DEEP_FOCUS_MODE] ?: false }

    suspend fun setDeepFocusEnabled(enabled: Boolean) {
        ctx.limitsDataStore.edit { pref ->
            pref[Keys.DEEP_FOCUS_MODE] = enabled
        }
    }
}