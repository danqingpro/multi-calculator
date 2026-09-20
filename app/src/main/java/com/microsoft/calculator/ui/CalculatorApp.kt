package com.microsoft.calculator.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.microsoft.calculator.engine.UnitConverter
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.ui.i18n.Strings
import com.microsoft.calculator.ui.i18n.rememberLanguageState
import com.microsoft.calculator.ui.screens.AboutScreen
import com.microsoft.calculator.ui.screens.ConverterScreen
import com.microsoft.calculator.ui.screens.DateScreen
import com.microsoft.calculator.ui.screens.FeedbackScreen
import com.microsoft.calculator.ui.screens.PlotScreen
import com.microsoft.calculator.ui.screens.ProgrammerScreen
import com.microsoft.calculator.ui.screens.ScientificScreen
import com.microsoft.calculator.ui.screens.StandardScreen
import com.microsoft.calculator.ui.theme.*
import com.microsoft.calculator.viewmodel.CalculatorViewModel
import com.microsoft.calculator.viewmodel.DateCalculatorViewModel
import com.microsoft.calculator.viewmodel.ProgrammerViewModel
import com.microsoft.calculator.viewmodel.UnitConverterViewModel
import kotlinx.coroutines.launch

/**
 * 顶层模式:计算器模式(5种)或转换器 Category。
 */
sealed interface CalcTop {
    fun title(s: Strings, lang: String): String
}

enum class CalcMode : CalcTop {
    STANDARD, SCIENTIFIC, PLOT, PROGRAMMER, DATE;

    override fun title(s: Strings, lang: String): String = when (this) {
        STANDARD -> s.modeStandard
        SCIENTIFIC -> s.modeScientific
        PLOT -> s.modePlot
        PROGRAMMER -> s.modeProgrammer
        DATE -> s.modeDate
    }
}

data class ConvCategoryMode(val category: UnitConverter.Category) : CalcTop {
    override fun title(s: Strings, lang: String): String = category.title(lang)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorApp() {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentTop by remember { mutableStateOf<CalcTop>(CalcMode.STANDARD) }

    val calcVm: CalculatorViewModel = viewModel()
    val progVm: ProgrammerViewModel = viewModel()
    val convVm: UnitConverterViewModel = viewModel()
    val dateVm: DateCalculatorViewModel = viewModel()

    var showMemory by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    // 绘图视图模式:false=函数列表, true=全屏画布
    var plotCanvasMode by remember { mutableStateOf(false) }

    // 语言 + 主题状态
    val langState = rememberLanguageState()
    val s = langState.strings
    val themeState = rememberThemeState(context)
    val isSystemDark = isSystemDarkTheme()
    val colors = themeState.resolveColors(isSystemDark)

    // 同时注入 语言包 与 主题颜色
    CompositionLocalProvider(
        LocalStrings provides s,
        LocalThemeColors provides colors
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = PanelBg,
                    drawerContentColor = TextPrimary
                ) {
                    // ===== 整体一个可滚动大列表,分三组 =====
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            s.appName,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(24.dp, 8.dp)
                        )
                        HorizontalDivider(color = Divider)

                        // ----- 分组:计算器 -----
                        GroupLabel(s.drawerGroupCalculator)
                        listOf(CalcMode.STANDARD, CalcMode.SCIENTIFIC, CalcMode.PLOT, CalcMode.PROGRAMMER, CalcMode.DATE).forEach { mode ->
                            DrawerItem(
                                label = mode.title(s, s.langCode),
                                selected = currentTop == mode,
                                onClick = {
                                    currentTop = mode
                                    showAbout = false
                                    showFeedback = false
                                    if (mode == CalcMode.STANDARD) calcVm.setMode(CalculatorViewModel.Mode.STANDARD)
                                    if (mode == CalcMode.SCIENTIFIC) calcVm.setMode(CalculatorViewModel.Mode.SCIENTIFIC)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }

                        // ----- 分组:转换器(12 个类别直接列出) -----
                        GroupLabel(s.drawerGroupConverter)
                        UnitConverter.Category.entries.forEach { cat ->
                            val catMode = ConvCategoryMode(cat)
                            DrawerItem(
                                label = cat.title(s.langCode),
                                selected = currentTop == catMode,
                                onClick = {
                                    currentTop = catMode
                                    showAbout = false
                                    showFeedback = false
                                    convVm.setCategory(cat)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }

                        // ----- 分组:设置 -----
                        GroupLabel(s.setting)
                        DrawerItem(
                            label = s.history,
                            icon = Icons.Filled.History,
                            selected = showHistory,
                            onClick = { showHistory = !showHistory; scope.launch { drawerState.close() } }
                        )
                        DrawerItem(
                            label = s.memory,
                            icon = Icons.Filled.Memory,
                            selected = showMemory,
                            onClick = { showMemory = !showMemory; scope.launch { drawerState.close() } }
                        )
                        DrawerItem(
                            label = s.language,
                            icon = Icons.Filled.Language,
                            selected = false,
                            trailing = {
                                Text(
                                    if (langState.langCode == "zh") s.languageZh else s.languageEn,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = { langState.toggle() }
                        )
                        DrawerItem(
                            label = s.theme,
                            icon = Icons.Filled.Palette,
                            selected = false,
                            trailing = {
                                Text(
                                    when (themeState.mode) {
                                        ThemeMode.LIGHT -> s.themeLight
                                        ThemeMode.DARK -> s.themeDark
                                        ThemeMode.SYSTEM -> s.themeSystem
                                    },
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = { showThemePicker = true }
                        )
                        DrawerItem(
                            label = s.about,
                            icon = Icons.Filled.Info,
                            selected = showAbout,
                            onClick = { showAbout = true; showFeedback = false; scope.launch { drawerState.close() } }
                        )
                        DrawerItem(
                            label = s.feedback,
                            icon = Icons.Filled.Feedback,
                            selected = showFeedback,
                            onClick = { showFeedback = true; showAbout = false; scope.launch { drawerState.close() } }
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when {
                                    showAbout -> s.about
                                    showFeedback -> s.feedback
                                    else -> currentTop.title(s, s.langCode)
                                },
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = s.menu, tint = TextPrimary)
                            }
                        },
                        actions = {
                            if (currentTop == CalcMode.PLOT && !showAbout && !showFeedback) {
                                IconButton(onClick = { plotCanvasMode = true }) {
                                    Icon(
                                        Icons.Filled.ShowChart,
                                        contentDescription = null,
                                        tint = if (plotCanvasMode) AccentBlue else TextSecondary
                                    )
                                }
                                IconButton(onClick = { plotCanvasMode = false }) {
                                    Text(
                                        "fₓ",
                                        color = if (!plotCanvasMode) AccentBlue else TextSecondary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DarkBg,
                            titleContentColor = TextPrimary
                        )
                    )
                },
                containerColor = DarkBg
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                        .padding(padding)
                ) {
                    if (showAbout) {
                        AboutScreen()
                    } else if (showFeedback) {
                        FeedbackScreen()
                    } else {
                        when (val top = currentTop) {
                            is CalcMode -> when (top) {
                                CalcMode.STANDARD -> StandardScreen(calcVm, showHistory, showMemory)
                                CalcMode.SCIENTIFIC -> ScientificScreen(calcVm, showHistory, showMemory)
                                CalcMode.PLOT -> PlotScreen(canvasMode = plotCanvasMode)
                                CalcMode.PROGRAMMER -> ProgrammerScreen(progVm)
                                CalcMode.DATE -> DateScreen(dateVm)
                            }
                            is ConvCategoryMode -> ConverterScreen(convVm)
                        }
                    }
                }

                // ===== 主题选择弹窗 =====
                if (showThemePicker) {
                    AlertDialog(
                        onDismissRequest = { showThemePicker = false },
                        containerColor = PanelBg,
                        titleContentColor = TextPrimary,
                        title = { Text(s.theme, fontSize = 18.sp) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ThemeMode.entries.forEach { m ->
                                    val label = when (m) {
                                        ThemeMode.LIGHT -> s.themeLight
                                        ThemeMode.DARK -> s.themeDark
                                        ThemeMode.SYSTEM -> s.themeSystem
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                            .clickableNoRipple { themeState.updateMode(m) }
                                    ) {
                                        RadioButton(
                                            selected = themeState.mode == m,
                                            onClick = { themeState.updateMode(m) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = AccentBlue,
                                                unselectedColor = TextSecondary
                                            )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(label, color = TextPrimary, fontSize = 16.sp)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemePicker = false }) {
                                Text(s.ok, color = AccentBlue)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ========================================================================
// 抽屉子组件
// ========================================================================

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 4.dp)
    )
}

@Composable
private fun DrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    NavigationDrawerItem(
        label = { Text(label, color = TextPrimary) },
        selected = selected,
        onClick = onClick,
        icon = {
            if (icon != null) Icon(icon, contentDescription = null, tint = TextSecondary)
        },
        badge = trailing?.let { { it() } },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = SurfaceBg,
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier.padding(8.dp, 2.dp)
    )
}

/** 无 ripple 的点击,用于主题弹窗内的整行点击 */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickableNoRippleInner(onClick))

@Composable
private fun Modifier.clickableNoRippleInner(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    )
}

@Composable
internal fun PanelBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
internal fun RoundedPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = PanelBg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
