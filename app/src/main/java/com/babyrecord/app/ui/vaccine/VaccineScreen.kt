package com.babyrecord.app.ui.vaccine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.data.VaccineEntity
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.DividerWarm
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.OnCoralContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val WarnRed = Color(0xFFD64545)
private val WarnRedContainer = Color(0xFFFDE3E3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineScreen(
    state: VaccineUiState,
    reminderEnabled: Boolean,
    onBack: () -> Unit,
    onRecord: (VaccineDoseDef, LocalDate) -> Unit,
    onDelete: (VaccineEntity) -> Unit,
    onToggleReminder: (Boolean) -> Unit
) {
    var pickDose by remember { mutableStateOf<VaccineDoseDef?>(null) }
    var deleteRecord by remember { mutableStateOf<VaccineEntity?>(null) }
    var pickDateFor by remember { mutableStateOf<VaccineDoseDef?>(null) }
    val zone = ZoneId.systemDefault()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("疫苗接种本", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(CoralContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("${state.doneCount} / ${state.total}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnCoralContainer)
            }
        }

        Spacer(Modifier.height(12.dp))
        val progress = if (state.total > 0) state.doneCount.toFloat() / state.total else 0f
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(CoralContainer)) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Coral))
        }
        state.nextDue?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                "下一剂：${it.def.vaccine} ${it.def.doseLabel} · 建议 ${it.def.ageMonths}月龄",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 接种提醒开关
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔔 到期提醒通知", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = reminderEnabled,
                onCheckedChange = onToggleReminder
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
            state.groups.forEach { group ->
                item(key = "header_${group.ageMonths}") {
                    val due = state.birthday?.plusMonths(group.ageMonths.toLong())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                    ) {
                        Text(group.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (group.isCurrent) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).background(Coral)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("当前阶段", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        due?.let {
                            Text("${it.year}年${it.monthValue}月", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.width(24.dp).height(1.dp).background(DividerWarm))
                    }
                }
                items(group.doses, key = { "${it.def.vaccine}_${it.def.doseLabel}" }) { dose ->
                    DoseRow(dose, state.birthday, onClick = {
                        if (dose.record == null) pickDose = dose.def else deleteRecord = dose.record
                    })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    pickDose?.let { def ->
        AlertDialog(
            onDismissRequest = { pickDose = null },
            title = { Text("${def.vaccine} · ${def.doseLabel}", fontWeight = FontWeight.Bold) },
            text = { Text("免疫规划建议 ${def.ageMonths} 月龄接种。\n可记为今天，或选择实际接种日期。") },
            confirmButton = {
                TextButton(onClick = { onRecord(def, LocalDate.now()); pickDose = null }) {
                    Text("记为今天", color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pickDateFor = def; pickDose = null }) { Text("选择日期", color = MaterialTheme.colorScheme.onSurface) }
                    TextButton(onClick = { pickDose = null }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        )
    }

    pickDateFor?.let { def ->
        DatePickerDialog(
            onDismissRequest = { pickDateFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onRecord(def, Instant.ofEpochMilli(it).atZone(zone).toLocalDate())
                    }
                    pickDateFor = null
                }) { Text("确定", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pickDateFor = null }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        ) { DatePicker(state = pickerState) }
    }

    deleteRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            title = { Text("删除接种记录？", fontWeight = FontWeight.Bold) },
            text = { Text("${rec.vaccineName} ${rec.doseLabel} · 接种于 ${formatDate(rec.dateEpochDay)}") },
            confirmButton = {
                TextButton(onClick = { onDelete(rec); deleteRecord = null }) {
                    Text("删除", color = WarnRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRecord = null }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

@Composable
private fun DoseRow(dose: VaccineDose, birthday: LocalDate?, onClick: () -> Unit) {
    val due = birthday?.plusMonths(dose.def.ageMonths.toLong())
    val overdue = dose.record == null && due != null && due.isBefore(LocalDate.now())
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${dose.def.vaccine} ${dose.def.doseLabel}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (dose.record != null) {
                    "接种日期：${formatDate(dose.record.dateEpochDay)}"
                } else if (due != null) {
                    "建议 ${due.year}年${due.monthValue}月"
                } else {
                    "免疫规划疫苗"
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            dose.record != null -> StatusPill("✓ 已接种", CoralContainer, OnCoralContainer)
            overdue -> StatusPill("到期待种", WarnRedContainer, WarnRed)
            else -> StatusPill("待种", MaterialTheme.colorScheme.surfaceVariant, InkSoft)
        }
    }
}

@Composable
private fun StatusPill(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}

private fun formatDate(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return "${d.year}年${d.monthValue}月${d.dayOfMonth}日"
}
