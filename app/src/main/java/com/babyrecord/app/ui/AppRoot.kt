package com.babyrecord.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.BabyEntity
import com.babyrecord.app.data.DiaperEntity
import com.babyrecord.app.data.FeedingEntity
import com.babyrecord.app.data.SleepEntity
import com.babyrecord.app.data.SolidFoodEntity
import com.babyrecord.app.data.SupplementEntity
import com.babyrecord.app.ui.RecordType
import com.babyrecord.app.ui.album.AlbumScreen
import com.babyrecord.app.ui.album.AlbumViewModel
import com.babyrecord.app.ui.backup.BackupScreen
import com.babyrecord.app.ui.backup.AutoSync
import com.babyrecord.app.ui.backup.BackupViewModel
import com.babyrecord.app.media.SleepMediaScreen
import com.babyrecord.app.media.SleepMediaViewModel
import com.babyrecord.app.ui.detail.DetailState
import com.babyrecord.app.ui.detail.RecordDetailScreen
import com.babyrecord.app.ui.detail.RecordDetailViewModel
import com.babyrecord.app.ui.growth.GrowthScreen
import com.babyrecord.app.ui.growth.GrowthViewModel
import com.babyrecord.app.ui.growth.MeasurementSheet
import com.babyrecord.app.ui.home.DiaperSheet
import com.babyrecord.app.ui.home.FeedingSheet
import com.babyrecord.app.ui.home.HomeScreen
import com.babyrecord.app.ui.home.HomeViewModel
import com.babyrecord.app.ui.home.SleepSheet
import com.babyrecord.app.ui.home.SolidFoodSheet
import com.babyrecord.app.ui.home.SupplementSheet
import com.babyrecord.app.ui.knowledge.KnowledgeData
import com.babyrecord.app.ui.knowledge.KnowledgeDetailScreen
import com.babyrecord.app.ui.knowledge.KnowledgeListScreen
import com.babyrecord.app.ui.knowledge.KnowledgeViewModel
import com.babyrecord.app.ui.more.FamilyScreen
import com.babyrecord.app.ui.stats.StatsScreen
import com.babyrecord.app.ui.stats.StatsViewModel
import com.babyrecord.app.ui.theme.BouncyDialog
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.ThemeSettings
import com.babyrecord.app.ui.theme.Ink
import com.babyrecord.app.ui.more.FamilyScreen
import com.babyrecord.app.ui.timeline.TimelineScreen
import com.babyrecord.app.ui.timeline.TimelineViewModel
import com.babyrecord.app.ui.vaccine.VaccineScreen
import com.babyrecord.app.ui.vaccine.VaccineViewModel
import com.babyrecord.app.widget.QuickStatsWidgetHelper
import com.babyrecord.app.sync.PhotoTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

object Routes {
    const val HOME = "home"
    const val TIMELINE = "timeline"
    const val GROWTH = "growth"
    const val MORE = "more"
    const val VACCINES = "vaccines"
    const val STATS = "stats"
    const val ALBUM = "album"
    const val KNOWLEDGE = "knowledge"
    const val KNOWLEDGE_DETAIL = "knowledge/{id}"
    const val APPEARANCE = "appearance"
    const val BACKUP = "backup"
    const val SLEEP_MEDIA = "sleep_media"
    const val MORE_SETTINGS = "more_settings"
    const val FAMILY = "family"
    const val RECORD = "record/{type}/{id}"

    fun record(type: String, id: String) = "record/$type/$id"
}

sealed interface RootState {
    data object Loading : RootState
    data object NeedSetup : RootState
    /** 新设备选择了“加入已有家庭”而非新建宝宝：展示家庭共享页，拉回宝宝后自动 Ready */
    data object JoinFamily : RootState
    data class Ready(val baby: BabyEntity) : RootState
}

class RootViewModel(application: Application) : AndroidViewModel(application) {
    private val babyDao = (application as BabyApp).database.babyDao()

    // 已加入家庭（同步凭据存在）：重装/清数据后凭据随 App 数据一起被清，
    // 但只要凭据在，就说明设备属于某个家庭，应直接等同步拉回宝宝，绝不弹建档页。
    // 配合 Onboarding 的「加入家庭」入口，从根上避免“每次安装多建一个宝宝”。
    private val joinedFamily = MutableStateFlow(
        application.getSharedPreferences("sync", Context.MODE_PRIVATE)
            .getString("token", "")?.isNotBlank() == true
    )

    // 用户在 Onboarding 选择“加入已有家庭”时置位；拉回 babies 后自动回 Ready
    val skipSetupForJoin = MutableStateFlow(false)

    val state: StateFlow<RootState> = combine(
        babyDao.observeBaby(), skipSetupForJoin, joinedFamily
    ) { baby, join, joined ->
        when {
            baby != null -> RootState.Ready(baby)
            join || joined -> RootState.JoinFamily   // 已入家庭但本地暂无宝宝：等同步拉回
            else -> RootState.NeedSetup
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RootState.Loading)

    fun createBaby(name: String, gender: String, birthday: java.time.LocalDate) {
        viewModelScope.launch {
            babyDao.insert(BabyEntity(name = name, birthdayEpochDay = birthday.toEpochDay(), gender = gender).withNewId())
        }
    }

    /**
     * 更换宝宝头像：读原图 bytes → sha256 内容寻址 → 落 filesDir/album/<sha>.jpg
     * （完全复用照片文件通道，服务端零改动）→ baby.avatar = 文件名 markDirty 推送。
     * 换头像成功后 kick 照片通道立即上传文件，其他设备拉到 avatar 字段后由
     * SyncEngine.ensureAvatarFile 补拉同名文件。
     */
    fun updateAvatar(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val app = getApplication<Application>()
                val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val sha = PhotoTransfer.sha256Bytes(bytes)
                if (sha.isEmpty()) return@launch
                val dir = File(app.filesDir, "album").apply { mkdirs() }
                val dest = File(dir, "$sha.jpg")
                if (!dest.exists()) dest.writeBytes(bytes)  // 幂等：同内容不重复写
                val baby = babyDao.observeBaby().first() ?: return@launch
                babyDao.update(baby.copy(avatar = dest.name).markDirty())
                // 先立即上传头像文件（不等去抖），再触发记录同步把 avatar 字段推上去
                (app as BabyApp).syncEngine.kickPhotoWork()
                app.syncEngine.requestSync()
            }
        }
    }

    /**
     * 删除当前宝宝档案（软删 + 同步）：deletedAt=now 并 markDirty，
     * 所有设备拉到后同样软删；observeBaby 自动切到下一个未删除的宝宝
     * （没有下一个则回到建档页/家庭页）。记录数据保留在服务端，只是不再显示。
     */
    fun deleteBaby() {
        viewModelScope.launch {
            runCatching {
                val baby = babyDao.observeBaby().first() ?: return@launch
                babyDao.update(
                    baby.copy(deletedAt = System.currentTimeMillis(), avatar = "").markDirty()
                )
                (getApplication<Application>() as BabyApp).syncEngine.requestSync()
            }
        }
    }
}

// TabItem 定义在 GlassNavBar.kt（磨砂玻璃导航栏）

private fun recordId(record: RecordDisplay): String =
    record.feeding?.id ?: record.diaper?.id ?: record.sleep?.id ?: record.solid?.id ?: ""

@Composable
fun AppRoot() {
    val app = LocalContext.current.applicationContext as BabyApp
    val rootVm: RootViewModel = viewModel { RootViewModel(app) }
    val state by rootVm.state.collectAsStateWithLifecycle()

    when (state) {
        RootState.Loading -> Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Coral) }

        RootState.NeedSetup -> OnboardingScreen(
            onDone = { name, gender, birthday -> rootVm.createBaby(name, gender, birthday) },
            // 新设备可选：跳过建档直接加入已有家庭，拉回家人的宝宝/记录后自动进入主界面
            onJoinFamily = { rootVm.skipSetupForJoin.value = true }
        )

        RootState.JoinFamily -> {
            // 无宝宝也能进入家庭页：复用完整导航；加入并拉回 babies 后 state 自动变 Ready
            MainScaffold(
                pendingSheet = null,
                onSheetConsumed = {},
                pendingBackup = false,
                onBackupConsumed = {},
                pendingTab = "family",
                onTabConsumed = {}
            )
        }

        is RootState.Ready -> MainScaffold(
            pendingSheet = com.babyrecord.app.WidgetCommands.pendingSheet,
            onSheetConsumed = { com.babyrecord.app.WidgetCommands.pendingSheet = null },
            pendingBackup = com.babyrecord.app.WidgetCommands.pendingBackup,
            onBackupConsumed = { com.babyrecord.app.WidgetCommands.pendingBackup = false },
            pendingTab = com.babyrecord.app.WidgetCommands.pendingTab,
            onTabConsumed = { com.babyrecord.app.WidgetCommands.pendingTab = null }
        )
    }
}

@Composable
fun MainScaffold(
    pendingSheet: String? = null,
    onSheetConsumed: () -> Unit = {},
    pendingBackup: Boolean = false,
    onBackupConsumed: () -> Unit = {},
    pendingTab: String? = null,
    onTabConsumed: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as BabyApp
    val db = app.database
    // 与 AppRoot 同一 Activity 作用域：拿到同一个 RootViewModel 实例（头像/删除档案都走它）
    val rootVm: RootViewModel = viewModel { RootViewModel(app) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFeedingSheet by remember { mutableStateOf(false) }
    var showDiaperSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showSolidSheet by remember { mutableStateOf(false) }
    var showSupplementSheet by remember { mutableStateOf(false) }
    var showMedicineSheet by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDeleteBaby by remember { mutableStateOf(false) }

    // 首页头像点击 → 系统图片选择器（Photo Picker，仅图片）
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) rootVm.updateAvatar(uri) }

    // 桌面小组件深链：自动切到「今日」并弹出对应记录面板
    LaunchedEffect(pendingSheet) {
        when (pendingSheet) {
            "feeding" -> showFeedingSheet = true
            "diaper" -> showDiaperSheet = true
            "sleep" -> showSleepSheet = true
            "solid" -> showSolidSheet = true
            "supplement" -> showSupplementSheet = true
        }
        if (pendingSheet != null) {
            navController.navigate(Routes.HOME) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onSheetConsumed()
        }
    }
    LaunchedEffect(pendingBackup) {
        if (pendingBackup) {
            navController.navigate(Routes.BACKUP)
            onBackupConsumed()
        }
    }
    // 桌面统计组件深链：直接切到「记录」页
    LaunchedEffect(pendingTab) {
        if (pendingTab != null) {
            navController.navigate(pendingTab) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onTabConsumed()
        }
    }

    val tabs = listOf(
        TabItem(Routes.HOME, "首页", Icons.Rounded.Home),
        TabItem(Routes.TIMELINE, "记录", Icons.Rounded.History),
        TabItem(Routes.GROWTH, "成长", Icons.Rounded.ChildCare),
        TabItem(Routes.MORE, "更多", Icons.Rounded.MoreHoriz)
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            GlassNavBar(
                tabs = tabs,
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel { HomeViewModel(app) }
                val s by vm.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    state = s,
                    onAvatarClick = {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onFeeding = { showFeedingSheet = true },
                    onDiaper = { showDiaperSheet = true },
                    onSleep = { showSleepSheet = true },
                    onSolid = { showSolidSheet = true },
                    onSupplement = { showSupplementSheet = true },
                    onMedicine = { showMedicineSheet = true },
                    onOpenRecord = { navController.navigate(Routes.record(it.type.name, recordId(it))) },
                onOpenTimeline = {
                    navController.navigate(Routes.TIMELINE) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                )
            }
            composable(Routes.TIMELINE) {
                val vm: TimelineViewModel = viewModel { TimelineViewModel(app) }
                val s by vm.uiState.collectAsStateWithLifecycle()
                TimelineScreen(
                    state = s,
                    onSelectDay = { vm.selectDay(it) },
                    onMarkAwake = { vm.markAwake(it) },
                    onOpenRecord = { navController.navigate(Routes.record(it.type.name, recordId(it))) },
                    onOpenStats = { navController.navigate(Routes.STATS) }
                )
            }
            composable(Routes.GROWTH) {
                val vm: GrowthViewModel = viewModel { GrowthViewModel(app) }
                val avm: AlbumViewModel = viewModel { AlbumViewModel(app) }
                val s by vm.uiState.collectAsStateWithLifecycle()
                val photos by avm.photos.collectAsStateWithLifecycle()
                val birthday by avm.babyBirthday.collectAsStateWithLifecycle()
                var showMeasureSheet by remember { mutableStateOf(false) }
                GrowthScreen(
                    state = s,
                    photos = photos,                    birthday = birthday,

                    onAddMeasure = { showMeasureSheet = true },
                    onDeleteMeasure = { vm.deleteMeasurement(it) },
                    onOpenAlbum = { navController.navigate(Routes.ALBUM) }
                )
                MeasurementSheet(
                    visible = showMeasureSheet,
                    onDismiss = { showMeasureSheet = false },
                    onSave = { type, value, date ->
                        vm.addMeasurement(type, value, date)
                        showMeasureSheet = false
                    }
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onKnowledge = { navController.navigate(Routes.KNOWLEDGE) },
                    onVaccines = { navController.navigate(Routes.VACCINES) },
                    onBackup = { navController.navigate(Routes.BACKUP) },
                    onSleepMedia = { navController.navigate(Routes.SLEEP_MEDIA) },
                    onFamily = { navController.navigate(Routes.FAMILY) },
                    onSettings = { navController.navigate(Routes.MORE_SETTINGS) }
                )
            }
            composable(Routes.FAMILY) {
                FamilyScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MORE_SETTINGS) {
                MoreSettingsScreen(
                    onAppearance = { navController.navigate(Routes.APPEARANCE) },
                    onAbout = { showAbout = true },
                    onDeleteBaby = { showDeleteBaby = true },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.APPEARANCE) {
                AppearanceScreen(
                    current = ThemeSettings.mode,
                    onBack = { navController.popBackStack() },
                    onSelect = { ThemeSettings.set(app, it) }
                )
            }
            composable(Routes.BACKUP) {
                val vm: BackupViewModel = viewModel { BackupViewModel(app) }
                BackupScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(Routes.SLEEP_MEDIA) {
                val vm: SleepMediaViewModel = viewModel { SleepMediaViewModel(app) }
                SleepMediaScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(Routes.ALBUM) {
                val vm: AlbumViewModel = viewModel { AlbumViewModel(app) }
                val photos by vm.photos.collectAsStateWithLifecycle()
                val birthday by vm.babyBirthday.collectAsStateWithLifecycle()
                val daySummaries by vm.daySummaries.collectAsStateWithLifecycle()
                AlbumScreen(
                    photos = photos,
                    birthday = birthday,
                    daySummaries = daySummaries,
                    onBack = { navController.popBackStack() },
                    onAddPhotos = { vm.addPhotos(it) },
                    onEditPhoto = { vm.updatePhoto(it) },
                    onDeletePhoto = { vm.delete(it) },
                    onToggleFavorite = { vm.toggleFavorite(it) },
                    onRequestDaySummary = { vm.ensureDaySummary(it) }
                )
            }
            composable(Routes.KNOWLEDGE) {
                val vm: KnowledgeViewModel = viewModel { KnowledgeViewModel(app) }
                val months by vm.babyMonths.collectAsStateWithLifecycle()
                KnowledgeListScreen(
                    babyMonths = months,
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate("knowledge/${it.id}") }
                )
            }
            composable(
                Routes.KNOWLEDGE_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: 0
                KnowledgeData.byId(id)?.let { article ->
                    KnowledgeDetailScreen(article = article, onBack = { navController.popBackStack() })
                }
            }
            composable(Routes.VACCINES) {
                val vm: VaccineViewModel = viewModel { VaccineViewModel(app) }
                val s by vm.uiState.collectAsStateWithLifecycle()
                val reminder by vm.reminderEnabled.collectAsStateWithLifecycle()
                VaccineScreen(
                    state = s,
                    reminderEnabled = reminder,
                    onBack = { navController.popBackStack() },
                    onRecord = { def, date -> vm.recordVaccine(def, date) },
                    onDelete = { vm.deleteRecord(it) },
                    onToggleReminder = { vm.setReminderEnabled(it) }
                )
            }
            composable(Routes.STATS) {
                val vm: StatsViewModel = viewModel { StatsViewModel(app) }
                val s by vm.uiState.collectAsStateWithLifecycle()
                StatsScreen(state = s, onBack = { navController.popBackStack() })
            }
            composable(
                Routes.RECORD,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType }
                )
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: RecordType.FEEDING.name
                val id = entry.arguments?.getString("id") ?: ""
                val vm: RecordDetailViewModel = viewModel(key = "record_${type}_$id") {
                    RecordDetailViewModel(app, RecordType.valueOf(type), id)
                }
                val s by vm.state.collectAsStateWithLifecycle()
                RecordDetailScreen(
                    state = s,
                    onBack = { navController.popBackStack() },
                    onSaveFeeding = { vm.saveFeeding(it) },
                    onSaveDiaper = { vm.saveDiaper(it) },
                    onSaveSleep = { vm.saveSleep(it) },
                    onSaveSolid = { vm.saveSolid(it) },
                    onSaveSupplement = { vm.saveSupplement(it) },
                    onDelete = { vm.delete() }
                )
            }
        }
    }

    FeedingSheet(
        visible = showFeedingSheet,
        onDismiss = { showFeedingSheet = false },
        onSave = { type, amount, note, time ->
            scope.launch {
                db.feedingDao().insert(
                    FeedingEntity(type = type.name, amountMl = amount, timeEpochMillis = toMillis(time), note = note).withNewId()
                )
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(app)
                com.babyrecord.app.ui.backup.AutoSync.requestPush(app)
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(app)
                snackbarHostState.showSnackbar("已记录 ✓")
            }
            showFeedingSheet = false
        }
    )

    DiaperSheet(
        visible = showDiaperSheet,
        onDismiss = { showDiaperSheet = false },
        onSave = { state, note, time ->
            scope.launch {
                db.diaperDao().insert(DiaperEntity(state = state.name, timeEpochMillis = toMillis(time), note = note).withNewId())
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(app)
                com.babyrecord.app.ui.backup.AutoSync.requestPush(app)
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(app)
                snackbarHostState.showSnackbar("已记录 ✓")
            }
            showDiaperSheet = false
        }
    )

    SleepSheet(
        visible = showSleepSheet,
        onDismiss = { showSleepSheet = false },
        onSave = { start, note ->
            scope.launch {
                db.sleepDao().insert(
                    SleepEntity(startEpochMillis = toMillis(start), endEpochMillis = 0L, note = note).withNewId()
                )
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(app)
                com.babyrecord.app.ui.backup.AutoSync.requestPush(app)
                snackbarHostState.showSnackbar("宝宝开始睡觉啦 😴")
            }
            showSleepSheet = false
        }
    )

    SolidFoodSheet(
        visible = showSolidSheet,
        onDismiss = { showSolidSheet = false },
        onSave = { food, note, time ->
            scope.launch {
                db.solidFoodDao().insert(SolidFoodEntity(foodName = food, timeEpochMillis = toMillis(time), note = note).withNewId())
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(app)
                com.babyrecord.app.ui.backup.AutoSync.requestPush(app)
                snackbarHostState.showSnackbar("已记录 ✓")
            }
            showSolidSheet = false
        }
    )

    SupplementSheet(
        visible = showMedicineSheet,
        title = "记药品",
        nameHint = "如：退烧药 / 益生菌",
        onDismiss = { showMedicineSheet = false },
        onSave = { name, note, time ->
            scope.launch {
                db.supplementDao().insert(SupplementEntity(name = "[药] $name", timeEpochMillis = toMillis(time), note = note).withNewId())
                QuickStatsWidgetHelper.refresh(app)
                AutoSync.requestPush(app)
                snackbarHostState.showSnackbar("已记录 ✓")
            }
            showMedicineSheet = false
        }
    )
    SupplementSheet(
        visible = showSupplementSheet,
        onDismiss = { showSupplementSheet = false },
        onSave = { name, note, time ->
            scope.launch {
                db.supplementDao().insert(SupplementEntity(name = name, timeEpochMillis = toMillis(time), note = note).withNewId())
                com.babyrecord.app.widget.QuickStatsWidgetHelper.refresh(app)
                com.babyrecord.app.ui.backup.AutoSync.requestPush(app)
                snackbarHostState.showSnackbar("已记录 ✓")
            }
            showSupplementSheet = false
        }
    )

    // 删除宝宝档案确认弹窗：红色警示文案，明确告知影响范围（全设备移除）
    if (showDeleteBaby) {
        AlertDialog(
            onDismissRequest = { showDeleteBaby = false },
            title = { Text("删除宝宝档案", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "删除后所有设备的该宝宝档案都会移除，\n历史记录会保留但不再显示。\n\n此操作不可撤销，确定删除吗？",
                    fontSize = 13.sp, color = Color(0xFFD8402F), lineHeight = 21.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteBaby = false
                    rootVm.deleteBaby()
                }) { Text("删除", color = Color(0xFFD8402F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBaby = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showAbout) {
        BouncyDialog(
            visible = true,
            onDismiss = { showAbout = false },
            title = "关于软件",
            content = {
                Text(
                    "自制软件，用它陪伴宝宝的成长 🍼\n\n所有数据仅存本机\n无广告、无推荐、无跟踪",
                    fontSize = 13.sp, color = Color(0xFF8A7568), lineHeight = 21.sp
                )
            }
        )
    }
}
