package com.babyrecord.app.ui.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.babyrecord.app.ui.formatDateTime
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.InkSoft

@Composable
fun BackupScreen(vm: BackupViewModel, onBack: () -> Unit) {
    val webdav = vm.savedWebdav()
    val s3 = vm.savedS3()

    var wdUrl by remember { mutableStateOf(webdav.url) }
    var wdUser by remember { mutableStateOf(webdav.user) }
    var wdPass by remember { mutableStateOf(webdav.pass) }
    var s3Endpoint by remember { mutableStateOf(s3.endpoint) }
    var s3Region by remember { mutableStateOf(s3.region) }
    var s3Bucket by remember { mutableStateOf(s3.bucket) }
    var s3Access by remember { mutableStateOf(s3.access) }
    var s3Secret by remember { mutableStateOf(s3.secret) }
    val autosync by vm.autosyncEnabled.collectAsStateWithLifecycle()

    var wdStatus by remember { mutableStateOf(if (vm.lastWebdavSync() > 0) "上次同步：${formatDateTime(vm.lastWebdavSync())}" else "") }
    var s3Status by remember { mutableStateOf(if (vm.lastS3Sync() > 0) "上次同步：${formatDateTime(vm.lastS3Sync())}" else "") }
    var wdBusy by remember { mutableStateOf(false) }
    var s3Busy by remember { mutableStateOf(false) }
    var wdConfirmRestore by remember { mutableStateOf(false) }
    var s3ConfirmRestore by remember { mutableStateOf(false) }
    var wdExpanded by remember { mutableStateOf(true) }
    var s3Expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("数据备份", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            "把本地数据库同步到你自己的网盘 / 对象存储，数据不经过任何第三方服务器",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 18.dp, top = 4.dp, end = 20.dp)
        )
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // WebDAV（可折叠）
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { wdExpanded = !wdExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("☁️ WebDAV 同步", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("适用于坚果云、群晖、NextCloud 等", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                    Icon(
                        if (wdExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (wdExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = wdExpanded) {
                    Column {
                Spacer(Modifier.height(12.dp))
                SettingField("服务器地址", wdUrl, "https://dav.jianguoyun.com/dav/") { wdUrl = it }
                Spacer(Modifier.height(10.dp))
                SettingField("账号", wdUser, "账号") { wdUser = it }
                Spacer(Modifier.height(10.dp))
                SettingField("密码 / 应用密码", wdPass, "密码", isPassword = true) { wdPass = it }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            wdBusy = true
                            vm.backupWebdav(wdUrl, wdUser, wdPass) { ok, msg ->
                                wdStatus = msg + if (ok) " · ${formatDateTime(System.currentTimeMillis())}" else ""
                                wdBusy = false
                            }
                        },
                        enabled = !wdBusy,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) { Text(if (wdBusy) "同步中…" else "立即同步", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { wdConfirmRestore = true },
                        enabled = !wdBusy,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("从云端恢复", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
                if (wdStatus.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(wdStatus, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // S3（可折叠）
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { s3Expanded = !s3Expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🪣 S3 对象存储同步", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("适用于 AWS S3、阿里云 OSS、腾讯云 COS、MinIO 等", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                    Icon(
                        if (s3Expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (s3Expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = s3Expanded) {
                    Column {
                Spacer(Modifier.height(12.dp))
                SettingField("Endpoint", s3Endpoint, "https://s3.cn-north-1.amazonaws.com.cn") { s3Endpoint = it }
                Spacer(Modifier.height(10.dp))
                SettingField("Region", s3Region, "cn-north-1") { s3Region = it }
                Spacer(Modifier.height(10.dp))
                SettingField("Bucket", s3Bucket, "my-bucket") { s3Bucket = it }
                Spacer(Modifier.height(10.dp))
                SettingField("AccessKey", s3Access, "AccessKeyId") { s3Access = it }
                Spacer(Modifier.height(10.dp))
                SettingField("SecretKey", s3Secret, "SecretAccessKey", isPassword = true) { s3Secret = it }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            s3Busy = true
                            vm.backupS3(
                                S3Config(s3Endpoint, s3Region, s3Bucket, s3Access, s3Secret)
                            ) { ok, msg ->
                                s3Status = msg + if (ok) " · ${formatDateTime(System.currentTimeMillis())}" else ""
                                s3Busy = false
                            }
                        },
                        enabled = !s3Busy,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) { Text(if (s3Busy) "同步中…" else "立即同步", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { s3ConfirmRestore = true },
                        enabled = !s3Busy,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("从云端恢复", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
                if (s3Status.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(s3Status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("自动同步", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = autosync,
                        onCheckedChange = { vm.setAutosync(it) }
                    )
                }
                Text(
                    "开启后，每次记录/上传照片会自动推送到你的 S3；另一台手机打开 App 时会提醒恢复，实现两台设备共享数据。",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "说明：同步会把宝宝记录的数据库快照上传到你配置的服务器（WebDAV 路径下或 S3 桶 backup/ 目录），随时可以恢复。请妥善保管账号密码。",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (wdConfirmRestore) {
        AlertDialog(
            onDismissRequest = { wdConfirmRestore = false },
            title = { Text("从 WebDAV 恢复？", fontWeight = FontWeight.Bold) },
            text = { Text("将用云端备份覆盖本机全部数据：喂养记录、相册照片等都会替换为云端版本。\n\n确认后应用会自动重启。") },
            confirmButton = {
                TextButton(onClick = {
                    wdConfirmRestore = false
                    wdBusy = true
                    wdStatus = "恢复中…"
                    vm.restoreWebdav(wdUrl, wdUser, wdPass) { ok, msg ->
                        wdBusy = false
                        wdStatus = msg
                    }
                }) { Text("确认恢复", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { wdConfirmRestore = false }) { Text("取消", color = InkSoft) }
            }
        )
    }

    if (s3ConfirmRestore) {
        AlertDialog(
            onDismissRequest = { s3ConfirmRestore = false },
            title = { Text("从 S3 恢复？", fontWeight = FontWeight.Bold) },
            text = { Text("将用云端备份覆盖本机全部数据：喂养记录、相册照片等都会替换为云端版本。\n\n确认后应用会自动重启。") },
            confirmButton = {
                TextButton(onClick = {
                    s3ConfirmRestore = false
                    s3Busy = true
                    s3Status = "恢复中…"
                    vm.restoreS3(S3Config(s3Endpoint, s3Region, s3Bucket, s3Access, s3Secret)) { ok, msg ->
                        s3Busy = false
                        s3Status = msg
                    }
                }) { Text("确认恢复", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { s3ConfirmRestore = false }) { Text("取消", color = InkSoft) }
            }
        )
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onChange: (String) -> Unit
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Coral
            ),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        )
    }
}
