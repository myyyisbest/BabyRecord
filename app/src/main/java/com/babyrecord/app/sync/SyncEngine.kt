package com.babyrecord.app.sync

import android.content.Context
import android.util.Log
import com.babyrecord.app.data.AppDatabase
import com.babyrecord.app.data.AuthResp
import com.babyrecord.app.data.PhotoEntity
import com.babyrecord.app.widget.QuickStatsWidgetHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 家庭同步引擎（第二阶段核心）。
 *
 * 职责：
 * 1. 创建/加入/退出家庭（凭据存 SharedPreferences("sync")）
 * 2. syncOnce：pushAll（9 表 pendingSyncList 分批 500 推送）+ pullAll（增量拉取 + continue 续拉 + LWW 合并）
 * 3. requestSync：去抖 3 秒触发 syncOnce（记录保存/WS 广播等都走这里）
 * 4. 维护 clockOffset（服务器时间 - 本地时间），供 markDirty 时间基准参考
 *
 * 失败策略：所有网络失败静默（仅 Log），绝不影响 UI；401 单独识别供设置页提示重新加入。
 */
class SyncEngine(private val context: Context) {
    companion object {
        private const val TAG = "SyncEngine"
        private const val PUSH_BATCH = 500          // 服务端单批上限 500 行，超限 400
        private const val DEBOUNCE_MS = 3_000L      // 记录保存后的去抖窗口
        private const val PULL_PAGE_LIMIT = 50      // 单次 syncOnce 最多续拉页数（防死循环）
        private const val PHOTO_PULL_LIMIT = 20      // 单次 syncOnce 最多补拉的照片文件数（并发下可放宽）
private const val PHOTO_CONCURRENCY = 3      // 照片上传/下载并发路数（局域网安全的轻并发）
    }

    val db: AppDatabase = AppDatabase.get(context)
    private val prefs = SyncApi.prefs(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()                     // 防并发重入
    private var debounceJob: Job? = null

    /** 上次同步完成时间（epochMillis），供设置页显示；从 prefs 恢复，重启不丢 */
    val lastSyncAt = MutableStateFlow(prefs.getLong("lastSyncAt", 0L))

    /** 同步是否正在进行（供设置页 loading 展示） */
    val syncing = MutableStateFlow(false)

    /** 最近一次 401（凭据失效），供设置页提示「请重新加入家庭」 */
    val unauthorized = MutableStateFlow(false)

    // ---- 凭据存取（SharedPreferences "sync"） ----

    fun token(): String? = prefs.getString("token", null)?.takeIf { it.isNotBlank() }
    fun familyId(): Long = prefs.getLong("familyId", 0L)
    fun memberId(): Long = prefs.getLong("memberId", 0L)
    fun inviteCode(): String = prefs.getString("inviteCode", "") ?: ""
    fun deviceName(): String = prefs.getString("deviceName", "") ?: ""
    fun lastPullSince(): Long = prefs.getLong("lastPullSince", 0L)
    fun lastPullId(): String = prefs.getString("lastPullId", null) ?: ""

    /** 是否已加入家庭（有 token 即视为已加入） */
    fun isJoined(): Boolean = token() != null

    fun setDeviceName(name: String) {
        prefs.edit().putString("deviceName", name.trim()).apply()
    }

    // ---- 创建 / 加入 / 退出 ----

    /** 创建家庭：保存凭据 → 全量 bootstrap 拉取 */
    suspend fun createFamily(familyName: String, deviceName: String): AuthResp {
        val resp = SyncApi.create(context, familyName, deviceName)
        saveAuth(resp, deviceName, resp.inviteCode ?: "")
        bootstrap()
        return resp
    }

    /** 凭邀请码加入家庭：保存凭据 → 只拉不推的全量拉取（避免把本机脏行静默推上去）。
     *  拉完后由 UI 层调 localPendingCount() 询问用户：上传合并 / 仅保留在本地。 */
    suspend fun joinFamily(inviteCode: String, deviceName: String): AuthResp {
        val resp = SyncApi.join(context, inviteCode.trim(), deviceName)
        saveAuth(resp, deviceName, "")
        SyncWs.start(context)
        // 🔧 P0 修复①：启动照片后台循环（原来 kick 信号发进 Channel 但循环从未启动，信号被丢）
        ensurePhotoLoop()
        // 🔧 P0 修复②：加入后的首次拉取必须切到引擎自身 scope——
        // 原来在 UI 调用方（Compose/ViewModel）的协程里直接执行，加入成功页面跳转时
        // 「coroutine scope left the composition」把 pullAll/mergeTable 掐死在半路，
        // 元数据合并不完、照片一张拉不到。引擎 scope 与页面生命周期无关，跳转/退出都不受影响。
        scope.launch { syncOncePullOnly() }
        return resp
    }

    private fun saveAuth(resp: AuthResp, deviceName: String, inviteCode: String) {
        prefs.edit()
            .putString("token", resp.token)
            .putLong("familyId", resp.familyId)
            .putLong("memberId", resp.memberId)
            .putString("inviteCode", inviteCode)
            .putString("deviceName", deviceName.trim())
            .putLong("lastPullSince", 0L)   // 新家庭从 0 开始增量拉取
            .putString("lastPullId", "")
            .putLong("clockOffset", resp.serverTime - System.currentTimeMillis())
            .apply()
        unauthorized.value = false
    }

    /** 新创建家庭后立即拉一次全量 + 启动 WS + 把本地存量脏数据推上去（新建家庭服务端无数据，推上去安全） */
    private fun bootstrap() {
        SyncWs.start(context)
        ensurePhotoLoop()
        requestSync()
    }

    /** 退出家庭：先尽力通知服务端（失败不阻塞），再清凭据，本地数据保留 */
    fun leave() {
        // 先调 DELETE /family/leave，runCatching 包裹：服务端不可达/401 也不卡本地清理
        scope.launch {
            runCatching { SyncApi.leave(context) }
        }
        prefs.edit()
            .remove("token").remove("familyId").remove("memberId")
            .remove("inviteCode").remove("deviceName")
            .remove("lastPullSince").remove("lastPullId").remove("clockOffset")
            .apply()
        unauthorized.value = false
        prefs.edit().putBoolean("unauthorized", false).apply()
        SyncWs.stop(context)
    }

    // ---- 触发入口 ----

    /** 外部直连照片通道：保存新照片后立即唤醒后台上传，不经过记录同步的去抖等待 */
    fun kickPhotoWork() {
        if (!isJoined()) return
        ensurePhotoLoop()
        photoWorkKick.trySend(Unit)
    }

    /** 去抖触发同步：记录保存/WS 广播等都调这里，3 秒内多次触发只执行一次。
     *  凭据已失效（401）时不再触发——避免每次保存记录都发起注定失败的同步，
     *  造成「同步中很久最后失败」的假象。用户在家庭页重新加入后恢复。 */
    fun requestSync(delayMs: Long = DEBOUNCE_MS) {
        if (!isJoined()) return
        // P2-1：401 状态持久化到 prefs（重启不丢），避免每次冷启动都发起注定失败的同步
        if (unauthorized.value || prefs.getBoolean("unauthorized", false)) {
            unauthorized.value = true
            return
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            syncOnce()
        }
    }

    /** App 启动/回前台时立即同步（带去抖防止连点） */
    private val resumedOnce = AtomicBoolean(false)
    fun onResume() {
        if (!isJoined()) return
        SyncWs.start(context)
        ensurePhotoLoop()
        requestSync(delayMs = if (resumedOnce.compareAndSet(false, true)) 500L else 0L)
    }

    /**
     * 完整同步一轮：pushAll → pullAll → 更新游标 → 清理已同步软删行。
     * Mutex 防重入；任何失败静默退出（下次触发再试）。
     * @return 成功返回 true；401 抛 UnauthorizedException 由调用方处理
     */
    suspend fun syncOnce(): Boolean {
        val tk = token() ?: return false
        if (!mutex.tryLock()) return false   // 已有一轮在进行，直接放弃（下次触发再补）
        syncing.value = true
        try {
            // ⚠️ 架构调整（1.0.2）：照片文件传输移出主同步路径，改由独立后台通道 photoWorkerLoop 处理。
            // 原因：照片（秒~分钟级）拖累记录同步（毫秒级），曾导致「一直同步中」且其他记录推不上去。
            pushAll(tk)
            pullAll(tk)
            // 通知照片通道：有新元数据/可能有新文件要传
            photoWorkKick.trySend(Unit)
            val now = System.currentTimeMillis()
            lastSyncAt.value = now
            // 同步落地了新数据（本机保存的或家人推送的）：桌面组件同步刷新，
            // 覆盖"另一台设备记录、本机组件没更新"的场景
            QuickStatsWidgetHelper.refresh(context)
            prefs.edit().putLong("lastSyncAt", now).apply()
            return true
        } catch (e: UnauthorizedException) {
            unauthorized.value = true
            prefs.edit().putBoolean("unauthorized", true).apply()   // P2-1：持久化，冷启动不再盲试
            // 断开 WS（token 无效连上也会被踢，白白重连）并停掉后续去抖任务
            SyncWs.stop(context)
            debounceJob?.cancel()
            Log.w(TAG, "凭据失效，请在设置中重新加入家庭")
        } catch (e: Exception) {
            Log.w(TAG, "同步失败（静默，待下次重试）: ${e.message}")
        } finally {
            syncing.value = false
            mutex.unlock()
        }
        return false
    }

    /** 本机待推脏行总数（9 张表 pendingSyncList 计数求和）：
     *  加入家庭后由 UI 询问用户「上传合并 / 仅保留在本地」用 */
    suspend fun localPendingCount(): Int =
        db.babyDao().pendingSyncList().size +
            db.feedingDao().pendingSyncList().size +
            db.sleepDao().pendingSyncList().size +
            db.diaperDao().pendingSyncList().size +
            db.solidFoodDao().pendingSyncList().size +
            db.supplementDao().pendingSyncList().size +
            db.measurementDao().pendingSyncList().size +
            db.vaccineDao().pendingSyncList().size +
            db.photoDao().pendingSyncList().size

    /** 把 9 张表的待推脏行全部回写 syncedAt=updatedAt（经 JSON 通道，同 withSyncedAt 模式），
     *  使其不再被推送但保留本地显示。「仅保留在本地」选项用。 */
    suspend fun markLocalPendingSynced() {
        suspend fun <T> markTable(pending: List<T>, codec: RowCodec<T>, upsertAll: suspend (List<T>) -> Unit) {
            if (pending.isEmpty()) return
            upsertAll(pending.map { withSyncedAt(it, codec, rowUpdatedAt(it, codec)) })
        }
        markTable(db.babyDao().pendingSyncList(), TableCodec.babies) { db.babyDao().upsertAll(it) }
        markTable(db.feedingDao().pendingSyncList(), TableCodec.feedings) { db.feedingDao().upsertAll(it) }
        markTable(db.sleepDao().pendingSyncList(), TableCodec.sleeps) { db.sleepDao().upsertAll(it) }
        markTable(db.diaperDao().pendingSyncList(), TableCodec.diapers) { db.diaperDao().upsertAll(it) }
        markTable(db.solidFoodDao().pendingSyncList(), TableCodec.solidFoods) { db.solidFoodDao().upsertAll(it) }
        markTable(db.supplementDao().pendingSyncList(), TableCodec.supplements) { db.supplementDao().upsertAll(it) }
        markTable(db.measurementDao().pendingSyncList(), TableCodec.measurements) { db.measurementDao().upsertAll(it) }
        markTable(db.vaccineDao().pendingSyncList(), TableCodec.vaccines) { db.vaccineDao().upsertAll(it) }
        markTable(db.photoDao().pendingSyncList(), TableCodec.photos) { db.photoDao().upsertAll(it) }
    }

    /** 从实体行提取 updatedAt（经 JSON 通道，markLocalPendingSynced 回写 syncedAt=updatedAt 用） */
    private fun <T> rowUpdatedAt(row: T, codec: RowCodec<T>): Long = codec.rowMeta(codec.toJson(row)).second

    /** 只拉不推的同步一轮：pullAll + downloadPhotoFiles + 更新游标。
     *  加入家庭时用：先把服务端现有数据拉下来，但不把本机脏行静默推上去（由用户在 UI 决定去留）。
     *  异常处理同 syncOnce（静默，401 单独标记）。 */
    private suspend fun syncOncePullOnly(): Boolean {
        val tk = token() ?: return false
        if (!mutex.tryLock()) return false
        syncing.value = true
        try {
            pullAll(tk)
            photoWorkKick.trySend(Unit)
            val now = System.currentTimeMillis()
            lastSyncAt.value = now
            QuickStatsWidgetHelper.refresh(context)
            prefs.edit().putLong("lastSyncAt", now).apply()
            return true
        } catch (e: UnauthorizedException) {
            unauthorized.value = true
            SyncWs.stop(context)
            debounceJob?.cancel()
            Log.w(TAG, "凭据失效，请在设置中重新加入家庭")
        } catch (e: Exception) {
            Log.w(TAG, "只拉同步失败（静默，待下次重试）: ${e.message}")
        } finally {
            syncing.value = false
            mutex.unlock()
        }
        return false
    }

    // ---- 推送 ----

    /** 遍历 9 张表：待推行（updatedAt > syncedAt）分批 500 条推送；成功行 syncedAt = 响应 serverTime */
    private suspend fun pushAll(token: String) {
        pushTable(token, "babies", TableCodec.babies, db.babyDao().pendingSyncList(), { db.babyDao().upsertAll(it) })
        pushTable(token, "feedings", TableCodec.feedings, db.feedingDao().pendingSyncList(), { db.feedingDao().upsertAll(it) })
        pushTable(token, "sleeps", TableCodec.sleeps, db.sleepDao().pendingSyncList(), { db.sleepDao().upsertAll(it) })
        pushTable(token, "diapers", TableCodec.diapers, db.diaperDao().pendingSyncList(), { db.diaperDao().upsertAll(it) })
        pushTable(token, "solid_foods", TableCodec.solidFoods, db.solidFoodDao().pendingSyncList(), { db.solidFoodDao().upsertAll(it) })
        pushTable(token, "supplements", TableCodec.supplements, db.supplementDao().pendingSyncList(), { db.supplementDao().upsertAll(it) })
        pushTable(token, "measurements", TableCodec.measurements, db.measurementDao().pendingSyncList(), { db.measurementDao().upsertAll(it) })
        pushTable(token, "vaccines", TableCodec.vaccines, db.vaccineDao().pendingSyncList(), { db.vaccineDao().upsertAll(it) })
        pushTable(token, "photos", TableCodec.photos, db.photoDao().pendingSyncList(), { db.photoDao().upsertAll(it) })
        // settings 纯 JSON 通道（无 DAO）：行.updatedAt > syncedAt_<key> 即待推，逐批推送
        pushSettings(token)
    }

    /** settings 表推送：读 SettingsSync 全部行，待推 = 行.updatedAt > syncedAt_<key>；
     *  成功后 syncedAt_<key> = serverTime（冲突败北也视为已同步，等 pull 覆盖，同 pushTable 语义） */
    private suspend fun pushSettings(token: String) {
        val pending = SettingsSync.readAll(context).mapNotNull { jo ->
            val (rid, rup) = TableCodec.settings.rowMeta(jo)
            if (rid.isBlank()) return@mapNotNull null
            val key = runCatching { jo["settingKey"]?.jsonPrimitive?.content }.getOrNull() ?: return@mapNotNull null
            // 待推判断：行.updatedAt > 本地确认时间 syncedAt_<key>
            if (rup > SettingsSync.syncedAt(context, key)) jo else null
        }
        if (pending.isEmpty()) return
        pending.chunked(PUSH_BATCH).forEach { batch ->
            val resp = SyncApi.push(context, token, SettingsSync.TABLE, batch)
            // accepted：已确认；conflict-lost：输给服务端新版本，也标已同步，等 pull 覆盖
            val okIds = resp.results.filter { it.status == "accepted" || it.status == "conflict-lost" }
                .map { it.id }.toSet()
            if (okIds.isNotEmpty()) {
                batch.forEach { jo ->
                    val (rid, _) = TableCodec.settings.rowMeta(jo)
                    val key = runCatching { jo["settingKey"]?.jsonPrimitive?.content }.getOrNull()
                    if (rid in okIds && !key.isNullOrBlank()) {
                        SettingsSync.markSynced(context, key, resp.serverTime)
                    }
                }
            }
        }
    }

    // ---- 照片文件传输（第三阶段） ----

    /**
     * 照片文件上传（性能修复版）：
     * 1. 用内存 set 记录本轮会话已上传成功的 sha256，避免同轮重复；
     * 2. sha256 已在实体上的行直接上传（不再先读全文件算哈希）；
     * 3. 并发 3 路上传，局域网下多张照片不再串行等待。
     * 上传成功回写 sha256/sizeBytes，保持 syncedAt=0（元数据待推，pushAll 才放行）。
     */
    private suspend fun uploadPhotoFiles() {
        runCatching {
            val dir = File(context.filesDir, "album")
            val uploadedThisSession = photoUploadedSet
            db.photoDao().pendingSyncList()
                .asSequence()
                .filter { it.deletedAt == null }
                .mapNotNull { photo ->
                    val file = File(dir, photo.filePath)
                    if (!file.exists()) return@mapNotNull null
                    val sha = photo.sha256.ifEmpty { PhotoTransfer.sha256(file) }
                    // P1-2：非法 sha（长度≠64/非hex）跳过——服务端必拒 400，无意义重试
                    if (!isValidSha(sha) || sha in uploadedThisSession) return@mapNotNull null
                    photo to Pair(file, sha)
                }
                .toList()
                .chunked(PHOTO_CONCURRENCY)
                .forEach { group ->
                    // 并发上传一组，全部完成后进入下一组
                    val jobs = group.map { (photo, fileSha) ->
                        scope.async {
                            val (file, sha) = fileSha
                            // P2-3：传入已算好的 sha，避免 upload 内部重新读全文件哈希
                            if (PhotoTransfer.upload(context, file, shaPrecomputed = sha)) {
                                uploadedThisSession.add(sha)
                                var dirty = false
                                if (photo.sha256.isEmpty()) {
                                    db.photoDao().update(
                                        photo.copy(sha256 = sha, sizeBytes = file.length())
                                    )
                                    dirty = true
                                }
                                // P1-1：上传成功后主动推元数据（原逻辑只等下一轮记录同步，
                                // 若那轮被广播打断，对端拿到元数据却拉不到文件）
                                if (dirty || photo.updatedAt > photo.syncedAt) {
                                    runCatching { pushAll(token() ?: return@async) }
                                }
                            }
                        }
                    }
                    jobs.forEach { it.await() }
                }
        }.onFailure { Log.w(TAG, "照片文件上传阶段失败（静默）: ${it.message}") }
    }

    /** 本进程内已成功上传的 sha256 集合：幂等缓存，避免每轮同步对已传照片做全文件哈希+重传 */
    private val photoUploadedSet = ConcurrentHashMap.newKeySet<String>()

    // ---- 照片独立后台通道（1.0.2）：与记录同步彻底解耦 ----
    /** 照片通道忙状态（UI 可选展示「照片同步中」，与 syncing 独立） */
    val photoBusy = MutableStateFlow(false)
    /** 唤醒照片通道的信号（记录同步完成后/照片保存后 kick 一下） */
    private val photoWorkKick = Channel<Unit>(capacity = Channel.CONFLATED)
    private var photoLoopStarted = false

    /** 启动照片后台循环：常驻协程，被 kick 后处理一轮上传+补拉，完成后休眠等下次 kick。
     *  单张失败不重试同轮（下轮再试）；异常全部隔离在此循环内，绝不影响记录同步主路径。 */
    private fun ensurePhotoLoop() {
        if (photoLoopStarted) return
        synchronized(this) {
            if (photoLoopStarted) return
            photoLoopStarted = true
            scope.launch {
                for (_kick in photoWorkKick) {
                    photoBusy.value = true
                    var remaining = 0
                    try {
                        runCatching { uploadPhotoFiles() }
                            .onFailure { Log.w(TAG, "照片上传轮失败（隔窗重试）: ${it.message}") }
                        runCatching { remaining = downloadPhotoFiles() }
                            .onFailure { Log.w(TAG, "照片补拉轮失败（隔窗重试）: ${it.message}") }
                        runCatching { ensureAvatarFile() }
                            .onFailure { Log.w(TAG, "头像补拉失败（隔窗重试）: ${it.message}") }
                    } finally {
                        photoBusy.value = false
                    }
                    // 还有未完成的照片（单轮限额未拉完/失败重试）：自我调度下一轮，
                    // 不等外部 kick——否则闲置设备要等到 WorkManager 15 分钟兜底才续拉
                    if (remaining > 0) {
                        delay(3000)
                        photoWorkKick.trySend(Unit)
                    }
                }
            }
        }
    }

    /**
     * 头像文件补拉：avatar 字段存的是 album 目录下的文件名（内容寻址 <sha>.jpg）。
     * 其他设备只拉到了 avatar 字段而没有文件时，这里从照片通道补拉同名文件；
     * 文件存在则跳过（幂等）。返回 true 表示本次补拉到了文件（可提示刷新 UI）。
     */
    private suspend fun ensureAvatarFile(): Boolean = runCatching {
        val baby = db.babyDao().observeBaby().firstOrNull() ?: return false
        if (baby.avatar.isBlank()) return false
        val dir = File(context.filesDir, "album").apply { mkdirs() }
        val dest = File(dir, baby.avatar)
        if (dest.exists()) return false   // 已有文件（含本机刚设置的）
        // sha = 文件名去扩展名；服务端按 sha 内容寻址存储；非法格式（非64位hex）不请求
        val sha = baby.avatar.substringBeforeLast('.')
        if (!isValidSha(sha)) return false
        val got = PhotoTransfer.download(context, sha, baby.avatar)
        got != null
    }.getOrDefault(false)

    /**
     * 照片文件补拉（修复版）：
     * - 查询改为 allWithSha()（全量未软删且有 sha 的行）——原来误用 pendingSyncList（待推列表），
     *   导致已同步完但缺文件的照片永远不会被补拉；
     * - 并发 3 路下载；单轮仍限 PHOTO_PULL_LIMIT 张，防止首轮大量照片长期占用同步。
     */
    /** 下载失败退避：同一 sha256 连续失败次数（内存即可，App 重启后重新尝试） */
    private val downloadFailCounts = ConcurrentHashMap<String, Int>()

    /** sha256 合法性：64 位小写 hex（服务端强校验，非法值上传/下载必拒 400） */
    private fun isValidSha(sha: String): Boolean =
        sha.length == 64 && sha.all { it in '0'..'9' || it in 'a'..'f' }

    /** @return 仍未下载完成的照片数（含本轮限额剩余与可重试的失败行），供照片循环自我调度。
     *  连续失败 3 次的 sha 本轮跳过（等冷启动/WS 新广播重置），非法 sha 永不入队，避免 404/400 风暴 */
    private suspend fun downloadPhotoFiles(): Int = runCatching {
        val dir = File(context.filesDir, "album")
        val missing = db.photoDao().allWithSha()
            .filter { isValidSha(it.sha256) }                                  // P1-2：非法 sha 不入队
            .filter { (downloadFailCounts[it.sha256] ?: 0) < 3 }              // P1-1：失败退避
            .filter { File(dir, it.filePath).exists().not() }
        if (missing.isEmpty()) return 0
        var downloaded = 0
        missing.take(PHOTO_PULL_LIMIT)
            .chunked(PHOTO_CONCURRENCY)
            .forEach { group ->
                val jobs = group.map { photo ->
                    scope.async {
                        val got = PhotoTransfer.download(context, photo.sha256, photo.filePath)
                        if (got != null) {
                            downloaded++
                            downloadFailCounts.remove(photo.sha256)
                            // 同名冲突时 download 会换名 <sha256>.jpg 落盘；回写数据库 filePath
                            if (got.name != photo.filePath) {
                                db.photoDao().update(photo.copy(filePath = got.name))
                            }
                        } else {
                            downloadFailCounts[photo.sha256] = (downloadFailCounts[photo.sha256] ?: 0) + 1
                        }
                    }
                }
                jobs.forEach { it.await() }
            }
        missing.size - downloaded
    }.getOrDefault(0)

    private suspend fun <T> pushTable(
        token: String,
        table: String,
        codec: RowCodec<T>,
        pending: List<T>,
        upsertAll: suspend (List<T>) -> Unit
    ) {
        if (pending.isEmpty()) return
        // photos 特例：sha256 为空说明文件还没上传到服务端，元数据先推过去别的设备也拉不到文件，
        // 暂时不推（继续 pending），等 uploadPhotoFiles 回写 sha256 后的下一批/下一轮再推
        val rows = pending
            .let { list -> if (table == "photos") list.filter { (it as PhotoEntity).sha256.isNotEmpty() } else list }
            .map { codec.toJson(it) }
        if (rows.isEmpty()) return
        // 分批 500 条（服务端单批上限）
        rows.chunked(PUSH_BATCH).forEach { batch ->
            val resp = SyncApi.push(context, token, table, batch)
            // accepted：已确认；conflict-lost：本地输给服务端新版本，也标记已同步，等 pull 覆盖
            val okIds = resp.results.filter { it.status == "accepted" || it.status == "conflict-lost" }.map { it.id }.toSet()
            if (okIds.isNotEmpty()) {
                val updated = pending.filter { rowId(it, codec) in okIds }
                    .map { withSyncedAt(it, codec, resp.serverTime) }
                upsertAll(updated)
            }
        }
    }

    /** 从实体行提取 id（经 JSON 通道，避免为每个实体写接口） */
    private fun <T> rowId(row: T, codec: RowCodec<T>): String = codec.rowMeta(codec.toJson(row)).first

    /** 实体行回写 syncedAt：不可变实体没有通用 setter，在 JSON 上补 syncedAt 后经 fromJson 还原 */
    private fun <T> withSyncedAt(row: T, codec: RowCodec<T>, syncedAt: Long): T {
        val jo = codec.toJson(row).toMutableMap()
        jo["syncedAt"] = JsonPrimitive(syncedAt)
        return codec.fromJson(JsonObject(jo))
    }

    // ---- 拉取 ----

    /**
     * 增量拉取：从 lastPullSince/lastPullId 起，处理 continue 分页续拉。
     * LWW 合并：服务端行 updatedAt >= 本地行 updatedAt 才覆盖，且本地脏行（updatedAt > syncedAt）跳过避免丢未推修改。
     * upsert 幂等（按 id REPLACE），serverTime 含 -2s 安全余量导致的重发行直接覆盖无副作用。
     */
    private suspend fun pullAll(token: String) {
        var since = lastPullSince()
        var id = lastPullId()
        var tables: String? = null
        var pages = 0
        var maxServerTime = since

        while (pages < PULL_PAGE_LIMIT) {
            pages++
            val resp = SyncApi.pull(context, token, since, id.ifBlank { null }, tables)
            if (resp.serverTime > maxServerTime) maxServerTime = resp.serverTime

            resp.tables.forEach { (table, rows) -> mergeTable(table, rows) }

            // continue 续拉：用游标里的 since/id 只拉指定表，直到不再出现
            val cursors = resp.continueCursors
            if (cursors.isNullOrEmpty()) break
            // 多表游标时按第一张表拉（服务端一页只会对一张表分页）；顺序拼接表名
            tables = cursors.keys.joinToString(",")
            since = cursors.values.minOf { it.since }
            id = cursors.values.map { it.id }.minOrNull() ?: ""
            if (id.isBlank()) id = ""
        }

        // 更新游标 + 时钟偏移
        prefs.edit()
            .putLong("lastPullSince", maxServerTime)
            .putString("lastPullId", "")
            .putLong("clockOffset", maxServerTime - System.currentTimeMillis())
            .apply()

        // 物理清理：已同步且早于当前服务端时间的软删行
        purgeDeleted(maxServerTime)
    }

    /** 单表合并：解码 → LWW 过滤 → upsertAll */
    private suspend fun mergeTable(table: String, rows: List<JsonElement>) {
        if (rows.isEmpty()) return
        runCatching {
            when (table) {
                "babies" -> merge(db.babyDao()::getByIds, db.babyDao()::upsertAll, TableCodec.babies, rows)
                "feedings" -> merge(db.feedingDao()::getByIds, db.feedingDao()::upsertAll, TableCodec.feedings, rows)
                "sleeps" -> merge(db.sleepDao()::getByIds, db.sleepDao()::upsertAll, TableCodec.sleeps, rows)
                "diapers" -> merge(db.diaperDao()::getByIds, db.diaperDao()::upsertAll, TableCodec.diapers, rows)
                "solid_foods" -> merge(db.solidFoodDao()::getByIds, db.solidFoodDao()::upsertAll, TableCodec.solidFoods, rows)
                "supplements" -> merge(db.supplementDao()::getByIds, db.supplementDao()::upsertAll, TableCodec.supplements, rows)
                "measurements" -> merge(db.measurementDao()::getByIds, db.measurementDao()::upsertAll, TableCodec.measurements, rows)
                "vaccines" -> merge(db.vaccineDao()::getByIds, db.vaccineDao()::upsertAll, TableCodec.vaccines, rows)
                "photos" -> merge(db.photoDao()::getByIds, db.photoDao()::upsertAll, TableCodec.photos, rows)
                // settings 旁路：无 DAO/LWW，直接落地到 prefs，收到即视为已同步（applyRows 内写 syncedAt）
                "settings" -> SettingsSync.applyRows(
                    context,
                    rows.mapNotNull { runCatching { it as? JsonObject }.getOrNull() }
                )
                else -> Log.w(TAG, "未知表名，跳过: $table")
            }
        }.onFailure { Log.w(TAG, "合并表 $table 失败: ${it.message}") }
    }

    /** 从任意行 JSON 提取 syncedAt（缺失/类型不符 → 0，即视为待推脏行） */
    private fun syncedAtOf(jo: JsonObject): Long =
        runCatching { jo["syncedAt"]?.jsonPrimitive?.longOrNull }.getOrNull() ?: 0L

    /** LWW 通用合并：本地不存在 / 服务端 updatedAt >= 本地 updatedAt 且本地非脏行 → 覆盖 */
    private suspend fun <T> merge(
        getByIds: suspend (List<String>) -> List<T>,
        upsertAll: suspend (List<T>) -> Unit,
        codec: RowCodec<T>,
        rows: List<JsonElement>
    ) {
        data class Parsed<T>(val entity: T, val id: String, val updatedAt: Long)

        val parsed = rows.mapNotNull { el ->
            runCatching {
                val jo = el.jsonObject
                val (rid, rup) = codec.rowMeta(jo)
                if (rid.isBlank()) null else Parsed(codec.fromJson(jo), rid, rup)
            }.getOrNull()
        }
        if (parsed.isEmpty()) return

        // 本地行的 (id → updatedAt)、(id → syncedAt)：同样经 JSON 通道提取
        val local = getByIds(parsed.map { it.id })
        val localUpdatedAt = HashMap<String, Long>(local.size)
        val localSyncedAt = HashMap<String, Long>(local.size)
        local.forEach { row ->
            val jo = codec.toJson(row)
            val (rid, rup) = codec.rowMeta(jo)
            localUpdatedAt[rid] = rup
            localSyncedAt[rid] = syncedAtOf(jo)
        }

        val winners = parsed.filter { p ->
            val lUp = localUpdatedAt[p.id]
            val lSynced = localSyncedAt[p.id] ?: 0L
            when {
                lUp == null -> true                                        // 本地不存在 → 直接插入
                lUp > lSynced -> false                                     // 本地脏行（有未推修改）→ 跳过避免丢
                p.updatedAt >= lUp -> true                                 // 服务端更新或相同 → 覆盖
                else -> false                                              // 本地已更新且已推 → 保留
            }
        }.map { it.entity }

        if (winners.isNotEmpty()) upsertAll(winners)
    }

    /** 物理清理已同步过的软删行（第一阶段 DAO 已提供） */
    private suspend fun purgeDeleted(before: Long) {
        runCatching {
            db.babyDao().purgeDeletedBefore(before)
            db.feedingDao().purgeDeletedBefore(before)
            db.sleepDao().purgeDeletedBefore(before)
            db.diaperDao().purgeDeletedBefore(before)
            db.solidFoodDao().purgeDeletedBefore(before)
            db.supplementDao().purgeDeletedBefore(before)
            db.measurementDao().purgeDeletedBefore(before)
            db.vaccineDao().purgeDeletedBefore(before)
            db.photoDao().purgeDeletedBefore(before)
        }.onFailure { Log.w(TAG, "purge 软删行失败: ${it.message}") }
    }
}
