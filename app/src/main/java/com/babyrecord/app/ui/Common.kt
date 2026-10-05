package com.babyrecord.app.ui

import androidx.compose.ui.graphics.Color
import com.babyrecord.app.data.DiaperEntity
import com.babyrecord.app.data.DiaperState
import com.babyrecord.app.data.FeedType
import com.babyrecord.app.data.FeedingEntity
import com.babyrecord.app.data.SleepEntity
import com.babyrecord.app.data.SolidFoodEntity
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.LilacContainer
import com.babyrecord.app.ui.theme.MintContainer
import com.babyrecord.app.ui.theme.SkyBlueContainer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class RecordType { FEEDING, DIAPER, SLEEP, SOLID, SUPPLEMENT }

data class RecordDisplay(
    val type: RecordType,
    val time: Long,
    val emoji: String,
    val containerColor: Color,
    val title: String,
    val subtitle: String,
    val feeding: FeedingEntity? = null,
    val diaper: DiaperEntity? = null,
    val sleep: SleepEntity? = null,
    val solid: SolidFoodEntity? = null,
    val supplement: com.babyrecord.app.data.SupplementEntity? = null
)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormatter)

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

fun formatDateTime(value: LocalDateTime): String = value.format(dateTimeFormatter)

fun toMillis(value: LocalDateTime): Long =
    value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun toLocalDateTime(millis: Long): LocalDateTime =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()

/** endEpochMillis == 0 表示宝宝还在睡觉中 */
fun isSleepOngoing(sleep: SleepEntity): Boolean = sleep.endEpochMillis == 0L

fun recordRoute(record: RecordDisplay): String {
    val id = record.feeding?.id ?: record.diaper?.id ?: record.sleep?.id
        ?: record.solid?.id ?: record.supplement?.id ?: ""
    return "record/${record.type.name}/$id"
}

fun sleepDurationText(minutes: Long): String = when {
    minutes >= 60 -> "${minutes / 60}小时${if (minutes % 60 > 0) "${minutes % 60}分" else ""}"
    else -> "${minutes}分钟"
}

fun feedTitle(feeding: FeedingEntity): String = when (feeding.type) {
    FeedType.BREAST.name -> "直接哺乳"
    FeedType.BOTTLE_MILK.name -> "母乳瓶喂 · ${feeding.amountMl ?: 0}ml"
    else -> "配方奶 · ${feeding.amountMl ?: 0}ml"
}

private fun feedEmoji(type: String): String = when (type) {
    FeedType.BREAST.name -> "🤱"
    FeedType.BOTTLE_MILK.name -> "🍼"
    else -> "🥛"
}

private fun feedContainerColor(type: String): Color = when (type) {
    FeedType.BREAST.name -> CoralContainer
    FeedType.BOTTLE_MILK.name -> SkyBlueContainer
    else -> MintContainer
}

fun diaperTitle(diaper: DiaperEntity): String = "换尿布 · " + when (diaper.state) {
    DiaperState.PEE.name -> "嘘嘘"
    DiaperState.POOP.name -> "便便"
    else -> "嘘嘘+便便"
}

private fun diaperEmoji(state: String): String = when (state) {
    DiaperState.PEE.name -> "💧"
    DiaperState.POOP.name -> "💩"
    else -> "💦"
}

fun solidTitle(solid: SolidFoodEntity): String = "辅食 · ${solid.foodName}"

fun feedingDisplay(feeding: FeedingEntity): RecordDisplay = RecordDisplay(
    type = RecordType.FEEDING,
    time = feeding.timeEpochMillis,
    emoji = feedEmoji(feeding.type),
    containerColor = feedContainerColor(feeding.type),
    title = feedTitle(feeding),
    subtitle = feeding.note,
    feeding = feeding
)

fun diaperDisplay(diaper: DiaperEntity): RecordDisplay = RecordDisplay(
    type = RecordType.DIAPER,
    time = diaper.timeEpochMillis,
    emoji = diaperEmoji(diaper.state),
    containerColor = SkyBlueContainer,
    title = diaperTitle(diaper),
    subtitle = diaper.note,
    diaper = diaper
)

fun sleepDisplay(sleep: SleepEntity): RecordDisplay {
    val ongoing = isSleepOngoing(sleep)
    val endMillis = if (ongoing) System.currentTimeMillis() else sleep.endEpochMillis
    val minutes = ((endMillis - sleep.startEpochMillis) / 60000).coerceAtLeast(0)
    return RecordDisplay(
        type = RecordType.SLEEP,
        time = sleep.startEpochMillis,
        emoji = "😴",
        containerColor = LilacContainer,
        title = if (ongoing) "睡眠 · 已睡${sleepDurationText(minutes)}" else "睡眠 · ${sleepDurationText(minutes)}",
        subtitle = if (ongoing) "宝宝睡觉中 · ${formatTime(sleep.startEpochMillis)}入睡"
        else "${formatTime(sleep.startEpochMillis)} – ${formatTime(endMillis)}",
        sleep = sleep
    )
}

fun solidDisplay(solid: SolidFoodEntity): RecordDisplay = RecordDisplay(
    type = RecordType.SOLID,
    time = solid.timeEpochMillis,
    emoji = "🥣",
    containerColor = MintContainer,
    title = solidTitle(solid),
    subtitle = solid.note,
    solid = solid
)

fun supplementDisplay(supplement: com.babyrecord.app.data.SupplementEntity): RecordDisplay = RecordDisplay(
    type = RecordType.SUPPLEMENT,
    time = supplement.timeEpochMillis,
    emoji = "💊",
    containerColor = CoralContainer,
    title = "营养品 · ${supplement.name}",
    subtitle = supplement.note,
    supplement = supplement
)
