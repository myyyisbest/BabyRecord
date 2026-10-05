package com.babyrecord.app.ui.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.data.MeasureType
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.OnCoralContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (MeasureType, Double, LocalDate) -> Unit
) {
    if (!visible) return
    var type by remember { mutableStateOf(MeasureType.WEIGHT) }
    var valueText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    val step = if (type == MeasureType.WEIGHT) 0.1 else 0.5
    val parsed = valueText.toDoubleOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text("记录测量", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeCard("⚖️", "体重", type == MeasureType.WEIGHT, Modifier.weight(1f)) { type = MeasureType.WEIGHT }
                TypeCard("📏", "身高", type == MeasureType.HEIGHT, Modifier.weight(1f)) { type = MeasureType.HEIGHT }
                TypeCard("🧢", "头围", type == MeasureType.HEAD, Modifier.weight(1f)) { type = MeasureType.HEAD }
            }

            Spacer(Modifier.height(12.dp))
            Text("数值（${if (type == MeasureType.WEIGHT) "kg" else "cm"}）", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(Icons.Rounded.Remove) {
                    val cur = parsed ?: defaultFor(type)
                    valueText = fmt(cur - step)
                }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    placeholder = { Text(defaultFor(type).toString(), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Coral
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )
                StepButton(Icons.Rounded.Add) {
                    val cur = parsed ?: defaultFor(type)
                    valueText = fmt(cur + step)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showPicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("测量日期", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("${date.year}年${date.monthValue}月${date.dayOfMonth}日", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(8.dp))
                Text("修改", fontSize = 13.sp, color = Coral, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { parsed?.let { onSave(type, it, date) } },
                enabled = parsed != null && parsed > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, disabledContainerColor = CoralContainer)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    }
                    showPicker = false
                }) { Text("确定", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        ) { DatePicker(state = pickerState) }
    }
}

private fun defaultFor(type: MeasureType): Double = when (type) {
    MeasureType.WEIGHT -> 7.0
    MeasureType.HEIGHT -> 65.0
    MeasureType.HEAD -> 42.0
}

@Composable
private fun TypeCard(emoji: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) CoralContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Coral else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick),
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

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp)) }
}
