package com.babyrecord.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BabyEntity::class, FeedingEntity::class, MeasurementEntity::class, VaccineEntity::class,
        DiaperEntity::class, SleepEntity::class, SolidFoodEntity::class, PhotoEntity::class,
        SupplementEntity::class
    ],
    version = 10,
    // 基线版本：v9（0.14.0）。从此以后 schema 变更必须手写 Migration（见下方 builder），
    // 禁止再使用破坏性重建——用户历史数据从本版本起必须无条件保留。
    // 导出 schema 到 schemas/ 目录以便 Room 生成迁移测试基准（建议后续开启 exportSchema = true）
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun feedingDao(): FeedingDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun vaccineDao(): VaccineDao
    abstract fun diaperDao(): DiaperDao
    abstract fun sleepDao(): SleepDao
    abstract fun solidFoodDao(): SolidFoodDao
    abstract fun photoDao(): PhotoDao
    abstract fun supplementDao(): SupplementDao

    companion object {
        const val DB_NAME = "baby_record.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // ⚠️ 数据保护基线（v9 起）：不再使用 fallbackToDestructiveMigration。
                    // 后续任何 schema 变更都必须在 MIGRATION_9_10、MIGRATION_10_11... 手写 SQL，
                    // 遗漏时 Room 会在运行期直接抛 IllegalStateException（快速暴露问题），
                    // 而不是静默清空用户数据。
                    // 常见迁移写法：
                    //   加字段: database.execSQL("ALTER TABLE feedings ADD COLUMN xxx INTEGER NOT NULL DEFAULT 0")
                    //   加表:  复制 @Database 中新实体的 CREATE TABLE 语句
                    //   重构:  建新表 → INSERT INTO new SELECT ... FROM old → DROP old → ALTER RENAME
                    .addMigrations(
                        // v9→v10：babies 新增 avatar 字段（自定义头像文件名，空=回退首字头像）
                        object : Migration(9, 10) {
                            override fun migrate(db: SupportSQLiteDatabase) {
                                db.execSQL("ALTER TABLE babies ADD COLUMN avatar TEXT NOT NULL DEFAULT ''")
                            }
                        }
                    )
                    .build()
                    .also { instance = it }
            }

        /** 恢复备份前关闭并置空单例，保证文件可以安全覆盖 */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
