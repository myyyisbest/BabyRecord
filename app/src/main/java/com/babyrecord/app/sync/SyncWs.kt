package com.babyrecord.app.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.babyrecord.app.BabyApp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * 家庭同步 WebSocket：服务端数据变更的实时广播通道。
 *
 * - 连接 ws://<server>/ws?token=xxx，未加入家庭不连接
 * - 收到 {"type":"changed","table":...,"ids":[...]} → 去抖 1 秒后 syncOnce
 * - 每 30s 发文本 "ping" 保活（服务端回 "pong"）
 * - 断线指数退避重连：5s / 15s / 60s 封顶
 */
object SyncWs {
    private const val TAG = "SyncWs"
    private const val PING_INTERVAL_S = 30L

    @Volatile private var socket: WebSocket? = null
    @Volatile private var wantConnect = false      // 用户意图：已加入家庭即希望保持连接
    private var reconnectDelayMs = 5_000L
    private val handler = Handler(Looper.getMainLooper())
    private var pingRunnable: Runnable? = null

    private val client = OkHttpClient.Builder()
        .pingInterval(PING_INTERVAL_S, TimeUnit.SECONDS) // 协议层 ping 也开着，双保险
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)           // WS 长连接不设读超时
        .build()

    /** 全局同步引擎（BabyApp 初始化） */
    private fun engine(context: Context): SyncEngine? =
        (context.applicationContext as? BabyApp)?.syncEngine

    /** 加入家庭 / 回前台时调用：幂等，已连接则跳过 */
    fun start(context: Context) {
        val engine = engine(context) ?: return
        val token = engine.token() ?: return  // 未加入家庭不连接
        if (wantConnect && socket != null) return
        wantConnect = true
        connect(context, token)
    }

    /** 退出家庭时调用：断开且不再重连 */
    fun stop(context: Context) {
        wantConnect = false
        handler.removeCallbacksAndMessages(null)
        pingRunnable?.let { handler.removeCallbacks(it) }
        socket?.close(1000, "leave")
        socket = null
    }

    private fun connect(context: Context, token: String) {
        if (!wantConnect) return
        val url = SyncApi.wsUrl(context) + "?token=" + token
        val req = Request.Builder().url(url).build()
        socket = client.newWebSocket(req, Listener(context))
    }

    private fun scheduleReconnect(context: Context) {
        if (!wantConnect) return
        Log.d(TAG, "将于 ${reconnectDelayMs / 1000}s 后重连")
        handler.postDelayed({
            val token = engine(context)?.token() ?: return@postDelayed
            connect(context, token)
        }, reconnectDelayMs)
        // 指数退避：5s → 15s → 60s 封顶
        reconnectDelayMs = min(reconnectDelayMs * 3, 60_000L)
    }

    /** 收到变更广播：去抖 1 秒触发同步（多条广播合并为一次）；照片表变更立即唤醒照片下载通道 */
    private fun onChanged(context: Context, table: String, ids: List<String>) {
        Log.d(TAG, "服务端变更广播: $table x${ids.size}")
        val eng = engine(context) ?: return
        eng.requestSync(delayMs = 1_000L)
        if (table == "photos") {
            eng.kickPhotoWork()   // 家人传了新照片：不等记录同步，立即开始下载文件
        }
    }

    private class Listener(private val context: Context) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket 已连接")
            reconnectDelayMs = 5_000L   // 连接成功重置退避
            startPing(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (text == "pong") return  // 保活心跳回包，忽略
            runCatching {
                val jo = Json.parseToJsonElement(text).jsonObject
                if (jo["type"]?.jsonPrimitive?.content == "changed") {
                    val table = jo["table"]?.jsonPrimitive?.content ?: return
                    val ids = jo["ids"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    onChanged(context, table, ids)
                }
            }.onFailure { Log.w(TAG, "无法解析广播消息: $text") }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket 关闭: $code")
            stopPing()
            socket = null
            scheduleReconnect(context)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "WebSocket 失败: ${t.message}")
            stopPing()
            socket = null
            scheduleReconnect(context)
        }
    }

    /** 应用层文本 ping：每 30s 发 "ping"，服务端回 "pong" */
    private fun startPing(ws: WebSocket) {
        stopPing()
        val r = object : Runnable {
            override fun run() {
                if (socket === ws && wantConnect) {
                    ws.send("ping")
                    handler.postDelayed(this, PING_INTERVAL_S * 1000)
                }
            }
        }
        pingRunnable = r
        handler.postDelayed(r, PING_INTERVAL_S * 1000)
    }

    private fun stopPing() {
        pingRunnable?.let { handler.removeCallbacks(it) }
        pingRunnable = null
    }
}
