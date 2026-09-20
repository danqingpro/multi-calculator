package com.microsoft.calculator.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * 语言管理器:
 *  - 首次启动:按系统语言自动检测(中文→zh,其他→en)
 *  - 手动切换后:用 SharedPreferences 持久化,重启 App 仍保留用户选择
 * 切换后整个 Compose 树自动重组(因为 langCode 是 mutableStateOf)。
 */
class LanguageState(
    context: Context,
    initial: String = detectSystemLang()
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 启动时:有保存值用保存值,否则用自动检测值
    private val initialCode: String = prefs.getString(KEY_LANG, null) ?: initial

    var langCode: String by mutableStateOf(initialCode)
        private set

    val strings: Strings
        get() = if (langCode == "zh") StringsZh else StringsEn

    fun setLanguage(code: String) {
        langCode = code
        prefs.edit().putString(KEY_LANG, code).apply()
    }

    fun toggle() {
        val newCode = if (langCode == "zh") "en" else "zh"
        setLanguage(newCode)
    }

    /** 用户是否曾手动设置过语言(true 表示有持久化记录) */
    val hasUserOverride: Boolean get() = prefs.contains(KEY_LANG)

    companion object {
        private const val PREFS_NAME = "calculator_prefs"
        private const val KEY_LANG = "lang_code"

        /** 检测系统语言:以 zh 开头 → 中文,其余 → 英文 */
        fun detectSystemLang(): String {
            val lang = Locale.getDefault().language
            return if (lang.startsWith("zh")) "zh" else "en"
        }
    }
}

/**
 * 在 Composable 中获取 [LanguageState]:
 * val langState = rememberLanguageState()
 * langState.toggle()   // 切换并持久化
 */
@Composable
fun rememberLanguageState(): LanguageState {
    val context = LocalContext.current
    return remember { LanguageState(context) }
}
