package com.babyrecord.app.ui.backup

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class WebdavConfig(val url: String, val user: String, val pass: String)
data class S3Config(val endpoint: String, val region: String, val bucket: String, val access: String, val secret: String)

/** 极简 AWS SigV4（PUT 上传 / GET 下载），适用于 S3 及兼容存储 */
object S3SigV4 {
    private fun hmac(key: ByteArray, data: String): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data.toByteArray(Charsets.UTF_8))

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun sha256Hex(data: String): String =
        hex(MessageDigest.getInstance("SHA-256").digest(data.toByteArray(Charsets.UTF_8)))

    private fun sha256HexFile(file: File): String =
        hex(MessageDigest.getInstance("SHA-256").digest(file.readBytes()))

    private fun client() = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    fun put(endpoint: String, region: String, bucket: String, access: String, secret: String, key: String, file: File): Boolean =
        request("PUT", endpoint, region, bucket, access, secret, key, file)?.use { it.isSuccessful } ?: false

    fun get(endpoint: String, region: String, bucket: String, access: String, secret: String, key: String, dest: File): Boolean =
        request("GET", endpoint, region, bucket, access, secret, key, null)?.use { resp ->
            if (!resp.isSuccessful) return false
            resp.body!!.byteStream().use { input -> FileOutputStream(dest).use { input.copyTo(it) } }
            true
        } ?: false

    private fun request(
        method: String, endpoint: String, region: String, bucket: String,
        access: String, secret: String, key: String, file: File?
    ) = runCatching {
        val base = URL(endpoint)
        val port = if (base.port != -1 && base.port != base.defaultPort) ":${base.port}" else ""
        val host = base.host + port
        val path = "/" + bucket.trim('/') + "/" + key
        val payloadHash = if (file != null) sha256HexFile(file) else sha256Hex("")
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        val dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
        val canonicalRequest = "$method\n$path\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val scope = "$dateStamp/$region/s3/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$scope\n${sha256Hex(canonicalRequest)}"

        var signingKey = hmac(("AWS4$secret").toByteArray(Charsets.UTF_8), dateStamp)
        signingKey = hmac(signingKey, region)
        signingKey = hmac(signingKey, "s3")
        signingKey = hmac(signingKey, "aws4_request")
        val signature = hex(hmac(signingKey, stringToSign))

        val url = "${base.protocol}://$host$path"
        val builder = Request.Builder().url(url)
        if (method == "PUT") {
            builder.put(file!!.asRequestBody("application/octet-stream".toMediaType()))
        } else {
            builder.get()
        }
        builder
            .header("x-amz-content-sha256", payloadHash)
            .header("x-amz-date", amzDate)
            .header(
                "Authorization",
                "AWS4-HMAC-SHA256 Credential=$access/$scope, SignedHeaders=$signedHeaders, Signature=$signature"
            )
        client().newCall(builder.build()).execute()
    }.getOrNull()
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("backup", android.content.Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    val app = getApplication<Application>()

    fun savedWebdav(): WebdavConfig {
        val url = prefs.getString("webdav_url", "") ?: ""
        val user = prefs.getString("webdav_user", "") ?: ""
        val pass = prefs.getString("webdav_pass", "") ?: ""
        if (url.isNotBlank()) return WebdavConfig(url, user, pass)
        // 未单独配置备份时，复用哄睡播放里已授权的媒体 WebDAV 账号
        val mediaUrl = prefs.getString("media_url", "") ?: ""
        if (mediaUrl.isNotBlank()) {
            return WebdavConfig(
                mediaUrl,
                prefs.getString("media_user", "") ?: "",
                prefs.getString("media_pass", "") ?: ""
            )
        }
        return WebdavConfig("", "", "")
    }

    fun savedS3(): S3Config {
        val endpoint = prefs.getString("s3_endpoint", "") ?: ""
        if (endpoint.isNotBlank()) {
            return S3Config(
                endpoint,
                prefs.getString("s3_region", "") ?: "",
                prefs.getString("s3_bucket", "") ?: "",
                prefs.getString("s3_access", "") ?: "",
                prefs.getString("s3_secret", "") ?: ""
            )
        }
        return S3Config("", "", "", "", "")
    }

    fun lastWebdavSync(): Long = prefs.getLong("webdav_last", 0L)
    fun lastS3Sync(): Long = prefs.getLong("s3_last", 0L)

    val autosyncEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean("s3_autosync", false)
    )

    fun setAutosync(on: Boolean) {
        prefs.edit().putBoolean("s3_autosync", on).apply()
        autosyncEnabled.value = on
    }

    /** 拼接 WebDAV 目标地址：中文/特殊字符逐段编码（OkHttp 不接受未编码的中文 URL，这是之前同步失败的根因） */
    private fun webdavUrl(base: String, fileName: String): String {
        val uri = android.net.Uri.parse(base.trim())
        val segments = base.trim('/').split('/').filter { it.isNotEmpty() } + fileName
        val encoded = "/" + segments.joinToString("/") { android.net.Uri.encode(it) }
        val port = if (uri.port != -1) ":${uri.port}" else if (uri.scheme == "https") ":443" else ":80"
        return "${uri.scheme}://${uri.host}$port$encoded"
    }

    private fun saveWebdav(url: String, user: String, pass: String) {
        prefs.edit().putString("webdav_url", url).putString("webdav_user", user).putString("webdav_pass", pass).apply()
    }

    private fun saveS3(endpoint: String, region: String, bucket: String, access: String, secret: String) {
        prefs.edit().putString("s3_endpoint", endpoint).putString("s3_region", region)
            .putString("s3_bucket", bucket).putString("s3_access", access).putString("s3_secret", secret).apply()
    }

    private fun prepareBackupZip(): File = buildBackupZip(app)

    private fun restoreFromZip(zip: File): Boolean = restoreBackupZip(app, zip)

    private fun restartApp() {
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) app.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    fun backupWebdav(url: String, user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        if (url.isBlank()) { onResult(false, "请先填写服务器地址"); return }
        saveWebdav(url, user, pass)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val zip = buildBackupZip(app)
                    val target = webdavUrl(url, BACKUP_ZIP_NAME)
                    client.newCall(
                        Request.Builder()
                            .url(target)
                            .put(zip.asRequestBody("application/zip".toMediaType()))
                            .apply { if (user.isNotEmpty()) header("Authorization", Credentials.basic(user, pass)) }
                            .build()
                    ).execute().use { it.isSuccessful }
                }
            }
            result.onSuccess { ok ->
                if (ok) prefs.edit().putLong("webdav_last", System.currentTimeMillis()).apply()
                onResult(ok, if (ok) "同步成功 ✓" else "同步失败：服务器拒绝了请求（检查目录是否存在、账号是否有写权限）")
            }.onFailure {
                onResult(false, "同步失败：${it.message}")
            }
        }
    }

    fun backupS3(config: S3Config, onResult: (Boolean, String) -> Unit) {
        if (config.endpoint.isBlank() || config.bucket.isBlank() || config.access.isBlank()) {
            onResult(false, "请先完整填写 Endpoint / Bucket / AccessKey")
            return
        }
        saveS3(config.endpoint, config.region, config.bucket, config.access, config.secret)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val zip = buildBackupZip(app)
                    S3SigV4.put(config.endpoint, config.region.ifBlank { "us-east-1" }, config.bucket, config.access, config.secret, BACKUP_KEY, zip)
                }
            }
            val ok = result.getOrDefault(false)
            if (ok) prefs.edit().putLong("s3_last", System.currentTimeMillis()).apply()
            onResult(ok, if (ok) "同步成功 ✓" else "同步失败，请检查设置和网络")
        }
    }

    fun restoreWebdav(url: String, user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        if (url.isBlank()) { onResult(false, "请先填写服务器地址"); return }
        saveWebdav(url, user, pass)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val target = webdavUrl(url, BACKUP_ZIP_NAME)
                    val request = Request.Builder().url(target).get()
                        .apply { if (user.isNotEmpty()) header("Authorization", Credentials.basic(user, pass)) }
                        .build()
                    client.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            if (resp.code == 404) return@runCatching false
                            error("HTTP ${resp.code}")
                        }
                        val zipFile = File(app.cacheDir, RESTORE_ZIP_NAME)
                        resp.body!!.byteStream().use { input -> FileOutputStream(zipFile).use { input.copyTo(it) } }
                        restoreFromZip(zipFile)
                    }
                }
            }
            result.onSuccess { ok ->
                if (ok) {
                    onResult(true, "恢复成功，应用即将重启")
                    delay(800)
                    restartApp()
                } else {
                    onResult(false, "恢复失败：云端没有找到备份文件")
                }
            }.onFailure {
                onResult(false, "恢复失败：${it.message}")
            }
        }
    }

    fun restoreS3(config: S3Config, onResult: (Boolean, String) -> Unit) {
        if (config.endpoint.isBlank() || config.bucket.isBlank() || config.access.isBlank()) {
            onResult(false, "请先完整填写 Endpoint / Bucket / AccessKey")
            return
        }
        saveS3(config.endpoint, config.region, config.bucket, config.access, config.secret)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val zipFile = File(app.cacheDir, RESTORE_ZIP_NAME)
                    val got = S3SigV4.get(config.endpoint, config.region.ifBlank { "us-east-1" }, config.bucket, config.access, config.secret, BACKUP_KEY, zipFile)
                    if (got) restoreFromZip(zipFile) else false
                }
            }
            val ok = result.getOrDefault(false)
            if (ok) {
                onResult(true, "恢复成功，应用即将重启")
                delay(800)
                restartApp()
            } else {
                onResult(false, "恢复失败：云端没有找到备份文件")
            }
        }
    }

    companion object {
        const val BACKUP_ZIP_NAME = "baby_record_backup.zip"
        const val RESTORE_ZIP_NAME = "baby_record_restore.zip"
        const val BACKUP_KEY = "backup/baby_record_backup.zip"
        const val META_KEY = "backup/meta.json"

        /** 生成完整备份包：数据库快照 + 相册全部图片 */
        fun buildBackupZip(context: Context): File {
            val app = context.applicationContext
            val database = (app as BabyApp).database
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val dbFile = app.getDatabasePath(AppDatabase.DB_NAME)
            val zip = File(app.cacheDir, BACKUP_ZIP_NAME)
            ZipOutputStream(FileOutputStream(zip)).use { zos ->
                fun put(file: File, entryName: String) {
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                put(dbFile, AppDatabase.DB_NAME)
                File(app.filesDir, "album").listFiles()?.filter { it.isFile }?.forEach {
                    put(it, "photos/${it.name}")
                }
            }
            return zip
        }

        /** 解压备份包并覆盖本地数据库与相册 */
        fun restoreBackupZip(context: Context, zip: File): Boolean {
            val app = context.applicationContext
            val tmp = File(app.cacheDir, "restore_tmp")
            tmp.deleteRecursively()
            tmp.mkdirs()
            var dbFile: File? = null
            val photos = mutableListOf<File>()
            ZipInputStream(FileInputStream(zip)).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val name = entry.name
                    val out: File? = when {
                        name == AppDatabase.DB_NAME -> File(tmp, "db").also { dbFile = it }
                        name.startsWith("photos/") && !name.contains("..") -> File(tmp, name).also { photos.add(it) }
                        else -> null
                    }
                    if (out != null) {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                }
            }
            val db = dbFile ?: return false

            AppDatabase.closeInstance()
            val target = app.getDatabasePath(AppDatabase.DB_NAME)
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            db.copyTo(target, overwrite = true)
            val album = File(app.filesDir, "album")
            album.deleteRecursively()
            album.mkdirs()
            photos.forEach { it.copyTo(File(album, it.name), overwrite = true) }
            tmp.deleteRecursively()
            return true
        }
    }
}

/** 云端备份指纹：用于多设备判断"云端是否有别人更新的数据" */
data class MetaInfo(val time: Long, val device: String)

object AutoSync {

    data class S3Conf(val endpoint: String, val region: String, val bucket: String, val access: String, val secret: String)

    private fun readS3(context: Context): S3Conf? {
        val p = context.getSharedPreferences("backup", Context.MODE_PRIVATE)
        val endpoint = p.getString("s3_endpoint", "") ?: ""
        if (endpoint.isBlank()) return null
        return S3Conf(
            endpoint,
            p.getString("s3_region", "") ?: "",
            p.getString("s3_bucket", "") ?: "",
            p.getString("s3_access", "") ?: "",
            p.getString("s3_secret", "") ?: ""
        )
    }

    fun deviceId(context: Context): String {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        var id = p.getString("device_id", null)
        if (id == null) {
            id = android.os.Build.MODEL.take(12) + "-" + java.util.UUID.randomUUID().toString().take(8)
            p.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun autosyncEnabled(context: Context): Boolean =
        context.getSharedPreferences("backup", Context.MODE_PRIVATE).getBoolean("s3_autosync", false)

    private fun writeMeta(context: Context, time: Long): File {
        val device = deviceId(context)
        val f = File(context.cacheDir, "meta.json")
        f.writeText("""{"t":$time,"d":"$device"}""")
        return f
    }

    fun readMeta(context: Context, cfg: S3Conf): MetaInfo? {
        val tmp = File(context.cacheDir, "meta_read.json")
        val region = cfg.region.ifBlank { "us-east-1" }
        val got = S3SigV4.get(cfg.endpoint, region, cfg.bucket, cfg.access, cfg.secret, BackupViewModel.META_KEY, tmp)
        if (!got) return null
        val text = tmp.readText()
        val t = Regex("\"t\":(\\d+)").find(text)?.groupValues?.get(1)?.toLong() ?: return null
        val d = Regex("\"d\":\"([^\"]*)\"").find(text)?.groupValues?.get(1) ?: ""
        return MetaInfo(t, d)
    }

    private suspend fun pushS3Now(context: Context, cfg: S3Conf): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val zip = BackupViewModel.buildBackupZip(context)
            val region = cfg.region.ifBlank { "us-east-1" }
            val ok = S3SigV4.put(cfg.endpoint, region, cfg.bucket, cfg.access, cfg.secret, BackupViewModel.BACKUP_KEY, zip)
            if (ok) {
                val meta = writeMeta(context, System.currentTimeMillis())
                S3SigV4.put(cfg.endpoint, region, cfg.bucket, cfg.access, cfg.secret, BackupViewModel.META_KEY, meta)
            }
            ok
        }.getOrDefault(false)
    }

    private val pushScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var pushJob: kotlinx.coroutines.Job? = null

    /**
     * 保存任意记录/照片后调用：
     * 1. 家庭同步引擎（若已加入家庭）：去抖 3 秒后 syncOnce（与 S3 自动备份互不影响）
     * 2. 开启 S3 自动同步时：防抖 8 秒后自动推送备份到 S3
     */
    fun requestPush(context: Context) {
        // 家庭同步：统一在此联动，所有调用点（AppRoot 6 处/相册/详情页/疫苗/生长等）自动覆盖
        (context.applicationContext as? BabyApp)?.syncEngine?.requestSync()
        if (!autosyncEnabled(context)) return
        val cfg = readS3(context) ?: return
        pushJob?.cancel()
        pushJob = pushScope.launch {
            delay(8000)
            val time = System.currentTimeMillis()
            val ok = pushS3Now(context, cfg)
            if (ok) {
                context.getSharedPreferences("backup", Context.MODE_PRIVATE)
                    .edit().putLong("s3_last", time).putLong("s3_seen_t", time).apply()
            }
        }
    }

    /** 检查云端是否有"其他设备"更新的数据；有则返回 MetaInfo（调用方发通知提醒恢复） */
    suspend fun checkCloudNewer(context: Context): MetaInfo? {
        if (!autosyncEnabled(context)) return null
        val cfg = readS3(context) ?: return null
        if (cfg.endpoint.isBlank() || cfg.bucket.isBlank() || cfg.access.isBlank()) return null
        val meta = withContext(Dispatchers.IO) { readMeta(context, cfg) } ?: return null
        val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)
        val seen = prefs.getLong("s3_seen_t", 0L)
        val mine = meta.device == deviceId(context)
        if (meta.time > seen && !mine) return meta
        return null
    }

    fun markCloudSeen(context: Context, time: Long) {
        context.getSharedPreferences("backup", Context.MODE_PRIVATE)
            .edit().putLong("s3_seen_t", time).apply()
    }

    /** 自动恢复：下载并覆盖本机（仅供确认后的恢复流程调用） */
    suspend fun restoreNow(context: Context, cfg: S3Conf): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val region = cfg.region.ifBlank { "us-east-1" }
            val zipFile = File(context.cacheDir, BackupViewModel.RESTORE_ZIP_NAME)
            val got = S3SigV4.get(cfg.endpoint, region, cfg.bucket, cfg.access, cfg.secret, BackupViewModel.BACKUP_KEY, zipFile)
            if (got) BackupViewModel.restoreBackupZip(context, zipFile) else false
        }.getOrDefault(false)
    }
}
