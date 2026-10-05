package com.babyrecord.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** 保存记录后调用：让桌面综合小组件立即刷新 */
object QuickStatsWidgetHelper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching {
                val manager = GlanceAppWidgetManager(app)
                manager.getGlanceIds(QuickRecordWidget::class.java).forEach { id ->
                    QuickRecordWidget().update(app, id)
                }
            }
        }
    }

    /** 注册定时刷新（15 分钟一次，系统允许的最短周期）：让相对时间、睡眠时长等动态信息保持新鲜 */
    fun schedulePeriodicRefresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "quick_widget_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<QuickWidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .build()
        )
    }
}

/** 定时重算组件上的相对时间（X分钟前 / 已睡X小时等）并重新渲染 */
class QuickWidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(QuickRecordWidget::class.java).forEach { id ->
                QuickRecordWidget().update(context, id)
            }
        }
        return Result.success()
    }
}
