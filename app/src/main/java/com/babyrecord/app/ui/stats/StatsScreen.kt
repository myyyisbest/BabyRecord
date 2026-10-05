package com.babyrecord.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.sleepDurationText
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.Lilac
import com.babyrecord.app.ui.theme.LilacContainer
import com.babyrecord.app.ui.theme.MintContainer
import com.babyrecord.app.ui.theme.MintGreen
import com.babyrecord.app.ui.theme.OnCoralContainer
import com.babyrecord.app.ui.theme.SkyBlue
import com.babyrecord.app.ui.theme.SkyBlueContainer
import java.time.LocalDate

private enum class StatsTab(val label: String) { FEEDING("喂奶"), DIAPER("换尿布"), SLEEP("睡眠"), SOLID("辅食") }

@Composable
fun StatsScreen(state: StatsUiState, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(StatsTab.FEEDING) }

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
            Text("一周统计", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
        }
        val range = state.days.firstOrNull()?.let { first ->
            state.days.lastOrNull()?.let { last ->
                "${first.date.monthValue}月${first.date.dayOfMonth}日 – ${last.date.monthValue}月${last.date.dayOfMonth}日"
            }
        } ?: ""
        Text(range, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            StatsTab.entries.forEach { t ->
                val selected = tab == t
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Coral else MaterialTheme.colorScheme.surface)
                        .clickable { tab = t }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        t.label, fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            when (tab) {
                StatsTab.FEEDING -> {
                    StatCard("每日喂奶次数（次）", Coral) { WeeklyBarChart(state.days.map { it.feedCount.toFloat() }, Coral) { v -> "${v.toInt()}次" } }
                    StatCard("每日奶量（ml）", Coral) { WeeklyBarChart(state.days.map { it.feedMl.toFloat() }, Coral) { v -> "${v.toInt()}ml" } }
                }
                StatsTab.DIAPER -> {
                    StatCard("每日换尿布次数（次）", SkyBlue) { WeeklyBarChart(state.days.map { it.diaperCount.toFloat() }, SkyBlue) { v -> "${v.toInt()}次" } }
                }
                StatsTab.SLEEP -> {
                    StatCard("每日睡眠时长（小时）", Lilac) { WeeklyBarChart(state.days.map { it.sleepMinutes / 60f }, Lilac) { v -> String.format("%.1fh", v) } }
                    StatCard("每日睡眠次数（次）", Lilac) { WeeklyBarChart(state.days.map { it.sleepCount.toFloat() }, Lilac) { v -> "${v.toInt()}次" } }
                }
                StatsTab.SOLID -> {
                    StatCard("每日辅食次数（次）", MintGreen) { WeeklyBarChart(state.days.map { it.solidCount.toFloat() }, MintGreen) { v -> "${v.toInt()}次" } }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(title: String, accent: Color, chart: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        chart()
    }
}

/**
 * 最近 7 天柱状图：values 长度必须为 7（旧 → 新）。
 */
@Composable
fun WeeklyBarChart(values: List<Float>, color: Color, valueFormat: (Float) -> String) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val w = size.width
            val h = size.height
            val padB = 24f
            val padT = 20f
            val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(1f) * 1.15f
            val slot = w / values.size
            val barW = slot * 0.5f
            val baseline = h - padB

            // 底线
            drawLine(Color(0xFFF3E4DB), Offset(0f, baseline), Offset(w, baseline), strokeWidth = 2f)

            values.forEachIndexed { i, v ->
                val cx = slot * i + slot / 2
                val ratio = (v / maxV).coerceIn(0f, 1f)
                val barH = ratio * (baseline - padT)
                val topLeft = androidx.compose.ui.geometry.Offset(cx - barW / 2, baseline - barH)
                val barSize = androidx.compose.ui.geometry.Size(barW, barH.coerceAtLeast(3f))
                drawRoundRect(
                    color = if (v > 0f) color else Color(0xFFF3E4DB),
                    topLeft = topLeft,
                    size = barSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                // 数值标签
                if (v > 0f) {
                    val t = measurer.measure(valueFormat(v), valueStyle)
                    val tx = (cx - t.size.width / 2).coerceIn(0f, w - t.size.width.toFloat())
                    drawText(t, color = onSurfaceColor, topLeft = androidx.compose.ui.geometry.Offset(tx, (topLeft.y - t.size.height - 2f).coerceAtLeast(0f)))
                }
                // 星期标签
                val day = LocalDate.now().minusDays(((values.size - 1 - i)).toLong())
                val wd = "一二三四五六日"[day.dayOfWeek.value % 7].toString()
                val t2 = measurer.measure(wd, labelStyle)
                drawText(t2, color = onSurfaceVariantColor, topLeft = androidx.compose.ui.geometry.Offset(cx - t2.size.width / 2, baseline + 6f))
            }
        }
    }
}
