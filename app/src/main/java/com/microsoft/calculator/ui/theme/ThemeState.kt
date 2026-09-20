package com.microsoft.calculator.ui.theme

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/** 主题模式:浅色 / 深色 / 跟随系统 */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * 主题状态管理:
 * - 浅色 / 深色 / 跟随系统
 * - 跟随系统时,读取系统当前 UI 模式决定实际用浅色还是深色
 * - 选择持久化到 SharedPreferences
 */
class ThemeState(context: Context, initial: ThemeMode = ThemeMode.SYSTEM) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)

    var mode: ThemeMode by mutableStateOf(initial)
        private set

    fun updateMode(m: ThemeMode) {
        mode = m
        prefs.edit().putString(KEY_THEME, m.name).apply()
    }

    /** 返回当前实际生效的颜色集合(跟随系统时会解析系统模式) */
    fun resolveColors(isSystemDark: Boolean): ThemeColors = when (mode) {
        ThemeMode.LIGHT -> lightColors
        ThemeMode.DARK -> darkColors
        ThemeMode.SYSTEM -> if (isSystemDark) darkColors else lightColors
    }

    companion object {
        private const val KEY_THEME = "theme_mode"

        fun load(context: Context): ThemeMode {
            val prefs = context.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)
            return prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
                ?: ThemeMode.SYSTEM
        }
    }
}

@Composable
fun rememberThemeState(context: Context): ThemeState {
    return remember { ThemeState(context, ThemeState.load(context)) }
}

/** 判断系统当前是否为深色模式 */
@Composable
fun isSystemDarkTheme(): Boolean {
    val config = LocalConfiguration.current
    return (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
}
