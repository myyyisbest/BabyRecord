package com.babyrecord.app.media

import android.net.Uri
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

data class DavEntry(val name: String, val isFolder: Boolean, val href: String)

data class MediaTrack(val title: String, val url: String)

/** 极简 WebDAV 客户端：只需要列目录（PROPFIND） */
class MediaDav(private val baseUrl: String, private val user: String, private val pass: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun authHeader() = if (user.isNotEmpty()) Credentials.basic(user, pass) else null

    /** path 形如 "SSD1_400G/baby_music_stories"；返回该目录下的条目（不含自身） */
    suspend fun list(path: String): Result<List<DavEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = Uri.parse(baseUrl)
            val encoded = "/" + path.trim('/').split('/')
                .filter { it.isNotEmpty() }
                .joinToString("/") { Uri.encode(it) } + "/"
            val full = "${base.scheme}://${base.host}:${if (base.port != -1) base.port else if (base.scheme == "https") 443 else 80}$encoded"
            val builder = Request.Builder().url(full).header("Depth", "1").method("PROPFIND", null)
            authHeader()?.let { builder.header("Authorization", it) }
            client.newCall(builder.build()).execute().use { resp ->
                check(resp.isSuccessful) { "列目录失败 (HTTP ${resp.code})" }
                parse(resp.body!!.byteStream(), path.trim('/'))
            }
        }
    }

    private fun parse(input: InputStream, requestPath: String): List<DavEntry> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)
        val out = mutableListOf<DavEntry>()
        var name = ""
        var isFolder = false
        var href = ""
        // 关闭命名空间时 name 带 "D:" 前缀，统一去掉
        fun tag() = parser.name.substringAfter(':').lowercase()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (tag()) {
                    "response" -> { name = ""; isFolder = false; href = "" }
                    "displayname" -> name = parser.nextText().trim()
                    "collection" -> isFolder = true
                    "href" -> if (href.isEmpty()) href = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> if (tag() == "response") {
                    val display = if (name.isNotEmpty()) name
                    else href.substringBefore('?').trimEnd('/').substringAfterLast('/').let { URLDecoder.decode(it, "UTF-8") }
                    // 跳过目录自身条目（href 与请求路径相同）
                    val self = href.trimEnd('/').let { URLDecoder.decode(it.substringAfterLast('/'), "UTF-8") }
                    val requestSelf = requestPath.trim('/').substringAfterLast('/')
                    if (display.isNotEmpty() && !(isFolder && self == requestSelf)) {
                        out.add(DavEntry(display, isFolder, href))
                    }
                }
            }
            event = parser.next()
        }
        return out
    }

    companion object {
        val AUDIO_EXT = listOf(".mp3", ".m4a", ".flac", ".aac", ".wav", ".ogg")
        fun isAudio(name: String): Boolean = AUDIO_EXT.any { name.lowercase().endsWith(it) }
    }
}
