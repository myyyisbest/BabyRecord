package com.babyrecord.app.ui.growth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.MeasureType
import com.babyrecord.app.data.MeasurementEntity
import com.babyrecord.app.ui.backup.AutoSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class GrowthUiState(
    val latest: Map<String, MeasurementEntity> = emptyMap(),
    val weightSeries: List<Pair<Float, Float>> = emptyList(),
    val heightSeries: List<Pair<Float, Float>> = emptyList(),
    val headSeries: List<Pair<Float, Float>> = emptyList(),
    val allMeasurements: List<MeasurementEntity> = emptyList(),
    val gender: String = "secret"
)

class GrowthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyApp).database
    private val zone = ZoneId.systemDefault()

    val uiState: StateFlow<GrowthUiState> = combine(
        db.measurementDao().observeAll(),
        db.babyDao().observeBaby()
    ) { list, baby ->
        val birthday = baby?.let { LocalDate.ofEpochDay(it.birthdayEpochDay) }

        fun series(type: String): List<Pair<Float, Float>> =
            list.filter { it.type == type }.sortedBy { it.timeEpochMillis }.map { m ->
                val ageMonths = if (birthday != null) {
                    ChronoUnit.DAYS.between(
                        birthday,
                        Instant.ofEpochMilli(m.timeEpochMillis).atZone(zone).toLocalDate()
                    ).toFloat() / 30.44f
                } else 0f
                ageMonths to m.value.toFloat()
            }

        val latest = buildMap {
            for (t in MeasureType.entries) {
                list.firstOrNull { it.type == t.name }?.let { put(t.name, it) }
            }
        }

        GrowthUiState(
            latest = latest,
            weightSeries = series(MeasureType.WEIGHT.name),
            heightSeries = series(MeasureType.HEIGHT.name),
            headSeries = series(MeasureType.HEAD.name),
            allMeasurements = list,
            gender = baby?.gender ?: "secret"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GrowthUiState())

    fun addMeasurement(type: MeasureType, value: Double, date: LocalDate) {
        viewModelScope.launch {
            db.measurementDao().insert(
                MeasurementEntity(
                    type = type.name,
                    value = value,
                    timeEpochMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                ).withNewId()
            )
            // 家庭同步：记录变更后去抖触发（AutoSync.requestPush 内部已联动）
            AutoSync.requestPush(getApplication())
        }
    }

    /** 删除改为软删：deletedAt + markDirty，随同步推送到其他设备 */
    fun deleteMeasurement(measurement: MeasurementEntity) {
        viewModelScope.launch {
            db.measurementDao().update(measurement.copy(deletedAt = System.currentTimeMillis()).markDirty())
            AutoSync.requestPush(getApplication())
        }
    }
}
