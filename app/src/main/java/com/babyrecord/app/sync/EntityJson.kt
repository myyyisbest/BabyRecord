package com.babyrecord.app.sync

import com.babyrecord.app.data.BabyEntity
import com.babyrecord.app.data.DiaperEntity
import com.babyrecord.app.data.FeedingEntity
import com.babyrecord.app.data.MeasurementEntity
import com.babyrecord.app.data.PhotoEntity
import com.babyrecord.app.data.SleepEntity
import com.babyrecord.app.data.SolidFoodEntity
import com.babyrecord.app.data.SupplementEntity
import com.babyrecord.app.data.VaccineEntity
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * 实体 ↔ 传输 JSON 的字段映射（9 张表）。
 *
 * 设计说明：
 * - toJson：id + updatedAt + deletedAt + 业务字段，全量透传给服务端
 * - fromJson：宽松解析，缺失/类型不符的字段一律用默认值，避免其他设备新版本多字段导致崩溃
 * - 表名 → 转换器注册在 TableCodec，同步引擎按表名统一调度
 */

/** 单张表的编码（实体→JSON）/解码（JSON→实体）函数对 */
class RowCodec<T>(
    val toJson: (T) -> JsonObject,
    val fromJson: (JsonObject) -> T,
    /** 从任意行提取 id/updatedAt，用于 LWW 合并（不感知具体实体类型） */
    val rowMeta: (JsonObject) -> Pair<String, Long> // id to updatedAt
)

// ---- 通用可空取值工具（缺失/null/类型不符 → 默认值） ----

private fun JsonObject.str(name: String, default: String = ""): String =
    runCatching { (this[name] as? JsonElement)?.jsonPrimitive?.content }.getOrNull() ?: default

private fun JsonObject.long(name: String, default: Long = 0L): Long =
    runCatching { (this[name] as? JsonElement)?.jsonPrimitive?.longOrNull }.getOrNull() ?: default

private fun JsonObject.longOrNull(name: String): Long? =
    runCatching { (this[name] as? JsonElement)?.jsonPrimitive?.longOrNull }.getOrNull()

private fun JsonObject.intOrNull(name: String): Int? =
    runCatching { (this[name] as? JsonElement)?.jsonPrimitive?.intOrNull }.getOrNull()

private fun JsonObject.double(name: String, default: Double = 0.0): Double =
    runCatching { (this[name] as? JsonElement)?.jsonPrimitive?.doubleOrNull }.getOrNull() ?: default

/** 通用行元数据：id + updatedAt */
private fun rowMetaOf(jo: JsonObject): Pair<String, Long> = jo.str("id") to jo.long("updatedAt")

/**
 * 表名 → 编解码器注册表。key 即服务端表名（feedings/sleeps/...）。
 */
object TableCodec {
    /** 参与同步的全部表名（与服务端约定一致）。
     *  settings 为纯 JSON 通道（无 Room 表，本地持久化走 SettingsSync/SharedPreferences） */
    val allTables = listOf(
        "babies", "feedings", "sleeps", "diapers", "solid_foods",
        "supplements", "measurements", "vaccines", "photos", "settings"
    )

    // ---- babies ----

    val babies = RowCodec<BabyEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("name", it.name); put("birthdayEpochDay", it.birthdayEpochDay); put("gender", it.gender)
                put("avatar", it.avatar)
            }
        },
        fromJson = { jo ->
            BabyEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                name = jo.str("name"), birthdayEpochDay = jo.long("birthdayEpochDay"), gender = jo.str("gender", "secret"),
                avatar = jo.str("avatar")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- feedings ----

    val feedings = RowCodec<FeedingEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("type", it.type); put("amountMl", it.amountMl)
                put("timeEpochMillis", it.timeEpochMillis); put("note", it.note)
            }
        },
        fromJson = { jo ->
            FeedingEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                // 大小写兼容：服务端存小写（如 "formula"），本地枚举大写；统一 uppercase()
                // （旧数据服务端已收过我们推的大写 "FORMULA"，uppercase() 对其无副作用）
                type = jo.str("type", "BOTTLE_MILK").uppercase(), amountMl = jo.intOrNull("amountMl"),
                timeEpochMillis = jo.long("timeEpochMillis"), note = jo.str("note")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- sleeps ----

    val sleeps = RowCodec<SleepEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("startEpochMillis", it.startEpochMillis); put("endEpochMillis", it.endEpochMillis); put("note", it.note)
            }
        },
        fromJson = { jo ->
            SleepEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                startEpochMillis = jo.long("startEpochMillis"), endEpochMillis = jo.long("endEpochMillis"), note = jo.str("note")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- diapers ----

    val diapers = RowCodec<DiaperEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("state", it.state); put("timeEpochMillis", it.timeEpochMillis); put("note", it.note)
            }
        },
        fromJson = { jo ->
            DiaperEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                state = jo.str("state", "PEE").uppercase(),   // 大小写兼容：服务端可能存小写
                timeEpochMillis = jo.long("timeEpochMillis"), note = jo.str("note")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- solid_foods ----

    val solidFoods = RowCodec<SolidFoodEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("foodName", it.foodName); put("timeEpochMillis", it.timeEpochMillis); put("note", it.note)
            }
        },
        fromJson = { jo ->
            SolidFoodEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                foodName = jo.str("foodName"), timeEpochMillis = jo.long("timeEpochMillis"), note = jo.str("note")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- supplements ----

    val supplements = RowCodec<SupplementEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("name", it.name); put("timeEpochMillis", it.timeEpochMillis); put("note", it.note)
            }
        },
        fromJson = { jo ->
            SupplementEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                name = jo.str("name"), timeEpochMillis = jo.long("timeEpochMillis"), note = jo.str("note")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- measurements ----
    // ⚠️ 服务端字段契约：字段名是 metric（非 type），枚举值小写（weight/height/head）。
    // App 本地实体仍用 type + 大写枚举（不改库），在此处做双向映射。

    val measurements = RowCodec<MeasurementEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("metric", it.type.lowercase()); put("value", it.value)
                put("timeEpochMillis", it.timeEpochMillis); put("note", it.note)
            }
        },
        fromJson = { jo ->
            MeasurementEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                // 服务端小写枚举 → 本地大写（兼容旧数据直接透传大写值）
                type = jo.str("metric", jo.str("type", "WEIGHT")).uppercase(),
                value = jo.double("value"),
                timeEpochMillis = jo.long("timeEpochMillis"), note = jo.str("note")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- vaccines ----

    val vaccines = RowCodec<VaccineEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("vaccineName", it.vaccineName); put("doseLabel", it.doseLabel)
                put("dateEpochDay", it.dateEpochDay); put("note", it.note)
            }
        },
        fromJson = { jo ->
            VaccineEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                vaccineName = jo.str("vaccineName"), doseLabel = jo.str("doseLabel"),
                dateEpochDay = jo.long("dateEpochDay"), note = jo.str("note")
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- photos ----
    // 照片元数据（不含图片文件本体）参与同步；文件本体不传输，属后续阶段范围
    // 本地 PhotoEntity 有 filePath/caption/title/description/tags 五个展示字段，
    // 服务端契约只定义 fileName/takenAtEpochMillis/width/height/sha256/note，
    // 这里把本地展示字段全部透传（服务端按行透传存储），接收端缺失时用默认值。

    val photos = RowCodec<PhotoEntity>(
        toJson = {
            buildJsonObject {
                put("id", it.id); put("updatedAt", it.updatedAt)
                it.deletedAt?.let { d -> put("deletedAt", d) }
                put("fileName", it.filePath); put("caption", it.caption)
                put("title", it.title); put("description", it.description); put("tags", it.tags)
                put("takenAtEpochMillis", it.takenAt)
                // 文件指纹：空 = 文件还没上传（服务端透传，接收端凭此判断是否需要补拉文件）
                put("sha256", it.sha256); put("sizeBytes", it.sizeBytes)
                // 精选标记（0=普通 1=精选），值为 0 也 put，接收端逻辑简单
                put("favorite", it.favorite)
            }
        },
        fromJson = { jo ->
            PhotoEntity(
                id = jo.str("id"), updatedAt = jo.long("updatedAt"), deletedAt = jo.longOrNull("deletedAt"), syncedAt = jo.long("syncedAt"),   // ⚠️ P0-1 修复：必须回读 syncedAt，否则推送确认回写/拉取合并/物理清理全部失效
                filePath = jo.str("fileName"), caption = jo.str("caption"),
                title = jo.str("title"), description = jo.str("description"), tags = jo.str("tags"),
                takenAt = jo.long("takenAtEpochMillis"),
                // sha256/sizeBytes 为文件同步阶段新增字段，旧版本服务端行可能缺失 → 默认空/0
                sha256 = jo.str("sha256"), sizeBytes = jo.long("sizeBytes"),
                // favorite 为相册升级新增字段，旧版本行缺失时 long() 容错返回 0
                favorite = jo.long("favorite").toInt()
            )
        },
        rowMeta = ::rowMetaOf
    )

    // ---- settings ----
    // 行结构因 settingKey 而异（如 sleepPlayer.nas 的 host/port/useTls/volume），
    // 不建 Room 表：行模型直接用 JsonObject 透传（toJson 原样返回、fromJson 原样入参），
    // 本地持久化/待推追踪由 SettingsSync（prefs "sync_settings"）负责。

    val settings = RowCodec<JsonObject>(
        toJson = { it },
        fromJson = { it },
        rowMeta = ::rowMetaOf
    )
}
