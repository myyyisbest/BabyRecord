package com.babyrecord.app.ui.album

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.PhotoEntity
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.InkSoft
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WarnRed = Color(0xFFD64545)
private val PhotoInk = Color(0xFF6B5A4E)
private val StarGold = Color(0xFFFFC53D)
private val AgePillBg = Color(0xFFFFF0C9)
private val AgePillInk = Color(0xFF8A6A1F)

private val zone: ZoneId get() = ZoneId.systemDefault()

private fun fullDate(millis: Long): String {
    val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val week = "一二三四五六日"[d.dayOfWeek.value % 7]
    return "${d.year}年${d.monthValue}月${d.dayOfMonth}日 星期$week"
}

/** 照片拍摄时宝宝的月龄，如 "6个月2天" */
private fun ageAtText(birthday: LocalDate?, takenAt: Long): String? {
    if (birthday == null) return null
    val d = Instant.ofEpochMilli(takenAt).atZone(zone).toLocalDate()
    if (d.isBefore(birthday)) return null
    val months = ChronoUnit.MONTHS.between(birthday, d).toInt()
    val days = ChronoUnit.DAYS.between(birthday.plusMonths(months.toLong()), d).toInt()
    return when {
        months <= 0 -> "${days}天"
        else -> "${months}个月${if (days > 0) "${days}天" else ""}"
    }
}

/** 整数月龄（组头/查看器用），如 "10个月"；未满月返回 "未满1个月" */
private fun monthAgeText(birthday: LocalDate?, takenAt: Long): String? {
    if (birthday == null) return null
    val d = Instant.ofEpochMilli(takenAt).atZone(zone).toLocalDate()
    if (d.isBefore(birthday)) return null
    val months = ChronoUnit.MONTHS.between(birthday, d).toInt()
    return if (months <= 0) "未满1个月" else "${months}个月"
}

private fun monthsEn(birthday: LocalDate?, takenAt: Long): String {
    if (birthday == null) return ""
    val d = Instant.ofEpochMilli(takenAt).atZone(zone).toLocalDate()
    val months = ChronoUnit.MONTHS.between(birthday, d).coerceAtLeast(0).toInt()
    return if (months <= 0) "newborn" else if (months == 1) "1 month" else "$months months"
}

/** 照片所属年月（本地时区），用于按月分组 */
private fun ymOf(takenAt: Long): YearMonth =
    YearMonth.from(Instant.ofEpochMilli(takenAt).atZone(zone))

/** tags 字符串 → 标签列表（逗号分隔存储，UI 层转换） */
private fun tagsOf(photo: PhotoEntity): List<String> =
    photo.tags.split(',', '，').map { it.trim() }.filter { it.isNotBlank() }

/** 睡眠分钟数 → 展示文本，如 "11.5小时" / "45分钟" */
private fun sleepDurationText(min: Long): String =
    if (min >= 60) String.format("%.1f小时", min / 60f) else "${min}分钟"

/**
 * 照片内容三分支（网格缩略图 / 拍立得卡 / 查看器共用）：
 * 1. 本地文件存在 → Coil AsyncImage 加载
 * 2. sha256 非空但文件缺失 → 元数据已同步、文件未到，显示「下载中」占位并触发同步补拉
 * 3. 其余 → 空占位（本地新建还没算出指纹，或文件意外丢失）
 */
@Composable
private fun PhotoImageBox(photo: PhotoEntity, contentScale: ContentScale, modifier: Modifier) {
    val context = LocalContext.current
    val file = albumPhotoFile(context, photo)
    when {
        file.exists() -> AsyncImage(
            model = file,
            contentDescription = photo.title.ifBlank { "照片" },
            modifier = modifier,
            contentScale = contentScale
        )
        photo.sha256.isNotEmpty() -> {
            // 文件在服务端但本地缺失 → 触发一次同步，SyncEngine 的补拉阶段会下载它
            LaunchedEffect(photo.id) {
                (context.applicationContext as? BabyApp)?.syncEngine?.requestSync()
            }
            Box(modifier.background(Color(0xFFF3EDE7)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.CloudDownload,
                        contentDescription = "下载中",
                        tint = Color(0xFFB9A89A),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("下载中", fontSize = 11.sp, color = Color(0xFFB9A89A))
                }
            }
        }
        else -> Box(modifier.background(Color(0xFFF3EDE7)))
    }
}

/** 拍立得照片卡：白边相纸 + 图片 +（可选）手写风说明（首页/成长页轮播用） */
@Composable
private fun PolaroidCard(
    photo: PhotoEntity,
    birthday: LocalDate?,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    scale: Float = 1f,
    showCaption: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .shadow(10.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(9.dp)
    ) {
        PhotoImageBox(
            photo = photo,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(RoundedCornerShape(3.dp))
        )
        if (showCaption) {
            Spacer(Modifier.height(9.dp))
            val title = photo.title.ifBlank { photo.caption }
            Text(
                if (title.isBlank()) " " else "${monthsEn(birthday, photo.takenAt)} · $title",
                fontSize = 13.sp,
                color = PhotoInk,
                maxLines = 1,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(3.dp))
        }
    }
}

/**
 * 拟物堆叠轮播（成长页预览用，只显示图片不放文字）：
 * 当前照片居中微微倾斜，上一张/下一张在两侧露出边缘，点击侧边即切换，随机自动轮播。
 */
@Composable
fun StackedPhotoCarousel(photos: List<PhotoEntity>, modifier: Modifier = Modifier, autoPlay: Boolean = true, birthday: LocalDate? = null) {
    var index by remember(photos.size) { mutableIntStateOf(0) }
    val n = photos.size

    LaunchedEffect(photos.size) {
        if (autoPlay && n > 1) {
            while (true) {
                delay(3200)
                index = (index + 1 + Random.nextInt(n - 1)) % n
            }
        }
    }

    Box(modifier.fillMaxWidth().height(246.dp)) {
        val prevIdx = (index - 1 + n) % n
        val nextIdx = (index + 1) % n

        if (n > 2) {
            PolaroidCard(
                photos[(index - 2 + n) % n], birthday,
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.62f),
                scale = 0.82f
            )
        }
        if (n > 1) {
            PolaroidCard(
                photos[prevIdx], birthday,
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.56f)
                    .offset(x = (-12).dp),
                rotation = -2.5f, scale = 0.92f,
                onClick = { index = prevIdx }
            )
            PolaroidCard(
                photos[nextIdx], birthday,
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.56f)
                    .offset(x = 12.dp),
                rotation = 2.5f, scale = 0.92f,
                onClick = { index = nextIdx }
            )
        }
        PolaroidCard(photos[index], birthday, Modifier.align(Alignment.Center).fillMaxWidth(0.78f), rotation = -2f)

        // 左右切换箭头：明确的翻页引导（点击边缘照片切换仍保留）
        if (n > 1) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable { index = prevIdx },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上一张",
                    tint = PhotoInk, modifier = Modifier.size(26.dp)
                )
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable { index = nextIdx },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下一张",
                    tint = PhotoInk, modifier = Modifier.size(26.dp)
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(n) { i ->
                Box(
                    Modifier
                        .size(if (i == index) 7.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == index) Coral
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

/** 成长页里的相册预览卡：只放图片的拟物堆叠轮播 + 点击进入管理 */
@Composable
fun AlbumPreviewCard(photos: List<PhotoEntity>, birthday: LocalDate?, onOpenAlbum: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("成长相册", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(
                "管理相册 ›", fontSize = 13.sp, color = Coral, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onOpenAlbum)
            )
        }
        Spacer(Modifier.height(8.dp))
        if (photos.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onOpenAlbum),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("记录宝宝的精彩瞬间", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            StackedPhotoCarousel(
                photos = photos,
                birthday = birthday,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                autoPlay = true
            )
        }
    }
}

/** 按月分组的照片组（组头 + 该月照片，照片按 takenAt 倒序） */
private data class MonthGroup(val ym: YearMonth, val photos: List<PhotoEntity>)

/**
 * 相册页（升级版）：月分组网格 + 筛选栏 + 全屏查看器。
 * 主视图为 LazyVerticalGrid（3 列），按拍摄月份分组，组头显示张数与该月宝宝月龄；
 * 点击缩略图打开 PhotoViewer 全屏浏览（左右滑切换 / 双指缩放 / 下滑关闭）。
 */
@Composable
fun AlbumScreen(
    photos: List<PhotoEntity>,
    birthday: LocalDate?,
    daySummaries: Map<String, DaySummary>,
    onBack: () -> Unit,
    onAddPhotos: (List<Uri>) -> Unit,
    onEditPhoto: (PhotoEntity) -> Unit,
    onDeletePhoto: (PhotoEntity) -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit,
    onRequestDaySummary: (LocalDate) -> Unit
) {
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris -> onAddPhotos(uris) }

    var editTarget by remember { mutableStateOf<PhotoEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<PhotoEntity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    // 筛选状态：null 表示「全部」
    var selMonth by remember { mutableStateOf<YearMonth?>(null) }
    var selTag by remember { mutableStateOf<String?>(null) }
    var onlyFavorite by remember { mutableStateOf(false) }

    // 查看器打开时的起始页（filtered 内下标）
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    // 筛选 chips 数据从照片数据动态派生
    val allTags = remember(photos) { photos.flatMap { tagsOf(it) }.distinct() }
    val allMonths = remember(photos) { photos.map { ymOf(it.takenAt) }.distinct().sortedDescending() }

    val filtered = remember(photos, selMonth, selTag, onlyFavorite) {
        photos.filter { p ->
            (selMonth == null || ymOf(p.takenAt) == selMonth) &&
                (selTag == null || tagsOf(p).contains(selTag)) &&
                (!onlyFavorite || p.favorite == 1)
        }
    }
    val groups = remember(filtered) {
        filtered.groupBy { ymOf(it.takenAt) }
            .toSortedMap(compareByDescending { it })
            .map { MonthGroup(it.key, it.value) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(10.dp))
        // 顶部栏：返回 / 标题 / ⋮ 菜单（添加图片）
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconBtn(Icons.AutoMirrored.Rounded.ArrowBack, "返回", onBack)
            Spacer(Modifier.weight(1f))
            Text("宝宝相册", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Box {
                CircleIconBtn(Icons.Rounded.MoreVert, "更多操作") { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("添加图片", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = Coral) },
                        onClick = {
                            menuOpen = false
                            pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
            }
        }

        if (photos.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) { Text("📷", fontSize = 44.sp) }
                    Spacer(Modifier.height(18.dp))
                    Text("相册还是空的", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text("从手机相册挑几张宝宝的照片吧", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    AddPill { pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
            }
        } else {
            // 筛选栏：月份 chips + 标签 chips + 只看精选
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectChip("全部", selMonth == null) { selMonth = null }
                allMonths.forEach { ym ->
                    SelectChip("${ym.year}-${ym.monthValue.toString().padStart(2, '0')}", selMonth == ym) { selMonth = ym }
                }
                if (allTags.isNotEmpty()) {
                    Box(Modifier.size(width = 1.dp, height = 18.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
                allTags.forEach { tag ->
                    SelectChip(tag, selTag == tag) { selTag = if (selTag == tag) null else tag }
                }
                Box(Modifier.size(width = 1.dp, height = 18.dp).background(MaterialTheme.colorScheme.outlineVariant))
                SelectChip("★ 只看精选", onlyFavorite) { onlyFavorite = !onlyFavorite }
            }

            // 月分组网格主体
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, bottom = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                groups.forEach { group ->
                    // 组头横跨整行
                    item(key = "header_${group.ym}", span = { GridItemSpan(maxLineSpan) }) {
                        MonthHeader(
                            ym = group.ym,
                            count = group.photos.size,
                            // 组头月龄取该月首张照片（列表为倒序，即该月最新一张）
                            ageText = monthAgeText(birthday, group.photos.first().takenAt)
                        )
                    }
                    items(group.photos, key = { it.id }) { photo ->
                        PhotoThumb(photo = photo) {
                            viewerIndex = filtered.indexOfFirst { it.id == photo.id }.takeIf { it >= 0 } ?: 0
                        }
                    }
                }
            }
        }
    }

    // 全屏查看器
    viewerIndex?.let { initial ->
        if (filtered.isNotEmpty()) {
            PhotoViewer(
                photos = filtered,
                initialIndex = initial,
                birthday = birthday,
                daySummaries = daySummaries,
                onRequestDaySummary = onRequestDaySummary,
                onDismiss = { viewerIndex = null },
                onEdit = { editTarget = it },
                onDelete = { deleteTarget = it },
                onToggleFavorite = onToggleFavorite
            )
        }
    }

    editTarget?.let { photo ->
        EditPhotoDialog(photo = photo, onDismiss = { editTarget = null }, onSave = { onEditPhoto(it); editTarget = null })
    }
    deleteTarget?.let { photo ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这张照片？", fontWeight = FontWeight.Bold) },
            text = { Text(photo.title.ifBlank { photo.caption.ifBlank { "删除后无法恢复" } }) },
            confirmButton = {
                TextButton(onClick = { onDeletePhoto(photo); deleteTarget = null }) {
                    Text("删除", color = WarnRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消", color = InkSoft) }
            }
        )
    }
}

/** 筛选 chip：选中 Coral 实底白字，未选中浅底灰字 */
@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Coral else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 月分组组头：「2026年10月 · 12张」+ 右侧该月宝宝月龄 */
@Composable
private fun MonthHeader(ym: YearMonth, count: Int, ageText: String?) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${ym.year}年${ym.monthValue}月 · ${count}张",
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        if (ageText != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AgePillBg)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(ageText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AgePillInk)
            }
        }
    }
}

/** 网格缩略图：正方形裁切 + 精选小星标 + 三分支图片内容 */
@Composable
private fun PhotoThumb(photo: PhotoEntity, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFF3EDE7))
            .clickable(onClick = onClick)
    ) {
        PhotoImageBox(photo = photo, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (photo.favorite == 1) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = "精选",
                tint = StarGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .padding(3.dp)
            )
        }
    }
}

/**
 * 全屏照片查看器：
 * - HorizontalPager 左右滑切换（缩放 >1 时禁用 pager 滑动）
 * - 单指双击缩放区域平移、双指缩放（1~5f）
 * - 单击切换工具栏显隐
 * - 未放大时单指向下拖动（>120px）关闭，图片跟随位移
 * - 底部显示日期/月龄、标题、标签与当天记录摘要
 */
@Composable
private fun PhotoViewer(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    birthday: LocalDate?,
    daySummaries: Map<String, DaySummary>,
    onRequestDaySummary: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onEdit: (PhotoEntity) -> Unit,
    onDelete: (PhotoEntity) -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    ) { photos.size }

    // 查看器级手势状态（翻页后重置）
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var dragDown by remember { mutableFloatStateOf(0f) }
    var chromeVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // 下滑关闭阈值（换算为像素）
    val dismissThresholdPx = with(density) { 120.dp.toPx() }

    val current = photos.getOrNull(pagerState.currentPage)

    // 照片被删光后自动关闭查看器
    LaunchedEffect(photos.size) {
        if (photos.isEmpty()) onDismiss()
    }

    // 翻页后重置缩放/拖拽，并请求当前照片所在日的记录摘要
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f; panX = 0f; panY = 0f; dragDown = 0f
    }
    // 底部信息改为展示照片编辑内容（标题/故事/标签）后，不再查询当天记录摘要

    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            // 放大后禁用 pager 滑动，让手势全部交给缩放/平移
            userScrollEnabled = scale <= 1f,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomablePhotoPage(
                photo = photos[page],
                scale = scale,
                panX = panX,
                panY = panY,
                dragDown = dragDown,
                onTap = { chromeVisible = !chromeVisible },
                onTransform = { zoomDelta, panDelta ->
                    val newScale = (scale * zoomDelta).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        panX += panDelta.x
                        panY += panDelta.y
                    } else {
                        panX = 0f; panY = 0f
                    }
                },
                onDragDown = { dy -> dragDown += dy },
                onDragRelease = { total ->
                    if (total > dismissThresholdPx) onDismiss()
                    else scope.launch {
                        animate(dragDown, 0f) { v, _ -> dragDown = v }
                    }
                }
            )
        }

        // 顶部工具栏：返回 / 页码 / 收藏 / 编辑 / 删除（单击切换显隐）
        if (chromeVisible) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewerBtn(Icons.AutoMirrored.Rounded.ArrowBack, "返回") { onDismiss() }
                Spacer(Modifier.weight(1f))
                Text(
                    "${pagerState.currentPage + 1} / ${photos.size}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
                Spacer(Modifier.weight(1f))
                ViewerBtn(
                    if (current?.favorite == 1) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    if (current?.favorite == 1) "取消精选" else "设为精选",
                    tint = if (current?.favorite == 1) StarGold else Color.White
                ) { current?.let(onToggleFavorite) }
                Spacer(Modifier.width(6.dp))
                ViewerBtn(Icons.Rounded.Edit, "编辑") { current?.let(onEdit) }
                Spacer(Modifier.width(6.dp))
                ViewerBtn(Icons.Rounded.Delete, "删除", tint = Color(0xFFFF8A80)) { current?.let(onDelete) }
            }
        }

        // 底部信息：日期/月龄 + 标题/描述 + 标签 + 当天记录摘要
        if (chromeVisible) {
            current?.let { photo ->
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fullDate(photo.takenAt), fontSize = 13.sp, color = Color(0xFFE8DDCF))
                        Spacer(Modifier.width(10.dp))
                        ageAtText(birthday, photo.takenAt)?.let {
                            Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AgePillInk,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AgePillBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    // 照片编辑内容：标题 / 这一刻的故事 / 标签（无内容时显示引导，点✏️可编辑）
                    val title = photo.title.ifBlank { photo.caption }
                    if (title.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (photo.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(photo.description, fontSize = 13.sp, color = Color(0xFFD8CFC5), lineHeight = 19.sp)
                    }
                    val tagList = tagsOf(photo)
                    if (tagList.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tagList.forEach { tag ->
                                Text(
                                    tag, fontSize = 12.sp, color = Color(0xFFF2B8C6), fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0x33FFFFFF))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (title.isBlank() && photo.description.isBlank() && tagList.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "点右上角 ✏️ 添加标题、这一刻的故事和标签",
                            fontSize = 12.sp, color = Color(0xFF9A918A)
                        )
                    }
                }
            }
        }
    }
}

/** 查看器单页：图片 + 自定义手势（缩放/平移/下滑关闭/单击切换工具栏） */
@Composable
private fun ZoomablePhotoPage(
    photo: PhotoEntity,
    scale: Float,
    panX: Float,
    panY: Float,
    dragDown: Float,
    onTap: () -> Unit,
    onTransform: (zoomDelta: Float, panDelta: Offset) -> Unit,
    onDragDown: (dy: Float) -> Unit,
    onDragRelease: (totalDy: Float) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(photo.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var multiOrZoomed = false   // 进入缩放/平移模式（双指或已放大）
                    var dragging = false        // 未放大时的下滑关闭拖动
                    var totalDown = 0f
                    var moved = false           // 是否发生过明显移动（用于区分单击）
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) {
                            // 全部手指抬起：判定单击 / 释放下滑
                            val up = event.changes.firstOrNull()
                            val tapLike = !moved && !dragging && up != null && !up.isConsumed &&
                                abs(up.position.x - down.position.x) < viewConfiguration.touchSlop * 3 &&
                                abs(up.position.y - down.position.y) < viewConfiguration.touchSlop * 3
                            if (tapLike) onTap()
                            if (dragging) onDragRelease(totalDown)
                            break
                        }
                        if (multiOrZoomed || event.changes.size > 1 || scale > 1f) {
                            // 缩放 + 平移（已放大后单指也可平移）
                            multiOrZoomed = true
                            val zoomDelta = event.calculateZoom()
                            val panDelta = event.calculatePan()
                            if (zoomDelta != 1f || panDelta != Offset.Zero) {
                                moved = true
                                onTransform(zoomDelta, panDelta)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } else {
                            // 未放大：仅响应向下的拖动（下滑关闭），横向留给 pager
                            val change = event.changes.first()
                            val dy = change.positionChange().y
                            if (dy != 0f) {
                                if (abs(dy) > viewConfiguration.touchSlop) moved = true
                                if (!dragging && dy > 0 && abs(dy) > viewConfiguration.touchSlop) dragging = true
                                if (dragging && dy > 0) {
                                    totalDown += dy
                                    onDragDown(dy)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = panX
                    translationY = panY + dragDown
                    // 下滑时轻微变淡，提示可关闭
                    alpha = 1f - (dragDown / 600f).coerceIn(0f, 0.4f)
                }
        ) {
            PhotoImageBox(
                photo = photo,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 查看器工具栏按钮：半透明白底圆形 */
@Composable
private fun ViewerBtn(icon: ImageVector, desc: String, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CircleIconBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun AddPill(onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Coral)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("添加图片", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/**
 * 编辑照片对话框：标题/描述 + 标签编辑。
 * 标签改为 FlowRow 展示已有标签（✕ 可删）+ 输入框回车/点 + 添加；
 * 数据层仍为逗号分隔字符串，UI 层做 List<String> ↔ String 转换。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditPhotoDialog(photo: PhotoEntity, onDismiss: () -> Unit, onSave: (PhotoEntity) -> Unit) {
    var title by remember(photo) { mutableStateOf(photo.title) }
    var description by remember(photo) { mutableStateOf(photo.description) }
    var tagList by remember(photo) { mutableStateOf(tagsOf(photo)) }
    var newTag by remember(photo) { mutableStateOf("") }

    fun addTag() {
        val t = newTag.trim()
        if (t.isNotEmpty() && !tagList.contains(t)) tagList = tagList + t
        newTag = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑这条记录", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    placeholder = { Text("标题，如：第一次自己坐起来", fontSize = 13.sp, color = InkSoft) },
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    placeholder = { Text("记录这一刻的故事…", fontSize = 13.sp, color = InkSoft) },
                    shape = RoundedCornerShape(12.dp), minLines = 3
                )
                // 已有标签：FlowRow 展示，每个可点 ✕ 删除
                if (tagList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagList.forEach { tag ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFFBE9EE))
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tag, fontSize = 13.sp, color = Color(0xFFC2556B), fontWeight = FontWeight.Medium)
                                IconButton(
                                    onClick = { tagList = tagList - tag },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "删除标签 $tag",
                                        tint = Color(0xFFC2556B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                // 新标签输入：回车或点 + 添加
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTag, onValueChange = { newTag = it },
                        placeholder = { Text("新标签，回车添加", fontSize = 13.sp, color = InkSoft) },
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addTag() })
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (newTag.isBlank()) MaterialTheme.colorScheme.surfaceVariant else Coral)
                            .clickable(enabled = newTag.isNotBlank()) { addTag() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "添加标签",
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        photo.copy(
                            title = title.trim(),
                            description = description.trim(),
                            tags = tagList.joinToString(",")
                        )
                    )
                }
            ) {
                Text("保存", color = Coral, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        }
    )
}
