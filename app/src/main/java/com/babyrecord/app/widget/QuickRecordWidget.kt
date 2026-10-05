package com.babyrecord.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.Instant
import com.babyrecord.app.BabyApp
import com.babyrecord.app.MainActivity
import com.babyrecord.app.R
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class QuickRecordWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickRecordWidget()

    // 桌面主动拉取（开机/桌面重启等场景）时也重算，避免“睡眠中”卡死
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        QuickStatsWidgetHelper.schedulePeriodicRefresh(context)
    }
}

/** 单列数据：当日累计值 + 最近一条记录 */
private data class StatCell(
    val value: String,          // 累计：120ml / 2h3m / 3次
    val recentTime: String,     // 最近一次 HH:mm（空串表示无记录）
    val recentDesc: String,     // 最近一次描述：120ml / 已睡1小时 / 臭臭+嘘嘘
    val recentAgo: String       // 相对时间：33分钟前
)

/**
 * 等比缩放布局：
 * - 以 220dp 高为设计基准（4×2），所有区域高度、间距、字号统一按 实际高度/220 缩放
 * - 4×2 与 4×3 是同一个布局整体放大，不会出现“只有中间被拉伸”的失衡
 * - 过矮（<120dp，如 4×1）时隐藏头部和文字，只留核心内容
 */
class QuickRecordWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = (context.applicationContext as BabyApp).database
        val baby = db.babyDao().observeBaby().first()
        val name = baby?.name ?: "宝宝"
        val birthday = baby?.let { LocalDate.ofEpochDay(it.birthdayEpochDay) }

        // 天数胶囊：X个月Y天；特殊纪念日（满月/百天/周岁）特殊显示
        var chip = ""
        var isMilestone = false
        if (birthday != null) {
            val months = ChronoUnit.MONTHS.between(birthday, LocalDate.now())
            val remDays = ChronoUnit.DAYS.between(birthday.plusMonths(months), LocalDate.now())
            val dayNo = ChronoUnit.DAYS.between(birthday, LocalDate.now()) + 1 // 出生当天算第1天
            val milestone = when {
                (months == 1L && remDays == 0L) -> "满月"
                dayNo == 100L -> "百天"
                (months >= 12 && months % 12 == 0L && remDays == 0L) -> "${months / 12}周岁"
                else -> null
            }
            if (milestone != null) {
                chip = "🎂 $milestone"
                isMilestone = true
            } else {
                chip = when {
                    months <= 0 -> "${remDays + 1}天"
                    remDays == 0L -> "${months}个月"
                    else -> "${months}个月${remDays}天"
                }
            }
        }

        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        fun timeText(millis: Long?) = millis?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone).format(timeFmt)
        } ?: "--:--"

        fun rel(millis: Long?): String {
            millis ?: return ""
            val min = (System.currentTimeMillis() - millis) / 60000
            return when {
                min < 1 -> "刚刚"
                min < 60 -> "${min}分钟前"
                min < (60 * 24) -> {
                    val h = min / 60
                    val m = min % 60
                    if (m > 0) "${h}小时${m}分前" else "${h}小时前"
                }
                else -> "${min / 60 / 24}天前"
            }
        }

        // 紧凑时长（单行文本用）：2d3h / 9h30m / 45m
        fun durShort(m: Long): String {
            val d = m / (60 * 24)
            val h = (m % (60 * 24)) / 60
            val mm = m % 60
            return when {
                d > 0 -> "${d}d" + if (h > 0) "${h}h" else ""
                m >= 60 -> "${m / 60}h" + if (mm > 0) "${mm}m" else ""
                else -> "${m}m"
            }
        }

        fun inToday(ts: Long) = ts >= todayStart && ts < todayEnd

        // ── 配方奶列：当日累计奶量 + 最近一次瓶喂 ──
        val feedings = db.feedingDao().observeAll().first()
        val todayMilkMl = feedings.filter { inToday(it.timeEpochMillis) }.sumOf { it.amountMl ?: 0 }
        val latestFeed = feedings.firstOrNull()
        val bottleCell = StatCell(
            value = if (todayMilkMl > 0) "${todayMilkMl}ml" else "0ml",
            recentTime = latestFeed?.let { timeText(it.timeEpochMillis) } ?: "",
            recentDesc = latestFeed?.amountMl?.takeIf { it > 0 }?.let { "${it}ml" } ?: (if (latestFeed != null) "亲喂" else "暂无"),
            recentAgo = rel(latestFeed?.timeEpochMillis)
        )

        // ── 睡眠列：当日累计时长（进行中计入）+ 最近一段 ──
        val sleeps = db.sleepDao().observeAll().first()
        val todaySleepMin = sleeps.sumOf { s ->
            val end = if (s.endEpochMillis == 0L) now else s.endEpochMillis
            ((minOf(end, todayEnd) - maxOf(s.startEpochMillis, todayStart)) / 60000)
                .coerceAtLeast(0)
        }
        val latestSleep = sleeps.firstOrNull()
        val sleepCell = StatCell(
            value = durShort(todaySleepMin),
            recentTime = latestSleep?.let { timeText(it.startEpochMillis) } ?: "",
            recentDesc = when {
                latestSleep == null -> "暂无"
                latestSleep.endEpochMillis == 0L -> "已睡${durShort(((now - latestSleep.startEpochMillis) / 60000).coerceAtLeast(0))}"
                else -> "睡了${durShort(((latestSleep.endEpochMillis - latestSleep.startEpochMillis) / 60000).coerceAtLeast(0))}"
            },
            recentAgo = rel(latestSleep?.startEpochMillis)
        )

        // ── 换尿布列：当日累计次数 + 最近一次 ──
        val diapers = db.diaperDao().observeAll().first()
        val todayDiaperCount = diapers.count { inToday(it.timeEpochMillis) }
        val latestDiaper = diapers.firstOrNull()
        val diaperCell = StatCell(
            value = "${todayDiaperCount}次",
            recentTime = latestDiaper?.let { timeText(it.timeEpochMillis) } ?: "",
            recentDesc = latestDiaper?.let {
                when (it.state) {
                    "PEE" -> "嘘嘘"
                    "POOP" -> "臭臭"
                    else -> "臭臭+嘘嘘"
                }
            } ?: "暂无",
            recentAgo = rel(latestDiaper?.timeEpochMillis)
        )

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                // 区域高度缩放：实际高度 / 基准高度220dp（决定头部/快捷栏高度、间距、图标）
                val scale = (size.height.value / 220f).coerceIn(0.75f, 1.25f)
                // 文字缩放：增长更保守（越大越慢），防止长文本如“配方奶”被截断
                // scale=1.25 时 textScale≈1.12，文字仅增 12%
                val textScale = 1f + (scale - 1f) * 0.45f
                // 过矮（如 4×1）：隐藏头部
                val showHeader = size.height >= 120.dp
                Content(context, scale, textScale, showHeader, name, chip, isMilestone, bottleCell, sleepCell, diaperCell)
            }
        }
    }
}

// ── 配色（日 / 夜）──
private val Ink = ColorProvider(Color(0xFF33302B), Color(0xFFEFE5DE))
private val Sub = ColorProvider(Color(0xFF6B625A), Color(0xFFC9B5A9))
private val Tip = ColorProvider(Color(0xFFB5ADA3), Color(0xFF8F7E74))
private val Accent = ColorProvider(Color(0xFFF27B97), Color(0xFFF2A7BD)) // 粉色强调：最近记录
private val Line = ColorProvider(Color(0xFFF0EBE3), Color(0xFF453932))
private val Card = ColorProvider(Color(0xFFFFFFFF), Color(0xFF211A16))
private val Tray = ColorProvider(Color(0xFFFCEEF3), Color(0xFF362A30))

// 头部胶囊：实心白底 + 深玫红文字（粉色渐变上对比度高，日/夜通用）
private val ChipBg = ColorProvider(Color(0xFFFFFFFF), Color(0xFFFFFFFF))
private val MilestoneBg = ColorProvider(Color(0xFFFFFFFF), Color(0xFFFFFFFF))
private val ChipText = ColorProvider(Color(0xFFB03A5B), Color(0xFFB03A5B))
private val MilestoneText = ColorProvider(Color(0xFFE85D80), Color(0xFFE85D80))

/** dp/sp 缩放辅助 */
private fun scaled(base: Float, scale: Float) = (base * scale).dp
private fun scaledSp(base: Float, scale: Float) = (base * scale).sp
/** 文本安全缩放：用更保守的 textScale（而非 scale），防止长文本如“配方奶”被截断 */
private fun textScaledSp(base: Float, textScale: Float) = (base * textScale).sp

/** 统一等比缩放布局：头部 / 统计区 / 快捷栏 全部随总高度同步缩放 */
@Composable
private fun Content(
    context: Context,
    scale: Float,
    textScale: Float,
    showHeader: Boolean,
    name: String,
    chip: String,
    isMilestone: Boolean,
    bottleCell: StatCell,
    sleepCell: StatCell,
    diaperCell: StatCell
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(Card)
    ) {
        // 头部：粉色渐变条，宝宝名胶囊 + 天数/纪念日胶囊（高度随 scale 缩放）
        if (showHeader) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(scaled(46f, scale))
                    .background(ImageProvider(R.drawable.bg_widget_head_gradient))
                    .padding(horizontal = scaled(14f, scale)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(99.dp)
                            .background(ChipBg)
                            .padding(horizontal = scaled(12f, scale), vertical = scaled(2f, scale))
                    ) {
                        Text(
                            name,
                            style = TextStyle(
                                color = ChipText,
                                fontSize = scaledSp(15f, scale),
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    if (chip.isNotBlank()) {
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(99.dp)
                                .background(if (isMilestone) MilestoneBg else ChipBg)
                                .padding(
                                    horizontal = scaled(if (isMilestone) 11f else 9f, scale),
                                    vertical = scaled(2f, scale)
                                )
                        ) {
                            Text(
                                chip,
                                style = TextStyle(
                                    color = if (isMilestone) MilestoneText else ChipText,
                                    fontSize = scaledSp(if (isMilestone) 14f else 12f, scale),
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 三等分统计列（弹性区，内容字号/图标随 scale 缩放）
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .padding(horizontal = 2.dp, vertical = scaled(4f, scale))
        ) {
            StatCol(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    .padding(start = scaled(14f, scale), end = scaled(8f, scale)),
                scale = scale, textScale = textScale,
                iconRes = R.drawable.ic_wg_bottle, label = "配方奶", cell = bottleCell
            )
            Spacer(GlanceModifier.width(1.dp).fillMaxHeight().background(Line))
            StatCol(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    .padding(start = scaled(8f, scale), end = scaled(8f, scale)),
                scale = scale, textScale = textScale,
                iconRes = R.drawable.ic_wg_moon, label = "睡眠", cell = sleepCell
            )
            Spacer(GlanceModifier.width(1.dp).fillMaxHeight().background(Line))
            StatCol(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    .padding(start = scaled(8f, scale), end = scaled(10f, scale)),
                scale = scale, textScale = textScale,
                iconRes = R.drawable.ic_wg_diaper, label = "换尿布", cell = diaperCell
            )
        }

        // 快捷记录：粉色色带，竖排按钮（高度随 scale 缩放，按钮同比放大）
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(scaled(76f, scale))
                .background(Tray)
                .padding(horizontal = scaled(6f, scale)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrayButton(context, "feeding", "喂奶", R.drawable.ic_wg_bottle, scale, textScale)
            TrayButton(context, "diaper", "换尿布", R.drawable.ic_wg_diaper, scale, textScale)
            TrayButton(context, "sleep", "睡眠", R.drawable.ic_wg_moon, scale, textScale)
            TrayButton(context, "solid", "辅食", R.drawable.ic_wg_food, scale, textScale)
            TrayButton(context, "supplement", "营养品", R.drawable.ic_wg_pill, scale, textScale)
        }
    }
}

/** 统计列：标签（贴顶）→ 当日累计大数字 → 弹性空隙 → 最近一条记录（贴底）；图标/间距用 scale，文字用 textScale */
@Composable
private fun RowScope.StatCol(
    modifier: GlanceModifier,
    scale: Float,
    textScale: Float,
    iconRes: Int,
    label: String,
    cell: StatCell
) {
    Column(
        modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.Top
    ) {
        Spacer(GlanceModifier.height(scaled(4f, scale)))
        // 标签行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(scaled(21f, scale))
            )
            Spacer(GlanceModifier.width(scaled(6f, scale)))
            Text(
                label,
                style = TextStyle(color = Sub, fontSize = textScaledSp(15f, textScale), fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        Spacer(GlanceModifier.height(scaled(3f, scale)))
        // 当日累计
        Text(
            cell.value,
            style = TextStyle(color = Ink, fontSize = scaledSp(18f, textScale), fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        // 弹性空隙：数字与最近记录之间
        Spacer(GlanceModifier.defaultWeight())
        // 最近一条：时间 · 描述
        Text(
            if (cell.recentTime.isBlank()) "暂无记录"
            else "${cell.recentTime} · ${cell.recentDesc}",
            style = TextStyle(color = Accent, fontSize = textScaledSp(11.5f, textScale), fontWeight = FontWeight.Medium),
            maxLines = 1
        )
        // 相对时间
        if (cell.recentAgo.isNotBlank()) {
            Text(cell.recentAgo, style = TextStyle(color = Tip, fontSize = scaledSp(9.5f, textScale)), maxLines = 1)
        }
    }
}

/** 快捷按钮：图标在上、文字在下，随 scale 等比放大 */
@Composable
private fun RowScope.TrayButton(
    context: Context,
    key: String,
    label: String,
    iconRes: Int,
    scale: Float,
    textScale: Float
) {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("open_sheet", key)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .fillMaxHeight()
            .clickable(actionStartActivity(intent)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(scaled(42f, scale))
                .cornerRadius(scaled(21f, scale))
                .background(ColorProvider(Color.White, Color(0xFF4A3B42))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = label,
                modifier = GlanceModifier.size(scaled(23f, scale))
            )
        }
        Spacer(GlanceModifier.height(scaled(3f, scale)))
        Text(
            label,
            style = TextStyle(color = Sub, fontSize = textScaledSp(12f, textScale), fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}
