package com.babyrecord.app.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.SupplementEntity
import com.babyrecord.app.ui.RecordDisplay
import com.babyrecord.app.ui.backup.AutoSync
import com.babyrecord.app.ui.diaperDisplay
import com.babyrecord.app.ui.feedingDisplay
import com.babyrecord.app.ui.sleepDisplay
import com.babyrecord.app.ui.solidDisplay
import com.babyrecord.app.ui.supplementDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DayGroup(
    val date: LocalDate,
    val dayAge: Int?,
    val stats: List<Pair<String, String>>, // 类别 → 共N次,明细
    val items: List<RecordDisplay>
)

data class TimelineUiState(
    val babyName: String = "宝宝",
    val days: List<LocalDate> = emptyList(), // 近 7 天，旧 → 新
    val selectedDay: LocalDate = LocalDate.now(),
    val group: DayGroup? = null // 选中当天的分组
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyApp).database
    private val zone = ZoneId.systemDefault()

    private val selectedDay = MutableStateFlow(LocalDate.now())

    fun selectDay(day: LocalDate) {
        selectedDay.value = day
    }

    private data class AllRecords(
        val feedings: List<com.babyrecord.app.data.FeedingEntity>,
        val diapers: List<com.babyrecord.app.data.DiaperEntity>,
        val sleeps: List<com.babyrecord.app.data.SleepEntity>,
        val solids: List<com.babyrecord.app.data.SolidFoodEntity>,
        val supplements: List<SupplementEntity>
    )

    private val allRecords = combine(
        db.feedingDao().observeAll(),
        db.diaperDao().observeAll(),
        db.sleepDao().observeAll(),
        db.solidFoodDao().observeAll(),
        db.supplementDao().observeAll()
    ) { f, d, s, sf, sup ->
        AllRecords(f, d, s, sf, sup)
    }

    val uiState: StateFlow<TimelineUiState> = combine(
        allRecords,
        db.babyDao().observeBaby(),
        selectedDay
    ) { all, baby, day ->
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        fun inDay(time: Long) = time in start..end

        val items = (
            all.feedings.filter { inDay(it.timeEpochMillis) }.map { feedingDisplay(it) } +
                all.diapers.filter { inDay(it.timeEpochMillis) }.map { diaperDisplay(it) } +
                all.sleeps.filter { inDay(it.startEpochMillis) }.map { sleepDisplay(it) } +
                all.solids.filter { inDay(it.timeEpochMillis) }.map { solidDisplay(it) } +
                all.supplements.filter { inDay(it.timeEpochMillis) }.map { supplementDisplay(it) }
            ).sortedByDescending { it.time }

        val birthday = baby?.let { LocalDate.ofEpochDay(it.birthdayEpochDay) }
        val dayAge = birthday?.let {
            java.time.temporal.ChronoUnit.DAYS.between(it, day).toInt() + 1
        }

        TimelineUiState(
            babyName = baby?.name ?: "宝宝",
            days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) },
            selectedDay = day,
            group = DayGroup(
                date = day,
                dayAge = dayAge,
                stats = dayStats(items),
                items = items
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState())

    /** 当日五类统计 */
    private fun dayStats(items: List<RecordDisplay>): List<Pair<String, String>> {
        val feeds = items.filter { it.type.name == "FEEDING" }
        val milk = feeds.sumOf { it.feeding?.amountMl ?: 0 }
        val diapers = items.filter { it.type.name == "DIAPER" }
        val poop = diapers.count {
            it.diaper?.state == com.babyrecord.app.data.DiaperState.POOP.name ||
                it.diaper?.state == com.babyrecord.app.data.DiaperState.BOTH.name
        }
        val sleeps = items.filter { it.type.name == "SLEEP" }
        val sleepMin = sleeps.sumOf { s ->
            s.sleep?.let {
                val end = if (it.endEpochMillis == 0L) System.currentTimeMillis() else it.endEpochMillis
                ((end - it.startEpochMillis) / 60000).coerceAtLeast(0)
            } ?: 0
        }
        val sleepText = com.babyrecord.app.ui.sleepDurationText(sleepMin)
        val supps = items.filter { it.type.name == "SUPPLEMENT" }

        return listOf(
            "喂奶" to "共${feeds.size}次" + if (milk > 0) " · ${milk}ml" else "",
            "换尿布" to "共${diapers.size}次" + if (poop > 0) " · ${poop}次便便" else "",
            "睡眠" to "共${sleeps.size}段" + if (sleeps.isNotEmpty()) " · $sleepText" else "",
            "营养品" to "共${supps.size}次"
        )
    }

    /** 宝宝醒了：把进行中的睡眠补上醒来时间（当前时刻） */
    fun markAwake(record: RecordDisplay) {
        viewModelScope.launch {
            record.sleep?.takeIf { it.endEpochMillis == 0L }?.let {
                db.sleepDao().update(it.copy(endEpochMillis = System.currentTimeMillis()).markDirty())
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(getApplication())
            }
            // 家庭同步：记录变更后去抖触发（AutoSync.requestPush 内部已联动）
            AutoSync.requestPush(getApplication())
        }
    }
}
