package com.babyrecord.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ── 品牌色（日/夜不变）──
val Coral = Color(0xFFE8735A)
val CoralLight = Color(0xFFF2A07B)
val CoralContainer = Color(0xFFFFDAD2)
val OnCoralContainer = Color(0xFF5F1A0C)
val SkyBlue = Color(0xFF5686BE)
val SkyBlueContainer = Color(0xFFD8E6F8)
val OnSkyBlueContainer = Color(0xFF103A5F)
val MintGreen = Color(0xFF56A26E)
val MintContainer = Color(0xFFD7F2DF)
val OnMintContainer = Color(0xFF0D3B20)
val Cream = Color(0xFFFFF8F4)
val CardWhite = Color(0xFFFFFFFF)
val OutlineWarm = Color(0xFFDCC8BE)
val Lilac = Color(0xFF8B7EC8)
val LilacContainer = Color(0xFFE7DFF8)
val OnLilacContainer = Color(0xFF2A1B54)

// ── 日/夜自适应中性色 ──
// 各界面大量直接引用这些颜色（color = Ink 等）。
// 用全局可变状态承载，BabyRecordTheme 切换日/夜时统一更新，
// 引用处会自动重组，彻底避免夜间"黑底黑字"。
var Ink by mutableStateOf(Color(0xFF2B1D16))
    private set
var InkSoft by mutableStateOf(Color(0xFF8A7568))
    private set
var DividerWarm by mutableStateOf(Color(0xFFF3E4DB))
    private set
var FieldWarm by mutableStateOf(Color(0xFFF7EFE9))
    private set

/** 由 BabyRecordTheme 调用：按日/夜更新全局中性色 */
fun applyNeutralColors(dark: Boolean) {
    if (dark) {
        Ink = Color(0xFFEFE5DE)      // 深色底上的浅色正文
        InkSoft = Color(0xFFC9B5A9)  // 深色底上的浅色次要文字
        DividerWarm = Color(0xFF3A2F29)
        FieldWarm = Color(0xFF332822) // 深色输入框/选中底
    } else {
        Ink = Color(0xFF2B1D16)
        InkSoft = Color(0xFF8A7568)
        DividerWarm = Color(0xFFF3E4DB)
        FieldWarm = Color(0xFFF7EFE9)
    }
}
