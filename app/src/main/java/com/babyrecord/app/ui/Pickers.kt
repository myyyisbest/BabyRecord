package com.babyrecord.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.theme.Coral
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 日期时间选择行：默认显示当前值，点击弹出一体化选择弹窗。
 */
@Composable
fun DateTimeField(
    label: String,
    value: LocalDateTime,
    onChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { show = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(formatDateTime(value), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text("修改", fontSize = 13.sp, color = Coral, fontWeight = FontWeight.Medium)
    }

    if (show) {
        DateTimePickerSheet(
            initial = value,
            onConfirm = {
                onChange(it)
                show = false
            },
            onDismiss = { show = false }
        )
    }
}

/** 一体化日期时间弹窗：快捷日期 + 左右翻日 + 可展开日历 + 数字时间输入，一步完成 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerSheet(
    initial: LocalDateTime,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableStateOf(initial.toLocalDate()) }
    var time by remember { mutableStateOf(initial.toLocalTime()) }
    var showCalendar by remember { mutableStateOf(false) }

    val today = LocalDate.now()

    if (showCalendar) {
        val zone = ZoneId.of("UTC")
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    }
                    showCalendar = false
                }) { Text("确定", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        ) { DatePicker(state = dateState) }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column {
                // 快捷日期：现在 / 今天 / 昨天 / 前天
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickChip("现在", date == today) {
                        date = today; time = LocalTime.now()
                    }
                    QuickChip("今天", date == today) { date = today }
                    QuickChip("昨天", date == today.minusDays(1)) { date = today.minusDays(1) }
                    QuickChip("前天", date == today.minusDays(2)) { date = today.minusDays(2) }
                }
                Spacer(Modifier.height(14.dp))
                // 日期行：‹ 2026年10月4日 周六 ›（点中间弹日历）
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { date = date.minusDays(1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一天", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showCalendar = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Event, contentDescription = null, tint = Coral, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${weekCn(date.dayOfWeek)}",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { date = date.plusDays(1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "后一天", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 时间：小时/分钟滚动轮（高亮带居中选中，上下滑动选数）
                TimeWheel(
                    hour = time.hour,
                    minute = time.minute,
                    onHourChange = { time = LocalTime.of(it, time.minute) },
                    onMinuteChange = { time = LocalTime.of(time.hour, it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalDateTime.of(date, time))
            }) { Text("确定", color = Coral, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

/**
 * 滚动轮时间选择：小时(0-23) / 分钟(0-59) 两列竖向滚轮。
 * - 惯性滑动 + 自动吸附到最近项
 * - 中间高亮带指示选中值，选中项加粗放大
 */
@Composable
private fun TimeWheel(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelColumn(
            count = 24,
            selected = hour,
            label = { "%02d".format(it) },
            onSelect = onHourChange
        )
        Text(
            ":",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        WheelColumn(
            count = 60,
            selected = minute,
            label = { "%02d".format(it) },
            onSelect = onMinuteChange
        )
    }
}

/** 单列滚轮：基于 LazyColumn + snap 吸附 */
@Composable
private fun WheelColumn(
    count: Int,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    val itemHeight = 44.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    // 用 derivedStateOf 避免滚动每帧全列表重组
    val selectedIdx by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // 滚动停止后回调选中项
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onSelect(listState.firstVisibleItemIndex)
        }
    }
    // 外部重置（如点“现在”快捷项）时滚到对应项
    LaunchedEffect(selected) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != selected) {
            listState.animateScrollToItem(selected)
        }
    }

    Box(
        Modifier
            .width(72.dp)
            .height(itemHeight * 3) // 上下各一项半透明，中间选中
    ) {
        // 高亮带
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count) { i ->
                val isSelected = i == selectedIdx
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label(i),
                        fontSize = if (isSelected) 20.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
    )
}

@Preview(name = "一体化时间选择弹窗")
@Composable
private fun DateTimePickerSheetPreview() {
    DateTimePickerSheet(
        initial = LocalDateTime.now(),
        onConfirm = { },
        onDismiss = { }
    )
}

private fun weekCn(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}

/** 仅选日期（用于测量、疫苗补记等）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickDate by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { pickDate = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("${value.year}年${value.monthValue}月${value.dayOfMonth}日", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text("修改", fontSize = 13.sp, color = Coral, fontWeight = FontWeight.Medium)
    }

    if (pickDate) {
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(zone).toLocalDate())
                    }
                    pickDate = false
                }) { Text("确定", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        ) { DatePicker(state = dateState) }
    }
}
