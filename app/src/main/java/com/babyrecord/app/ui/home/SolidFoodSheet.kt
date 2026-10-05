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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.DateTimeField
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import java.time.LocalDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolidFoodSheet(visible: Boolean, onDismiss: () -> Unit, onSave: (String, String, LocalDateTime) -> Unit) {
    if (!visible) return
    var food by remember { mutableStateOf("") }
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
            Text("记辅食", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))

            Text("吃了什么", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = food,
                onValueChange = { food = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：高铁米粉 / 苹果泥", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选），如：爱吃，还要", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Coral
                ),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(food.trim(), note.trim(), time) },
                enabled = food.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, disabledContainerColor = CoralContainer)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
