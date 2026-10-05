package com.babyrecord.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.media.RepeatModeUi
import com.babyrecord.app.media.SleepMediaViewModel
import com.babyrecord.app.ui.theme.EqualizerBars
import com.babyrecord.app.ui.theme.Lilac
import com.babyrecord.app.ui.theme.LilacContainer
import com.babyrecord.app.ui.theme.OnLilacContainer
import com.babyrecord.app.ui.theme.PulseHalo
import com.babyrecord.app.ui.theme.frostGlass

/**
 * 首页迷你哄睡播放卡片：与哄睡播放页 PlayerCard 同款视觉。
 * - 封面图标（🌙/📖）+ 曲名 + 进度条 + 循环模式
 * - 标题下拉切换音乐/故事，右上角设置
 * - 播放键脉冲光晕 + 按压弹性 + 均衡器条动效
 */
@Composable
fun MusicPlayerCard(
    vm: SleepMediaViewModel,
    onOpenSettings: () -> Unit
) {
    val isPlaying by vm.isPlaying.collectAsState()
    val title by vm.currentTitle.collectAsState()
    val position by vm.position.collectAsState()
    val duration by vm.duration.collectAsState()
    val queueTab by vm.queueTab.collectAsState()
    val queueCount by vm.queueCount.collectAsState()
    val currentIndex by vm.currentIndex.collectAsState()
    val repeat by vm.repeatUi.collectAsState()
    val configSaved by vm.configSaved.collectAsState()
    val tab by vm.activeTab.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    val showLive = queueTab != null && queueTab == tab
    val label = if (tab == 0) "哄睡音乐" else "哄睡故事"
    val subtitle = if (showLive && queueCount > 0) {
        "$label · 第 ${currentIndex + 1} 首 / 共 $queueCount 首"
    } else if (!configSaved) {
        "未配置 NAS，点右上角设置"
    } else {
        "从哄睡播放页选择曲目"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .frostGlass(
                tint = Color.Transparent,
                highlight = Lilac.copy(alpha = 0.25f)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 封面图标：与播放页一致
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(
                        if (tab == 0) Brush.linearGradient(listOf(Lilac, Color(0xFF8E7FD4)))
                        else Brush.linearGradient(listOf(Color(0xFFE8735A), Color(0xFFF2A07B)))
                    )
            ) {
                if (tab == 0) {
                    Text("🌙", fontSize = 24.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                    Text("🌟", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp))
                } else {
                    Text("📖", fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // 曲目名：切换时滑动动画
                AnimatedContent(
                    targetState = if (showLive && title.isNotBlank()) title else "还没有播放",
                    transitionSpec = {
                        (slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 } + fadeOut())
                    },
                    label = "trackTitle"
                ) { text ->
                    Text(
                        text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                // 循环模式胶囊
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(LilacContainer)
                        .clickable { vm.cycleRepeat() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("🔁 ${repeat.label}", fontSize = 10.sp, color = OnLilacContainer, fontWeight = FontWeight.Bold)
                }
            }
            // 均衡器 + 设置
            if (isPlaying) {
                EqualizerBars(playing = true, color = Lilac)
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = Lilac, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        // 进度条
        val dur = (if (showLive && duration > 0) duration else 1L).toFloat()
        var dragging by remember { mutableStateOf<Float?>(null) }
        Slider(
            value = dragging ?: (if (showLive) position.toFloat() / dur else 0f).coerceIn(0f, 1f),
            onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let { vm.seekToFraction(it) }; dragging = null },
            colors = SliderDefaults.colors(
                thumbColor = Lilac, activeTrackColor = Lilac,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row {
            Text(fmt(position), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(fmt(duration), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(2.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 循环模式
            IconButton(onClick = { vm.cycleRepeat() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (repeat == RepeatModeUi.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = repeat.label,
                    tint = if (repeat == RepeatModeUi.OFF) MaterialTheme.colorScheme.onSurfaceVariant else Lilac,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = { vm.prev() }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(10.dp))
            // 播放大按钮：与播放页同款 Lilac 圆钮 + 脉冲光晕 + 按压弹性
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "playScale"
            )
            Box(
                Modifier
                    .size(58.dp)
                    .scale(btnScale)
                    .clip(CircleShape)
                    .background(Lilac)
                    .clickable(interactionSource = interaction, indication = null) { vm.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                PulseHalo(playing = isPlaying, color = Lilac, maxScale = 1.55f)
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White, modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = { vm.next() }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(6.dp))
            // 音乐/故事下拉切换
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "切换", tint = Lilac, modifier = Modifier.size(24.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    listOf("🎵 哄睡音乐" to 0, "📖 哄睡故事" to 1).forEach { (label, idx) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 14.sp) },
                            onClick = {
                                vm.selectTab(idx)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun fmt(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
