package com.babyrecord.app.sync

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * settings 表同步（家庭同步第四类通道）：行结构因 settingKey 而异，
 * 不适合 Room 固定表，因此不建表——JSON 行是唯一形态，
 * 本地持久化到 SharedPreferences("sync_settings")：
 *   - "<settingKey>"        → 行 JSON 字符串（与服务端行完全一致的透传）
 *   - "syncedAt_<settingKey>" → 该行上次确认同步的时间（epochMillis），
 *     pending 判断：行.updatedAt > syncedAt_<key> 即待推
 *   - "id_<settingKey>"     → 该行的固定 ULID（首次生成后持久化，避免每次生成新 id）
 *
 * 与 Room 表的差别：无 DAO、无 LWW 脏行保护——setting 行以「最后写入胜」为准，
 * pull 收到即落地并视为已同步（写 syncedAt=行.updatedAt，避免回推回声）。
 */
object SettingsSync {
    const val TABLE = "settings"

    private fun prefs(context: Context) =
        context.getSharedPreferences("sync_settings", Context.MODE_PRIVATE)

    // ---- 通用取值（宽松解析，与 EntityJson.kt 同风格） ----

    private fun JsonObject.str(name: String, default: String = ""): String =
        runCatching { (this[name] as? JsonElement)?.jsonPrimitive?.content }.getOrNull() ?: default

    private fun JsonObject.long(name: String, default: Long = 0L): Long =
        runCatching { (this[name] as? JsonElement)?.jsonPrimitive?.longOrNull }.getOrNull() ?: default

    /** 固定行 id：每个 settingKey 首次生成 ULID 后持久化复用（服务端按 id 做行去重/冲突判定） */
    fun rowId(context: Context, key: String): String {
        prefs(context).getString("id_$key", null)?.let { if (it.isNotBlank()) return it }
        val id = newUlid()
        prefs(context).edit().putString("id_$key", id).apply()
        return id
    }

    /** 简易 ULID 生成：48bit 毫秒时间戳 + 80bit 随机，Crockford Base32 共 26 字符 */
    private fun newUlid(): String {
        val encoding = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        var t = System.currentTimeMillis()
        val time = CharArray(10)
        for (i in 9 downTo 0) {
            time[i] = encoding[(t and 0x1FL).toInt()]
            t = t shr 5
        }
        val rnd = CharArray(16) { encoding[(Math.random() * 32).toInt()] }
        return String(time) + String(rnd)
    }

    /** 读取全部本地 settings 行（跳过 syncedAt_/id_ 元数据键；坏 JSON 静默丢弃） */
    fun readAll(context: Context): List<JsonObject> =
        prefs(context).all.entries.mapNotNull { (k, v) ->
            if (k.startsWith("syncedAt_") || k.startsWith("id_")) return@mapNotNull null
            (v as? String)?.let { s ->
                runCatching { Json.parseToJsonElement(s) as? JsonObject }.getOrNull()
            }
        }

    /** 该行上次确认同步的时间（无记录 → 0，视为待推） */
    fun syncedAt(context: Context, key: String): Long = prefs(context).getLong("syncedAt_$key", 0L)

    /** 同步成功后回写确认时间（pushAll 用 serverTime / pullAll 用行.updatedAt） */
    fun markSynced(context: Context, key: String, syncedAt: Long) {
        prefs(context).edit().putLong("syncedAt_$key", syncedAt).apply()
    }

    /**
     * 服务端拉回的行落地：按 settingKey 为键存 JSON 串，
     * 并同步写 syncedAt_<key>=行.updatedAt（收到即视为已同步，避免下一轮回推）
     */
    fun applyRows(context: Context, rows: List<JsonObject>) {
        val ed = prefs(context).edit()
        var changed = false
        rows.forEach { jo ->
            val key = jo.str("settingKey")
            if (key.isBlank()) return@forEach
            ed.putString(key, jo.toString())
            ed.putLong("syncedAt_$key", jo.long("updatedAt"))
            changed = true
        }
        if (changed) ed.apply()
    }

    /** App 侧更新某配置：只写行 JSON，不动 syncedAt（保持待推状态，pushAll 会挑出去推） */
    fun upsertRow(context: Context, key: String, json: JsonObject) {
        prefs(context).edit().putString(key, json.toString()).apply()
    }
}
