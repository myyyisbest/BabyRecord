package com.babyrecord.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.BabyChangingStation
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.babyrecord.app.BabyApp
import com.babyrecord.app.media.SleepMediaViewModel
import com.babyrecord.app.ui.RecordDisplay
import com.babyrecord.app.ui.RecordRow
import com.babyrecord.app.ui.formatTime
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralLight
import com.babyrecord.app.ui.theme.DividerWarm
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.theme.InkSoft
import java.io.File
import java.time.LocalDate

@Composable
fun HomeScreen(
    state: HomeUiState,
    onFeeding: () -> Unit,
    onDiaper: () -> Unit,
    onSleep: () -> Unit,
    onSolid: () -> Unit,
    onSupplement: () -> Unit,
    onMedicine: () -> Unit,
    onOpenRecord: (RecordDisplay) -> Unit,
    onOpenTimeline: () -> Unit,
    // 点击头像触发换头像（AppRoot 接系统图片选择器）
    onAvatarClick: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        val today = LocalDate.now()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("宝宝成长记", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(
                "${today.monthValue}月${today.dayOfMonth}日 · 周${"日一二三四五六"[today.dayOfWeek.value % 7]}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(18.dp))
        BabyHeaderCard(state, onAvatarClick)
        Spacer(Modifier.height(16.dp))
        TodayStatsCard(state.stats)
        Spacer(Modifier.height(22.dp))
        Text("快捷记录", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        var moreOpen by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionCard("喂奶", Icons.Rounded.WaterDrop, Color(0xFFFF9A7B), Color(0xFFF0705A), Modifier.weight(1f), onClick = onFeeding)
            QuickActionCard("尿布", Icons.Rounded.BabyChangingStation, Color(0xFF6FB1E8), Color(0xFF4F86C6), Modifier.weight(1f), onClick = onDiaper)
            QuickActionCard("睡眠", Icons.Rounded.Bedtime, Color(0xFF9C8FD8), Color(0xFF7A66C4), Modifier.weight(1f), onClick = onSleep)
            QuickActionCard("辅食", Icons.Rounded.Restaurant, Color(0xFF6FC491), Color(0xFF4A9F6E), Modifier.weight(1f), onClick = onSolid)
            // 第 5 位：紧凑“更多”开关，点击后下方展开额外选项
            MoreToggleCard(open = moreOpen, onClick = { moreOpen = !moreOpen })
        }
        // 展开行：与首行同样 5 等分，营养品/药品占前两格，与喂奶/尿布列位对齐
        AnimatedVisibility(
            visible = moreOpen,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard("营养品", Icons.Rounded.Medication, Color(0xFFF2B01E), Color(0xFFE09E1F), Modifier.weight(1f), onClick = onSupplement)
                QuickActionCard("药品", Icons.Rounded.Vaccines, Color(0xFFF48FB1), Color(0xFFD16BA5), Modifier.weight(1f), onClick = onMedicine)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(62.dp)) // 占位与首行 MoreToggleCard 同宽
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("最近记录", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        if (state.recent.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "今天还没有记录\n点上方快捷按钮开始吧",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 22.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.recent.take(3).forEach { r ->
                    RecordRow(record = r, onClick = { onOpenRecord(r) })
                }
            }
            if (state.recent.size > 3) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onOpenTimeline)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "查看全部 ${state.recent.size} 条记录 ›",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BabyHeaderCard(state: HomeUiState, onAvatarClick: () -> Unit) {
    // 头像文件路径：avatar 字段存文件名，复用照片通道存放在 filesDir/album；不存在回退首字头像
    // 先在组合内拿 context，再进 remember 块（remember 的 lambda 非组合上下文）
    val context = LocalContext.current
    val avatarFile = remember(state.avatar) {
        if (state.avatar.isNotBlank()) {
            File(File(context.filesDir, "album"), state.avatar).takeIf { it.exists() }
        } else null
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Coral, CoralLight)))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 仅头像区域可点：有头像文件 → AsyncImage 圆形裁切；无 → 保持首字样式不变
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            if (avatarFile != null) {
                AsyncImage(
                    model = avatarFile,
                    contentDescription = "宝宝头像",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(state.babyName.take(1), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.babyName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (state.gender != "secret") {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.gender == "boy") "男宝" else "女宝",
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("出生 ${state.babyDayAge} 天啦", fontSize = 13.sp, color = Color.White.copy(alpha = 0.92f))
        }
        Text("🧸", fontSize = 28.sp)
    }
}

@Composable
private fun TodayStatsCard(stats: TodayStats) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(Modifier.weight(1f), "${stats.feedCount}", "喂奶(次)")
        StatDivider()
        StatCell(Modifier.weight(1f), if (stats.milkTotalMl > 0) "${stats.milkTotalMl}" else "—", "今日奶量(ml)")
        StatDivider()
        StatCell(Modifier.weight(1f), stats.lastFeedingLabel, "距上次喂奶")
    }
}

@Composable
private fun StatCell(modifier: Modifier = Modifier, value: String, label: String) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = if (value.length >= 5) 15.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).height(34.dp).background(DividerWarm))
}

/** 窄条“更多”开关卡：展开时箭头旋转 180°并高亮（文字与图标分离，文字在卡片下方） */
@Composable
private fun MoreToggleCard(open: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "moreArrow"
    )
    Column(
        Modifier.width(62.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // 与主卡同高，保持方形高度
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (open) Brush.verticalGradient(listOf(Color(0xFFB5ADA3), Color(0xFF8A7568)))
                    else Brush.verticalGradient(listOf(Color(0xFFE8E2DB), Color(0xFFC9BFB4)))
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (open) "收起" else "更多",
                tint = Color.White,
                modifier = Modifier.size(20.dp).rotate(rotation)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "更多",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // 图标在渐变卡片内，文字移到卡片下方（与图标分离）
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // 宽 = 高，正方形卡片
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(startColor, endColor)))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // 去除底衬，图标直接以扁平圆角样式居中展示
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.fillMaxWidth(0.65f) // 图标占卡片 65% 宽
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
