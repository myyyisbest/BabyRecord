package com.babyrecord.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.DiaperEntity
import com.babyrecord.app.data.FeedingEntity
import com.babyrecord.app.data.SleepEntity
import com.babyrecord.app.data.SupplementEntity
import com.babyrecord.app.data.SolidFoodEntity
import com.babyrecord.app.ui.RecordType
import com.babyrecord.app.ui.backup.AutoSync
import com.babyrecord.app.widget.QuickStatsWidgetHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data class Feeding(val data: FeedingEntity) : DetailState
    data class Diaper(val data: DiaperEntity) : DetailState
    data class Sleep(val data: SleepEntity) : DetailState
    data class Solid(val data: SolidFoodEntity) : DetailState
    data class Supplement(val data: SupplementEntity) : DetailState
}

class RecordDetailViewModel(
    application: Application,
    private val type: RecordType,
    private val id: String
) : AndroidViewModel(application) {
    private val db = (application as BabyApp).database

    val state: StateFlow<DetailState> = when (type) {
        RecordType.FEEDING ->
            db.feedingDao().observeById(id).map { if (it == null) DetailState.Loading else DetailState.Feeding(it) }
        RecordType.DIAPER ->
            db.diaperDao().observeById(id).map { if (it == null) DetailState.Loading else DetailState.Diaper(it) }
        RecordType.SLEEP ->
            db.sleepDao().observeById(id).map { if (it == null) DetailState.Loading else DetailState.Sleep(it) }
        RecordType.SOLID ->
            db.solidFoodDao().observeById(id).map { if (it == null) DetailState.Loading else DetailState.Solid(it) }
        RecordType.SUPPLEMENT ->
            db.supplementDao().observeById(id).map { if (it == null) DetailState.Loading else DetailState.Supplement(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailState.Loading)

    // 保存/删除后同步刷新桌面小组件，避免组件显示过时状态（如“睡眠中…”）
    private fun refreshWidget() {
        QuickStatsWidgetHelper.refresh(getApplication())
    }

    fun saveFeeding(data: FeedingEntity) = launch { db.feedingDao().update(data.markDirty()); refreshWidget() }
    fun saveDiaper(data: DiaperEntity) = launch { db.diaperDao().update(data.markDirty()); refreshWidget() }
    fun saveSleep(data: SleepEntity) = launch { db.sleepDao().update(data.markDirty()); refreshWidget() }
    fun saveSolid(data: SolidFoodEntity) = launch { db.solidFoodDao().update(data.markDirty()); refreshWidget() }
    fun saveSupplement(data: SupplementEntity) = launch { db.supplementDao().update(data.markDirty()); refreshWidget() }

    /**
     * 删除改为软删：copy(deletedAt=now).markDirty() 后回写。
     * markDirty 会重置 updatedAt/syncedAt，deletedAt 随行同步到其他设备；
     * 第一阶段 DAO 的查询已过滤 deletedAt，UI 立即隐藏该记录。
     */
    fun delete() = launch {
        val now = System.currentTimeMillis()
        (state.value as? DetailState.Feeding)?.data?.let { db.feedingDao().update(it.copy(deletedAt = now).markDirty()) }
        (state.value as? DetailState.Diaper)?.data?.let { db.diaperDao().update(it.copy(deletedAt = now).markDirty()) }
        (state.value as? DetailState.Sleep)?.data?.let { db.sleepDao().update(it.copy(deletedAt = now).markDirty()) }
        (state.value as? DetailState.Solid)?.data?.let { db.solidFoodDao().update(it.copy(deletedAt = now).markDirty()) }
        (state.value as? DetailState.Supplement)?.data?.let { db.supplementDao().update(it.copy(deletedAt = now).markDirty()) }
        // 同步引擎统一触发（AutoSync.requestPush 内部已联动）
        AutoSync.requestPush(getApplication())
        refreshWidget()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
