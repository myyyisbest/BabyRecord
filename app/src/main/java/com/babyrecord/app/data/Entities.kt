package com.babyrecord.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FeedType { BREAST, BOTTLE_MILK, FORMULA }

/**
 * 家庭同步改造说明：
 * - 主键 id 由 Long 自增改为 String（ULID），多设备生成 id 不冲突且时间有序
 * - updatedAt：本地编辑时刷新（max(本地时间+服务器offset, ...)，offset 由同步引擎维护）
 * - deletedAt：软删时间戳，null 表示未删除；删除也要同步，故待推条件为 updatedAt > syncedAt
 * - syncedAt：最后一次确认同步成功的服务器时间，0 = 待推
 */

@Entity(tableName = "babies")
data class BabyEntity(
    @PrimaryKey val id: String = "",
    val name: String,
    val birthdayEpochDay: Long,
    val gender: String, // boy / girl / secret
    /** 头像文件名（相对于 filesDir/avatar，无则为空 → UI 回退首字头像） */
    @ColumnInfo(defaultValue = "") val avatar: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

@Entity(tableName = "feedings")
data class FeedingEntity(
    @PrimaryKey val id: String = "",
    val type: String,
    val amountMl: Int?,
    val timeEpochMillis: Long,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

enum class MeasureType { WEIGHT, HEIGHT, HEAD }

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String = "",
    val type: String,
    val value: Double,
    val timeEpochMillis: Long,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

@Entity(tableName = "vaccines")
data class VaccineEntity(
    @PrimaryKey val id: String = "",
    val vaccineName: String,
    val doseLabel: String,
    val dateEpochDay: Long,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

enum class DiaperState { PEE, POOP, BOTH }

@Entity(tableName = "diapers")
data class DiaperEntity(
    @PrimaryKey val id: String = "",
    val state: String,
    val timeEpochMillis: Long,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

@Entity(tableName = "sleeps")
data class SleepEntity(
    @PrimaryKey val id: String = "",
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

@Entity(tableName = "solid_foods")
data class SolidFoodEntity(
    @PrimaryKey val id: String = "",
    val foodName: String,
    val timeEpochMillis: Long,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey val id: String = "",
    val name: String,
    val timeEpochMillis: Long,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String = "",
    val filePath: String, // 相对于 filesDir/album 的文件名
    val caption: String = "",
    @ColumnInfo(defaultValue = "") val title: String = "",
    @ColumnInfo(defaultValue = "") val description: String = "",
    @ColumnInfo(defaultValue = "") val tags: String = "",
    val takenAt: Long,
    @ColumnInfo(defaultValue = "") val sha256: String = "", // 内容指纹，也是服务端文件存储名；空 = 文件还没上传
    @ColumnInfo(defaultValue = "0") val sizeBytes: Long = 0L, // 文件字节数（展示/统计用）
    // 精选标记：1=精选（星标收藏）。参与同步透传。
    @ColumnInfo(defaultValue = "0") val favorite: Int = 0,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long = 0L
) {
    /** 新建记录：生成 ULID 并标脏待推 */
    fun withNewId() = copy(id = Ulid.generate(), updatedAt = System.currentTimeMillis(), syncedAt = 0L)

    /** 本地编辑后标脏，等待推送 */
    fun markDirty() = copy(updatedAt = System.currentTimeMillis(), syncedAt = 0L)
}
