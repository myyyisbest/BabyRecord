package com.babyrecord.app.media

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.Lilac
import com.babyrecord.app.ui.theme.LilacContainer
import com.babyrecord.app.ui.theme.OnLilacContainer
import java.util.Locale

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.cs() = collectAsState()

@Composable
fun SleepMediaScreen(vm: SleepMediaViewModel, onBack: () -> Unit) {
    var showConfig by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf<String?>(null) }
    var sourceExpanded by remember { mutableStateOf(false) }
    val configSaved by vm.configSaved.cs()
    val musicDir by vm.musicDir.cs()
    val storyDir by vm.storyDir.cs()
    val scanning by vm.scanning.cs()
    val playerError by vm.playerError.cs()
    val tab by vm.activeTab.cs()
    val tracks = if (tab == 0) vm.tracksMusic.cs().value else vm.tracksStory.cs().value

    LaunchedEffect(Unit) { if (configSaved) vm.rescan() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("哄睡播放", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = { showConfig = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (!configSaved) {
            ConnectFirstCard { showConfig = true }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    SourceCard(
                        vm, musicDir, storyDir, scanning, sourceExpanded,
                        onToggleExpand = { sourceExpanded = !sourceExpanded },
                        onRescan = { vm.rescan() },
                        onOpenSettings = { showConfig = true },
                        onPickMusic = { pickTarget = "music" },
                        onPickStory = { pickTarget = "story" }
                    )
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    SegmentedTabs(vm)
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    PlayerCard(vm, tab, onShowPlaylist = { showPlaylist = true })
                }
                item {
                    Spacer(Modifier.height(14.dp))
                    if (playerError != null) {
                        ErrorBanner(playerError ?: "")
                        Spacer(Modifier.height(10.dp))
                    }
                    ListHeader(tab, tracks.size)
                }
                when {
                    scanning -> item {
                        Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Lilac, modifier = Modifier.size(26.dp))
                        }
                    }
                    tracks.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "这个文件夹里还没有音频\n点右上角设置重新选择文件夹",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center, lineHeight = 20.sp
                            )
                        }
                    }
                    else -> itemsIndexed(tracks) { i, t ->
                        val queueTab by vm.queueTab.cs()
                        val currentIndex by vm.currentIndex.cs()
                        val isPlaying by vm.isPlaying.cs()
                        val playingThis = queueTab == tab && currentIndex == i && isPlaying
                        Spacer(Modifier.height(8.dp))
                        TrackRow(i, t.title, playingThis) { vm.playTrack(i) }
                    }
                }
                item { Spacer(Modifier.height(30.dp)) }
            }
        }
    }

    if (showConfig) {
        ConfigDialog(
            vm,
            onPickMusic = { pickTarget = "music" },
            onPickStory = { pickTarget = "story" },
            onDismiss = { showConfig = false }
        )
    }

    if (showPlaylist) {
        PlaylistSheet(vm) { showPlaylist = false }
    }

    pickTarget?.let { target ->
        FolderPickerDialog(
            vm = vm,
            title = if (target == "music") "选择哄睡音乐文件夹" else "选择睡前故事文件夹",
            onPick = { path ->
                if (target == "music") vm.setMusicDir(path) else vm.setStoryDir(path)
                pickTarget = null
            },
            onDismiss = { pickTarget = null }
        )
    }
}

// ── NAS 来源卡（默认折叠为一行） ──
@Composable
private fun SourceCard(
    vm: SleepMediaViewModel,
    musicDir: String,
    storyDir: String,
    scanning: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onRescan: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickMusic: () -> Unit,
    onPickStory: () -> Unit
) {
    val musicTracks by vm.tracksMusic.cs()
    val storyTracks by vm.tracksStory.cs()
    val cfgUrl by vm.cfgUrl.cs()
    val host = cfgUrl.removePrefix("http://").removePrefix("https://").substringBefore('/')
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
            Spacer(Modifier.width(7.dp))
            Text("NAS 来源 · 已连接", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(
                if (scanning) "扫描中…" else "重新扫描",
                fontSize = 12.sp, color = Coral, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !scanning, onClick = onRescan).padding(horizontal = 4.dp)
            )
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            SourceRow("🎵", "哄睡音乐", "webdav://$host/${musicDir.trim('/')}", "${musicTracks.size} 首", onClick = onPickMusic)
            Spacer(Modifier.height(10.dp))
            SourceRow("📖", "睡前故事", "webdav://$host/${storyDir.trim('/')}", "${storyTracks.size} 个", onClick = onPickStory)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "点来源行可重新指定文件夹",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
                )
                Text(
                    "WebDAV 设置", fontSize = 12.sp, color = Coral, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpenSettings).padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceRow(emoji: String, label: String, path: String, count: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 15.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(path, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(count, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── 页签 ──
@Composable
private fun SegmentedTabs(vm: SleepMediaViewModel) {
    val tab by vm.activeTab.cs()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)
    ) {
        listOf("哄睡音乐", "睡前故事").forEachIndexed { i, label ->
            val selected = tab == i
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { vm.selectTab(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── 播放器卡（Spotify 风格沉浸式）：深色渐变底 + 大封面 + 粗曲名 + 细进度条 ──
@Composable
private fun PlayerCard(vm: SleepMediaViewModel, tab: Int, onShowPlaylist: () -> Unit) {
    val isPlaying by vm.isPlaying.cs()
    val currentTitle by vm.currentTitle.cs()
    val position by vm.position.cs()
    val duration by vm.duration.cs()
    val queueTab by vm.queueTab.cs()
    val queueCount by vm.queueCount.cs()
    val currentIndex by vm.currentIndex.cs()
    val repeat by vm.repeatUi.cs()
    val playerError by vm.playerError.cs()

    val showLive = queueTab != null && queueTab == tab
    val label = vm.labelForTab(tab)
    val subtitle = if (showLive && queueCount > 0) {
        "$label · 第 ${currentIndex + 1} 首 / 共 $queueCount 首"
    } else {
        "从下方清单选择播放"
    }

    // 深色沉浸渐变（Spotify 风）：紫色/珊瑚各自的主题深色底
    val bgBrush = if (tab == 0) {
        Brush.verticalGradient(listOf(Color(0xFF322B52), Color(0xFF221D3B), Color(0xFF1A1729)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF4A2C24), Color(0xFF33211C), Color(0xFF241813)))
    }
    val accent = if (tab == 0) Lilac else Coral
    val accentBright = if (tab == 0) Color(0xFFB9AEF0) else Color(0xFFF2A07B)

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(28.dp), spotColor = Color(0x408B7EC8), ambientColor = Color(0x2E8B7EC8))
            .clip(RoundedCornerShape(28.dp))
            .background(bgBrush)
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        // 大封面：播放中缓慢呼吸放大
        val coverScale by animateFloatAsState(
            targetValue = if (isPlaying) 1.03f else 1f,
            animationSpec = tween(900),
            label = "coverScale"
        )
        Box(
            Modifier
                .size(96.dp)
                .scale(coverScale)
                .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (tab == 0) Brush.linearGradient(listOf(Color(0xFF9C8FD8), Color(0xFF6F5FB5)))
                    else Brush.linearGradient(listOf(Color(0xFFE8735A), Color(0xFFB8543C)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (tab == 0) {
                Text("🌙", fontSize = 42.sp, modifier = Modifier.align(Alignment.Center).padding(end = 6.dp))
                Text("🌟", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp))
            } else {
                Text("📖", fontSize = 44.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        // 曲名：大号粗体（Spotify 风）+ 副标题
        Text(
            if (showLive && currentTitle.isNotBlank()) currentTitle else "还没有播放",
            fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.62f), maxLines = 1)

        Spacer(Modifier.height(16.dp))
        // 细腻进度条：细轨道 + 小圆点
        val dur = (if (showLive && duration > 0) duration else 1L).toFloat()
        var dragging by remember { mutableStateOf<Float?>(null) }
        val progress = dragging ?: (if (showLive) position.toFloat() / dur else 0f).coerceIn(0f, 1f)
        var trackWidth by remember { mutableStateOf(0f) }
        Column(Modifier.fillMaxWidth()) {
            fun seekFraction(f: Float) {
                if (showLive && duration > 0) vm.seekToFraction(f.coerceIn(0f, 1f))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp) // 增大触摸区，好拖
                    .onGloballyPositioned { trackWidth = it.size.width.toFloat() }
                    .pointerInput(showLive, duration, trackWidth) {
                        if (trackWidth <= 0f) return@pointerInput
                        // 拖动快进快退：拖动中实时预览，松手 seek
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragging = (offset.x / trackWidth).coerceIn(0f, 1f)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragging = (change.position.x / trackWidth).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                dragging?.let { seekFraction(it) }
                                dragging = null
                            },
                            onDragCancel = { dragging = null }
                        )
                    }
                    .pointerInput(showLive, duration, trackWidth) {
                        if (trackWidth <= 0f) return@pointerInput
                        // 点按轨道任意位置直接跳转
                        detectTapGestures { offset ->
                            seekFraction(offset.x / trackWidth)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // 背景轨道
                Box(
                    Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                )
                // 播放进度
                Box(
                    Modifier.fillMaxWidth(progress).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentBright)
                )
                // 拖动圆点
                Box(
                    Modifier
                        .padding(start = with(LocalDensity.current) {
                            ((progress * trackWidth).toDp() - 4.dp).coerceAtLeast(0.dp)
                        })
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .align(Alignment.CenterStart)
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Text(fmt(if (showLive) position else 0), fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
                Spacer(Modifier.weight(1f))
                Text(fmt(if (showLive) duration else 0), fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
            }
        }

        Spacer(Modifier.height(6.dp))
        // 控制排：Spotify 布局——循环/上一切/大播放/下一首/清单
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.cycleRepeat() }, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (repeat == RepeatModeUi.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = repeat.label,
                    tint = if (repeat == RepeatModeUi.OFF) Color.White.copy(alpha = 0.45f) else accentBright,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { vm.prev() }, modifier = Modifier.size(52.dp)) {
                Icon(
                    Icons.Rounded.SkipPrevious, contentDescription = "上一首",
                    tint = Color.White, modifier = Modifier.size(40.dp)
                )
            }
            // 播放大按钮：白色圆钮黑图标（Spotify 标志性）+ 按压弹性 + 脉冲光晕
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playScale"
            )
            Box(
                Modifier
                    .size(68.dp)
                    .scale(btnScale)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(interactionSource = interaction, indication = null) { vm.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color(0xFF1A1729),
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = { vm.next() }, modifier = Modifier.size(52.dp)) {
                Icon(
                    Icons.Rounded.SkipNext, contentDescription = "下一首",
                    tint = Color.White, modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onShowPlaylist, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "播放清单",
                    tint = accentBright, modifier = Modifier.size(26.dp)
                )
            }
        }

        if (showLive && playerError != null) {
            Spacer(Modifier.height(8.dp))
            Text("播放出错：$playerError", fontSize = 11.sp, color = Color(0xFFFF8A80), maxLines = 2)
        }
    }
}

// ── 播放清单弹层 ──
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSheet(vm: SleepMediaViewModel, onDismiss: () -> Unit) {
    val tab by vm.activeTab.cs()
    val tracks = if (tab == 0) vm.tracksMusic.cs().value else vm.tracksStory.cs().value
    val current by vm.currentIndex.cs()
    val isPlaying by vm.isPlaying.cs()
    val queueTab by vm.queueTab.cs()
    val label = vm.labelForTab(tab)
    val listLive = queueTab == tab

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$label · 共 ${tracks.size} ${if (tab == 0) "首" else "个"}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(420.dp)
            ) {
                itemsIndexed(tracks) { i, t ->
                    val playingThis = listLive && i == current && isPlaying
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (playingThis) LilacContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { vm.playTrack(i) }
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (playingThis) "♪" else "${i + 1}",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (playingThis) OnLilacContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(26.dp)
                        )
                        Text(
                            t.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = if (playingThis) OnLilacContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ── 列表标题行 ──
@Composable
private fun ListHeader(tab: Int, count: Int) {
    val label = if (tab == 0) "哄睡音乐" else "睡前故事"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label 清单", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Text("共 $count ${if (tab == 0) "首" else "个"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun TrackRow(index: Int, title: String, playingThis: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (playingThis) LilacContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (playingThis) Lilac else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (playingThis) "♪" else "${index + 1}",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (playingThis) OnLilacContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = if (playingThis) OnLilacContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFDEEEE))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "播放出错：$message（检查手机与 NAS 是否在同一网络）",
            fontSize = 11.sp, color = Color(0xFFD64545), lineHeight = 16.sp, maxLines = 3
        )
    }
}

// ── 连接引导 ──
@Composable
private fun ConnectFirstCard(onOpenConfig: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) { Text("🎵", fontSize = 44.sp) }
        Spacer(Modifier.height(18.dp))
        Text("连接你的 NAS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            "填一次 WebDAV 信息，故事和音乐\n直接从家里播放",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onOpenConfig,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral)
        ) { Text("＋ 填写连接信息", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

// ── 连接设置对话框 ──
@Composable
private fun ConfigDialog(
    vm: SleepMediaViewModel,
    onPickMusic: () -> Unit,
    onPickStory: () -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(vm.cfgUrl.value) }
    var user by remember { mutableStateOf(vm.cfgUser.value) }
    var pass by remember { mutableStateOf(vm.cfgPass.value) }
    var showPass by remember { mutableStateOf(false) }
    val musicDir by vm.musicDir.cs()
    val storyDir by vm.storyDir.cs()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebDAV 设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("WebDAV 地址") },
                    placeholder = { Text("http://你的NAS地址:端口", fontSize = 13.sp, color = InkSoft) },
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                OutlinedTextField(
                    value = user, onValueChange = { user = it },
                    label = { Text("账号") },
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it },
                    label = { Text("密码") },
                    placeholder = { Text("密码", fontSize = 13.sp, color = InkSoft) },
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPass) "隐藏密码" else "显示密码",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
                DirPickRow("🎵 哄睡音乐文件夹", musicDir, onClick = onPickMusic)
                DirPickRow("📖 睡前故事文件夹", storyDir, onClick = onPickStory)
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.saveConnection(url, user, pass); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        }
    )
}

@Composable
private fun DirPickRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            if (value.isBlank()) "点按选择文件夹" else value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (value.isBlank()) InkSoft else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

// ── 文件夹选择器 ──
@Composable
private fun FolderPickerDialog(
    vm: SleepMediaViewModel,
    title: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) { vm.openPicker() }
    val path by vm.pickerPath.cs()
    val entries by vm.pickerEntries.cs()
    val loading by vm.pickerLoading.cs()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    if (path.isBlank()) "当前：WebDAV 根目录" else "当前：/$path",
                    fontSize = 12.sp, color = InkSoft,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (path.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { vm.pickerUp() }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("上一级", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Lilac, modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyColumn(Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        itemsIndexed(entries.filter { it.isFolder }) { _, e ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.pickerDive(e.name) }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📁", fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(e.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("点「使用这个文件夹」完成设置", fontSize = 11.sp, color = InkSoft)
            }
        },
        confirmButton = {
            Button(
                onClick = { onPick(path) },
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) { Text("使用这个文件夹", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        }
    )
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "${totalSec / 60}:${String.format(Locale.US, "%02d", totalSec % 60)}"
}
