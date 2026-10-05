package com.babyrecord.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/**
 * ── 磨砂玻璃效果 ──
 * 半透明底 + 顶部高光描边，配合 content 使用时置于渐变/图片之上形成毛玻璃层次。
 * （真 backdrop-blur 需 API 31+ 的 RenderEffect，此处用安全通用方案）
 */
fun Modifier.frostGlass(
    tint: Color = Color.White.copy(alpha = 0.14f),
    highlight: Color = Color.White.copy(alpha = 0.22f),
    radius: Float = 40f
): Modifier = drawBehind {
    drawRoundRect(color = tint, cornerRadius = CornerRadius(radius, radius))
    // 顶部高光线，营造玻璃上沿反光
    val lineH = 1.5.dp.toPx()
    drawRoundRect(
        color = highlight,
        topLeft = Offset(0f, 0f),
        size = Size(size.width, lineH),
        cornerRadius = CornerRadius(radius, radius)
    )
}

/** ── 按压弹性缩放：按下 0.93，松开回弹（spring 过冲）── */
@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )
    return this.then(Modifier.drawBehind { scale(scale, scale) {} })
}

/**
 * ── 播放呼吸脉冲：播放中在控件外圈扩散光晕（两圈相位差），停止时淡出 ──
 */
@Composable
fun BoxScope.PulseHalo(
    playing: Boolean,
    color: Color = Color.White,
    maxScale: Float = 1.55f
) {
    if (!playing) return
    val transition = rememberInfiniteTransition(label = "halo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloPhase"
    )
    // 两圈相位差 0.5 循环扩散
    listOf(0f, 0.5f).forEach { offsetPhase ->
        val p = ((phase + offsetPhase) % 1f)
        val alpha = (1f - p) * 0.35f
        Box(
            Modifier
                .align(Alignment.Center)
                .alpha(alpha)
                .drawBehind {
                    val s = 1f + (maxScale - 1f) * p
                    drawCircle(color = color.copy(alpha = 0.6f), radius = size.minDimension / 2f * s)
                }
                .matchParentSize()
        )
    }
}

/**
 * ── 跳动均衡器条：播放中随机相位上下起伏，停止时静止低位 ──
 */
@Composable
fun EqualizerBars(
    playing: Boolean,
    color: Color = Color.White,
    barCount: Int = 4
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = (0 until barCount).map { i ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 340 + i * 90, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq$i"
        )
    }
    Row(verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { i, anim ->
            val fraction = if (playing) anim.value else 0.25f
            Box(
                Modifier
                    .width(3.dp)
                    .height((20 * fraction).dp)
                    .drawBehind {
                        drawRoundRect(
                            color = color.copy(alpha = if (playing) 0.95f else 0.4f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
            )
            if (i != barCount - 1) Spacer(Modifier.width(2.dp))
        }
    }
}
