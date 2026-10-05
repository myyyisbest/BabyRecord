package com.babyrecord.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.MintContainer
import com.babyrecord.app.ui.theme.MintGreen
import com.babyrecord.app.ui.theme.SkyBlue
import com.babyrecord.app.ui.theme.SkyBlueContainer
import com.babyrecord.app.ui.theme.SkyBlueContainer
import com.babyrecord.app.ui.theme.ThemeMode

private data class MenuItem(
    val icon: ImageVector,
    val container: Color,
    val tint: Color,
    val title: String,
    val subtitle: String
)

@Composable
fun MoreScreen(
    onKnowledge: () -> Unit,
    onVaccines: () -> Unit,
    onBackup: () -> Unit,
    onSleepMedia: () -> Unit,
    onFamily: () -> Unit,
    onSettings: () -> Unit
) {
    // 图标用固定品牌色 tint：浅色胶囊底上日夜均清晰，不随 onSurface 变浅
    // 顺序：疫苗接种本 → 育儿百科 → 成长相册 → 哄睡播放 → 数据备份 → 更多设置
    val items = listOf(
        MenuItem(Icons.Rounded.MedicalServices, CoralContainer, Coral, "疫苗接种本", "国家免疫规划 22 剂管理") to onVaccines,
        MenuItem(Icons.Rounded.MenuBook, MintContainer, MintGreen, "育儿百科", "按月龄整理的喂养与护理知识") to onKnowledge,
        MenuItem(Icons.Rounded.NightsStay, CoralContainer, Coral, "哄睡播放", "睡前故事 · 哄睡音乐，来自你的 NAS") to onSleepMedia,
        MenuItem(Icons.Rounded.Backup, SkyBlueContainer, SkyBlue, "数据备份", "WebDAV / S3 云端同步") to onBackup,
        MenuItem(Icons.Rounded.Diversity3, MintContainer, MintGreen, "家庭共享", "多台设备实时同步记录") to onFamily,
        MenuItem(Icons.Rounded.Settings, SkyBlueContainer, SkyBlue, "更多设置", "外观显示 · 关于软件") to onSettings
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("更多", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text("外观、备份与更多能力", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (item, onClick) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(item.container), contentAlignment = Alignment.Center) {
                        Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text(item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 更多设置子页：外观显示、关于软件、删除宝宝档案 */
@Composable
fun MoreSettingsScreen(
    onAppearance: () -> Unit,
    onAbout: () -> Unit,
    onDeleteBaby: () -> Unit = {},
    onBack: () -> Unit
) {
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
            Text("更多设置", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        Text("外观与软件信息", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onAppearance)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(SkyBlueContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("外观显示", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text("日间 / 夜间 / 跟随系统", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onAbout)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(CoralContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Coral, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("关于软件", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text("版本与说明", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 删除宝宝档案：危险操作，红色警示样式，点击后由上层弹确认弹窗
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onDeleteBaby)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFDE8E4)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFD8402F), modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("删除宝宝档案", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD8402F))
                    Spacer(Modifier.height(2.dp))
                    Text("所有设备同步移除，操作不可撤销", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 外观显示设置页：日间 / 夜间 / 跟随系统 */
@Composable
fun AppearanceScreen(current: ThemeMode, onBack: () -> Unit, onSelect: (ThemeMode) -> Unit) {
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
            Text("外观显示", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("选择界面配色，跟随系统时会随手机日夜间模式自动切换", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple(ThemeMode.LIGHT, "☀️", "日间模式"),
                Triple(ThemeMode.DARK, "🌙", "夜间模式"),
                Triple(ThemeMode.SYSTEM, "📱", "跟随系统")
            ).forEach { (mode, emoji, label) ->
                val selected = current == mode
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (selected) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(Coral),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "夜间模式适合夜里喂奶、哄睡时使用，屏幕更柔和。",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp)
        )
    }
}
