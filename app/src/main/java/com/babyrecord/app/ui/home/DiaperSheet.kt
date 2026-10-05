package com.babyrecord.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.data.DiaperState
import com.babyrecord.app.ui.DateTimeField
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.OnCoralContainer
import java.time.LocalDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaperSheet(visible: Boolean, onDismiss: () -> Unit, onSave: (DiaperState, String, LocalDateTime) -> Unit) {
    if (!visible) return
    var state by remember { mutableStateOf(DiaperState.PEE) }
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
            Text("换尿布", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeCard("💧", "嘘嘘", state == DiaperState.PEE, Modifier.weight(1f)) { state = DiaperState.PEE }
                TypeCard("💩", "便便", state == DiaperState.POOP, Modifier.weight(1f)) { state = DiaperState.POOP }
                TypeCard("💦", "都有", state == DiaperState.BOTH, Modifier.weight(1f)) { state = DiaperState.BOTH }
            }

            Spacer(Modifier.height(12.dp))
            DateTimeField(label = "记录时间", value = time, onChange = { time = it })

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选），如：颜色正常", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Coral
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(state, note.trim(), time) },
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
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
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
