package com.babyrecord.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.data.DiaperEntity
import com.babyrecord.app.data.DiaperState
import com.babyrecord.app.data.FeedType
import com.babyrecord.app.data.FeedingEntity
import com.babyrecord.app.data.SleepEntity
import com.babyrecord.app.data.SolidFoodEntity
import com.babyrecord.app.data.SupplementEntity
import com.babyrecord.app.ui.DateTimeField
import com.babyrecord.app.ui.isSleepOngoing
import com.babyrecord.app.ui.toLocalDateTime
import com.babyrecord.app.ui.toMillis
import com.babyrecord.app.ui.sleepDurationText
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.OnCoralContainer
import java.time.LocalDateTime

private val WarnRed = Color(0xFFD64545)
private val WarnRedContainer = Color(0xFFFDEEEE)

@Composable
fun RecordDetailScreen(
    state: DetailState,
    onBack: () -> Unit,
    onSaveFeeding: (FeedingEntity) -> Unit,
    onSaveDiaper: (DiaperEntity) -> Unit,
    onSaveSleep: (SleepEntity) -> Unit,
    onSaveSolid: (SolidFoodEntity) -> Unit,
    onSaveSupplement: (SupplementEntity) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                when (state) {
                    is DetailState.Feeding -> "喂养记录"
                    is DetailState.Diaper -> "换尿布记录"
                    is DetailState.Sleep -> "睡眠记录"
                    is DetailState.Supplement -> "营养品记录"
                    is DetailState.Solid -> "辅食记录"
                    DetailState.Loading -> "记录详情"
                },
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
            )
        }

        when (state) {
            DetailState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Coral)
            }
            is DetailState.Feeding -> FeedingBody(Modifier.weight(1f), state.data, onBack, onSaveFeeding, onDelete)
            is DetailState.Diaper -> DiaperBody(Modifier.weight(1f), state.data, onBack, onSaveDiaper, onDelete)
            is DetailState.Sleep -> SleepBody(Modifier.weight(1f), state.data, onBack, onSaveSleep, onDelete)
            is DetailState.Solid -> SolidBody(Modifier.weight(1f), state.data, onBack, onSaveSolid, onDelete)
            is DetailState.Supplement -> SupplementBody(Modifier.weight(1f), state.data, onBack, onSaveSupplement, onDelete)
        }
    }
}

@Composable
private fun FeedingBody(modifier: Modifier = Modifier, data: FeedingEntity, onBack: () -> Unit, onSave: (FeedingEntity) -> Unit, onDelete: () -> Unit) {
    var type by remember(data) { mutableStateOf(FeedType.valueOf(data.type)) }
    var amount by remember(data) { mutableIntStateOf(data.amountMl ?: 120) }
    var note by remember(data) { mutableStateOf(data.note) }
    var time by remember(data) { mutableStateOf(toLocalDateTime(data.timeEpochMillis)) }
    var confirmDelete by remember { mutableStateOf(false) }

    DetailScroll(modifier) {
        TypeCardsRow(
            listOf(
                Triple("🥛", "配方奶", FeedType.FORMULA),
                Triple("🍼", "母乳瓶喂", FeedType.BOTTLE_MILK),
                Triple("🤱", "直接哺乳", FeedType.BREAST)
            ),
            type.name
        ) { type = it as FeedType }

        if (type != FeedType.BREAST) {
            Spacer(Modifier.height(20.dp))
            Text("奶量（ml）", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(Icons.Rounded.Remove) { if (amount > 10) amount -= 10 }
                Text("$amount", Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                StepButton(Icons.Rounded.Add) { if (amount < 500) amount += 10 }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 150, 180).forEach { v ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (amount == v) CoralContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { amount = v }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${v}ml", fontSize = 12.sp, color = if (amount == v) OnCoralContainer else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        DateTimeField(label = "记录时间", value = time, onChange = { time = it })

        Spacer(Modifier.height(16.dp))
        NoteField(note) { note = it }

        SaveDeleteButtons(
            onSave = {
                onSave(data.copy(type = type.name, amountMl = if (type == FeedType.BREAST) null else amount, note = note, timeEpochMillis = toMillis(time)))
                onBack()
            },
            onDelete = { confirmDelete = true }
        )
    }

    if (confirmDelete) DeleteDialog(onDismiss = { confirmDelete = false }, onDelete = { onDelete(); onBack() })
}

@Composable
private fun DiaperBody(modifier: Modifier = Modifier, data: DiaperEntity, onBack: () -> Unit, onSave: (DiaperEntity) -> Unit, onDelete: () -> Unit) {
    var state by remember(data) { mutableStateOf(DiaperState.valueOf(data.state)) }
    var note by remember(data) { mutableStateOf(data.note) }
    var time by remember(data) { mutableStateOf(toLocalDateTime(data.timeEpochMillis)) }
    var confirmDelete by remember { mutableStateOf(false) }

    DetailScroll(modifier) {
        TypeCardsRow(
            listOf(
                Triple("💧", "嘘嘘", DiaperState.PEE),
                Triple("💩", "便便", DiaperState.POOP),
                Triple("💦", "都有", DiaperState.BOTH)
            ),
            state.name
        ) { state = it as DiaperState }

        Spacer(Modifier.height(20.dp))
        DateTimeField(label = "记录时间", value = time, onChange = { time = it })

        Spacer(Modifier.height(16.dp))
        NoteField(note) { note = it }

        SaveDeleteButtons(
            onSave = {
                onSave(data.copy(state = state.name, note = note, timeEpochMillis = toMillis(time)))
                onBack()
            },
            onDelete = { confirmDelete = true }
        )
    }

    if (confirmDelete) DeleteDialog(onDismiss = { confirmDelete = false }, onDelete = { onDelete(); onBack() })
}

@Composable
private fun SleepBody(modifier: Modifier = Modifier, data: SleepEntity, onBack: () -> Unit, onSave: (SleepEntity) -> Unit, onDelete: () -> Unit) {
    val ongoing = isSleepOngoing(data)
    var start by remember(data) { mutableStateOf(toLocalDateTime(data.startEpochMillis)) }
    var end by remember(data, ongoing) {
        mutableStateOf(if (ongoing) LocalDateTime.now() else toLocalDateTime(data.endEpochMillis))
    }
    var note by remember(data) { mutableStateOf(data.note) }
    var confirmDelete by remember { mutableStateOf(false) }
    val effectiveEnd = if (end.isAfter(start)) end else end.plusDays(1)
    val minutes = java.time.Duration.between(start, effectiveEnd).toMinutes().coerceAtLeast(0)

    DetailScroll(modifier) {
        if (ongoing) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE7DFF8)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("宝宝睡觉中 😴", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(3.dp))
                    Text("已睡 ${sleepDurationText(minutes)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { onSave(data.copy(endEpochMillis = System.currentTimeMillis())) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC53D),
                        contentColor = Color(0xFF4A3200)
                    )
                ) {
                    Text("宝宝醒了", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        DateTimeField(label = "入睡时间", value = start, onChange = { start = it })

        if (!ongoing) {
            Spacer(Modifier.height(12.dp))
            DateTimeField(label = "醒来时间", value = end, onChange = { end = it })
            Spacer(Modifier.height(10.dp))
            Text("时长：${sleepDurationText(minutes)}${if (!end.isAfter(start)) "（跨天）" else ""}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        NoteField(note) { note = it }

        SaveDeleteButtons(
            onSave = {
                onSave(
                    data.copy(
                        startEpochMillis = toMillis(start),
                        endEpochMillis = if (ongoing) 0L else toMillis(effectiveEnd),
                        note = note
                    )
                )
                onBack()
            },
            onDelete = { confirmDelete = true }
        )
    }

    if (confirmDelete) DeleteDialog(onDismiss = { confirmDelete = false }, onDelete = { onDelete(); onBack() })
}

@Composable
private fun SupplementBody(modifier: Modifier = Modifier, data: SupplementEntity, onBack: () -> Unit, onSave: (SupplementEntity) -> Unit, onDelete: () -> Unit) {
    var food by remember(data) { mutableStateOf(data.name) }
    var note by remember(data) { mutableStateOf(data.note) }
    var time by remember(data) { mutableStateOf(toLocalDateTime(data.timeEpochMillis)) }
    var confirmDelete by remember { mutableStateOf(false) }

    DetailScroll(modifier) {
        Text("名称", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = food,
            onValueChange = { food = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Coral
            ),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))
        DateTimeField(label = "记录时间", value = time, onChange = { time = it })

        Spacer(Modifier.height(16.dp))
        NoteField(note) { note = it }

        SaveDeleteButtons(
            onSave = {
                onSave(data.copy(name = food.trim(), note = note, timeEpochMillis = toMillis(time)))
                onBack()
            },
            onDelete = { confirmDelete = true }
        )
    }

    if (confirmDelete) DeleteDialog(onDismiss = { confirmDelete = false }, onDelete = { onDelete(); onBack() })
}

@Composable
private fun SolidBody(modifier: Modifier = Modifier, data: SolidFoodEntity, onBack: () -> Unit, onSave: (SolidFoodEntity) -> Unit, onDelete: () -> Unit) {
    var food by remember(data) { mutableStateOf(data.foodName) }
    var note by remember(data) { mutableStateOf(data.note) }
    var time by remember(data) { mutableStateOf(toLocalDateTime(data.timeEpochMillis)) }
    var confirmDelete by remember { mutableStateOf(false) }

    DetailScroll(modifier) {
        Text("吃了什么", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = food,
            onValueChange = { food = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Coral
            ),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))
        DateTimeField(label = "记录时间", value = time, onChange = { time = it })

        Spacer(Modifier.height(16.dp))
        NoteField(note) { note = it }

        SaveDeleteButtons(
            onSave = {
                onSave(data.copy(foodName = food.trim(), note = note, timeEpochMillis = toMillis(time)))
                onBack()
            },
            onDelete = { confirmDelete = true }
        )
    }

    if (confirmDelete) DeleteDialog(onDismiss = { confirmDelete = false }, onDelete = { onDelete(); onBack() })
}

@Composable
private fun DetailScroll(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TypeCardsRow(options: List<Triple<String, String, Enum<*>>>, selectedName: String, onSelect: (Enum<*>) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (emoji, label, value) ->
            val selected = value.name == selectedName
            Column(
                Modifier
                    .weight(1f)
                    .height(74.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) CoralContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) Coral else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(value) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(emoji, fontSize = 22.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    label, fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) OnCoralContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun NoteField(note: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = note,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("备注（可选）", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Coral
        ),
        singleLine = true
    )
}

@Composable
private fun SaveDeleteButtons(onSave: () -> Unit, onDelete: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral)
    ) {
        Text("保存修改", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = onDelete,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = WarnRedContainer, contentColor = WarnRed)
    ) {
        Text("删除记录", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeleteDialog(onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除这条记录？", fontWeight = FontWeight.Bold) },
        text = { Text("删除后无法恢复") },
        confirmButton = {
            TextButton(onClick = onDelete) { Text("删除", color = WarnRed, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}
