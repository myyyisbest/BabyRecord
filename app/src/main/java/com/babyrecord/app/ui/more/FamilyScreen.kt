package com.babyrecord.app.ui.more

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babyrecord.app.BabyApp
import com.babyrecord.app.sync.SyncApi
import com.babyrecord.app.sync.SyncEngine
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.MintContainer
import com.babyrecord.app.ui.theme.MintGreen
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * 家庭共享设置页：
 * - 未加入：创建家庭 / 加入家庭 两个 Tab，服务器地址可编辑
 * - 已加入：家庭信息、邀请码复制、立即同步、退出家庭
 */
@Composable
fun FamilyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BabyApp
    val engine = app.syncEngine
    val scope = rememberCoroutineScope()

    var joined by remember { mutableStateOf(engine.isJoined()) }
    val syncing by engine.syncing.collectAsStateWithLifecycle()
    val unauthorized by engine.unauthorized.collectAsStateWithLifecycle()
    val lastSync by engine.lastSyncAt.collectAsStateWithLifecycle()
    var serverUrl by remember { mutableStateOf(SyncApi.serverUrl(context)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    // 加入家庭后发现本机有待推脏行：弹窗让用户决定上传合并 / 仅保留本地
    var pendingCount by remember { mutableStateOf(0) }
    var showPendingDialog by remember { mutableStateOf(false) }

    // 同步状态或 401 变化时刷新 UI；401 提示重新加入
    LaunchedEffect(unauthorized) {
        if (unauthorized) message = "登录已失效，请退出家庭后重新加入"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("家庭共享", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "多台设备实时同步宝宝记录，数据保存在自己的服务器",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
        Spacer(Modifier.height(16.dp))

        // 服务器地址（两种状态都可见可改）
        CardBox {
            Column(Modifier.padding(16.dp)) {
                Text("服务器地址", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    singleLine = true,
                    placeholder = { Text("http://192.168.1.100:8443", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "修改后立即生效，需两台设备填同一地址",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp)

        )

        if (!joined) {
            JoinOrCreateSection(
                busy = busy,
                onCreate = { familyName, deviceName ->
                    scope.launch {
                        busy = true
                        try {
                            SyncApi.setServerUrl(context, serverUrl)
                            engine.createFamily(familyName, deviceName)
                            joined = engine.isJoined()
                            message = "家庭创建成功 🎉 邀请码已生成"
                        } catch (e: Exception) {
                            // 展示服务端返回的 error 文案（如 403 创建锁定），并引导改用「加入家庭」
                            message = "${e.message ?: "网络错误"}\n可改用「加入家庭」凭邀请码加入现有家庭"
                        } finally { busy = false }
                    }
                },
                onJoin = { inviteCode, deviceName ->
                    scope.launch {
                        busy = true
                        try {
                            SyncApi.setServerUrl(context, serverUrl)
                            engine.joinFamily(inviteCode, deviceName)
                            joined = engine.isJoined()
                            // 只拉不推完成后，若本机仍有待推脏行：交由用户决定是否上传合并
                            val pending = engine.localPendingCount()
                            if (pending > 0) {
                                pendingCount = pending
                                showPendingDialog = true
                                message = "已加入家庭 🎉"
                            } else {
                                message = "已加入家庭 🎉 数据同步中"
                            }
                        } catch (e: Exception) {
                            message = "加入失败：${e.message ?: "网络错误"}"
                        } finally { busy = false }
                    }
                }
            )
        } else {
            JoinedSection(
                engine = engine,
                serverUrl = serverUrl,
                syncing = syncing,
                lastSyncAt = lastSync,
                onSyncNow = {
                    scope.launch {
                        SyncApi.setServerUrl(context, serverUrl)
                        val ok = engine.syncOnce()
                        message = if (ok) "同步完成 ✓" else "同步失败，请检查服务器"
                    }
                },
                onLeave = { showLeaveDialog = true }
            )
        }

        // 操作结果提示（简单 snackbar 替代：顶部 Toast）
        LaunchedEffect(message) {
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                message = null
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("退出家庭") },
            text = { Text("退出后本机不再同步，记录仍保留在本机。确定退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    engine.leave()
                    joined = false
                    showLeaveDialog = false
                    message = "已退出家庭"
                }) { Text("退出", color = Coral) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("取消") }
            }
        )
    }

    // 加入家庭后发现本机有未上传的本地记录：询问如何处理
    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { showPendingDialog = false },
            title = { Text("发现本地记录") },
            text = { Text("本机有 $pendingCount 条本地记录尚未上传，如何处理？") },
            confirmButton = {
                TextButton(onClick = {
                    showPendingDialog = false
                    // 恢复正常推拉：把本机脏行推上去与家人数据合并
                    engine.requestSync()
                    message = "正在上传合并本地记录…"
                }) { Text("上传合并", color = MintGreen) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPendingDialog = false
                    scope.launch {
                        // 待推行标记为已同步：不再推送，仅保留本地显示
                        engine.markLocalPendingSynced()
                        message = "本地记录已保留在本机"
                    }
                }) { Text("仅保留在本地", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

/** 未加入：创建家庭 / 加入家庭 两个 Tab */
@Composable
private fun JoinOrCreateSection(
    busy: Boolean,
    onCreate: (String, String) -> Unit,
    onJoin: (String, String) -> Unit
) {
    var tab by remember { mutableStateOf(0) }   // 0=创建 1=加入
    var familyName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf(Build.MODEL ?: "我的设备") }

    CardBox {
        Column(Modifier.padding(16.dp)) {
            // Tab 切换
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                listOf("创建家庭", "加入家庭").forEachIndexed { i, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (tab == i) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { tab = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, fontSize = 14.sp,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                            color = if (tab == i) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (tab == 0) {
                OutlinedTextField(
                    value = familyName, onValueChange = { familyName = it },
                    singleLine = true, label = { Text("家庭名称") },
                    placeholder = { Text("如：我们家") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = inviteCode, onValueChange = { inviteCode = it.uppercase() },
                    singleLine = true, label = { Text("邀请码") },
                    placeholder = { Text("向创建家庭的设备索取") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = deviceName, onValueChange = { deviceName = it },
                singleLine = true, label = { Text("本机设备名") },
                placeholder = { Text("如：爸爸的手机") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            ActionButton(
                text = if (tab == 0) "创建家庭" else "加入家庭",
                loading = busy,
                enabled = !busy && deviceName.isNotBlank() &&
                    (if (tab == 0) familyName.isNotBlank() else inviteCode.isNotBlank())
            ) {
                if (tab == 0) onCreate(familyName, deviceName) else onJoin(inviteCode, deviceName)
            }
        }
    }
}

/** 已加入：信息展示 + 立即同步 + 退出 */
@Composable
private fun JoinedSection(
    engine: SyncEngine,
    serverUrl: String,
    syncing: Boolean,
    lastSyncAt: Long,
    onSyncNow: () -> Unit,
    onLeave: () -> Unit
) {
    val context = LocalContext.current

    CardBox {
        Column(Modifier.padding(16.dp)) {
            InfoRow("家庭 ID", engine.familyId().toString())
            InfoRow("服务器", serverUrl)
            Spacer(Modifier.height(4.dp))
            // 本机设备名可改（仅本地展示，下次加入/创建时生效）
            var devName by remember { mutableStateOf(engine.deviceName()) }
            OutlinedTextField(
                value = devName,
                onValueChange = {
                    devName = it
                    engine.setDeviceName(it)
                },
                singleLine = true,
                label = { Text("本机设备名") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            // 邀请码：点击复制
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MintContainer)
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("inviteCode", engine.inviteCode()))
                        Toast.makeText(context, "邀请码已复制", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("邀请码（点击复制）", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        engine.inviteCode().ifBlank { "见创建家庭的设备" },
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp, color = MintGreen
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            ActionButton(
                text = if (syncing) "同步中…" else if (lastSyncAt > 0) "立即同步（上次：${formatTime(lastSyncAt)}）" else "立即同步",
                loading = syncing,
                enabled = !syncing,
                container = CoralContainer,
                tint = Coral
            ) { onSyncNow() }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onLeave, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("退出家庭", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}

// ---- 小组件件 ----

@Composable
private fun CardBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) { content() }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ActionButton(
    text: String,
    loading: Boolean,
    enabled: Boolean = true,
    container: Color = CoralContainer,
    tint: Color = Coral,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) container else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = tint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
