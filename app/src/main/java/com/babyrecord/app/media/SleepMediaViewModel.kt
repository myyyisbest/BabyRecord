package com.babyrecord.app.media

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.babyrecord.app.sync.SettingsSync
import com.babyrecord.app.ui.backup.AutoSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URL
import java.util.concurrent.Future

enum class RepeatModeUi(val label: String) {
    OFF("顺序播放"), ALL("列表循环"), ONE("单曲循环")
}

class SleepMediaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)

    // ── 连接配置 ──
    val configSaved = MutableStateFlow(prefs.getString("media_url", "").isNullOrBlank().not())
    val cfgUrl = MutableStateFlow(prefs.getString("media_url", "") ?: "")
    val cfgUser = MutableStateFlow(prefs.getString("media_user", "") ?: "")
    val cfgPass = MutableStateFlow(prefs.getString("media_pass", "") ?: "")

    // 音乐 / 故事各自独立的文件夹（相对 WebDAV 根的路径）
    val musicDir = MutableStateFlow(prefs.getString("media_music_dir", "") ?: "")
    val storyDir = MutableStateFlow(prefs.getString("media_story_dir", "") ?: "")

    // ── 扫描结果 ──
    val tracksMusic = MutableStateFlow<List<MediaTrack>>(emptyList())
    val tracksStory = MutableStateFlow<List<MediaTrack>>(emptyList())
    val scanning = MutableStateFlow(false)
    val scanError = MutableStateFlow<String?>(null)

    val activeTab = MutableStateFlow(0) // 0=哄睡音乐 1=睡前故事

    // ── 播放状态 ──
    val isPlaying = MutableStateFlow(false)
    val position = MutableStateFlow(0L)
    val duration = MutableStateFlow(0L)
    val currentTitle = MutableStateFlow("")
    val currentIndex = MutableStateFlow(-1)
    val queueCount = MutableStateFlow(0)
    val queueTab = MutableStateFlow<Int?>(null) // 当前播放队列属于哪个页签
    val playerError = MutableStateFlow<String?>(null)
    val repeatUi = MutableStateFlow(RepeatModeUi.ALL)

    private var controller: MediaController? = null
    private var pollJob: Job? = null
    // 控制器代数：重建时+1，过期 future 完成后直接丢弃，避免拿到旧服务死连接
    private var controllerGeneration = 0
    private var pendingFuture: Future<MediaController>? = null

    // ── 文件夹选择器 ──
    val pickerPath = MutableStateFlow("")
    val pickerEntries = MutableStateFlow<List<DavEntry>>(emptyList())
    val pickerLoading = MutableStateFlow(false)

    init {
        buildController()
        // 家庭共享：本地从未配置过 NAS 时，自动采用家人同步来的配置（一处配置全家受益）。
        // 本地已配置过则尊重本地，不用家人配置覆盖（避免多套环境互相干扰）。
        if (!configSaved.value) {
            applyFamilyNasConfigIfAvailable()
        }
        // 补同步：本地已配置过但从未上行过（settings 通道晚于配置存在），启动时自动补推一次。
        // 判定：本地 media_url 非空 且 settings 通道里没有 sleepPlayer.nas 行。
        if (configSaved.value) {
            backfillNasConfigToSync()
        }
        if (configSaved.value && (musicDir.value.isBlank() || storyDir.value.isBlank())) {
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        autoDiscoverDirs()
                        rescanSync()
                    }
                }
            }
        }
    }

    /**
     * 从家庭同步的 settings（sleepPlayer.nas 行）恢复本地配置：
     * host/port/useTls → 拼 URL；user/pass 若已同步一并恢复。
     * 恢复成功后写入旧 prefs（media_url/media_user/media_pass），现有读取链路零改动。
     */
    private fun applyFamilyNasConfigIfAvailable() {
        val app = getApplication<Application>()
        runCatching {
            val row = SettingsSync.readAll(app)
                .firstOrNull { (it["settingKey"] as? JsonElement)?.jsonPrimitive?.content == "sleepPlayer.nas" } ?: return
            // kotlinx JsonObject 手动取原始值：数字/布尔经 jsonPrimitive.content 再转
            fun raw(key: String): String? = (row[key] as? JsonElement)?.jsonPrimitive?.content
            val host = raw("host") ?: ""
            if (host.isBlank()) return
            val port = raw("port")?.toIntOrNull() ?: 80
            val useTls = raw("useTls")?.toBooleanStrictOrNull() ?: false
            val scheme = if (useTls) "https" else "http"
            val url = "$scheme://$host" + if (port == 80 || port == 443) "" else ":$port"
            val user = raw("user") ?: ""
            val pass = raw("pass") ?: ""
            prefs.edit()
                .putString("media_url", url)
                .putString("media_user", user)
                .putString("media_pass", pass)
                .apply()
            cfgUrl.value = url; cfgUser.value = user; cfgPass.value = pass
            configSaved.value = true
        }
    }

    /** 把本地已有的 NAS 配置补写到 settings 同步通道（仅当通道里没有时，避免覆盖更新的行） */
    private fun backfillNasConfigToSync() {
        val app = getApplication<Application>()
        runCatching {
            val key = "sleepPlayer.nas"
            val existing = SettingsSync.readAll(app).any {
                (it["settingKey"] as? JsonElement)?.jsonPrimitive?.content == key
            }
            if (existing) return
            val url = cfgUrl.value.ifBlank { return }
            val u = URL(url.trim())
            SettingsSync.upsertRow(app, key, buildJsonObject {
                put("id", SettingsSync.rowId(app, key))
                put("settingKey", key)
                put("host", u.host)
                put("port", if (u.port == -1) u.defaultPort else u.port)
                put("useTls", url.trim().startsWith("https"))
                put("volume", 40)
                put("user", cfgUser.value); put("pass", cfgPass.value)
                put("updatedAt", System.currentTimeMillis())
            })
            AutoSync.requestPush(app)
        }
    }

    private fun buildController() {
        val ctx = getApplication<Application>()
        controllerGeneration++
        val myGen = controllerGeneration
        // 若存在未完成的旧连接，取消它（否则它完成后会覆盖 controller 为死连接）
        pendingFuture?.cancel(false)
        val future = MediaController.Builder(
            ctx,
            SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        ).buildAsync()
        pendingFuture = future
        future.addListener({
            runCatching {
                val c = future.get()
                if (myGen != controllerGeneration) {
                    // 已过期（期间又重建了）：直接释放，不赋值
                    c.release()
                    return@runCatching
                }
                controller = c
                c.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying.value = playing
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        // 缓冲/就绪切换时同步时长，重进 App 后进度条能立即拿到总长
                        duration.value = c.duration.coerceAtLeast(0)
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        currentTitle.value = mediaItem?.mediaMetadata?.title?.toString() ?: ""
                        currentIndex.value = c.currentMediaItemIndex
                        queueCount.value = c.mediaItemCount
                        // 交叉页签播放时，恢复队列归属页签，保证进度条/曲名实时显示
                        mediaItem?.let {
                            val artist = it.mediaMetadata?.artist?.toString()
                            queueTab.value = when (artist) {
                                "哄睡音乐" -> 0
                                "睡前故事" -> 1
                                else -> queueTab.value
                            }
                        }
                    }
                    override fun onRepeatModeChanged(repeatMode: Int) {
                        repeatUi.value = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> RepeatModeUi.ONE
                            Player.REPEAT_MODE_ALL -> RepeatModeUi.ALL
                            else -> RepeatModeUi.OFF
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playerError.value = error.errorCodeName + " " + (error.message ?: "")
                    }
                })
                // 重进 App：从存活的播放服务恢复全部状态（含播放中/进度/时长/队列归属）
                val item = c.currentMediaItem
                if (item != null) {
                    isPlaying.value = c.isPlaying
                    position.value = c.currentPosition.coerceAtLeast(0)
                    duration.value = c.duration.coerceAtLeast(0)
                    currentTitle.value = item.mediaMetadata?.title?.toString() ?: ""
                    val artist = item.mediaMetadata?.artist?.toString()
                    queueTab.value = when (artist) {
                        "哄睡音乐" -> 0
                        "睡前故事" -> 1
                        else -> queueTab.value
                    }
                }
                queueCount.value = c.mediaItemCount
                currentIndex.value = c.currentMediaItemIndex
                repeatUi.value = when (c.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatModeUi.ONE
                    Player.REPEAT_MODE_ALL -> RepeatModeUi.ALL
                    else -> RepeatModeUi.OFF
                }
                startPolling()
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    // 有队列时始终同步（含暂停态），重进 App 进度条不会卡在 0
                    if (c.mediaItemCount > 0) {
                        isPlaying.value = c.isPlaying
                        position.value = c.currentPosition.coerceAtLeast(0)
                        if (c.duration > 0) duration.value = c.duration
                    }
                }
                delay(500)
            }
        }
    }

    // ── 连接 ──
    fun saveConnection(url: String, user: String, pass: String) {
        prefs.edit()
            .putString("media_url", url.trim()).putString("media_user", user.trim())
            .putString("media_pass", pass.trim()).apply()
        cfgUrl.value = url.trim(); cfgUser.value = user.trim(); cfgPass.value = pass.trim()
        configSaved.value = url.trim().isNotBlank()

        // 关键：重启播放服务并重建控制器，让播放器拿到新的 WebDAV 凭证。
        // 顺序：先释放旧控制器→停服务→待服务真正停止后再重建，避免连到正在销毁的实例
        // 家庭同步联动（一期只上行）：把 NAS 连接信息以新格式（host/port/useTls/volume）
        // 写入 settings 同步通道（不建 Room 表，存 prefs "sync_settings"），
        // 供其他设备拉取；本页读取仍走旧 prefs("settings")，互不影响。
        // url 解析失败时静默跳过——同步是增强能力，绝不阻塞本地保存。
        val app = getApplication<Application>()
        runCatching {
            val u = URL(url.trim())
            val key = "sleepPlayer.nas"
            SettingsSync.upsertRow(app, key, buildJsonObject {
                put("id", SettingsSync.rowId(app, key))
                put("settingKey", key)
                put("host", u.host)
                put("port", if (u.port == -1) u.defaultPort else u.port)
                put("useTls", url.trim().startsWith("https"))
                put("volume", 40)   // 暂无音量设置，占位 40
                // WebDAV 凭证一并同步：家人设备自动应用时才有完整连接能力（内网家庭场景）
                put("user", user.trim()); put("pass", pass.trim())
                put("updatedAt", System.currentTimeMillis())
            })
            AutoSync.requestPush(app)   // 已联动家庭同步引擎（去抖 3s syncOnce）
        }
        val ctx = getApplication<Application>()
        controllerGeneration++ // 使未完成的旧 future 失效
        pendingFuture?.cancel(false)
        pollJob?.cancel()
        controller?.release()
        controller = null
        ctx.stopService(Intent(ctx, PlaybackService::class.java))
        viewModelScope.launch {
            delay(300) // 等待服务销毁完成
            buildController()
            runCatching {
                withContext(Dispatchers.IO) {
                    if (musicDir.value.isBlank() || storyDir.value.isBlank()) autoDiscoverDirs()
                    rescanSync()
                }
            }
        }
    }

    /** 自动识别：服务器根目录及其下一层里找含 baby_music/哄睡/宝宝成长记 的文件夹作为媒体根；音乐=根，故事=第一个子文件夹 */
    private suspend fun autoDiscoverDirs() {
        val dav = MediaDav(cfgUrl.value, cfgUser.value, cfgPass.value)
        val keywords = listOf("baby_music", "哄睡", "宝宝成长记")
        fun match(n: String) = keywords.any { n.contains(it, ignoreCase = true) }
        val root = dav.list("").getOrThrow()
        val direct = root.firstOrNull { it.isFolder && match(it.name) }?.name
            ?: root.filter { it.isFolder }.firstNotNullOfOrNull { f ->
                runCatching { dav.list(f.name).getOrThrow() }.getOrNull()
                    ?.firstOrNull { it.isFolder && match(it.name) }
                    ?.let { sub -> "${f.name}/${sub.name}" }
            }
        val mediaRoot = direct
            ?: root.firstOrNull { it.isFolder && it.name != "迅雷下载" }?.name
            ?: return
        val storySub = dav.list(mediaRoot).getOrNull()
            ?.firstOrNull { it.isFolder }?.let { "$mediaRoot/${it.name}" } ?: mediaRoot
        musicDir.value = mediaRoot
        storyDir.value = storySub
        prefs.edit().putString("media_music_dir", mediaRoot).putString("media_story_dir", storySub).apply()
    }

    // ── 扫描 ──
    fun rescan() {
        viewModelScope.launch {
            scanning.value = true
            scanError.value = null
            runCatching { withContext(Dispatchers.IO) { rescanSync() } }
                .onFailure { scanError.value = it.message ?: "扫描失败" }
            scanning.value = false
        }
    }

    private suspend fun rescanSync() {
        val dav = MediaDav(cfgUrl.value, cfgUser.value, cfgPass.value)
        tracksMusic.value = listAudio(dav, musicDir.value)
        tracksStory.value = listAudio(dav, storyDir.value)
    }

    private suspend fun listAudio(dav: MediaDav, dir: String): List<MediaTrack> {
        if (dir.isBlank()) return emptyList()
        return dav.list(dir).getOrThrow()
            .filter { !it.isFolder && MediaDav.isAudio(it.name) }
            .map { MediaTrack(it.name.removeSuffix(".mp3").removeSuffix(".m4a").removeSuffix(".flac"), trackUrl(it.href)) }
    }

    private fun trackUrl(href: String): String {
        val base = cfgUrl.value.trimEnd('/')
        val clean = if (href.startsWith("/")) href else "/$href"
        return base + clean
    }

    fun selectTab(tab: Int) { activeTab.value = tab }

    fun setMusicDir(path: String) {
        musicDir.value = path.trim('/')
        prefs.edit().putString("media_music_dir", musicDir.value).apply()
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { tracksMusic.value = listAudio(MediaDav(cfgUrl.value, cfgUser.value, cfgPass.value), musicDir.value) } }
        }
    }

    fun setStoryDir(path: String) {
        storyDir.value = path.trim('/')
        prefs.edit().putString("media_story_dir", storyDir.value).apply()
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { tracksStory.value = listAudio(MediaDav(cfgUrl.value, cfgUser.value, cfgPass.value), storyDir.value) } }
        }
    }

    // ── 文件夹选择器 ──
    fun openPicker() {
        pickerPath.value = ""
        loadPicker("")
    }

    fun loadPicker(path: String) {
        viewModelScope.launch {
            pickerLoading.value = true
            runCatching {
                withContext(Dispatchers.IO) {
                    MediaDav(cfgUrl.value, cfgUser.value, cfgPass.value).list(path).getOrThrow()
                        .filter { it.isFolder }
                }
            }.onSuccess { pickerEntries.value = it }
                .onFailure { pickerEntries.value = emptyList() }
            pickerLoading.value = false
        }
    }

    fun pickerDive(name: String) {
        val p = listOf(pickerPath.value.trim('/'), name).filter { it.isNotEmpty() }.joinToString("/")
        pickerPath.value = p
        loadPicker(p)
    }

    fun pickerUp() {
        val parts = pickerPath.value.trim('/').split('/').filter { it.isNotEmpty() }
        val p = parts.dropLast(1).joinToString("/")
        pickerPath.value = p
        loadPicker(p)
    }

    // ── 播放 ──
    private fun currentList(): List<MediaTrack> =
        if (activeTab.value == 0) tracksMusic.value else tracksStory.value

    fun playTrack(index: Int) {
        val c = controller ?: return
        val list = currentList()
        if (list.isEmpty()) return
        val label = if (activeTab.value == 0) "哄睡音乐" else "睡前故事"
        val items = list.map {
            MediaItem.Builder()
                .setUri(it.url)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).setArtist(label).build())
                .build()
        }
        playerError.value = null
        c.setMediaItems(items, index.coerceIn(0, items.size - 1), 0)
        c.prepare()
        c.play()
        queueTab.value = activeTab.value
        currentIndex.value = index
        currentTitle.value = list[index].title
        queueCount.value = items.size
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.mediaItemCount == 0 && currentList().isNotEmpty()) playTrack(0) else c.play()
        }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun prev() { controller?.seekToPreviousMediaItem() }
    fun seekToFraction(fraction: Float) { controller?.seekTo((duration.value * fraction).toLong()) }

    fun cycleRepeat() {
        val c = controller ?: return
        val nextMode = when (repeatUi.value) {
            RepeatModeUi.OFF -> RepeatModeUi.ALL to Player.REPEAT_MODE_ALL
            RepeatModeUi.ALL -> RepeatModeUi.ONE to Player.REPEAT_MODE_ONE
            RepeatModeUi.ONE -> RepeatModeUi.OFF to Player.REPEAT_MODE_OFF
        }
        c.repeatMode = nextMode.second
        repeatUi.value = nextMode.first
    }

    fun labelForTab(tab: Int) = if (tab == 0) "哄睡音乐" else "睡前故事"

    override fun onCleared() {
        pollJob?.cancel()
        controller?.release()
        controller = null
        super.onCleared()
    }
}
