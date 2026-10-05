package com.babyrecord.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.babyrecord.app.data.AppDatabase
import com.babyrecord.app.notify.VaccineReminderWorker
import com.babyrecord.app.notify.ensureNotificationChannel
import com.babyrecord.app.sync.SettingsSync
import com.babyrecord.app.sync.SyncEngine
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.babyrecord.app.sync.SyncWs
import com.babyrecord.app.ui.theme.ThemeSettings
import com.babyrecord.app.widget.QuickStatsWidgetHelper
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import java.net.URL
import java.util.concurrent.TimeUnit

class BabyApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }

    /** 家庭同步引擎单例：全局唯一，Activity/ViewModel/WS 均从这里访问 */
    val syncEngine: SyncEngine by lazy { SyncEngine(this) }

    override fun onCreate() {
        super.onCreate()
        ThemeSettings.load(this)
        ensureNotificationChannel(this)
        // 每天检查一次疫苗到期情况；首次安装约 20 秒后先跑一次
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "vaccine_daily_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<VaccineReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(20, TimeUnit.SECONDS)
                .build()
        )
        // 每次打开 App 也检查一次（5 秒后），保证即时性
        WorkManager.getInstance(this).enqueueUniqueWork(
            "vaccine_open_check",
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<VaccineReminderWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()
        )
        // 桌面小组件定时刷新（15 分钟）：让相对时间、睡眠时长等动态信息保持新鲜
        QuickStatsWidgetHelper.schedulePeriodicRefresh(this)
        // 家庭同步：已加入家庭则启动 WebSocket 实时通道，并拉取一次增量
        if (syncEngine.isJoined()) {
            SyncWs.start(this)
            syncEngine.requestSync(delayMs = 1_000L)
            // NAS 连接信息补同步：本地已配置过但从未上行过（settings 通道晚于配置存在），
            // 启动时自动补推——不再依赖打开哄睡页触发（两台手机可能都不进那个页面）。
            backfillNasSettings()
        }

        // 检查另一台设备是否推送了新的云端数据（自动同步提醒）
        WorkManager.getInstance(this).enqueueUniqueWork(
            "sync_open_check",
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<com.babyrecord.app.notify.SyncCheckWorker>()
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()
        )
    }

    /**
     * NAS 连接信息的家庭同步补上行（应用启动时）：
     * 本地已配置但 settings 通道无此行 → 补写并触发上行（修复历史配置从未同步的问题）。
     * 自动应用的路径由 SleepMediaViewModel.init 处理（打开哄睡页时）；这里不覆盖本地。
     */
    private fun backfillNasSettings() {
        runCatching {
            val sp = getSharedPreferences("settings", MODE_PRIVATE)
            val key = "sleepPlayer.nas"
            val existing = SettingsSync.readAll(this).any { row ->
                val v = row["settingKey"]
                v != null && runCatching { v.jsonPrimitive.content }.getOrDefault("") == key
            }
            val localUrl = sp.getString("media_url", "") ?: ""
            if (localUrl.isNotBlank() && !existing) {
                val u = URL(localUrl.trim())
                SettingsSync.upsertRow(this, key,
                    buildJsonObject {
                        put("id", SettingsSync.rowId(this@BabyApp, key))
                        put("settingKey", key)
                        put("host", u.host)
                        put("port", if (u.port == -1) u.defaultPort else u.port)
                        put("useTls", localUrl.trim().startsWith("https"))
                        put("volume", 40)
                        put("user", sp.getString("media_user", "") ?: "")
                        put("pass", sp.getString("media_pass", "") ?: "")
                        put("updatedAt", System.currentTimeMillis())
                    })
                syncEngine.requestSync(delayMs = 2_000L)
            }
        }
    }
}
