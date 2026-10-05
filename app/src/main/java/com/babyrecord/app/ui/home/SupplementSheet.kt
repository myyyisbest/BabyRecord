package com.babyrecord.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.DateTimeField
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.FieldWarm
import com.babyrecord.app.ui.theme.InkSoft
import java.time.LocalDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementSheet(
    visible: Boolean,
    title: String = "记营养品",
    nameHint: String = "如：维生素D3 / 钙铁锌",
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDateTime) -> Unit
) {
    if (!visible) return
    var name by remember { mutableStateOf("") }
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
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(20.dp))

            Text("名称", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(nameHint, fontSize = 14.sp, color = InkSoft) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FieldWarm,
                    focusedContainerColor = FieldWarm,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Coral
                ),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))
            DateTimeField(label = "记录时间", value = time, onChange = { time = it })

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选），如：滴剂 1 粒", fontSize = 14.sp, color = InkSoft) },
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
                onClick = { onSave(name.trim(), note.trim(), time) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, disabledContainerColor = CoralContainer)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
