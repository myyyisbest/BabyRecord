package com.babyrecord.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.RecordDisplay
import com.babyrecord.app.ui.RecordType
import com.babyrecord.app.ui.formatTime
import com.babyrecord.app.ui.isSleepOngoing
import com.babyrecord.app.ui.sleepDurationText
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.DividerWarm
import com.babyrecord.app.ui.theme.Lilac
import com.babyrecord.app.ui.theme.MintGreen
import java.time.LocalDate

private val PinkAccent = Color(0xFFE0597F)
private val Amber = Color(0xFFF5A623)
private val OrangeDot = Color(0xFFE8825A)

private fun dotColor(type: RecordType): Color = when (type) {
    RecordType.FEEDING -> Amber
    RecordType.SLEEP -> Lilac
    RecordType.DIAPER -> MintGreen
    RecordType.SOLID -> OrangeDot
    RecordType.SUPPLEMENT -> PinkAccent
}

@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onSelectDay: (LocalDate) -> Unit,
    onMarkAwake: (RecordDisplay) -> Unit,
    onOpenRecord: (RecordDisplay) -> Unit,
    onOpenStats: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("记录", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenStats) {
                Icon(Icons.Rounded.BarChart, contentDescription = "统计", tint = Coral)
            }
        }
        Spacer(Modifier.height(10.dp))

        WeekStrip(state.days, state.selectedDay, onSelectDay)
        Spacer(Modifier.height(12.dp))

        val group = state.group
        if (group == null || group.items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) { Text("🍼", fontSize = 44.sp) }
                    Spacer(Modifier.height(18.dp))
                    Text("这一天还没有记录", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text("去「今日」页记一笔吧", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    DayHeader(group, state.babyName)
                    Spacer(Modifier.height(14.dp))
                }
                val lastIndex = group.items.size - 1
                itemsIndexed(group.items) { i, r ->
                    val ongoingSleep = r.sleep?.let { isSleepOngoing(it) } == true
                    TimelineEntry(
                        time = formatTime(
                            if (r.type.name == "SLEEP") r.sleep?.startEpochMillis ?: r.time else r.time
                        ),
                        isLast = i == lastIndex,
                        record = r,
                        showAwakeButton = ongoingSleep,
                        onClick = { onOpenRecord(r) },
                        onMarkAwake = { onMarkAwake(r) }
                    )
                }
            }
        }
    }
}

// ── 近一周日期切换条：今日字样 + 圆形日期高亮 ──
@Composable
private fun WeekStrip(days: List<LocalDate>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            val isSelected = day == selected
            val isToday = day == LocalDate.now()
            val weekday = "日一二三四五六"[day.dayOfWeek.value % 7].toString()
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(day) }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isToday) "今日" else weekday.toString(),
                    fontSize = 11.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isSelected || isToday -> Coral
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Coral else Color.Transparent)
                        .then(
                            if (isToday && !isSelected) Modifier.border(1.5.dp, Coral, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${day.dayOfMonth}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isSelected -> Color.White
                            isToday -> Coral
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

// ── 当日总结卡：日期 + 2×2 紧凑统计网格 ──
@Composable
private fun DayHeader(group: DayGroup, babyName: String) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${group.date.monthValue}月${group.date.dayOfMonth}日",
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                color = if (group.date == LocalDate.now()) Coral else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            group.dayAge?.let {
                Text(
                    "${babyName}第${it}天", fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            group.stats.chunked(2).forEach { rowPairs ->
                Row(Modifier.fillMaxWidth()) {
                    StatGridCell(Modifier.weight(1f), rowPairs[0].first, rowPairs[0].second)
                    Spacer(Modifier.width(16.dp))
                    rowPairs.getOrNull(1)?.let {
                        StatGridCell(Modifier.weight(1f), it.first, it.second)
                    } ?: Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatGridCell(modifier: Modifier, label: String, value: String) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(10.dp))
        Text(
            value, fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
        )
    }
}

// ── 时间轴条目：左时间 + 彩色圆点连线 + 紧凑记录卡 ──
@Composable
private fun TimelineEntry(
    time: String,
    isLast: Boolean,
    record: RecordDisplay,
    showAwakeButton: Boolean,
    onClick: () -> Unit,
    onMarkAwake: () -> Unit
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.TopEnd) {
            Text(
                time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 13.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.width(13.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.padding(top = 13.dp).size(8.dp).clip(CircleShape).background(dotColor(record.type))
            )
            if (!isLast) {
                Box(Modifier.width(1.5.dp).weight(1f).background(DividerWarm))
            } else {
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).padding(bottom = 6.dp)) {
            RecordCard(record, showAwakeButton, onClick, onMarkAwake)
        }
    }
}

@Composable
private fun RecordCard(
    record: RecordDisplay,
    showAwakeButton: Boolean,
    onClick: () -> Unit,
    onMarkAwake: () -> Unit
) {
    val sleep = record.sleep
    val sleepEnded = record.type == RecordType.SLEEP && sleep != null && !isSleepOngoing(sleep)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(record.containerColor),
            contentAlignment = Alignment.Center
        ) { Text(record.emoji, fontSize = 15.sp) }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (record.type == RecordType.SLEEP) "睡眠" else record.title,
                fontSize = 13.5.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (record.type != RecordType.SLEEP && record.subtitle.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    record.subtitle, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        when {
            showAwakeButton -> {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onMarkAwake,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC53D),
                        contentColor = Color(0xFF4A3200)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("宝宝醒了", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            sleepEnded && sleep != null -> {
                val minutes = (sleep.endEpochMillis - sleep.startEpochMillis) / 60000
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "共${sleepDurationText(minutes)}",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PinkAccent
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        "时段 ${formatTime(sleep.startEpochMillis)} – ${formatTime(sleep.endEpochMillis)}",
                        fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> { }
        }
    }
}
