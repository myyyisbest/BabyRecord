package com.babyrecord.app.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.MessageDigest
import java.time.Duration

/**
 * 家庭共享相册：照片文件传输（第三阶段）。
 *
 * 与元数据同步（/sync/push|pull）独立的两条二进制通道：
 * - POST /photos/upload  —— Bearer 认证，body 为原图 bytes，header X-Sha256；服务端按 sha256
 *   内容寻址存储，已存在直接 200（幂等，重传无副作用）
 * - GET  /photos/file/<sha256>?token=<token> —— 返回二进制；query token 认证，
 *   方便图片加载器直接用 URL（AsyncImage/Coil 无法加请求头）
 *
 * 服务器地址/token 与 SyncApi 共用 SharedPreferences("sync")。
 * 所有网络/磁盘操作走 IO 调度器；失败一律静默（仅 Log），由 SyncEngine 下轮重试。
 */
object PhotoTransfer {
    private const val TAG = "PhotoTransfer"

    /** 图片较大，超时放宽到 60s（照片通常几 MB，局域网足够，公网/弱网也留余量） */
    private val client = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(60))
        .writeTimeout(Duration.ofSeconds(60))
        .build()

    private val IMAGE_MEDIA = "application/octet-stream".toMediaType()

    /** 计算文件 sha256（8KB 流式读取），返回 hex 小写；读失败返回空串 */
    fun sha256(file: File): String = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        // 注意：Kotlin 的 Byte 是有符号的，%02x 对负 byte 会输出超长串（ffffffffxx），
        // 可能产生 63/66 位非法哈希。必须先 & 0xFF 转无符号再格式化，固定 64 位。
        md.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }.getOrDefault("")

    /** 直接对内存 bytes 计算 sha256（hex 小写），供保存新照片时按内容命名文件用 */
    fun sha256Bytes(bytes: ByteArray): String = runCatching {
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }.getOrDefault("")

    /**
     * 上传照片文件：POST /photos/upload，body = 文件 bytes，header X-Sha256 + Bearer token。
     * 服务端按 sha256 内容寻址，重复上传幂等。网络异常返回 false 静默。
     */
    suspend fun upload(context: Context, file: File, shaPrecomputed: String? = null): Boolean = withContext(Dispatchers.IO) {
        val token = SyncApi.prefs(context).getString("token", null)
        if (token.isNullOrBlank()) return@withContext false
        runCatching {
            val sha = shaPrecomputed ?: sha256(file)
            // 幂等预检：服务端已有同指纹文件（Range 探测或直接 GET 头）则跳过传输，
            // 避免每轮同步把已传过的大文件重发一遍（服务端内容寻址永不变化）
            val exists = runCatching {
                client.newCall(
                    Request.Builder()
                        .url(SyncApi.serverUrl(context) + "/photos/file/" + sha + "?token=" + token)
                        .head()
                        .build()
                ).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            if (exists) return@withContext true

            val bytes = file.readBytes()   // 相册照片几 MB，一次读入内存可接受
            val req = Request.Builder()
                .url(SyncApi.serverUrl(context) + "/photos/upload")
                .header("Authorization", "Bearer $token")
                .header("X-Sha256", sha)
                .post(bytes.toRequestBody(IMAGE_MEDIA))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "照片上传失败 HTTP ${resp.code}: ${file.name}")
                    false
                } else true
            }
        }.getOrNull() ?: false
    }

    /**
     * 下载照片文件：GET /photos/file/<sha256>?token=...，落到 filesDir/album/<destName>。
     * 先写临时文件（.tmp 后缀）成功后原子重命名，避免半截文件被当成已下载。
     * 失败返回 null（并清理临时文件），调用方下轮再试。
     */
    suspend fun download(context: Context, sha256: String, destName: String): File? = withContext(Dispatchers.IO) {
        if (sha256.isBlank()) return@withContext null
        val dir = File(context.filesDir, "album").apply { mkdirs() }
        val dest = File(dir, destName)
        // 幂等检查加强版：同名文件存在且内容指纹匹配才算已下载；
        // 不匹配（不同照片同名，多设备各自命名冲突）则换名 <sha256>.jpg 落盘并返回新路径——
        // 否则这张照片的文件永远"存在但不是它"，UI 永远显示下载中。
        if (dest.exists()) {
            if (sha256(dest) == sha256) return@withContext dest
            val alt = File(dir, "$sha256.jpg")
            if (alt.exists()) return@withContext alt
            val got = fetchToFile(context, sha256, alt)
            return@withContext got ?: alt.takeIf { it.exists() }
        }
        fetchToFile(context, sha256, dest)
    }

    /** 实际网络下载到指定文件：先 .tmp 后原子重命名；失败清理临时文件返回 null */
    private fun fetchToFile(context: Context, sha256: String, dest: File): File? {
        val token = SyncApi.prefs(context).getString("token", null) ?: return null
        if (token.isBlank()) return null
        return runCatching {
            val url = SyncApi.serverUrl(context) + "/photos/file/" + sha256 + "?token=" + token
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "照片下载失败 HTTP ${resp.code}: $sha256")
                    return null
                }
                val body = resp.body ?: return null
                val tmp = File(dest.parentFile, dest.name + ".tmp")
                try {
                    body.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    if (tmp.length() == 0L) return null
                    if (!tmp.renameTo(dest)) return null
                    dest
                } finally {
                    if (tmp.exists()) tmp.delete()
                }
            }
        }.getOrNull()
    }
}
