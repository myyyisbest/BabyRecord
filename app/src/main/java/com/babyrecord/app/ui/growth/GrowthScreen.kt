package com.babyrecord.app.ui.growth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.data.MeasureType
import com.babyrecord.app.data.MeasurementEntity
import com.babyrecord.app.data.PhotoEntity
import com.babyrecord.app.ui.album.AlbumPreviewCard
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.MintContainer
import com.babyrecord.app.ui.theme.MintGreen
import com.babyrecord.app.ui.theme.OnCoralContainer
import com.babyrecord.app.ui.theme.SkyBlue
import com.babyrecord.app.ui.theme.SkyBlueContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt


private val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

@Composable
fun GrowthScreen(
    state: GrowthUiState,
    photos: List<PhotoEntity>,    birthday: LocalDate?,

    onAddMeasure: () -> Unit,
    onDeleteMeasure: (MeasurementEntity) -> Unit,
    onOpenAlbum: () -> Unit
) {
    var metric by remember { mutableStateOf(MeasureType.WEIGHT) }
    var pendingDelete by remember { mutableStateOf<MeasurementEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("成长记录", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text("见证每一厘米的成长", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        AlbumPreviewCard(photos = photos, birthday = birthday, onOpenAlbum = onOpenAlbum)
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(
                Modifier.weight(1f), Icons.Rounded.MonitorWeight, CoralContainer,
                state.latest[MeasureType.WEIGHT.name]?.let { fmt(it.value) } ?: "—", "体重(kg)"
            )
            StatBox(
                Modifier.weight(1f), Icons.Rounded.Height, SkyBlueContainer,
                state.latest[MeasureType.HEIGHT.name]?.let { fmt(it.value) } ?: "—", "身高(cm)"
            )
            StatBox(
                Modifier.weight(1f), Icons.Rounded.Face, MintContainer,
                state.latest[MeasureType.HEAD.name]?.let { fmt(it.value) } ?: "—", "头围(cm)"
            )
        }

        Spacer(Modifier.height(18.dp))
        val series = when (metric) {
            MeasureType.WEIGHT -> state.weightSeries
            MeasureType.HEIGHT -> state.heightSeries
            MeasureType.HEAD -> state.headSeries
        }
        ChartCard(metric, series, state.gender, onMetricChange = { metric = it })

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onAddMeasure,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral)
        ) {
            Text("＋ 记录一次测量", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(22.dp))
        Text("测量历史", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        if (state.allMeasurements.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有测量记录，从体重开始吧", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.allMeasurements.take(10).forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                            .clickable { pendingDelete = m }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(containerOf(m.type)),
                            contentAlignment = Alignment.Center
                        ) { Icon(iconOf(m.type), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(17.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${labelOf(m.type)} ${fmt(m.value)} ${unitOf(m.type)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            Instant.ofEpochMilli(m.timeEpochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    pendingDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条测量？", fontWeight = FontWeight.Bold) },
            text = { Text("${labelOf(m.type)} ${fmt(m.value)} ${unitOf(m.type)}") },
            confirmButton = {
                TextButton(onClick = { onDeleteMeasure(m); pendingDelete = null }) {
                    Text("删除", color = Color(0xFFD64545), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

@Composable
private fun StatBox(modifier: Modifier = Modifier, icon: ImageVector, container: Color, value: String, label: String) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChartCard(metric: MeasureType, series: List<Pair<Float, Float>>, gender: String, onMetricChange: (MeasureType) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("成长曲线", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(MeasureType.WEIGHT to "体重", MeasureType.HEIGHT to "身高", MeasureType.HEAD to "头围").forEach { (t, label) ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (metric == t) CoralContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onMetricChange(t) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            label, fontSize = 12.sp,
                            color = if (metric == t) OnCoralContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (metric == t) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val unit = if (metric == MeasureType.WEIGHT) "kg" else "cm"
        if (series.size < 2) {
            Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📈", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("记录两次以上即可生成曲线", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val ref = WhoStandards.forMetric(metric, gender)
            GrowthChart(series, unit, ref)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(RoundedCornerShape(2.dp))
                        .background(MintGreen.copy(alpha = 0.18f))
                )
                Spacer(Modifier.width(5.dp))
                Text("WHO 区间", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(16.dp).height(2.dp).background(MintGreen.copy(alpha = 0.55f)))
                Spacer(Modifier.width(5.dp))
                Text("中位数", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("区间为 -2SD ~ +2SD", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "参考区间为 WHO 生长标准近似值，仅供家庭参考",
                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GrowthChart(data: List<Pair<Float, Float>>, unit: String, ref: List<RefPoint>) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = TextStyle(fontSize = 12.sp, color = Coral, fontWeight = FontWeight.Bold)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bandColor = MintGreen.copy(alpha = 0.16f)
    val medianColor = MintGreen.copy(alpha = 0.55f)

    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        val padL = 52f
        val padR = 16f
        val padT = 30f
        val padB = 24f

        var xMin = data.minOf { it.first }
        var xMax = data.maxOf { it.first }
        if (xMax - xMin < 0.5f) xMax = xMin + 0.5f
        var yMin = data.minOf { it.second }
        var yMax = data.maxOf { it.second }

        // 让 WHO 参考带也参与纵向范围计算
        val bandXs = (sortedSetOf(xMin, xMax) + ref.map { it.month.toFloat() }.filter { it in xMin..xMax }).toList()
        val band = bandXs.mapNotNull { x -> WhoStandards.sample(ref, x)?.let { x to it } }
        band.forEach { (_, v) ->
            yMin = minOf(yMin, v.first)
            yMax = maxOf(yMax, v.third)
        }

        if (yMax - yMin < 0.001f) {
            yMin -= 1f; yMax += 1f
        } else {
            val pad = (yMax - yMin) * 0.12f
            yMin -= pad; yMax += pad
        }

        fun px(x: Float) = padL + (x - xMin) / (xMax - xMin) * (w - padL - padR)
        fun py(y: Float) = padT + (1f - (y - yMin) / (yMax - yMin)) * (h - padT - padB)

        // WHO 参考带（先画在底层）
        if (band.size >= 2) {
            val bandPath = Path().apply {
                band.forEachIndexed { i, (_, v) ->
                    val x = px(bandXs[i])
                    if (i == 0) moveTo(x, py(v.third)) else lineTo(x, py(v.third))
                }
                for (i in band.indices.reversed()) {
                    lineTo(px(bandXs[i]), py(band[i].second.first))
                }
                close()
            }
            drawPath(bandPath, color = bandColor)

            val medianPath = Path().apply {
                band.forEachIndexed { i, (_, v) ->
                    val x = px(bandXs[i])
                    if (i == 0) moveTo(x, py(v.second)) else lineTo(x, py(v.second))
                }
            }
            drawPath(
                medianPath, color = medianColor,
                style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            )
        }

        val line = Path().apply {
            data.forEachIndexed { i, p ->
                if (i == 0) moveTo(px(p.first), py(p.second)) else lineTo(px(p.first), py(p.second))
            }
        }
        drawPath(
            line, color = Coral,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val fill = Path().apply {
            addPath(line)
            lineTo(px(data.last().first), h - padB)
            lineTo(px(data.first().first), h - padB)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(listOf(Coral.copy(alpha = 0.20f), Color.Transparent), startY = padT, endY = h - padB)
        )

        data.forEach { p ->
            val c = Offset(px(p.first), py(p.second))
            drawCircle(Color.White, radius = 7f, center = c, style = Fill)
            drawCircle(Coral, radius = 7f, center = c, style = Stroke(width = 3.5f))
        }

        val yMaxText = measurer.measure(fmtOf(yMax), labelStyle)
        drawText(yMaxText, color = onSurfaceVariantColor, topLeft = Offset(0f, padT - 10f))
        val yMinText = measurer.measure(fmtOf(yMin), labelStyle)
        drawText(yMinText, color = onSurfaceVariantColor, topLeft = Offset(0f, h - padB - yMinText.size.height + 10f))

        val x0 = measurer.measure("${xMin.roundToInt()}月", labelStyle)
        drawText(x0, color = onSurfaceVariantColor, topLeft = Offset(padL, h - 12f))
        val x1 = measurer.measure("${xMax.roundToInt()}月", labelStyle)
        drawText(x1, color = onSurfaceVariantColor, topLeft = Offset(w - x1.size.width - 4f, h - 12f))

        val last = data.last()
        val lv = measurer.measure("${fmtOf(last.second)}$unit", valueStyle)
        val lx = (px(last.first) - lv.size.width / 2).coerceIn(padL, w - padR - lv.size.width)
        drawText(lv, color = Coral, topLeft = Offset(lx, (py(last.second) - lv.size.height - 10f).coerceAtLeast(0f)))
    }
}


private fun iconOf(type: String): ImageVector = when (type) {
    MeasureType.WEIGHT.name -> Icons.Rounded.MonitorWeight
    MeasureType.HEIGHT.name -> Icons.Rounded.Height
    else -> Icons.Rounded.Face
}

private fun containerOf(type: String): Color = when (type) {
    MeasureType.WEIGHT.name -> CoralContainer
    MeasureType.HEIGHT.name -> SkyBlueContainer
    else -> MintContainer
}

fun labelOf(type: String): String = when (type) {
    MeasureType.WEIGHT.name -> "体重"
    MeasureType.HEIGHT.name -> "身高"
    else -> "头围"
}

fun unitOf(type: String): String = if (type == MeasureType.WEIGHT.name) "kg" else "cm"

/** 测量值显示：体重/头围保留两位小数（如 4.45kg 不丢精度），身高一位足够 */
fun fmt(value: Double): String = String.format("%.2f", value).trimEnd('0').trimEnd('.')

private fun fmtOf(value: Float): String = String.format("%.2f", value).trimEnd('0').trimEnd('.')
