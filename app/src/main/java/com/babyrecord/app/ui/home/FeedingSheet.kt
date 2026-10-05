package com.babyrecord.app.ui.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.babyrecord.app.data.FeedType
import com.babyrecord.app.ui.DateTimeField
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.FieldWarm
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.OnCoralContainer
import java.time.LocalDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingSheet(visible: Boolean, onDismiss: () -> Unit, onSave: (FeedType, Int?, String, LocalDateTime) -> Unit) {
    if (!visible) return
    var type by remember { mutableStateOf(FeedType.FORMULA) }
    var amount by remember { mutableIntStateOf(120) }
    var note by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(LocalDateTime.now()) }

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
            Text("记一次喂奶", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeedTypeCard("🥛", "配方奶", type == FeedType.FORMULA, Modifier.weight(1f)) { type = FeedType.FORMULA }
                FeedTypeCard("🍼", "母乳瓶喂", type == FeedType.BOTTLE_MILK, Modifier.weight(1f)) { type = FeedType.BOTTLE_MILK }
                FeedTypeCard("🤱", "直接哺乳", type == FeedType.BREAST, Modifier.weight(1f)) { type = FeedType.BREAST }
            }

            if (type != FeedType.BREAST) {
                Spacer(Modifier.height(20.dp))
                Text("奶量（ml）", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton(Icons.Rounded.Remove) { if (amount > 10) amount -= 10 }
                    Text(
                        "$amount", Modifier.weight(1f), textAlign = TextAlign.Center,
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink
                    )
                    StepButton(Icons.Rounded.Add) { if (amount < 500) amount += 10 }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 90, 120, 150, 180).forEach { v ->
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (amount == v) CoralContainer else FieldWarm)
                                .clickable { amount = v }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${v}ml", fontSize = 12.sp,
                                color = if (amount == v) OnCoralContainer else InkSoft,
                                fontWeight = if (amount == v) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            DateTimeField(label = "记录时间", value = time, onChange = { time = it })

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选），如：左侧 15 分钟", fontSize = 14.sp, color = InkSoft) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FieldWarm,
                    focusedContainerColor = FieldWarm,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Coral
                ),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(type, if (type == FeedType.BREAST) null else amount, note.trim(), time) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FeedTypeCard(
    emoji: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) CoralContainer else FieldWarm)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Coral else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OnCoralContainer else InkSoft
        )
    }
}

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(FieldWarm).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp)) }
}
