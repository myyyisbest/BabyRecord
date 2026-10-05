package com.babyrecord.app.sync

import android.content.Context
import com.babyrecord.app.data.AuthResp
import com.babyrecord.app.data.PullResp
import com.babyrecord.app.data.PushResp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.Duration

/** 认证失效（服务端返回 401）：上层应提示「请重新加入家庭」 */
class UnauthorizedException(message: String = "unauthorized") : IOException(message)

/**
 * 家庭同步网络层：OkHttp + kotlinx-serialization 手写，不引 Retrofit。
 *
 * 服务器地址从 SharedPreferences("sync") 的 "server_url" 读取（默认 http://192.168.1.100:8443），
 * 可在家庭共享设置页修改。
 */
object SyncApi {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /** OkHttp 超时 15s（局域网同步服务足够） */
    private val client = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(15))
        .writeTimeout(Duration.ofSeconds(15))
        .build()

    fun prefs(context: Context) = context.getSharedPreferences("sync", Context.MODE_PRIVATE)

    /** 服务器地址（末尾去掉 /），默认家庭同步服务器 */
    fun serverUrl(context: Context): String =
        prefs(context).getString("server_url", "http://192.168.1.100:8443")!!
            .trim().trimEnd('/').ifBlank { "http://192.168.1.100:8443" }

    /** 修改服务器地址（用户在设置页输入） */
    fun setServerUrl(context: Context, url: String) {
        prefs(context).edit().putString("server_url", url.trim().trimEnd('/')).apply()
    }

    /** WebSocket 地址：http(s) → ws(s)，端口与 HTTP 相同 */
    fun wsUrl(context: Context): String =
        serverUrl(context).replaceFirst("http://", "ws://").replaceFirst("https://", "wss://") + "/ws"

    // ---- 认证 ----

    /** 创建家庭：返回 token/familyId/memberId/inviteCode */
    suspend fun create(context: Context, familyName: String, deviceName: String): AuthResp =
        post(context, "/auth/create", buildJsonObject {
            put("familyName", familyName)
            put("deviceName", deviceName)
        }.toString(), token = null).let { decode(it, AuthResp.serializer()) }

    /** 凭邀请码加入家庭 */
    suspend fun join(context: Context, inviteCode: String, deviceName: String): AuthResp =
        post(context, "/auth/join", buildJsonObject {
            put("inviteCode", inviteCode)
            put("deviceName", deviceName)
        }.toString(), token = null).let { decode(it, AuthResp.serializer()) }

    /** 退出家庭：DELETE /family/leave 通知服务端注销成员。
     *  2xx → true；401/网络失败也返回 true——本地凭据照常清理，不因服务端不可达卡住用户退出。 */
    suspend fun leave(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(base(context, "/family/leave"))
                .header("Authorization", "Bearer ${prefs(context).getString("token", "").orEmpty()}")
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(true)   // 网络异常/401 均视为成功：以本地清理为准
    }

    // ---- 同步 ----

    /** 推送：单批 ≤500 行，行内容为实体全字段透传 JSON（必须含 id/updatedAt） */
    suspend fun push(context: Context, token: String, table: String, rows: List<JsonObject>): PushResp =
        post(context, "/sync/push", buildJsonObject {
            put("table", table)
            put("rows", JsonArray(rows))
        }.toString(), token).let { decode(it, PushResp.serializer()) }

    /** 增量拉取：since 严格大于；id 为平局游标；tables 限定续拉的表（逗号分隔） */
    suspend fun pull(
        context: Context,
        token: String,
        since: Long,
        id: String? = null,
        tables: String? = null
    ): PullResp {
        val body = get(context, "/sync/pull", token) { builder ->
            builder.addQueryParameter("since", since.toString())
            if (!id.isNullOrBlank()) builder.addQueryParameter("id", id)
            if (!tables.isNullOrBlank()) builder.addQueryParameter("tables", tables)
        }
        return decode(body, PullResp.serializer())
    }

    // ---- 内部 ----

    /** 发 POST；401 抛 UnauthorizedException，其他非 2xx 抛 IOException */
    private suspend fun post(context: Context, path: String, body: String, token: String?): String =
        execute(context, Request.Builder()
            .url(base(context, path))
            .header("Authorization", "Bearer $token")
            .post(body.toRequestBody(JSON_MEDIA))
            .build())

    private suspend fun get(context: Context, path: String, token: String?, query: (HttpUrl.Builder) -> Unit = {}): String {
        val builder = base(context, path).toHttpUrlOrNull()?.newBuilder()
            ?: throw IOException("服务器地址无效: ${serverUrl(context)}")
        query(builder)
        return execute(context, Request.Builder()
            .url(builder.build())
            .header("Authorization", "Bearer $token")
            .get()
            .build())
    }

    private fun base(context: Context, path: String): String = serverUrl(context) + path

    /** 执行请求并返回响应体文本；统一在 IO 调度器 */
    private suspend fun execute(context: Context, request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            when {
                resp.code == 401 -> throw UnauthorizedException()
                !resp.isSuccessful -> throw IOException(errorText(resp.code, text))
                else -> text
            }
        }
    }

    /** 从错误响应体提取服务端 error 文案（如 403 的「family creation is locked…」），
     *  取不到时退回通用 HTTP 描述。上层 catch 后直接展示该 message 即可。 */
    private fun errorText(code: Int, body: String): String {
        val err = runCatching {
            (json.parseToJsonElement(body) as? JsonObject)
                ?.get("error")?.jsonPrimitive?.content
        }.getOrNull()
        return err?.takeIf { it.isNotBlank() } ?: "HTTP $code: ${body.take(200)}"
    }

    private fun <T> decode(text: String, serializer: KSerializer<T>): T =
        json.decodeFromString(serializer, text)
}
