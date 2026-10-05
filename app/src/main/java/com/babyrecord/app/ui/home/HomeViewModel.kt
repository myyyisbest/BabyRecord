package com.babyrecord.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.FeedType
import com.babyrecord.app.data.FeedingEntity
import com.babyrecord.app.ui.RecordDisplay
import com.babyrecord.app.ui.diaperDisplay
import com.babyrecord.app.ui.feedingDisplay
import com.babyrecord.app.ui.sleepDisplay
import com.babyrecord.app.ui.solidDisplay
import com.babyrecord.app.ui.supplementDisplay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class TodayStats(
    val feedCount: Int = 0,
    val milkTotalMl: Int = 0,
    val lastFeedingLabel: String = "—"
)

data class HomeUiState(
    val babyName: String = "宝宝",
    val babyDayAge: Int = 1,
    val gender: String = "secret",
    // 头像文件名（相对于 filesDir/album，复用照片通道传输）；空 = 未设置，UI 回退首字头像
    val avatar: String = "",
    val stats: TodayStats = TodayStats(),
    val recent: List<RecordDisplay> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyApp).database
    private val zone = ZoneId.systemDefault()

    private val todayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    private val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    val uiState: StateFlow<HomeUiState> = combine(
        db.babyDao().observeBaby(),
        db.feedingDao().observeBetween(todayStart, todayEnd),
        db.diaperDao().observeBetween(todayStart, todayEnd),
        db.sleepDao().observeBetween(todayStart, todayEnd),
        db.solidFoodDao().observeBetween(todayStart, todayEnd)
    ) { baby, feedings, diapers, sleeps, solids ->
        HomeUiState(
            avatar = baby?.avatar ?: "",
            babyName = baby?.name ?: "宝宝",
            babyDayAge = baby?.let {
                ChronoUnit.DAYS.between(LocalDate.ofEpochDay(it.birthdayEpochDay), LocalDate.now()).toInt() + 1
            } ?: 1,
            gender = baby?.gender ?: "secret",
            stats = computeStats(feedings),
            recent = (
                feedings.map { feedingDisplay(it) } +
                    diapers.map { diaperDisplay(it) } +
                    sleeps.map { sleepDisplay(it) } +
                    solids.map { solidDisplay(it) } +
                    db.supplementDao().observeBetween(todayStart, todayEnd).first()
                        .map { supplementDisplay(it) }
                ).sortedByDescending { it.time }.take(5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun computeStats(feedings: List<FeedingEntity>): TodayStats {
        if (feedings.isEmpty()) return TodayStats()
        val last = feedings.first() // 已按时间倒序
        val minutes = Duration.between(
            Instant.ofEpochMilli(last.timeEpochMillis),
            Instant.now()
        ).toMinutes().coerceAtLeast(0)
        val label = when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            minutes < 60 * 24 -> {
                val h = minutes / 60
                val m = minutes % 60
                if (m > 0) "${h}小时${m}分前" else "${h}小时前"
            }
            else -> "${minutes / (60 * 24)}天前"
        }
        return TodayStats(
            feedCount = feedings.size,
            milkTotalMl = feedings.filter { it.type != FeedType.BREAST.name }.sumOf { it.amountMl ?: 0 },
            lastFeedingLabel = label
        )
    }
}
