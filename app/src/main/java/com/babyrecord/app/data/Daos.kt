package com.babyrecord.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 家庭同步改造说明：
// - 待推条件统一为 updatedAt > syncedAt（含软删行，删除操作也要同步）
// - getByIds：拉取时按 id 批量查本地，比较 updatedAt 做增量合并
// - purgeDeletedBefore：物理清理「已同步过的软删行」（syncedAt > deletedAt）
// - upsertAll：服务端拉回的行按 id 本地 upsert（ON CONFLICT REPLACE）

@Dao
interface BabyDao {
    // 多宝宝（家庭同步拉回）时固定显示「最早创建」的那个：ULID 主键本身按生成时间有序，
    // 全端 id 完全一致 → 各设备排序结果一致，不会出现“这台显示A那台显示B”。
    @Query("SELECT * FROM babies WHERE deletedAt IS NULL ORDER BY id ASC LIMIT 1")
    fun observeBaby(): Flow<BabyEntity?>

    @Insert
    suspend fun insert(baby: BabyEntity)

    @Update
    suspend fun update(baby: BabyEntity)

    // ---- 同步专用 ----
    @Query("SELECT * FROM babies WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<BabyEntity>>

    @Query("SELECT * FROM babies WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<BabyEntity>

    @Query("SELECT * FROM babies WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<BabyEntity>

    @Query("DELETE FROM babies WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BabyEntity>)
}

@Dao
interface FeedingDao {
    @Insert
    suspend fun insert(feeding: FeedingEntity)

    @Update
    suspend fun update(feeding: FeedingEntity)

    @Delete
    suspend fun delete(feeding: FeedingEntity)

    @Query("SELECT * FROM feedings WHERE id = :id")
    fun observeById(id: String): Flow<FeedingEntity?>

    @Query("DELETE FROM feedings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM feedings WHERE deletedAt IS NULL AND timeEpochMillis BETWEEN :from AND :to ORDER BY timeEpochMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<FeedingEntity>>

    @Query("SELECT * FROM feedings WHERE deletedAt IS NULL ORDER BY timeEpochMillis DESC")
    fun observeAll(): Flow<List<FeedingEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM feedings WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<FeedingEntity>>

    @Query("SELECT * FROM feedings WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<FeedingEntity>

    @Query("SELECT * FROM feedings WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<FeedingEntity>

    @Query("DELETE FROM feedings WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FeedingEntity>)
}

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(measurement: MeasurementEntity)

    @Update
    suspend fun update(measurement: MeasurementEntity)

    @Delete
    suspend fun delete(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements WHERE deletedAt IS NULL ORDER BY timeEpochMillis DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM measurements WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MeasurementEntity>

    @Query("DELETE FROM measurements WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MeasurementEntity>)
}

@Dao
interface VaccineDao {
    @Insert
    suspend fun insert(record: VaccineEntity)

    @Update
    suspend fun update(record: VaccineEntity)

    @Delete
    suspend fun delete(record: VaccineEntity)

    @Query("SELECT * FROM vaccines WHERE vaccineName = :name AND doseLabel = :dose AND deletedAt IS NULL LIMIT 1")
    suspend fun find(name: String, dose: String): VaccineEntity?

    @Query("SELECT * FROM vaccines WHERE deletedAt IS NULL")
    fun observeAll(): Flow<List<VaccineEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM vaccines WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<VaccineEntity>>

    @Query("SELECT * FROM vaccines WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<VaccineEntity>

    @Query("SELECT * FROM vaccines WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<VaccineEntity>

    @Query("DELETE FROM vaccines WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VaccineEntity>)
}

@Dao
interface DiaperDao {
    @Insert
    suspend fun insert(diaper: DiaperEntity)

    @Update
    suspend fun update(diaper: DiaperEntity)

    @Delete
    suspend fun delete(diaper: DiaperEntity)

    @Query("SELECT * FROM diapers WHERE id = :id")
    fun observeById(id: String): Flow<DiaperEntity?>

    @Query("DELETE FROM diapers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM diapers WHERE deletedAt IS NULL ORDER BY timeEpochMillis DESC")
    fun observeAll(): Flow<List<DiaperEntity>>

    @Query("SELECT * FROM diapers WHERE deletedAt IS NULL AND timeEpochMillis BETWEEN :from AND :to ORDER BY timeEpochMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<DiaperEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM diapers WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<DiaperEntity>>

    @Query("SELECT * FROM diapers WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<DiaperEntity>

    @Query("SELECT * FROM diapers WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<DiaperEntity>

    @Query("DELETE FROM diapers WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DiaperEntity>)
}

@Dao
interface SleepDao {
    @Insert
    suspend fun insert(sleep: SleepEntity)

    @Update
    suspend fun update(sleep: SleepEntity)

    @Delete
    suspend fun delete(sleep: SleepEntity)

    @Query("SELECT * FROM sleeps WHERE id = :id")
    fun observeById(id: String): Flow<SleepEntity?>

    @Query("DELETE FROM sleeps WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM sleeps WHERE deletedAt IS NULL ORDER BY startEpochMillis DESC")
    fun observeAll(): Flow<List<SleepEntity>>

    @Query("SELECT * FROM sleeps WHERE deletedAt IS NULL AND startEpochMillis BETWEEN :from AND :to ORDER BY startEpochMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<SleepEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM sleeps WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<SleepEntity>>

    @Query("SELECT * FROM sleeps WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<SleepEntity>

    @Query("SELECT * FROM sleeps WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SleepEntity>

    @Query("DELETE FROM sleeps WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SleepEntity>)
}

@Dao
interface SolidFoodDao {
    @Insert
    suspend fun insert(solidFood: SolidFoodEntity)

    @Update
    suspend fun update(solidFood: SolidFoodEntity)

    @Delete
    suspend fun delete(solidFood: SolidFoodEntity)

    @Query("SELECT * FROM solid_foods WHERE id = :id")
    fun observeById(id: String): Flow<SolidFoodEntity?>

    @Query("DELETE FROM solid_foods WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM solid_foods WHERE deletedAt IS NULL ORDER BY timeEpochMillis DESC")
    fun observeAll(): Flow<List<SolidFoodEntity>>

    @Query("SELECT * FROM solid_foods WHERE deletedAt IS NULL AND timeEpochMillis BETWEEN :from AND :to ORDER BY timeEpochMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<SolidFoodEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM solid_foods WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<SolidFoodEntity>>

    @Query("SELECT * FROM solid_foods WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<SolidFoodEntity>

    @Query("SELECT * FROM solid_foods WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SolidFoodEntity>

    @Query("DELETE FROM solid_foods WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SolidFoodEntity>)
}

@Dao
interface SupplementDao {
    @Insert
    suspend fun insert(supplement: SupplementEntity)

    @Update
    suspend fun update(supplement: SupplementEntity)

    @Query("DELETE FROM supplements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM supplements WHERE id = :id")
    fun observeById(id: String): Flow<SupplementEntity?>

    @Query("SELECT * FROM supplements WHERE deletedAt IS NULL ORDER BY timeEpochMillis DESC")
    fun observeAll(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE deletedAt IS NULL AND timeEpochMillis BETWEEN :from AND :to ORDER BY timeEpochMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<SupplementEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM supplements WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<SupplementEntity>

    @Query("SELECT * FROM supplements WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SupplementEntity>

    @Query("DELETE FROM supplements WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SupplementEntity>)
}

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntity)

    @Update
    suspend fun update(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM photos WHERE deletedAt IS NULL ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    // ---- 同步专用 ----
    @Query("SELECT * FROM photos WHERE updatedAt > syncedAt")
    fun pendingSync(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE updatedAt > syncedAt")
    suspend fun pendingSyncList(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<PhotoEntity>

    /** 所有需要文件补拉的照片：未软删、有 sha256（其他设备上传过）、且 sha256 非 0=文件需校验存在 */
    @Query("SELECT * FROM photos WHERE deletedAt IS NULL AND sha256 != '' ORDER BY takenAt DESC")
    suspend fun allWithSha(): List<PhotoEntity>

    @Query("DELETE FROM photos WHERE deletedAt IS NOT NULL AND syncedAt > deletedAt AND deletedAt < :ts")
    suspend fun purgeDeletedBefore(ts: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PhotoEntity>)
}
