package com.babyrecord.app.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayStat(
    val date: LocalDate,
    val feedCount: Int = 0,
    val feedMl: Int = 0,
    val diaperCount: Int = 0,
    val sleepMinutes: Long = 0,
    val sleepCount: Int = 0,
    val solidCount: Int = 0
)

data class StatsUiState(val days: List<DayStat> = emptyList())

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyApp).database
    private val zone = ZoneId.systemDefault()

    private fun dayOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    val uiState: StateFlow<StatsUiState> = combine(
        db.feedingDao().observeAll(),
        db.diaperDao().observeAll(),
        db.sleepDao().observeAll(),
        db.solidFoodDao().observeAll()
    ) { feedings, diapers, sleeps, solids ->
        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        StatsUiState(
            days.map { day ->
                DayStat(
                    date = day,
                    feedCount = feedings.count { dayOf(it.timeEpochMillis) == day },
                    feedMl = feedings.filter { dayOf(it.timeEpochMillis) == day }.sumOf { it.amountMl ?: 0 },
                    diaperCount = diapers.count { dayOf(it.timeEpochMillis) == day },
                    sleepMinutes = sleeps.filter { dayOf(it.startEpochMillis) == day }
                        .sumOf {
                            val endMillis = if (it.endEpochMillis == 0L) System.currentTimeMillis() else it.endEpochMillis
                            Duration.between(
                                Instant.ofEpochMilli(it.startEpochMillis),
                                Instant.ofEpochMilli(endMillis)
                            ).toMinutes().coerceAtLeast(0)
                        },
                    sleepCount = sleeps.count { dayOf(it.startEpochMillis) == day },
                    solidCount = solids.count { dayOf(it.timeEpochMillis) == day }
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
