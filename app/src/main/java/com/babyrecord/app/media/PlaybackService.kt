package com.babyrecord.app.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.babyrecord.app.MainActivity
import okhttp3.Credentials

/**
 * 哄睡播放后台服务：熄屏/切出 App 后继续播放，通知栏带播放控制。
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(30000)
            .setUserAgent("BabyRecord/0.7")
        val user = prefs.getString("media_user", "") ?: ""
        val pass = prefs.getString("media_pass", "") ?: ""
        if (user.isNotEmpty()) {
            dataSourceFactory.setDefaultRequestProperties(mapOf("Authorization" to Credentials.basic(user, pass)))
        }
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        // 系统媒体控制器点击时跳转到哄睡播放页（而非 launcher 默认主页）
        val sessionIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_tab", "sleep_media")
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val sessionActivity = PendingIntent.getActivity(
            this, 0, sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
