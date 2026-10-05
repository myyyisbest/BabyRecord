package com.babyrecord.app.ui.album

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.PhotoEntity
import com.babyrecord.app.sync.PhotoTransfer
import com.babyrecord.app.ui.backup.AutoSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

fun albumPhotoFile(context: Context, photo: PhotoEntity): File =
    File(File(context.filesDir, "album"), photo.filePath)

/** 某自然日的三类记录汇总（照片详情底部「当天记录」用） */
data class DaySummary(val feeds: Int, val sleepsMin: Long, val diapers: Int)

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyApp).database
    private val dao = db.photoDao()

    val photos: StateFlow<List<PhotoEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 宝宝生日，用于照片的月龄标签 */
    val babyBirthday: StateFlow<LocalDate?> = (application as BabyApp).database.babyDao().observeBaby()
        .map { it?.let { b -> LocalDate.ofEpochDay(b.birthdayEpochDay) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 把系统相册选中的图片复制到应用私有目录并登记；拍摄时间优先读 EXIF。
     * 文件名用相册原始文件名（DISPLAY_NAME），查不到时退回 IMG_<时间戳>.jpg；
     * 同名冲突时循环加 _1/_2… 后缀。sha256 仍照常计算（同步传输/去重用），但不再用作文件名。
     */
    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val dir = File(app.filesDir, "album").apply { mkdirs() }
            val added = mutableListOf<PhotoEntity>()
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    runCatching {
                        val bytes: ByteArray? = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes == null || bytes.isEmpty()) return@runCatching
                        val sha = PhotoTransfer.sha256Bytes(bytes)
                        if (sha.isEmpty()) return@runCatching
                        val file = uniqueFile(dir, queryDisplayName(app, uri))
                        file.writeBytes(bytes)
                        added.add(
                            PhotoEntity(
                                filePath = file.name,
                                takenAt = readExifTime(file) ?: file.lastModified(),
                                sha256 = sha,
                                sizeBytes = bytes.size.toLong()
                            ).withNewId()
                        )
                    }
                }
            }
            added.forEach { dao.insert(it) }
            com.babyrecord.app.ui.backup.AutoSync.requestPush(app)
            // 照片通道直连：不等记录同步，立即唤醒照片后台上传循环
            (app as? BabyApp)?.syncEngine?.kickPhotoWork()
        }
    }

    /** 查询相册 Uri 的原始文件名（OpenableColumns.DISPLAY_NAME），
     *  查不到或非法时退回 IMG_<时间戳>.jpg */
    private fun queryDisplayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) {
                    c.getString(idx)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return "IMG_${System.currentTimeMillis()}.jpg"
    }

    /** 在目录下找不冲突的文件：原名已存在时循环加 _1/_2… 后缀直到不冲突 */
    private fun uniqueFile(dir: File, name: String): File {
        val safe = name.replace(Regex("[/\\\\\\n\r]"), "_").ifBlank { "IMG_${System.currentTimeMillis()}.jpg" }
        var f = File(dir, safe)
        if (!f.exists()) return f
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val ext = if (dot > 0) safe.substring(dot) else ""
        var i = 1
        do {
            f = File(dir, "${base}_${i}$ext")
            i++
        } while (f.exists())
        return f
    }

    /** 读取 EXIF 原始拍摄时间（yyyy:MM:dd HH:mm:ss），读不到返回 null */
    private fun readExifTime(file: File): Long? = runCatching {
        val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
        val value = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)
            ?: return null
        java.time.LocalDateTime.parse(
            value.trim(),
            java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
        ).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()

    /** 当天记录摘要缓存，key = "yyyy-MM-dd"（LocalDate.toString 正好是此格式） */
    val daySummaries = MutableStateFlow<Map<String, DaySummary>>(emptyMap())

    /** 查询某自然日（本地时区）的喂奶/睡眠/尿布汇总，按天缓存避免重复查库 */
    fun ensureDaySummary(date: LocalDate) {
        val key = date.toString() // yyyy-MM-dd
        if (daySummaries.value.containsKey(key)) return
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val feeds = db.feedingDao().observeBetween(from, to).first()
                .count { it.deletedAt == null }
            val sleeps = db.sleepDao().observeBetween(from, to).first()
                .filter { it.deletedAt == null }
                .sumOf { (it.endEpochMillis - it.startEpochMillis).coerceAtLeast(0) } / 60000
            val diapers = db.diaperDao().observeBetween(from, to).first()
                .count { it.deletedAt == null }
            // 只在缓存里仍缺失时写入，避免并发重复查询时后到的覆盖先到的
            daySummaries.compareAndSet(daySummaries.value, daySummaries.value + (key to DaySummary(feeds, sleepsMin = sleeps, diapers = diapers)))
        }
    }

    /** 切换精选（星标）标记：1↔0，标脏待推 */
    fun toggleFavorite(photo: PhotoEntity) {
        viewModelScope.launch {
            dao.update(photo.copy(favorite = if (photo.favorite == 1) 0 else 1).markDirty())
            AutoSync.requestPush(getApplication())
        }
    }

    fun updatePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            dao.update(photo.markDirty())
            com.babyrecord.app.ui.backup.AutoSync.requestPush(getApplication())
        }
    }

    /**
     * 删除改为软删：copy(deletedAt=now).markDirty() 后回写，随同步推送到其他设备。
     * 图片文件本体保留（其他设备若已同步元数据仍可对照），物理清理交给同步引擎的 purgeDeletedBefore。
     */
    fun delete(photo: PhotoEntity) {
        viewModelScope.launch {
            dao.update(photo.copy(deletedAt = System.currentTimeMillis()).markDirty())
            AutoSync.requestPush(getApplication())
        }
    }
}
