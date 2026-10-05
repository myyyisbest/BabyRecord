package com.babyrecord.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.babyrecord.app.ui.AppRoot
import com.babyrecord.app.ui.theme.BabyRecordTheme
import com.babyrecord.app.ui.theme.ThemeMode
import com.babyrecord.app.ui.theme.ThemeSettings

/** 桌面小组件发给主界面的命令：要自动弹出的记录面板 */
object WidgetCommands {
    var pendingSheet by mutableStateOf<String?>(null)
    var pendingBackup by mutableStateOf(false)
    var pendingTab by mutableStateOf<String?>(null)
}

class MainActivity : ComponentActivity() {

    /** 回前台即拉一次增量（SyncEngine 内部有去抖，不会重复连发） */
    override fun onResume() {
        super.onResume()
        (application as? BabyApp)?.syncEngine?.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val dark = when (ThemeSettings.mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // 跟随应用主题（而非仅系统）切换状态栏/导航栏图标明暗
            SideEffect {
                val style = SystemBarStyle.auto(
                    Color.Transparent.toArgb(),
                    Color.Transparent.toArgb()
                ) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            // 疫苗提醒需要通知权限：首次打开时申请一次
            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) {
                    val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                    val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    if (!granted && !prefs.getBoolean("notif_asked", false)) {
                        prefs.edit().putBoolean("notif_asked", true).apply()
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            BabyRecordTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("open_sheet")?.let { WidgetCommands.pendingSheet = it }
        intent?.getStringExtra("open_tab")?.let { WidgetCommands.pendingTab = it }
        if (intent?.getBooleanExtra("open_backup", false) == true) WidgetCommands.pendingBackup = true
    }
}
