package com.babyrecord.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 全局主题设置：内存态（驱动重组）+ SharedPreferences（持久化） */
object ThemeSettings {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        mode = try {
            ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode = newMode
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putString("theme", newMode.name).apply()
    }
}

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = CardWhite,
    primaryContainer = CoralContainer,
    onPrimaryContainer = OnCoralContainer,
    secondary = SkyBlue,
    onSecondary = CardWhite,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = OnSkyBlueContainer,
    tertiary = MintGreen,
    onTertiary = CardWhite,
    tertiaryContainer = MintContainer,
    onTertiaryContainer = OnMintContainer,
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = DividerWarm,
    onSurfaceVariant = InkSoft,
    outline = OutlineWarm
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9276),
    onPrimary = Color(0xFF3A130A),
    primaryContainer = Color(0xFF7A3524),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFF9FC6F0),
    onSecondary = Color(0xFF0E3355),
    secondaryContainer = Color(0xFF2C4A6B),
    onSecondaryContainer = Color(0xFFD8E6F8),
    tertiary = Color(0xFF8FD6A6),
    onTertiary = Color(0xFF0E3A22),
    tertiaryContainer = Color(0xFF27563A),
    onTertiaryContainer = Color(0xFFD7F2DF),
    background = Color(0xFF171210),
    onBackground = Color(0xFFEFE5DE),
    surface = Color(0xFF211A16),
    onSurface = Color(0xFFEFE5DE),
    surfaceVariant = Color(0xFF3A2F29),
    onSurfaceVariant = Color(0xFFC9B5A9),
    outline = Color(0xFF6B564A)
)

@Composable
fun BabyRecordTheme(mode: ThemeMode = ThemeSettings.mode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // 同步全局自适应中性色（Ink/InkSoft/DividerWarm/FieldWarm），避免夜间黑底黑字
    applyNeutralColors(dark)
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
