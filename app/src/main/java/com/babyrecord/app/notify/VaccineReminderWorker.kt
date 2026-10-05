package com.babyrecord.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.babyrecord.app.BabyApp
import com.babyrecord.app.MainActivity
import com.babyrecord.app.R
import com.babyrecord.app.ui.backup.AutoSync
import com.babyrecord.app.ui.vaccine.NATIONAL_SCHEDULE
import kotlinx.coroutines.flow.first
import java.time.LocalDate

const val VACCINE_CHANNEL_ID = "vaccine_reminder"
const val SYNC_CHANNEL_ID = "data_sync"
private const val VACCINE_NOTIF_ID = 1001
private const val SYNC_NOTIF_ID = 1002

fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(VACCINE_CHANNEL_ID, "疫苗接种提醒", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "按免疫规划月龄提醒即将到期或已到期的疫苗" }
        )
        nm.createNotificationChannel(
            NotificationChannel(SYNC_CHANNEL_ID, "数据同步", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "另一台设备更新了云端数据时提醒你同步" }
        )
    }
}

private fun postNotification(context: Context, channelId: String, notifId: Int, title: String, text: String, openBackup: Boolean) {
    if (Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    ensureNotificationChannel(context)
    val intent = Intent(context, MainActivity::class.java)
    if (openBackup) intent.putExtra("open_backup", true)
    val pending = PendingIntent.getActivity(
        context, notifId, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setAutoCancel(true)
        .setContentIntent(pending)
        .build()
    try {
        NotificationManagerCompat.from(context).notify(notifId, notification)
    } catch (_: SecurityException) {
    }
}

fun postVaccineNotification(context: Context, title: String, text: String) =
    postNotification(context, VACCINE_CHANNEL_ID, VACCINE_NOTIF_ID, title, text, openBackup = false)

fun postSyncNotification(context: Context, title: String, text: String) =
    postNotification(context, SYNC_CHANNEL_ID, SYNC_NOTIF_ID, title, text, openBackup = true)

/** 每日检查一次：对照国家免疫规划与接种记录，找出 7 天内到期或 90 天内已过期的剂次并发提醒。 */
class VaccineReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? BabyApp ?: return Result.success()
        val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("vaccine_reminder_enabled", true)) return Result.success()

        val db = app.database
        val baby = db.babyDao().observeBaby().first() ?: return Result.success()
        val records = db.vaccineDao().observeAll().first()
        val done = records.map { it.vaccineName to it.doseLabel }.toSet()
        val birthday = LocalDate.ofEpochDay(baby.birthdayEpochDay)
        val today = LocalDate.now()

        val pending = NATIONAL_SCHEDULE
            .filter { (it.vaccine to it.doseLabel) !in done }
            .map { it to birthday.plusMonths(it.ageMonths.toLong()) }
        val overdue = pending.filter { (_, due) -> due < today && due >= today.minusDays(90) }
        val soon = pending.filter { (_, due) -> due in today..today.plusDays(7) }

        val parts = buildList {
            overdue.take(2).forEach { (def, due) ->
                add("${def.vaccine}${def.doseLabel} 已到期${today.toEpochDay() - due.toEpochDay()} 天")
            }
            soon.take(2).forEach { (def, due) ->
                val days = due.toEpochDay() - today.toEpochDay()
                add("${def.vaccine}${def.doseLabel} " + if (days == 0L) "今天到期" else "${days}天后到期")
            }
        }
        if (parts.isEmpty()) return Result.success()

        val title = if (overdue.isNotEmpty()) "💉 宝宝有疫苗到期待种" else "💉 疫苗接种提醒"
        val more = (overdue.size + soon.size) - parts.size
        val text = parts.joinToString("；") + if (more > 0) " 等${more + parts.size}剂" else ""
        postVaccineNotification(app, title, "$text，打开查看接种本。")
        return Result.success()
    }
}

/** App 打开时检查一次：另一台设备是否推送了新的云端数据 */
class SyncCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? BabyApp ?: return Result.success()
        val meta = AutoSync.checkCloudNewer(app) ?: return Result.success()
        AutoSync.markCloudSeen(app, meta.time)
        postSyncNotification(
            app,
            "🔄 云端有新数据",
            "来自设备 ${meta.device}，点此在「数据备份」页恢复查看。"
        )
        return Result.success()
    }
}
