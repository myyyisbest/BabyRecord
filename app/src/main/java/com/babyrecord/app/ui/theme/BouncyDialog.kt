package com.babyrecord.app.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 弹性弹窗：遮罩渐入 + 内容 spring 缩放弹入（0.85→1 带过冲），
 * 关闭时反向缩小渐隐。给设置类子界面/信息弹窗统一高级动效。
 */
@Composable
fun BouncyDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    confirmText: String? = null,
    onConfirm: () -> Unit = {}
) {
    if (!visible) return
    val outsideInteraction = remember { MutableInteractionSource() }
    val dialogBg = MaterialTheme.colorScheme.surface
    val titleColor = MaterialTheme.colorScheme.onSurface
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = outsideInteraction) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut()
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(dialogBg)
                        .clickable(enabled = false) {} // 拦截穿透，不响应点击
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                    Spacer(Modifier.height(12.dp))
                    content()
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { onConfirm(); onDismiss() }) {
                            Text(confirmText ?: "好的", color = Coral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
