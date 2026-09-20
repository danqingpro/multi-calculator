package com.microsoft.calculator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 主题颜色集合。深色 / 浅色各一套,通过 [LocalThemeColors] 在 Compose 树中动态提供。
 *
 * 为了不改动大量现有代码,原有全局颜色名(DarkBg / PanelBg 等)保留为
 * [@Composable] getter,运行时读取当前主题的对应字段。
 */
data class ThemeColors(
    val DarkBg: Color,
    val PanelBg: Color,
    val SurfaceBg: Color,
    val ButtonBg: Color,
    val ButtonBgLight: Color,
    val NumberBtnBg: Color,
    val OperatorBtnBg: Color,
    val EqualsBtnBg: Color,
    val EqualsBtnFg: Color,
    val AccentBlue: Color,
    val AccentPurple: Color,
    val DisabledDotBg: Color,
    val AccentRed: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val ErrorColor: Color,
    val Divider: Color,
    val MemoryChipBg: Color,
    val HistoryItemBg: Color,
)

val darkColors = ThemeColors(
    DarkBg = Color(0xFF0B0B10),
    PanelBg = Color(0xFF1C1C26),
    SurfaceBg = Color(0xFF2A2A38),
    ButtonBg = Color(0xFF2A2A38),
    ButtonBgLight = Color(0xFF33333F),
    NumberBtnBg = Color(0xFF3B3B48),
    OperatorBtnBg = Color(0xFF2A2A38),
    EqualsBtnBg = Color(0xFFB897FF),
    EqualsBtnFg = Color(0xFF1F1F2E),
    AccentBlue = Color(0xFF0B5BFF),
    AccentPurple = Color(0xFFB897FF),
    DisabledDotBg = Color(0xFF1C1C26),
    AccentRed = Color(0xFFE0455A),
    TextPrimary = Color(0xFFE6E6F0),
    TextSecondary = Color(0xFF9A9AAE),
    TextMuted = Color(0xFF6E6E80),
    ErrorColor = Color(0xFFE0455A),
    Divider = Color(0xFF33333F),
    MemoryChipBg = Color(0xFF23232E),
    HistoryItemBg = Color(0xFF1C1C26),
)

val lightColors = ThemeColors(
    DarkBg = Color(0xFFF3F3F3),
    PanelBg = Color(0xFFFFFFFF),
    SurfaceBg = Color(0xFFE8E8EF),
    ButtonBg = Color(0xFFE8E8EF),
    ButtonBgLight = Color(0xFFDDDDE5),
    NumberBtnBg = Color(0xFFD0D0D9),
    OperatorBtnBg = Color(0xFFE8E8EF),
    EqualsBtnBg = Color(0xFF0B5BFF),
    EqualsBtnFg = Color(0xFFFFFFFF),
    AccentBlue = Color(0xFF0B5BFF),
    AccentPurple = Color(0xFF7A4FFF),
    DisabledDotBg = Color(0xFFE8E8EF),
    AccentRed = Color(0xFFD83B4E),
    TextPrimary = Color(0xFF1A1A22),
    TextSecondary = Color(0xFF5A5A66),
    TextMuted = Color(0xFF8A8A96),
    ErrorColor = Color(0xFFD83B4E),
    Divider = Color(0xFFD8D8E0),
    MemoryChipBg = Color(0xFFF0F0F5),
    HistoryItemBg = Color(0xFFF5F5FA),
)

val LocalThemeColors = staticCompositionLocalOf { darkColors }

// 以下为兼容现有代码的 Composable getter,运行时读取当前主题颜色
val DarkBg: Color
    @Composable get() = LocalThemeColors.current.DarkBg
val PanelBg: Color
    @Composable get() = LocalThemeColors.current.PanelBg
val SurfaceBg: Color
    @Composable get() = LocalThemeColors.current.SurfaceBg
val ButtonBg: Color
    @Composable get() = LocalThemeColors.current.ButtonBg
val ButtonBgLight: Color
    @Composable get() = LocalThemeColors.current.ButtonBgLight
val NumberBtnBg: Color
    @Composable get() = LocalThemeColors.current.NumberBtnBg
val OperatorBtnBg: Color
    @Composable get() = LocalThemeColors.current.OperatorBtnBg
val EqualsBtnBg: Color
    @Composable get() = LocalThemeColors.current.EqualsBtnBg
val EqualsBtnFg: Color
    @Composable get() = LocalThemeColors.current.EqualsBtnFg
val AccentBlue: Color
    @Composable get() = LocalThemeColors.current.AccentBlue
val AccentPurple: Color
    @Composable get() = LocalThemeColors.current.AccentPurple
val DisabledDotBg: Color
    @Composable get() = LocalThemeColors.current.DisabledDotBg
val AccentRed: Color
    @Composable get() = LocalThemeColors.current.AccentRed
val TextPrimary: Color
    @Composable get() = LocalThemeColors.current.TextPrimary
val TextSecondary: Color
    @Composable get() = LocalThemeColors.current.TextSecondary
val TextMuted: Color
    @Composable get() = LocalThemeColors.current.TextMuted
val ErrorColor: Color
    @Composable get() = LocalThemeColors.current.ErrorColor
val Divider: Color
    @Composable get() = LocalThemeColors.current.Divider
val MemoryChipBg: Color
    @Composable get() = LocalThemeColors.current.MemoryChipBg
val HistoryItemBg: Color
    @Composable get() = LocalThemeColors.current.HistoryItemBg
