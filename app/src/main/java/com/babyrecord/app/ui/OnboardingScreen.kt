package com.babyrecord.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.FieldWarm
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.OnCoralContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onDone: (String, String, LocalDate) -> Unit,
    onJoinFamily: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("secret") }
    var birthday by remember { mutableStateOf(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    // 背景/卡片/文字全部跟主题：夜间用暗色卡片与亮色文字，避免浅色硬编码在暗色下刺眼难读
    val bgTop = if (isSystemInDarkTheme()) Color(0xFF241D18) else Color(0xFFFFF3EC)
    val bgBottom = if (isSystemInDarkTheme()) Color(0xFF2E2119) else CoralContainer
    val cardBg = if (isSystemInDarkTheme()) Color(0xFF332A24) else Color.White
    val fieldBg = if (isSystemInDarkTheme()) Color(0xFF3D332C) else FieldWarm
    val inkStrong = if (isSystemInDarkTheme()) Color(0xFFF2E7DC) else Ink
    val inkSoft = if (isSystemInDarkTheme()) Color(0xFFC9B8AA) else InkSoft
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgTop, bgBottom)))) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            Box(
                Modifier.size(92.dp).clip(CircleShape).background(cardBg.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) { Text("👶", fontSize = 44.sp) }
            Spacer(Modifier.height(20.dp))
            Text("欢迎使用宝宝记录", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = inkStrong)
            Spacer(Modifier.height(8.dp))
            Text("从第一口奶开始，陪伴宝宝的每一天", fontSize = 14.sp, color = inkSoft)
            Spacer(Modifier.height(30.dp))

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).padding(24.dp)
            ) {
                Text("宝宝昵称", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = inkSoft)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：小糯米", fontSize = 14.sp, color = inkSoft) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = fieldBg,
                        focusedContainerColor = fieldBg,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Coral
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(20.dp))
                Text("宝宝是", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = inkSoft)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GenderCard("👦", "男宝", gender == "boy", Modifier.weight(1f)) { gender = "boy" }
                    GenderCard("👧", "女宝", gender == "girl", Modifier.weight(1f)) { gender = "girl" }
                    GenderCard("🌟", "保密", gender == "secret", Modifier.weight(1f)) { gender = "secret" }
                }

                Spacer(Modifier.height(20.dp))
                Text("宝宝生日", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkSoft)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(fieldBg)
                        .clickable { showPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${birthday.year}年${birthday.monthValue}月${birthday.dayOfMonth}日",
                        fontSize = 15.sp, color = inkStrong, modifier = Modifier.weight(1f)
                    )
                    Text("修改", fontSize = 13.sp, color = Coral, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { onDone(name.trim(), gender, birthday) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral,
                        disabledContainerColor = CoralContainer
                    )
                ) {
                    Text("开始记录 ✨", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
            // 新设备入口：已在家人的设备上建过档？直接加入家庭同步全部数据，无需重复建档
            TextButton(
                onClick = onJoinFamily,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    "家人已建过宝宝档案？加入家庭同步 ›",
                    fontSize = 13.sp,
                    color = if (isSystemInDarkTheme()) Color(0xFFD9A7A0) else Coral,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        val zone = ZoneId.systemDefault()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        birthday = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    }
                    showPicker = false
                }) { Text("确定", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消", color = InkSoft) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun GenderCard(
    emoji: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) CoralContainer else if (isSystemInDarkTheme()) Color(0xFF3D332C) else FieldWarm)
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
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OnCoralContainer else if (isSystemInDarkTheme()) Color(0xFFC9B8AA) else InkSoft
        )
    }
}
