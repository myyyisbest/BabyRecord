package com.babyrecord.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.theme.frostGlass
import kotlin.math.roundToInt

data class TabItem(val route: String, val label: String, val icon: ImageVector)

/**
 * 磨砂玻璃底部导航栏：
 * - 半透明底 + 顶部高光描边，透出页面内容形成毛玻璃层次
 * - 选中指示药丸带 spring 滑动过渡（在 Tab 间滑动而非瞬跳）
 * - 图标点选时弹性缩放
 * - 支持在导航栏上水平拖动切换页面：药丸跟手滑动，松手后落到最近 Tab
 */
@Composable
fun GlassNavBar(
    tabs: List<TabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    // 子页面（设置/备份/哄睡播放等）不属于任何 Tab：
    // 药丸留在最近一个 Tab 的位置并降低透明度，图标全部恢复未选中态
    val exactIndex = tabs.indexOfFirst { it.route == currentRoute }
    val isSubPage = exactIndex < 0
    // 记住最近一次真实选中的 Tab，子页面时药丸停在这里
    var lastTab by remember { mutableStateOf(0) }
    val selectedIndex = if (isSubPage) lastTab else exactIndex.also { lastTab = it }
    val itemWidth = 88.dp
    val pillWidth = 64.dp
    val pillColor = MaterialTheme.colorScheme.primaryContainer
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemWidthPx = with(density) { itemWidth.toPx() }

    // 拖动状态：拖动中记录累计位移（px），药丸跟手；非拖动时回退到 spring 动画
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    // 松手前跟随到的目标索引（用于拖动中即时高亮图标）
    var dragTargetIndex by remember { mutableStateOf(selectedIndex) }

    // 非拖动时药丸位置：spring 过冲，形成“滑动+回弹”手感
    val animatedOffset by animateFloatAsState(
        targetValue = selectedIndex * itemWidthPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillOffset"
    )
    val pillOffsetPx = if (isDragging) dragTargetIndex * itemWidthPx else animatedOffset
    val pillInsetPx = with(density) { ((itemWidth - pillWidth) / 2).toPx() }

    Surface(
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .frostGlass(
                    tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
                // 底部额外抬高：手势导航条（如 OPPO 小白条）会遮挡文字底边，
                // 系统导航条 inset 之外再抬 4dp，确保标签完整可见；磨砂背景仍延伸到屏幕底部
                .padding(top = 10.dp, bottom = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = 2.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(itemWidth * tabs.size)
                        // 水平拖动切换页面：药丸跟手，松手落到最近的 Tab
                        .pointerInput(tabs, selectedIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragOffsetPx = 0f
                                    dragTargetIndex = selectedIndex
                                },
                                onDragEnd = {
                                    val target = dragTargetIndex
                                    isDragging = false
                                    dragOffsetPx = 0f
                                    dragTargetIndex = selectedIndex
                                    if (target != selectedIndex) {
                                        onSelect(tabs[target.coerceIn(0, tabs.lastIndex)].route)
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragOffsetPx = 0f
                                    dragTargetIndex = selectedIndex
                                }
                            ) { _, dragAmount ->
                                dragOffsetPx += dragAmount
                                // 根据累计位移估算落在哪个 Tab（拖过一半即计入）
                                val estimate = selectedIndex + (dragOffsetPx / itemWidthPx).roundToInt()
                                val newTarget = estimate.coerceIn(0, tabs.lastIndex)
                                if (newTarget != dragTargetIndex) {
                                    dragTargetIndex = newTarget
                                    // 跨过 Tab 边界时轻震反馈
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                ) {
                    // 滑动药丸指示器（底层）：子页面时降透明度停靠在最近 Tab
                    val pillAlpha by animateFloatAsState(if (isSubPage) 0.35f else 1f, label = "pillAlpha")
                    Box(
                        Modifier
                            .offset { IntOffset((pillOffsetPx + pillInsetPx).roundToInt(), 0) }
                            .width(pillWidth)
                            .height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .drawBehind { drawRect(pillColor.copy(alpha = pillColor.alpha * pillAlpha)) }
                    )
                    // Tab 项（上层）
                    Row {
                        tabs.forEachIndexed { i, tab ->
                            // 拖动中按拖动目标即时高亮；否则按选中项；子页面时全部未选中
                            val selected = !isSubPage && i == (if (isDragging) dragTargetIndex else selectedIndex)
                            // 图标弹性缩放
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.12f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "icon$i"
                            )
                            val tint by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(200),
                                label = "tint$i"
                            )
                            Column(
                                Modifier
                                    .width(itemWidth)
                                    .height(56.dp)
                                    .clickable(
                                        interactionSource = MutableInteractionSource(),
                                        indication = null
                                    ) { onSelect(tab.route) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    tab.icon, contentDescription = tab.label,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp).scale(iconScale)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    tab.label, fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
