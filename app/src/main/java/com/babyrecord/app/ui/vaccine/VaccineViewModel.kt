package com.babyrecord.app.ui.vaccine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.VaccineEntity
import com.babyrecord.app.ui.backup.AutoSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class VaccineDoseDef(val vaccine: String, val doseLabel: String, val ageMonths: Int)

data class VaccineDose(val def: VaccineDoseDef, val record: VaccineEntity?)

data class VaccineAgeGroup(
    val ageMonths: Int,
    val label: String,
    val doses: List<VaccineDose>,
    val isCurrent: Boolean
)

data class VaccineUiState(
    val birthday: LocalDate? = null,
    val groups: List<VaccineAgeGroup> = emptyList(),
    val doneCount: Int = 0,
    val total: Int = NATIONAL_SCHEDULE.size,
    val nextDue: VaccineDose? = null
)

// 国家免疫规划疫苗儿童免疫程序（2021年版），共 22 剂，按月龄排序
val NATIONAL_SCHEDULE: List<VaccineDoseDef> = listOf(
    VaccineDoseDef("乙肝疫苗", "第1剂", 0),
    VaccineDoseDef("卡介苗", "1剂", 0),
    VaccineDoseDef("乙肝疫苗", "第2剂", 1),
    VaccineDoseDef("脊灰疫苗", "第1剂", 2),
    VaccineDoseDef("脊灰疫苗", "第2剂", 3),
    VaccineDoseDef("百白破疫苗", "第1剂", 3),
    VaccineDoseDef("脊灰疫苗", "第3剂", 4),
    VaccineDoseDef("百白破疫苗", "第2剂", 4),
    VaccineDoseDef("百白破疫苗", "第3剂", 5),
    VaccineDoseDef("乙肝疫苗", "第3剂", 6),
    VaccineDoseDef("流脑疫苗(A群)", "第1剂", 6),
    VaccineDoseDef("麻腮风疫苗", "第1剂", 8),
    VaccineDoseDef("乙脑疫苗(减毒)", "第1剂", 8),
    VaccineDoseDef("流脑疫苗(A群)", "第2剂", 9),
    VaccineDoseDef("百白破疫苗", "第4剂", 18),
    VaccineDoseDef("麻腮风疫苗", "第2剂", 18),
    VaccineDoseDef("甲肝疫苗(减毒)", "1剂", 18),
    VaccineDoseDef("乙脑疫苗(减毒)", "第2剂", 24),
    VaccineDoseDef("流脑疫苗(A+C)", "第1剂", 36),
    VaccineDoseDef("脊灰疫苗", "第4剂", 48),
    VaccineDoseDef("白破疫苗", "1剂", 72),
    VaccineDoseDef("流脑疫苗(A+C)", "第2剂", 72)
)

fun ageLabel(months: Int): String = when (months) {
    0 -> "出生 · 0月龄"
    18 -> "18月龄 · 1岁半"
    24 -> "2岁"
    36 -> "3岁"
    48 -> "4岁"
    72 -> "6岁"
    else -> "${months}月龄"
}

class VaccineViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyApp).database
    private val prefs = application.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)

    val reminderEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean("vaccine_reminder_enabled", true)
    )

    fun setReminderEnabled(on: Boolean) {
        prefs.edit().putBoolean("vaccine_reminder_enabled", on).apply()
        reminderEnabled.value = on
    }

    val uiState: StateFlow<VaccineUiState> = combine(db.vaccineDao().observeAll(), db.babyDao().observeBaby()) { records, baby ->
        val birthday = baby?.let { LocalDate.ofEpochDay(it.birthdayEpochDay) }
        val byKey = records.associateBy { it.vaccineName to it.doseLabel }
        val allDoses = NATIONAL_SCHEDULE.map { VaccineDose(it, byKey[it.vaccine to it.doseLabel]) }

        val babyMonths = birthday?.let { ChronoUnit.MONTHS.between(it, LocalDate.now()).toInt() }
        val grouped = allDoses.groupBy { it.def.ageMonths }.toSortedMap()
        val currentAge = grouped.keys.lastOrNull { it <= (babyMonths ?: -1) }

        VaccineUiState(
            birthday = birthday,
            groups = grouped.map { (age, doses) ->
                VaccineAgeGroup(
                    ageMonths = age,
                    label = ageLabel(age),
                    doses = doses,
                    isCurrent = age == currentAge
                )
            },
            doneCount = records.size,
            total = NATIONAL_SCHEDULE.size,
            nextDue = allDoses.filter { it.record == null }.minByOrNull { it.def.ageMonths }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaccineUiState())

    fun recordVaccine(def: VaccineDoseDef, date: LocalDate) {
        viewModelScope.launch {
            val dao = db.vaccineDao()
            val existing = dao.find(def.vaccine, def.doseLabel)
            if (existing == null) {
                dao.insert(VaccineEntity(vaccineName = def.vaccine, doseLabel = def.doseLabel, dateEpochDay = date.toEpochDay()).withNewId())
            } else {
                dao.update(existing.copy(dateEpochDay = date.toEpochDay()).markDirty())
            }
            // 家庭同步：记录变更后去抖触发（AutoSync.requestPush 内部已联动）
            AutoSync.requestPush(getApplication())
        }
    }

    /** 删除改为软删：deletedAt + markDirty，随同步推送到其他设备 */
    fun deleteRecord(record: VaccineEntity) {
        viewModelScope.launch {
            db.vaccineDao().update(record.copy(deletedAt = System.currentTimeMillis()).markDirty())
            AutoSync.requestPush(getApplication())
        }
    }
}
