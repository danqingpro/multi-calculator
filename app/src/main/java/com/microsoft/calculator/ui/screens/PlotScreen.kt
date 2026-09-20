package com.microsoft.calculator.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import com.microsoft.calculator.engine.ExpressionEvaluator
import com.microsoft.calculator.ui.theme.*
import java.io.File

/** 单个绘图函数:颜色 + 表达式 + 是否隐藏 */
private data class PlotFunc(val color: Color, val expr: String, val hidden: Boolean = false)

/** 函数调色板:f₁ 蓝、f₂ 绿、f₃ 橙、f₄ 红... */
private val FUNC_COLORS = listOf(
    Color(0xFF4A90FF),  // blue
    Color(0xFF50C878),  // green
    Color(0xFFFF8C42),  // orange
    Color(0xFFE0455A),  // red
    Color(0xFFB897FF),  // purple
    Color(0xFFFFD23F),  // yellow
)

/** 下拉面板类型 */
private enum class DropdownType { NONE, TRIG, INEQ, FUNC }

/** 角度单位 */
private enum class AngleUnit { RADIANS, DEGREES, GRADIANS }

/** 线条样式 */
private enum class LineStyle(val label: String, val width: Float, val dash: Boolean) {
    THIN("细", 1.5f, false),
    MEDIUM("中", 3f, false),
    THICK("粗", 5f, false),
    DASHED("虚线", 2f, true)
}

/** 图形主题 */
private enum class GraphTheme { ALWAYS_LIGHT, MATCH_APP }

/** 图形设置 */
private data class GraphSettings(
    val xMin: Float = -10f,
    val xMax: Float = 10f,
    val yMin: Float = -10f,
    val yMax: Float = 10f,
    val angleUnit: AngleUnit = AngleUnit.RADIANS,
    val lineStyle: LineStyle = LineStyle.MEDIUM,
    val theme: GraphTheme = GraphTheme.ALWAYS_LIGHT
)

/**
 * 绘图模式。
 * @param canvasMode true=全屏画布视图, false=函数列表+输入视图
 */
@Composable
fun PlotScreen(canvasMode: Boolean = false) {
    val evaluator = remember { ExpressionEvaluator() }
    val context = LocalContext.current

    // 已确认的函数列表
    var funcs by remember { mutableStateOf(listOf<PlotFunc>()) }
    // 当前编辑的表达式(用 TextFieldValue 控制光标位置)
    var editingValue by remember { mutableStateOf(TextFieldValue("")) }

    // 下拉面板状态
    var dropdown by remember { mutableStateOf(DropdownType.NONE) }
    var is2nd by remember { mutableStateOf(false) }

    // 图形设置(窗口边界 xMin/xMax/yMin/yMax 同时作为可视区域)
    var graphSettings by remember { mutableStateOf(GraphSettings()) }
    var showGraphOptions by remember { mutableStateOf(false) }

    // 样式编辑弹窗(修改颜色/线条)
    var showStyleDialog by remember { mutableStateOf(false) }
    var styleTargetIdx by remember { mutableStateOf(-1) }

    // 分析视图:null=函数列表视图, Int=正在分析第几个函数
    var analysisTarget by remember { mutableStateOf<Int?>(null) }

    // 画布尺寸(由 DrawCanvas 上报,用于把像素平移/缩放换算成边界)
    var canvasW by remember { mutableStateOf(1f) }
    var canvasH by remember { mutableStateOf(1f) }

    var panEnabled by remember { mutableStateOf(true) }  // 拖动工具默认开启

    // 用于分享的根视图引用
    val rootView = LocalView.current

    /** 根据当前边界计算每单位像素数(均匀缩放,取较小者以完整显示) */
    fun scaleFromBounds(s: GraphSettings): Float {
        if (canvasW <= 0f || canvasH <= 0f) return 1f
        val sx = canvasW / (s.xMax - s.xMin)
        val sy = canvasH / (s.yMax - s.yMin)
        return minOf(sx, sy)
    }

    /** 平移:dx/dy 为像素,内容同向移动 */
    fun handlePan(dx: Float, dy: Float) {
        val s = graphSettings
        val scale = scaleFromBounds(s)
        if (scale <= 0f) return
        val shiftX = dx / scale
        val shiftY = dy / scale
        graphSettings = s.copy(
            xMin = s.xMin - shiftX,
            xMax = s.xMax - shiftX,
            yMin = s.yMin + shiftY,
            yMax = s.yMax + shiftY
        )
    }

    /** 以焦点为中心缩放 */
    fun handleZoom(factor: Float, focusX: Float, focusY: Float) {
        val s = graphSettings
        val scale = scaleFromBounds(s)
        if (scale <= 0f) return
        val centerX = (s.xMin + s.xMax) / 2f
        val centerY = (s.yMin + s.yMax) / 2f
        val cx = canvasW / 2f - centerX * scale
        val cy = canvasH / 2f + centerY * scale
        val fx = (focusX - cx) / scale
        val fy = (cy - focusY) / scale
        val newScale = scale * factor
        val newCx = focusX - fx * newScale
        val newCy = focusY + fy * newScale
        graphSettings = s.copy(
            xMin = (0f - newCx) / newScale,
            xMax = (canvasW - newCx) / newScale,
            yMin = (newCy - canvasH) / newScale,
            yMax = newCy / newScale
        )
    }

    val activeColor = FUNC_COLORS[funcs.size % FUNC_COLORS.size]

    val editingExpr = editingValue.text

    // ---------- 按键输入处理(带光标定位) ----------
    // 在光标处插入文本;若文本以 "(" 结尾,光标定位到括号内(即 "(" 之后)
    fun insertAtCursor(text: String) {
        val cur = editingValue.selection.start
        val sb = StringBuilder(editingExpr)
        sb.insert(cur, text)
        val newText = sb.toString()
        // 光标定位到插入文本之后;若以 "(" 结尾则正好在括号内部
        val cursorPos = cur + text.length
        editingValue = TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(cursorPos))
    }
    fun appendText(s: String) = insertAtCursor(s)
    fun backspace() {
        val cur = editingValue.selection.start
        if (cur > 0) {
            val sb = StringBuilder(editingExpr)
            sb.deleteCharAt(cur - 1)
            val newText = sb.toString()
            editingValue = TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(cur - 1))
        }
    }
    fun clearAll() {
        editingValue = TextFieldValue("")
    }
    fun commitExpr() {
        val e = editingExpr.trim()
        if (e.isNotEmpty() && funcs.size < FUNC_COLORS.size) {
            funcs = funcs + PlotFunc(activeColor, e)
            editingValue = TextFieldValue("")
        }
    }

    /** 切换函数隐藏状态 */
    fun toggleHidden(idx: Int) {
        funcs = funcs.mapIndexed { i, f ->
            if (i == idx) f.copy(hidden = !f.hidden) else f
        }
    }

    /** 判断表达式是否看起来"完整"(括号匹配、非空、不残留未闭合) */
    fun isExprComplete(expr: String): Boolean {
        val e = expr.trim()
        if (e.isEmpty()) return false
        var depth = 0
        for (c in e) {
            when (c) {
                '(' -> depth++
                ')' -> depth--
                else -> {}
            }
            if (depth < 0) return false
        }
        if (depth != 0) return false
        // 不以运算符结尾
        val last = e.last()
        if ("+-*/^%".contains(last)) return false
        return true
    }

    // 当切换到画布视图时,自动提交已完成的当前表达式
    LaunchedEffect(canvasMode) {
        if (canvasMode) {
            val e = editingExpr.trim()
            if (e.isNotEmpty() && isExprComplete(e) && funcs.size < FUNC_COLORS.size) {
                funcs = funcs + PlotFunc(activeColor, e)
                editingValue = TextFieldValue("")
            }
        }
    }

    fun styleDialogFor(idx: Int, _color: Color) {
        styleTargetIdx = idx
        showStyleDialog = true
    }

    // ---------- 布局 ----------
    if (canvasMode) {
        // ===== 全屏画布视图 =====
        CanvasView(
            funcs = funcs,
            evaluator = evaluator,
            settings = graphSettings,
            panEnabled = panEnabled,
            onSizeChanged = { w, h -> canvasW = w; canvasH = h },
            onTogglePan = { panEnabled = !panEnabled },
            onZoomIn = { handleZoom(1.3f, canvasW / 2f, canvasH / 2f) },
            onZoomOut = { handleZoom(1f / 1.3f, canvasW / 2f, canvasH / 2f) },
            onReset = { graphSettings = graphSettings.copy(xMin = -10f, xMax = 10f, yMin = -10f, yMax = 10f) },
            onPan = { dx, dy -> handlePan(dx, dy) },
            onZoomBy = { factor, fx, fy -> handleZoom(factor, fx, fy) },
            onOpenSettings = { showGraphOptions = true },
            onShare = { shareGraph(context, rootView) }
        )
    } else if (analysisTarget != null && analysisTarget in funcs.indices) {
        // ===== 函数分析视图(可滚动) =====
        val target = funcs[analysisTarget!!]
        Column(Modifier.fillMaxSize().background(DarkBg)) {
            // 顶部函数行(返回按钮形态)
            FunctionListItem(
                index = analysisTarget!!,
                color = target.color,
                expr = target.expr,
                enabled = true,
                hidden = target.hidden,
                isBackMode = true,
                onToggleHidden = { toggleHidden(analysisTarget!!) },
                onBack = { analysisTarget = null },
                onAnalyze = {},
                onEditStyle = { styleDialogFor(analysisTarget!!, target.color) },
                onRemove = {
                    funcs = funcs.filterIndexed { i, _ -> i != analysisTarget }
                    analysisTarget = null
                }
            )
            // 分析内容(可滚动)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(PanelBg)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                FunctionAnalysisContent(target.expr, evaluator, graphSettings.angleUnit)
            }
        }
    } else {
        // ===== 函数列表 + 键盘(含三角函数/函数/不等式标题栏) 视图 =====
        Column(Modifier.fillMaxSize().background(DarkBg)) {
            // 1. 函数列表区域(占满剩余空间,可滚动)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(PanelBg)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                funcs.forEachIndexed { idx, f ->
                    FunctionListItem(
                        index = idx,
                        color = f.color,
                        expr = f.expr,
                        enabled = true,
                        hidden = f.hidden,
                        onToggleHidden = { toggleHidden(idx) },
                        onAnalyze = { analysisTarget = idx },
                        onEditStyle = { styleDialogFor(idx, f.color) },
                        onRemove = { funcs = funcs.filterIndexed { i, _ -> i != idx } }
                    )
                }
                // 正在编辑中的表达式也以函数项形态显示,让三个按钮常驻
                FunctionInputRow(
                    color = activeColor,
                    value = editingValue,
                    onValueChange = { editingValue = it },
                    onEnter = ::commitExpr,
                    enabled = isExprComplete(editingExpr),
                    onAnalyze = { /* TODO */ },
                    onEditStyle = { styleDialogFor(funcs.size, activeColor) },
                    onRemove = { clearAll() }
                )
            }

            // 2. 键盘区域:标题栏(三角函数 / 不等式 / 函数)+ 按键
            Surface(color = DarkBg, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 标题栏:三个下拉入口
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            ToolbarDropdown("三角函数", dropdown == DropdownType.TRIG) {
                                dropdown = if (dropdown == DropdownType.TRIG) DropdownType.NONE else DropdownType.TRIG
                            }
                            DropdownMenu(
                                expanded = dropdown == DropdownType.TRIG,
                                onDismissRequest = { dropdown = DropdownType.NONE },
                                containerColor = SurfaceBg,
                                shadowElevation = 8.dp
                            ) {
                                TrigPanel(is2nd, { is2nd = !is2nd }, ::appendText)
                            }
                        }
                        Box {
                            ToolbarDropdown("不等式", dropdown == DropdownType.INEQ) {
                                dropdown = if (dropdown == DropdownType.INEQ) DropdownType.NONE else DropdownType.INEQ
                            }
                            DropdownMenu(
                                expanded = dropdown == DropdownType.INEQ,
                                onDismissRequest = { dropdown = DropdownType.NONE },
                                containerColor = SurfaceBg,
                                shadowElevation = 8.dp
                            ) {
                                IneqPanel(::appendText)
                            }
                        }
                        Box {
                            ToolbarDropdown("函数", dropdown == DropdownType.FUNC) {
                                dropdown = if (dropdown == DropdownType.FUNC) DropdownType.NONE else DropdownType.FUNC
                            }
                            DropdownMenu(
                                expanded = dropdown == DropdownType.FUNC,
                                onDismissRequest = { dropdown = DropdownType.NONE },
                                containerColor = SurfaceBg,
                                shadowElevation = 8.dp
                            ) {
                                FuncPanel(::appendText)
                            }
                        }
                    }

                    // 按键面板
                    KeypadButtons(
                        onAppend = ::appendText,
                        onBackspace = ::backspace,
                        onClear = ::clearAll,
                        onEnter = ::commitExpr,
                        activeColor = activeColor,
                        is2nd = is2nd,
                        onToggle2nd = { is2nd = !is2nd }
                    )
                }
            }
        }
    }

    // ===== 图形选项弹窗 =====
    if (showGraphOptions) {
        GraphOptionsDialog(
            settings = graphSettings,
            onSettingsChange = { graphSettings = it },
            onResetView = { graphSettings = graphSettings.copy(xMin = -10f, xMax = 10f, yMin = -10f, yMax = 10f) },
            onDismiss = { showGraphOptions = false }
        )
    }

    // ===== 公式样式弹窗 =====
    if (showStyleDialog && styleTargetIdx in funcs.indices) {
        val target = funcs[styleTargetIdx]
        var selColor by remember(styleTargetIdx) { mutableStateOf(target.color) }
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            containerColor = SurfaceBg,
            title = { Text("更改公式样式", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("颜色", color = TextSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FUNC_COLORS.forEach { c ->
                            val selected = c == selColor
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(c)
                                    .clickable { selColor = c },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    funcs = funcs.mapIndexed { i, f ->
                        if (i == styleTargetIdx) f.copy(color = selColor) else f
                    }
                    showStyleDialog = false
                }) { Text("确定", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showStyleDialog = false }) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

// =====================================================================
// 全屏画布视图(含拖动/缩放控制)
// =====================================================================
@Composable
private fun CanvasView(
    funcs: List<PlotFunc>,
    evaluator: ExpressionEvaluator,
    settings: GraphSettings,
    panEnabled: Boolean,
    onSizeChanged: (Float, Float) -> Unit,
    onTogglePan: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onPan: (Float, Float) -> Unit,
    onZoomBy: (Float, Float, Float) -> Unit,
    onOpenSettings: () -> Unit,
    onShare: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(if (settings.theme == GraphTheme.ALWAYS_LIGHT) Color.White else DarkBg)) {
        // 画布(支持拖动 + 双指缩放)
        DrawCanvas(
            funcs = funcs,
            evaluator = evaluator,
            settings = settings,
            panEnabled = panEnabled,
            onSizeChanged = onSizeChanged,
            onPan = onPan,
            onZoomBy = onZoomBy
        )

        // 右上角工具列:拖动工具 / 分享 / 设置
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconBtn(
                icon = Icons.Default.OpenWith,
                bg = if (panEnabled) AccentBlue else SurfaceBg,
                fg = if (panEnabled) Color.White else TextPrimary,
                onClick = onTogglePan,
                contentDesc = "拖动"
            )
            if (funcs.isNotEmpty()) {
                IconBtn(
                    icon = Icons.Default.Share,
                    onClick = onShare,
                    contentDesc = "分享"
                )
            }
            IconBtn(
                icon = Icons.Default.Settings,
                onClick = onOpenSettings,
                contentDesc = "设置"
            )
        }

        // 右下角缩放控制:放大 / 缩小 / 归零
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconBtn(icon = Icons.Default.Add, onClick = onZoomIn, contentDesc = "放大")
            IconBtn(icon = Icons.Default.Remove, onClick = onZoomOut, contentDesc = "缩小")
            IconBtn(icon = Icons.Default.ZoomOutMap, onClick = onReset, contentDesc = "重置")
        }
    }
}

@Composable
private fun IconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color = SurfaceBg,
    fg: Color = TextPrimary,
    contentDesc: String = "",
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDesc, tint = fg, modifier = Modifier.size(20.dp))
    }
}

// =====================================================================
// 图形选项弹窗
// =====================================================================
@Composable
private fun GraphOptionsDialog(
    settings: GraphSettings,
    onSettingsChange: (GraphSettings) -> Unit,
    onResetView: () -> Unit,
    onDismiss: () -> Unit
) {
    var xMin by remember(settings) { mutableStateOf(settings.xMin.toString()) }
    var xMax by remember(settings) { mutableStateOf(settings.xMax.toString()) }
    var yMin by remember(settings) { mutableStateOf(settings.yMin.toString()) }
    var yMax by remember(settings) { mutableStateOf(settings.yMax.toString()) }
    var angleUnit by remember(settings) { mutableStateOf(settings.angleUnit) }
    var lineStyle by remember(settings) { mutableStateOf(settings.lineStyle) }
    var theme by remember(settings) { mutableStateOf(settings.theme) }
    var showLineDropdown by remember { mutableStateOf(false) }

    fun applyAndDismiss() {
        val s = settings.copy(
            xMin = xMin.toFloatOrNull() ?: settings.xMin,
            xMax = xMax.toFloatOrNull() ?: settings.xMax,
            yMin = yMin.toFloatOrNull() ?: settings.yMin,
            yMax = yMax.toFloatOrNull() ?: settings.yMax,
            angleUnit = angleUnit,
            lineStyle = lineStyle,
            theme = theme
        )
        onSettingsChange(s)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBg,
        title = { Text("图形选项", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 窗口
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("窗口", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onResetView() }) {
                        Text("重置视图", color = AccentPurple, fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionNumberField("X-最小值", xMin, { xMin = it }, Modifier.weight(1f))
                    OptionNumberField("X-最大值", xMax, { xMax = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionNumberField("Y-最小值", yMin, { yMin = it }, Modifier.weight(1f))
                    OptionNumberField("Y-最大值", yMax, { yMax = it }, Modifier.weight(1f))
                }

                // 单位
                Text("单位", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnitButton("弧度", angleUnit == AngleUnit.RADIANS, { angleUnit = AngleUnit.RADIANS }, Modifier.weight(1f))
                    UnitButton("度", angleUnit == AngleUnit.DEGREES, { angleUnit = AngleUnit.DEGREES }, Modifier.weight(1f))
                    UnitButton("百分度", angleUnit == AngleUnit.GRADIANS, { angleUnit = AngleUnit.GRADIANS }, Modifier.weight(1f))
                }

                // 线条粗细
                Text("线条粗细", color = TextSecondary, fontSize = 13.sp)
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ButtonBg)
                            .clickable { showLineDropdown = !showLineDropdown }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lineStyle.label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("▾", color = TextSecondary, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = showLineDropdown,
                        onDismissRequest = { showLineDropdown = false },
                        containerColor = SurfaceBg
                    ) {
                        LineStyle.values().forEach { ls ->
                            DropdownMenuItem(
                                text = { Text(ls.label, color = TextPrimary) },
                                onClick = { lineStyle = ls; showLineDropdown = false }
                            )
                        }
                    }
                }

                // 图形主题
                Text("图形主题", color = TextSecondary, fontSize = 13.sp)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { theme = GraphTheme.ALWAYS_LIGHT }) {
                        RadioButton(selected = theme == GraphTheme.ALWAYS_LIGHT, onClick = { theme = GraphTheme.ALWAYS_LIGHT }, colors = RadioButtonDefaults.colors(selectedColor = AccentPurple))
                        Text("始终亮", color = TextPrimary, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { theme = GraphTheme.MATCH_APP }) {
                        RadioButton(selected = theme == GraphTheme.MATCH_APP, onClick = { theme = GraphTheme.MATCH_APP }, colors = RadioButtonDefaults.colors(selectedColor = AccentPurple))
                        Text("匹配应用主题", color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { applyAndDismiss() }) { Text("确定", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@Composable
private fun OptionNumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = TextMuted, fontSize = 11.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ButtonBg,
                unfocusedContainerColor = ButtonBg,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = ButtonBgLight
            )
        )
    }
}

@Composable
private fun UnitButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) AccentPurple else ButtonBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else TextPrimary, fontSize = 13.sp)
    }
}

// =====================================================================
// 画布绘制(支持平移偏移 + 拖动 + 双指缩放 + 坐标轴刻度)
// =====================================================================
@Composable
private fun DrawCanvas(
    funcs: List<PlotFunc>,
    evaluator: ExpressionEvaluator,
    settings: GraphSettings,
    panEnabled: Boolean,
    onSizeChanged: (Float, Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onZoomBy: (Float, Float, Float) -> Unit
) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    // 图形主题:ALWAYS_LIGHT 用浅色背景+深色网格;MATCH_APP 跟随应用主题
    val isLight = settings.theme == GraphTheme.ALWAYS_LIGHT
    val gridMinor = if (isLight) Color(0xFFE6E6EC) else ButtonBg
    val gridMajor = if (isLight) Color(0xFFC8C8D2) else ButtonBgLight
    val axisColor = if (isLight) Color(0xFF3A3A44) else TextSecondary
    val labelColor = if (isLight) Color(0xFF3A3A44) else TextSecondary
    val coordBg = if (isLight) Color(0xFF222228) else Color(0xFFE6E6F0)
    val coordFg = if (isLight) Color.White else Color(0xFF1A1A22)

    // 点击显示坐标的状态
    var clickPos by remember { mutableStateOf<Offset?>(null) }
    var clickWorldX by remember { mutableStateOf(0f) }
    var clickWorldY by remember { mutableStateOf(0f) }

    Canvas(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { onSizeChanged(it.size.width.toFloat(), it.size.height.toFloat()) }
            .pointerInput(panEnabled) {
                // 单指拖动(平移)+ 双指捏合(缩放),统一由 transform 手势处理
                detectTransformGestures { centroid, pan, zoomGesture, _ ->
                    if (panEnabled && (kotlin.math.abs(pan.x) > 0.5f || kotlin.math.abs(pan.y) > 0.5f)) {
                        onPan(pan.x, pan.y)
                        clickPos = null  // 拖动时隐藏坐标提示
                    }
                    if (kotlin.math.abs(zoomGesture - 1f) > 0.001f) {
                        onZoomBy(zoomGesture, centroid.x, centroid.y)
                        clickPos = null
                    }
                }
            }
            .pointerInput(Unit) {
                // 单击显示点击处的坐标
                detectTapGestures { pos ->
                    // 计算世界坐标并短暂显示
                    val w = size.width; val h = size.height
                    val centerX = (settings.xMin + settings.xMax) / 2f
                    val centerY = (settings.yMin + settings.yMax) / 2f
                    val scaleX = w / (settings.xMax - settings.xMin)
                    val scaleY = h / (settings.yMax - settings.yMin)
                    val scale = minOf(scaleX, scaleY)
                    val cx = w / 2f - centerX * scale
                    val cy = h / 2f + centerY * scale
                    val wx = (pos.x - cx) / scale
                    val wy = (cy - pos.y) / scale
                    clickPos = pos
                    clickWorldX = wx
                    clickWorldY = wy
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 由窗口边界计算原点屏幕坐标与缩放比例(均匀缩放,完整显示)
        val centerX = (settings.xMin + settings.xMax) / 2f
        val centerY = (settings.yMin + settings.yMax) / 2f
        val scaleX = w / (settings.xMax - settings.xMin)
        val scaleY = h / (settings.yMax - settings.yMin)
        val scale = minOf(scaleX, scaleY)
        val cx = w / 2f - centerX * scale
        val cy = h / 2f + centerY * scale

        val xStep = niceStep(settings.xMax - settings.xMin)
        val yStep = niceStep(settings.yMax - settings.yMin)

        drawGrid(cx, cy, scale, w, h, gridMinor, gridMajor)
        drawAxes(cx, cy, w, h, scale, axisColor, xStep, yStep)
        drawAxisLabels(cx, cy, scale, w, h, labelColor, textMeasurer)

        val stroke = Stroke(
            width = settings.lineStyle.width,
            pathEffect = if (settings.lineStyle.dash) {
                androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            } else null
        )

        // 角度单位映射
        val angleMode = when (settings.angleUnit) {
            AngleUnit.RADIANS -> ExpressionEvaluator.AngleMode.RAD
            AngleUnit.DEGREES -> ExpressionEvaluator.AngleMode.DEG
            AngleUnit.GRADIANS -> ExpressionEvaluator.AngleMode.GRAD
        }

        funcs.forEach { f ->
            if (f.expr.isNotBlank() && !f.hidden) {
                drawFunction(f.expr, f.color, cx, cy, scale, w, h, evaluator, stroke, angleMode)
            }
        }

        // 点击坐标提示
        clickPos?.let { pos ->
            val text = "(${formatCoord(clickWorldX)}, ${formatCoord(clickWorldY)})"
            val layout = textMeasurer.measure(
                androidx.compose.ui.text.AnnotatedString(text),
                TextStyle(fontSize = 13.sp, color = coordFg)
            )
            val tw = layout.size.width.toFloat() + 16f
            val th = layout.size.height.toFloat() + 10f
            // 保证在画布内
            val px = (pos.x - tw / 2f).coerceIn(8f, w - tw - 8f)
            val py = (pos.y - th - 16f).coerceIn(8f, h - th - 8f)
            drawRoundRect(
                color = coordBg,
                topLeft = Offset(px, py),
                size = androidx.compose.ui.geometry.Size(tw, th),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            drawPath(
                Path().apply {
                    moveTo(pos.x, pos.y)
                    lineTo(px + tw / 2f, py + th)
                    lineTo(px + tw / 2f - 6f, py + th)
                    close()
                },
                color = coordBg
            )
            drawCircle(axisColor, radius = 4f, center = pos)
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset(px + 8f, py + 5f),
                style = TextStyle(fontSize = 13.sp, color = coordFg)
            )
        }
    }
}

private fun formatCoord(v: Float): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1000f -> String.format("%.1e", v)
        abs >= 10f -> String.format("%.1f", v)
        abs >= 1f -> String.format("%.2f", v)
        else -> String.format("%.3f", v).trimEnd('0').trimEnd('.')
    }
}

// =====================================================================
// 函数列表项(常驻 3 个图标按钮:分析 / 样式 / 删除)
// 左侧 f₁ 图标:默认显示函数颜色;点击可隐藏/显示曲线(变灰+划线);
// 在分析视图中(isBackMode),f₁ 图标变为返回按钮形态(< f₁)
// =====================================================================
@Composable
private fun FunctionListItem(
    index: Int,
    color: Color,
    expr: String,
    enabled: Boolean = true,
    hidden: Boolean = false,
    isBackMode: Boolean = false,
    onToggleHidden: () -> Unit = {},
    onBack: () -> Unit = {},
    onAnalyze: () -> Unit,
    onEditStyle: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(ButtonBg).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // f₁ 图标:分析视图中显示为返回按钮(< f₁);否则点击切换隐藏
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (hidden) TextMuted else color)
                .clickable(onClick = if (isBackMode) onBack else onToggleHidden),
            contentAlignment = Alignment.Center
        ) {
            if (isBackMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White, modifier = Modifier.size(13.dp))
                    Text("f${subscript(index + 1)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (hidden) {
                // 隐藏状态:划线 f₁
                Box(contentAlignment = Alignment.Center) {
                    Text("f${subscript(index + 1)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    // 斜杠
                    Canvas(Modifier.size(22.dp)) {
                        drawLine(Color.White, Offset(2f, size.height - 2f), Offset(size.width - 2f, 2f), 1.5f)
                    }
                }
            } else {
                Text("f${subscript(index + 1)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            expr,
            color = if (hidden) TextMuted else TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Row {
            IconButton(
                onClick = onAnalyze,
                enabled = enabled,
                modifier = Modifier.size(28.dp),
                content = { Icon(Icons.Default.AutoGraph, contentDescription = "分析函数", tint = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) }
            )
            IconButton(
                onClick = onEditStyle,
                enabled = enabled,
                modifier = Modifier.size(28.dp),
                content = { Icon(Icons.Default.Palette, contentDescription = "更改公式样式", tint = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) }
            )
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(28.dp),
                content = { Icon(Icons.Default.Delete, contentDescription = "删除", tint = if (enabled) TextMuted else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

// =====================================================================
// 函数输入行(支持光标控制 + 3 个常驻按钮,未输完时置灰)
// =====================================================================
@Composable
private fun FunctionInputRow(
    color: Color,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onEnter: () -> Unit,
    enabled: Boolean,
    onAnalyze: () -> Unit,
    onEditStyle: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(ButtonBg).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(color), contentAlignment = Alignment.Center) {
            Text("f", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(ButtonBgLight).padding(horizontal = 10.dp),
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
            singleLine = true,
            cursorBrush = SolidColor(AccentBlue),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.text.isEmpty()) {
                        Text("输入表达式", color = TextMuted, fontSize = 14.sp)
                    }
                    inner()
                }
            }
        )
        Row {
            IconButton(
                onClick = onAnalyze,
                enabled = enabled,
                modifier = Modifier.size(28.dp),
                content = { Icon(Icons.Default.AutoGraph, contentDescription = "分析函数", tint = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) }
            )
            IconButton(
                onClick = onEditStyle,
                enabled = enabled,
                modifier = Modifier.size(28.dp),
                content = { Icon(Icons.Default.Palette, contentDescription = "更改公式样式", tint = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) }
            )
            IconButton(
                onClick = onRemove,
                enabled = enabled || value.text.isNotEmpty(),
                modifier = Modifier.size(28.dp),
                content = { Icon(Icons.Default.Delete, contentDescription = "删除", tint = if (enabled || value.text.isNotEmpty()) TextMuted else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

private fun subscript(n: Int): String =
    n.toString().map { c -> "₀₁₂₃₄₅₆₇₈₉"[c.digitToInt()] }.joinToString("")

// =====================================================================
// 工具栏下拉按钮
// =====================================================================
@Composable
private fun ToolbarDropdown(label: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(6.dp)).background(if (expanded) SurfaceBg else ButtonBg)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text("▾", color = TextSecondary, fontSize = 12.sp)
    }
}

// =====================================================================
// 浮动弹窗
// =====================================================================
@Composable
private fun TrigPanel(is2nd: Boolean, onToggle2nd: () -> Unit, onAppend: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelKey("2ⁿᵈ", bg = if (is2nd) AccentPurple else ButtonBg, fg = if (is2nd) Color.White else TextPrimary, onClick = onToggle2nd)
            PanelKey(if (is2nd) "sin⁻¹" else "sin") { onAppend(if (is2nd) "asin(" else "sin(") }
            PanelKey(if (is2nd) "cos⁻¹" else "cos") { onAppend(if (is2nd) "acos(" else "cos(") }
            PanelKey(if (is2nd) "tan⁻¹" else "tan") { onAppend(if (is2nd) "atan(" else "tan(") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelKey("hyp") { onAppend(if (is2nd) "asinh(" else "sinh(") }
            PanelKey(if (is2nd) "sec⁻¹" else "sec") { onAppend(if (is2nd) "asec(" else "sec(") }
            PanelKey(if (is2nd) "csc⁻¹" else "csc") { onAppend(if (is2nd) "acsc(" else "csc(") }
            PanelKey(if (is2nd) "cot⁻¹" else "cot") { onAppend(if (is2nd) "acot(" else "cot(") }
        }
    }
}

@Composable
private fun IneqPanel(onAppend: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PanelKey("<") { onAppend("<") }
        PanelKey("≤") { onAppend("≤") }
        PanelKey("=") { onAppend("=") }
        PanelKey("≥") { onAppend("≥") }
        PanelKey(">") { onAppend(">") }
    }
}

@Composable
private fun FuncPanel(onAppend: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PanelKey("|x|") { onAppend("abs(") }
        PanelKey("⌊x⌋") { onAppend("floor(") }
        PanelKey("⌈x⌉") { onAppend("ceil(") }
    }
}

@Composable
private fun PanelKey(label: String, bg: Color = NumberBtnBg, fg: Color = TextPrimary, onClick: () -> Unit) {
    Box(Modifier.width(64.dp).height(48.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = fg, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

// =====================================================================
// 主按键面板
// =====================================================================
@Composable
private fun KeypadButtons(
    onAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEnter: () -> Unit,
    activeColor: Color,
    is2nd: Boolean,
    onToggle2nd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KeyRow(listOf(
            KeySpec("2ⁿᵈ", bg = if (is2nd) AccentPurple else ButtonBg, fg = if (is2nd) Color.White else TextPrimary, onClick = onToggle2nd),
            KeySpec("π", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("pi") },
            KeySpec("e", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("e") },
            KeySpec("C", bg = ButtonBg, fg = AccentRed, onClick = onClear),
            KeySpec("⌫", bg = ButtonBg, fg = TextPrimary, onClick = onBackspace),
        ))
        KeyRow(listOf(
            KeySpec("x²", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("^2") },
            KeySpec("⅟x", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("1/(") },
            KeySpec("|x|", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("abs(") },
            KeySpec("x", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("x") },
            KeySpec("y", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("y") },
        ))
        KeyRow(listOf(
            KeySpec("∛x̅", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("cbrt(") },
            KeySpec("(", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("(") },
            KeySpec(")", bg = OperatorBtnBg, fg = TextPrimary) { onAppend(")") },
            KeySpec("=", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("=") },
            KeySpec("÷", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("/") },
        ))
        KeyRow(listOf(
            KeySpec("xʸ", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("^") },
            KeySpec("7", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("7") },
            KeySpec("8", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("8") },
            KeySpec("9", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("9") },
            KeySpec("×", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("*") },
        ))
        KeyRow(listOf(
            KeySpec("10ˣ", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("10^") },
            KeySpec("4", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("4") },
            KeySpec("5", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("5") },
            KeySpec("6", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("6") },
            KeySpec("−", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("-") },
        ))
        KeyRow(listOf(
            KeySpec("log", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("log(") },
            KeySpec("1", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("1") },
            KeySpec("2", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("2") },
            KeySpec("3", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("3") },
            KeySpec("+", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("+") },
        ))
        KeyRow(listOf(
            KeySpec("ln", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("ln(") },
            KeySpec("(−)", bg = OperatorBtnBg, fg = TextPrimary) { onAppend("-") },
            KeySpec("0", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend("0") },
            KeySpec(".", bg = NumberBtnBg, fg = TextPrimary, num = true) { onAppend(".") },
            KeySpec("↵", bg = activeColor, fg = Color.White, onClick = onEnter),
        ))
    }
}

private data class KeySpec(val label: String, val bg: Color, val fg: Color, val num: Boolean = false, val onClick: () -> Unit)

@Composable
private fun KeyRow(keys: List<KeySpec>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEach { k ->
            Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(8.dp)).background(k.bg).clickable(onClick = k.onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(k.label, color = k.fg, fontSize = if (k.num) 20.sp else 16.sp,
                    fontWeight = if (k.num) FontWeight.Medium else FontWeight.Normal, textAlign = TextAlign.Center)
            }
        }
    }
}

// =====================================================================
// Canvas 绘图辅助
// =====================================================================
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFunction(
    expr: String, color: Color, cx: Float, cy: Float, scale: Float, w: Float, h: Float,
    evaluator: ExpressionEvaluator, stroke: Stroke,
    angleMode: ExpressionEvaluator.AngleMode
) {
    val path = Path()
    var first = true
    var lastY: Float? = null
    var jumped = false
    for (px in 0 until w.toInt()) {
        val x = (px - cx) / scale
        val y = try { evaluator.evaluateWithVars(expr, mapOf("x" to x.toDouble()), angleMode) } catch (_: Exception) { null }
        val yDouble = y?.toDouble()
        if (yDouble != null && !yDouble.isNaN() && !yDouble.isInfinite()) {
            val yf = yDouble.toFloat()
            if (kotlin.math.abs(yf) > 10000f) { lastY = null; first = true; continue }
            val screenY = cy - yf * scale
            if (lastY != null && kotlin.math.abs(screenY - lastY) > h * 0.7f) jumped = true
            if (first || jumped) { path.moveTo(px.toFloat(), screenY); first = false; jumped = false }
            else path.lineTo(px.toFloat(), screenY)
            lastY = screenY
        } else { lastY = null; first = true }
    }
    drawPath(path, color, style = stroke)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    cx: Float, cy: Float, scale: Float, w: Float, h: Float, minor: Color, major: Color
) {
    var x = cx
    while (x < w) { drawLine(if (((x - cx) / scale).toInt() % 5 == 0) major else minor, Offset(x, 0f), Offset(x, h), 0.5f); x += scale }
    x = cx - scale
    while (x > 0) { drawLine(if (((x - cx) / scale).toInt() % 5 == 0) major else minor, Offset(x, 0f), Offset(x, h), 0.5f); x -= scale }
    var y = cy
    while (y < h) { drawLine(if (((y - cy) / scale).toInt() % 5 == 0) major else minor, Offset(0f, y), Offset(w, y), 0.5f); y += scale }
    y = cy - scale
    while (y > 0) { drawLine(if (((y - cy) / scale).toInt() % 5 == 0) major else minor, Offset(0f, y), Offset(w, y), 0.5f); y -= scale }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxes(cx: Float, cy: Float, w: Float, h: Float, scale: Float, axisColor: Color, xStep: Float, yStep: Float) {
    // 轴线
    drawLine(axisColor, Offset(0f, cy), Offset(w, cy), 1.5f)
    drawLine(axisColor, Offset(cx, 0f), Offset(cx, h), 1.5f)

    // X 轴箭头(右端)
    val ah = 8f  // arrowhead size
    drawPath(
        Path().apply {
            moveTo(w - ah, cy - ah * 0.6f)
            lineTo(w, cy)
            lineTo(w - ah, cy + ah * 0.6f)
            close()
        },
        color = axisColor
    )
    // Y 轴箭头(上端)
    drawPath(
        Path().apply {
            moveTo(cx - ah * 0.6f, ah)
            lineTo(cx, 0f)
            lineTo(cx + ah * 0.6f, ah)
            close()
        },
        color = axisColor
    )

    // X 轴刻度(沿 X 轴线)
    var xv = cx - ((cx / scale) / xStep).toInt() * xStep * scale
    while (xv <= w) {
        val xVal = (xv - cx) / scale
        if (kotlin.math.abs(xVal) > 0.001f) {
            drawLine(axisColor, Offset(xv, cy - 3f), Offset(xv, cy + 3f), 1.2f)
        }
        xv += xStep * scale
    }

    // Y 轴刻度(沿 Y 轴线,用户特别要求)
    var yv = cy - ((cy / scale) / yStep).toInt() * yStep * scale
    while (yv <= h) {
        val yVal = (cy - yv) / scale
        if (kotlin.math.abs(yVal) > 0.001f) {
            drawLine(axisColor, Offset(cx - 3f, yv), Offset(cx + 3f, yv), 1.2f)
        }
        yv += yStep * scale
    }
}

/** 计算美观的刻度步长 */
private fun niceStep(range: Float, targetTicks: Int = 10): Float {
    val rawStep = range / targetTicks
    val pow10 = Math.pow(10.0, Math.floor(Math.log10(rawStep.toDouble()))).toFloat()
    val normalized = rawStep / pow10
    val nice = when {
        normalized < 1.5f -> 1f
        normalized < 3f -> 2f
        normalized < 7f -> 5f
        else -> 10f
    }
    return nice * pow10
}

/** 绘制坐标轴刻度标签 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxisLabels(
    cx: Float, cy: Float, scale: Float, w: Float, h: Float, color: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val xMin = (0 - cx) / scale
    val xMax = (w - cx) / scale
    // 屏幕坐标 y 向下递增,世界坐标 y 向上递增:worldY = (cy - screenY) / scale
    val yMin = (cy - h) / scale
    val yMax = cy / scale

    val xStep = niceStep(xMax - xMin)
    val yStep = niceStep(yMax - yMin)

    val labelStyle = TextStyle(fontSize = 11.sp, color = color)
    val boldStyle = TextStyle(fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)

    fun drawLabel(text: String, x: Float, y: Float, style: TextStyle = labelStyle) {
        drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = Offset(x, y),
            style = style
        )
    }

    fun measureWidth(text: String, style: TextStyle = labelStyle): Float {
        val layout = textMeasurer.measure(androidx.compose.ui.text.AnnotatedString(text), style)
        return layout.size.width.toFloat()
    }

    // X 轴标签
    val xStart = Math.ceil((xMin / xStep).toDouble()).toFloat() * xStep
    var xVal = xStart
    while (xVal <= xMax) {
        val screenX = cx + xVal * scale
        if (screenX > 20 && screenX < w - 20 && kotlin.math.abs(xVal) > 0.001f) {
            val label = formatLabel(xVal)
            val tw = measureWidth(label)
            val textY = if (cy < h - 20) cy + 14f else cy - 16f
            drawLabel(label, screenX - tw / 2, textY)
        }
        xVal += xStep
    }

    // Y 轴标签
    val yStart = Math.ceil((yMin / yStep).toDouble()).toFloat() * yStep
    var yVal = yStart
    while (yVal <= yMax) {
        val screenY = cy - yVal * scale
        if (screenY > 20 && screenY < h - 20 && kotlin.math.abs(yVal) > 0.001f) {
            val label = formatLabel(yVal)
            val tw = measureWidth(label)
            val textX = if (cx > 40) cx - tw - 6f else cx + 6f
            drawLabel(label, textX, screenY - 6f)
        }
        yVal += yStep
    }

    // 原点 "0"
    if (cx in 20f..(w - 20) && cy in 20f..(h - 20)) {
        val tw = measureWidth("0")
        drawLabel("0", cx - tw - 6f, cy + 14f)
    }

    // x / y 轴标记
    if (cx < w - 30) {
        drawLabel("x", w - 18f, cy - 6f, boldStyle)
    }
    if (cy > 30) {
        val tw = measureWidth("y", boldStyle)
        drawLabel("y", cx - tw / 2, 2f, boldStyle)
    }
}

private fun formatLabel(v: Float): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1000f || (abs < 0.01f && abs > 0f) -> String.format("%.1e", v)
        abs >= 100f -> String.format("%.0f", v)
        abs >= 10f -> String.format("%.1f", v)
        else -> String.format("%.2f", v).trimEnd('0').trimEnd('.')
    }
}

/**
 * 截图整个绘图界面并发送系统分享 Intent。
 * 用户可选择微信、微信收藏、隔空传图、豆包、文件管理、第三方 APP、复制图片 等多种方式。
 */
private fun shareGraph(context: Context, view: android.view.View) {
    try {
        val bitmap: Bitmap = view.drawToBitmap()
        val cacheDir = File(context.cacheDir, "share")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "graph_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "com.microsoft.calculator.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "图形分享")
        }
        context.startActivity(Intent.createChooser(intent, "分享图形"))
    } catch (_: Exception) {
        // 如果分享失败(例如还没完全布局),静默忽略
    }
}

// =====================================================================
// 函数分析视图内容(可滚动)
// =====================================================================
@Composable
private fun FunctionAnalysisContent(expr: String, evaluator: ExpressionEvaluator, angleUnit: AngleUnit) {
    val analysis = remember(expr, angleUnit) { analyzeFunction(expr, evaluator, angleUnit) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("函数分析", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("f(x) = $expr", color = AccentBlue, fontSize = 15.sp)

        AnalysisField("定义域", analysis.domain)
        AnalysisField("值域", analysis.range)
        AnalysisField("X 轴截距", analysis.xIntercept)
        AnalysisField("Y 轴截距", analysis.yIntercept)
        AnalysisField("极小值", analysis.minima)
        AnalysisField("极大值", analysis.maxima)
        AnalysisField("拐点", analysis.inflectionPoints)
        AnalysisField("垂直渐近线", analysis.verticalAsymptotes)
        AnalysisField("水平渐近线", analysis.horizontalAsymptotes)
        AnalysisField("斜渐近线", analysis.obliqueAsymptotes)
        AnalysisField("奇偶性", analysis.parity)
        AnalysisField("周期", analysis.period)
        AnalysisField("单调性", analysis.monotonicity)
    }
}

@Composable
private fun AnalysisField(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** 函数分析结果 */
private data class FunctionAnalysis(
    val domain: String,
    val range: String,
    val xIntercept: String,
    val yIntercept: String,
    val minima: String,
    val maxima: String,
    val inflectionPoints: String,
    val verticalAsymptotes: String,
    val horizontalAsymptotes: String = "此函数没有任何水平渐近线。",
    val obliqueAsymptotes: String = "此函数没有任何倾斜渐近线。",
    val parity: String = "此函数为非奇非偶函数。",
    val period: String = "无",
    val monotonicity: String = "无"
)

/** 符号常量 */
private const val PI = "π"
private const val Z = "Z"

/**
 * 主分析入口:优先尝试符号化模式匹配(sin/cos/tan/多项式等),
 * 匹配失败时回退到数值采样分析。
 */
private fun analyzeFunction(expr: String, evaluator: ExpressionEvaluator, angleUnit: AngleUnit): FunctionAnalysis {
    val symbolic = analyzeSymbolic(expr)
    if (symbolic != null) return symbolic
    return analyzeNumerical(expr, evaluator, angleUnit)
}

// =====================================================================
// 符号化分析:匹配常见函数并输出精确数学表达式
// =====================================================================
private fun analyzeSymbolic(expr: String): FunctionAnalysis? {
    val e = expr.replace(" ", "")

    // 1. 形如 [a*]*(sin|cos|tan|csc|sec|cot)([k*]x[±phase])
    Regex("""^(-?\d*\.?\d*)?\*?(sin|cos|tan|csc|sec|cot)\((-?\d*\.?\d*)?\*?x([+-]\d*\.?\d*pi?)?\)$""")
        .matchEntire(e)?.let { m ->
            val amp = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
            val fn = m.groupValues[2]
            val k = (m.groupValues[3].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
            val phase = parsePhase(m.groupValues[4])
            return when (fn) {
                "sin" -> analyzeSin(amp, k, phase)
                "cos" -> analyzeCos(amp, k, phase)
                "tan" -> analyzeTan(k, phase)
                "csc" -> analyzeCsc(amp, k, phase)
                "sec" -> analyzeSec(amp, k, phase)
                "cot" -> analyzeCot(k, phase)
                else -> null
            }
        }

    // 2. 形如 a*sin(x) + b*cos(x) 或 a*cos(x) + b*sin(x) → 合并为 R*sin(x+φ)
    Regex("""^(-?\d*\.?\d*)?\*?sin\(x\)([+-]\d*\.?\d*)?\*?cos\(x\)$""")
        .matchEntire(e)?.let { m ->
            val a = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
            val bStr = m.groupValues[2]
            val b = if (bStr.isEmpty()) 1.0 else (if (bStr == "+" || bStr == "-") bStr + "1" else bStr).toDoubleOrNull() ?: 1.0
            return analyzeSinCosSum(a, b)
        }
    Regex("""^(-?\d*\.?\d*)?\*?cos\(x\)([+-]\d*\.?\d*)?\*?sin\(x\)$""")
        .matchEntire(e)?.let { m ->
            val b = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
            val aStr = m.groupValues[2]
            val a = if (aStr.isEmpty()) 1.0 else (if (aStr == "+" || aStr == "-") aStr + "1" else aStr).toDoubleOrNull() ?: 1.0
            return analyzeSinCosSum(a, b)
        }

    // 3. 多项式: x^n
    Regex("""^(-?\d*\.?\d*)?\*?x\^(-?\d+)$""").matchEntire(e)?.let { m ->
        val coeff = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
        val n = m.groupValues[2].toInt()
        return analyzePower(coeff, n)
    }
    // 多项式: x (即 x^1)
    if (e == "x") return analyzePower(1.0, 1)
    Regex("""^(-?\d*\.?\d*)?\*?x$""").matchEntire(e)?.let { m ->
        val coeff = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
        return analyzePower(coeff, 1)
    }

    // 4. 线性: a*x + b
    Regex("""^(-?\d*\.?\d*)?\*?x([+-]\d*\.?\d+)$""").matchEntire(e)?.let { m ->
        val a = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
        val b = m.groupValues[2].toDoubleOrNull() ?: 0.0
        return analyzeLinear(a, b)
    }

    // 5. 二次: a*x^2 + b*x + c (简化匹配)
    Regex("""^(-?\d*\.?\d*)?\*?x\^2([+-]\d*\.?\d*)?\*?x([+-]\d*\.?\d+)?$""").matchEntire(e)?.let { m ->
        val a = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
        val b = if (m.groupValues[3].isNotEmpty()) m.groupValues[2].toDoubleOrNull() ?: 0.0 else 0.0
        val c = m.groupValues[3].toDoubleOrNull() ?: 0.0
        return analyzeQuadratic(a, b, c)
    }

    // 6. 反比例: a/x, 1/x, a/x + b
    Regex("""^(-?\d*\.?\d*)?\*?/x([+-]\d*\.?\d+)?$""").matchEntire(e)?.let { m ->
        val a = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
        val b = m.groupValues[2].toDoubleOrNull() ?: 0.0
        return analyzeReciprocal(a, b)
    }
    Regex("""^1/x\^(\d+)([+-]\d*\.?\d+)?$""").matchEntire(e)?.let { m ->
        val n = m.groupValues[1].toInt()
        val b = m.groupValues[2].toDoubleOrNull() ?: 0.0
        return analyzeInversePower(n, b)
    }

    // 7. 根号: sqrt(x), x^(1/2)
    if (e == "sqrt(x)" || e == "x^(1/2)") return analyzeSqrt(1.0, 0.0)
    Regex("""^(-?\d*\.?\d*)?\*?sqrt\(x\)([+-]\d*\.?\d+)?$""").matchEntire(e)?.let { m ->
        val a = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
        val b = m.groupValues[2].toDoubleOrNull() ?: 0.0
        return analyzeSqrt(a, b)
    }

    // 8. 对数: ln(x), log(x), log10(x)
    Regex("""^(ln|log|log10)\(x\)$""").matchEntire(e)?.let { m ->
        val base = when (m.groupValues[1]) {
            "ln" -> Math.E
            "log10" -> 10.0
            else -> 10.0
        }
        return analyzeLog(base)
    }

    // 9. 指数: e^x, exp(x), a^x
    if (e == "e^x" || e == "exp(x)") return analyzeExp(Math.E)
    Regex("""^(\d*\.?\d+)\^x$""").matchEntire(e)?.let { m ->
        val base = m.groupValues[1].toDoubleOrNull() ?: return null
        return analyzeExp(base)
    }

    // 10. 绝对值: abs(x), |x|
    if (e == "abs(x)" || e == "|x|") return analyzeAbs(1.0, 0.0)
    Regex("""^(-?\d*\.?\d*)?\*?abs\(x\)([+-]\d*\.?\d+)?$""").matchEntire(e)?.let { m ->
        val a = (m.groupValues[1].let { if (it.isEmpty() || it == "-") it + "1" else it }).toDoubleOrNull() ?: 1.0
        val b = m.groupValues[2].toDoubleOrNull() ?: 0.0
        return analyzeAbs(a, b)
    }

    return null
}

// =====================================================================
// sin(x)+cos(x) 型:a*sin(x)+b*cos(x) = R*sin(x+φ), R=√(a²+b²), φ=atan2(b,a)
// =====================================================================
private fun analyzeSinCosSum(a: Double, b: Double): FunctionAnalysis {
    val R = kotlin.math.sqrt(a * a + b * b)
    val phi = kotlin.math.atan2(b, a)  // phase
    val rStr = if (kotlin.math.abs(R - kotlin.math.round(R)) < 1e-9) R.toInt().toString()
    else if (kotlin.math.abs(R / kotlin.math.sqrt(2.0) - 1.0) < 1e-6) "√2"
    else if (kotlin.math.abs(R / kotlin.math.sqrt(3.0) - 1.0) < 1e-6) "√3"
    else fmt(R)

    val rootsExpr = "x = ${fmtX("πnᵢ", phi, 1.0)}, nᵢ ∈ $Z"
    val minExpr = "(${fmtX("3π/2 + 2πnᵢ", phi, 1.0)}, -$rStr), nᵢ ∈ $Z"
    val maxExpr = "(${fmtX("π/2 + 2πnᵢ", phi, 1.0)}, $rStr), nᵢ ∈ $Z"
    val inflExpr = "(${fmtX("πnᵢ", phi, 1.0)}, 0), nᵢ ∈ $Z"

    val parity = when {
        kotlin.math.abs(b) < 1e-9 -> "此函数为奇函数。"
        kotlin.math.abs(a) < 1e-9 -> "此函数为偶函数。"
        else -> "此函数既不是偶函数也不是奇函数。"
    }

    val decInterval = "(${fmtX("π/2 + 2πnᵢ", phi, 1.0)}, ${fmtX("3π/2 + 2πnᵢ", phi, 1.0)}), nᵢ ∈ $Z 下降"
    val incInterval = "(${fmtX("3π/2 + 2πnᵢ", phi, 1.0)}, ${fmtX("5π/2 + 2πnᵢ", phi, 1.0)}), nᵢ ∈ $Z 上升"

    return FunctionAnalysis(
        domain = "x ∈ ℝ",
        range = "y ∈ [-$rStr, $rStr]",
        xIntercept = rootsExpr,
        yIntercept = "y = ${fmt(b)}",
        minima = minExpr,
        maxima = maxExpr,
        inflectionPoints = inflExpr,
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = parity,
        period = "2π",
        monotonicity = "$decInterval\n$incInterval"
    )
}

// =====================================================================
// 多项式 a*x^n
// =====================================================================
private fun analyzePower(a: Double, n: Int): FunctionAnalysis {
    val aStr = if (kotlin.math.abs(a - 1.0) < 1e-9) "" else if (kotlin.math.abs(a + 1.0) < 1e-9) "-" else "${fmt(a)}*"
    val xn = "x" + superScript(n)

    val parity = when {
        n % 2 == 0 && kotlin.math.abs(a % (2 * a)) < 1e-9 -> "此函数为偶函数。"
        n % 2 == 1 -> "此函数为奇函数。"
        else -> "此函数为非奇非偶函数。"
    }
    // Actually parity depends only on n, not a
    val realParity = if (n % 2 == 0) "此函数为偶函数。" else "此函数为奇函数。"

    val domain = "x ∈ ℝ"
    val range = when {
        n % 2 == 0 && a > 0 -> "y ∈ [0, +∞)"
        n % 2 == 0 && a < 0 -> "y ∈ (-∞, 0]"
        n % 2 == 1 -> "y ∈ ℝ"
        else -> "y ∈ ℝ"
    }

    val xIntercept = "x = 0"
    val yIntercept = "y = 0"

    val minStr = "(0, 0) （最小值）"
    val maxStr = "(0, 0) （最大值）"
    val pair: Pair<String, String> = when {
        n % 2 == 0 && a > 0 -> minStr to "无"
        n % 2 == 0 && a < 0 -> "无" to maxStr
        else -> "无" to "无"
    }
    val minima = pair.first
    val maxima = pair.second

    val inflection = if (n >= 3) "(0, 0)" else "无"

    val mono = when {
        n == 1 && a > 0 -> "在 ℝ 上单调上升。"
        n == 1 && a < 0 -> "在 ℝ 上单调下降。"
        n % 2 == 0 && a > 0 -> "在 (-∞, 0) 上下降,在 (0, +∞) 上上升。"
        n % 2 == 0 && a < 0 -> "在 (-∞, 0) 上上升,在 (0, +∞) 上下降。"
        n % 2 == 1 && n > 1 && a > 0 -> "在 ℝ 上单调上升。"
        n % 2 == 1 && n > 1 && a < 0 -> "在 ℝ 上单调下降。"
        else -> "无"
    }

    return FunctionAnalysis(
        domain = domain,
        range = range,
        xIntercept = xIntercept,
        yIntercept = yIntercept,
        minima = minima,
        maxima = maxima,
        inflectionPoints = inflection,
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = realParity,
        period = "无",
        monotonicity = mono
    )
}

private fun superScript(n: Int): String {
    val map = mapOf('0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '-' to '⁻')
    return n.toString().map { map[it] ?: it }.joinToString("")
}

// =====================================================================
// 线性 a*x + b
// =====================================================================
private fun analyzeLinear(a: Double, b: Double): FunctionAnalysis {
    return FunctionAnalysis(
        domain = "x ∈ ℝ",
        range = "y ∈ ℝ",
        xIntercept = if (kotlin.math.abs(a) < 1e-9) "无" else "x = ${fmt(-b / a)}",
        yIntercept = "y = ${fmt(b)}",
        minima = "无",
        maxima = "无",
        inflectionPoints = "无",
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = if (kotlin.math.abs(a) < 1e-9) "此函数没有任何倾斜渐近线。" else "y = ${fmt(a)}x ${if (b >= 0) "+" else "-"} ${fmt(kotlin.math.abs(b))}",
        parity = if (kotlin.math.abs(b) < 1e-9) "此函数为奇函数。" else "此函数为非奇非偶函数。",
        period = "无",
        monotonicity = if (a > 0) "在 ℝ 上单调上升。" else if (a < 0) "在 ℝ 上单调下降。" else "无"
    )
}

// =====================================================================
// 二次 a*x² + b*x + c
// =====================================================================
private fun analyzeQuadratic(a: Double, b: Double, c: Double): FunctionAnalysis {
    val disc = b * b - 4 * a * c
    val xv = -b / (2 * a)
    val yv = a * xv * xv + b * xv + c
    val xvStr = fmt(xv)
    val yvStr = fmt(yv)

    val xIntercept = when {
        disc > 1e-9 -> "x = ${fmt((-b + kotlin.math.sqrt(disc)) / (2 * a))}, ${fmt((-b - kotlin.math.sqrt(disc)) / (2 * a))}"
        kotlin.math.abs(disc) <= 1e-9 -> "x = $xvStr"
        else -> "无（判别式 < 0）"
    }

    val minPt = "($xvStr, $yvStr) （最小值）"
    val maxPt = "($xvStr, $yvStr) （最大值）"
    val (minima, maxima) = if (a > 0) minPt to "无" else "无" to maxPt

    return FunctionAnalysis(
        domain = "x ∈ ℝ",
        range = if (a > 0) "y ∈ [$yvStr, +∞)" else "y ∈ (-∞, $yvStr]",
        xIntercept = xIntercept,
        yIntercept = "y = ${fmt(c)}",
        minima = minima,
        maxima = maxima,
        inflectionPoints = "无",
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = if (kotlin.math.abs(b) < 1e-9) "此函数为偶函数。" else "此函数为非奇非偶函数。",
        period = "无",
        monotonicity = if (a > 0) "在 (-∞, $xvStr) 上下降,在 ($xvStr, +∞) 上上升。" else "在 (-∞, $xvStr) 上上升,在 ($xvStr, +∞) 上下降。"
    )
}

// =====================================================================
// 反比例 a/x + b
// =====================================================================
private fun analyzeReciprocal(a: Double, b: Double): FunctionAnalysis {
    val bStr = if (kotlin.math.abs(b) < 1e-9) "" else " ${if (b > 0) "+" else "-"} ${fmt(kotlin.math.abs(b))}"
    return FunctionAnalysis(
        domain = "x ≠ 0",
        range = if (kotlin.math.abs(b) < 1e-9) "y ≠ 0" else "y ≠ ${fmt(b)}",
        xIntercept = if (kotlin.math.abs(a) < 1e-9 || kotlin.math.abs(b) > 1e-9) "无" else "无",
        yIntercept = "无",
        minima = "无",
        maxima = "无",
        inflectionPoints = "无",
        verticalAsymptotes = "x = 0",
        horizontalAsymptotes = "y = ${fmt(b)}",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = if (kotlin.math.abs(b) < 1e-9) "此函数为奇函数。" else "此函数为非奇非偶函数。",
        period = "无",
        monotonicity = if (a > 0) "在 (-∞, 0) 和 (0, +∞) 上单调下降。" else "在 (-∞, 0) 和 (0, +∞) 上单调上升。"
    )
}

// =====================================================================
// 1/x^n + b
// =====================================================================
private fun analyzeInversePower(n: Int, b: Double): FunctionAnalysis {
    val parity = if (n % 2 == 0) "此函数为偶函数。" else "此函数为奇函数。"
    return FunctionAnalysis(
        domain = "x ≠ 0",
        range = if (n % 2 == 0) "y > ${fmt(b)}" else "y ≠ ${fmt(b)}",
        xIntercept = "无",
        yIntercept = "无",
        minima = if (n % 2 == 0 && b > 0) "无" else "无",
        maxima = "无",
        inflectionPoints = "无",
        verticalAsymptotes = "x = 0",
        horizontalAsymptotes = "y = ${fmt(b)}",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = parity,
        period = "无",
        monotonicity = if (n % 2 == 1) "在 (-∞, 0) 和 (0, +∞) 上单调下降。" else "在 (-∞, 0) 上上升,在 (0, +∞) 上下降。"
    )
}

// =====================================================================
// a*sqrt(x) + b
// =====================================================================
private fun analyzeSqrt(a: Double, b: Double): FunctionAnalysis {
    return FunctionAnalysis(
        domain = "x ≥ 0",
        range = if (a > 0) "y ≥ ${fmt(b)}" else "y ≤ ${fmt(b)}",
        xIntercept = if (a > 0 && b <= 0) "x = ${fmt(b * b / (a * a))}" else if (a < 0 && b >= 0) "x = ${fmt(b * b / (a * a))}" else "无",
        yIntercept = "y = ${fmt(b)}",
        minima = if (a > 0) "(0, ${fmt(b)})（最小值）" else "无",
        maxima = if (a < 0) "(0, ${fmt(b)})（最大值）" else "无",
        inflectionPoints = "无",
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = "此函数为非奇非偶函数。",
        period = "无",
        monotonicity = if (a > 0) "在 [0, +∞) 上单调上升。" else "在 [0, +∞) 上单调下降。"
    )
}

// =====================================================================
// 对数 log_base(x)
// =====================================================================
private fun analyzeLog(base: Double): FunctionAnalysis {
    val baseStr = if (kotlin.math.abs(base - Math.E) < 1e-9) "e" else fmt(base)
    return FunctionAnalysis(
        domain = "x > 0",
        range = "y ∈ ℝ",
        xIntercept = "x = 1",
        yIntercept = "无",
        minima = "无",
        maxima = "无",
        inflectionPoints = "无",
        verticalAsymptotes = "x = 0",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = "此函数为非奇非偶函数。",
        period = "无",
        monotonicity = if (base > 1) "在 (0, +∞) 上单调上升。" else "在 (0, +∞) 上单调下降。"
    )
}

// =====================================================================
// 指数 base^x
// =====================================================================
private fun analyzeExp(base: Double): FunctionAnalysis {
    val baseStr = if (kotlin.math.abs(base - Math.E) < 1e-9) "e" else fmt(base)
    return FunctionAnalysis(
        domain = "x ∈ ℝ",
        range = "y > 0",
        xIntercept = "无",
        yIntercept = "y = 1",
        minima = "无",
        maxima = "无",
        inflectionPoints = "无",
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "y = 0",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = "此函数为非奇非偶函数。",
        period = "无",
        monotonicity = if (base > 1) "在 ℝ 上单调上升。" else if (base < 1) "在 ℝ 上单调下降。" else "无"
    )
}

// =====================================================================
// a*|x| + b
// =====================================================================
private fun analyzeAbs(a: Double, b: Double): FunctionAnalysis {
    return FunctionAnalysis(
        domain = "x ∈ ℝ",
        range = if (a > 0) "y ≥ ${fmt(b)}" else "y ≤ ${fmt(b)}",
        xIntercept = when {
            kotlin.math.abs(a) < 1e-9 && kotlin.math.abs(b) < 1e-9 -> "x ∈ ℝ"
            kotlin.math.abs(a) < 1e-9 -> "无"
            (a > 0 && b <= 0) || (a < 0 && b >= 0) -> "x = ±${fmt(kotlin.math.abs(b / a))}"
            else -> "无"
        },
        yIntercept = "y = ${fmt(b)}",
        minima = if (a > 0) "(0, ${fmt(b)})（最小值）" else "无",
        maxima = if (a < 0) "(0, ${fmt(b)})（最大值）" else "无",
        inflectionPoints = "无",
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = if (kotlin.math.abs(b) < 1e-9) "此函数为偶函数。" else "此函数为非奇非偶函数。",
        period = "无",
        monotonicity = if (a > 0) "在 (-∞, 0) 上下降,在 (0, +∞) 上上升。" else "在 (-∞, 0) 上上升,在 (0, +∞) 上下降。"
    )
}

// =====================================================================
// csc / sec / cot
// =====================================================================
private fun analyzeCsc(amp: Double, k: Double, phase: Double): FunctionAnalysis {
    val period = if (kotlin.math.abs(k) < 1e-9) "无" else "${fmtK(2.0 / kotlin.math.abs(k))}π"
    val asymp = "x = ${fmtX("πnᵢ", phase, k)}, nᵢ ∈ $Z"
    val absAmp = kotlin.math.abs(amp)
    return FunctionAnalysis(
        domain = "x ≠ ${fmtX("πnᵢ", phase, k)}, nᵢ ∈ $Z",
        range = "y ∈ (-∞, -$absAmp] ∪ [$absAmp, +∞)",
        xIntercept = "无",
        yIntercept = if (kotlin.math.abs(kotlin.math.sin(phase)) < 1e-9) "无" else "y = ${fmt(amp / kotlin.math.sin(phase))}",
        minima = if (amp > 0) "(${fmtX("π/2 + 2πnᵢ", phase, k)}, $absAmp), nᵢ ∈ $Z（局部极小）" else "无",
        maxima = if (amp < 0) "(${fmtX("π/2 + 2πnᵢ", phase, k)}, -$absAmp), nᵢ ∈ $Z（局部极大）" else "无",
        inflectionPoints = "无",
        verticalAsymptotes = asymp,
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = if (kotlin.math.abs(phase % Math.PI) < 1e-6) "此函数为奇函数。" else "此函数为非奇非偶函数。",
        period = period,
        monotonicity = "在每个连续区间内不单调。"
    )
}

private fun analyzeSec(amp: Double, k: Double, phase: Double): FunctionAnalysis {
    val period = if (kotlin.math.abs(k) < 1e-9) "无" else "${fmtK(2.0 / kotlin.math.abs(k))}π"
    val asymp = "x = ${fmtX("π/2 + πnᵢ", phase, k)}, nᵢ ∈ $Z"
    val absAmp = kotlin.math.abs(amp)
    return FunctionAnalysis(
        domain = "x ≠ ${fmtX("π/2 + πnᵢ", phase, k)}, nᵢ ∈ $Z",
        range = "y ∈ (-∞, -$absAmp] ∪ [$absAmp, +∞)",
        xIntercept = "无",
        yIntercept = if (kotlin.math.abs(kotlin.math.cos(phase)) < 1e-9) "无" else "y = ${fmt(amp / kotlin.math.cos(phase))}",
        minima = if (amp > 0) "(${fmtX("2πnᵢ", phase, k)}, $absAmp), nᵢ ∈ $Z（局部极小）" else "无",
        maxima = if (amp < 0) "(${fmtX("2πnᵢ", phase, k)}, -$absAmp), nᵢ ∈ $Z（局部极大）" else "无",
        inflectionPoints = "无",
        verticalAsymptotes = asymp,
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = if (kotlin.math.abs(phase % Math.PI) < 1e-6) "此函数为偶函数。" else "此函数为非奇非偶函数。",
        period = period,
        monotonicity = "在每个连续区间内不单调。"
    )
}

private fun analyzeCot(k: Double, phase: Double): FunctionAnalysis {
    val period = if (kotlin.math.abs(k) < 1e-9) "无" else "${fmtK(1.0 / kotlin.math.abs(k))}π"
    val asymp = "x = ${fmtX("πnᵢ", phase, k)}, nᵢ ∈ $Z"
    return FunctionAnalysis(
        domain = "x ≠ ${fmtX("πnᵢ", phase, k)}, nᵢ ∈ $Z",
        range = "y ∈ ℝ",
        xIntercept = "x = ${fmtX("π/2 + πnᵢ", phase, k)}, nᵢ ∈ $Z",
        yIntercept = if (kotlin.math.abs(kotlin.math.sin(phase)) < 1e-9) "无" else "y = ${fmt(kotlin.math.cos(phase) / kotlin.math.sin(phase))}",
        minima = "此函数没有极小值。",
        maxima = "此函数没有极大值。",
        inflectionPoints = "无",
        verticalAsymptotes = asymp,
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = if (kotlin.math.abs(phase % Math.PI) < 1e-6) "此函数为奇函数。" else "此函数为非奇非偶函数。",
        period = period,
        monotonicity = "在每个连续区间内单调下降。"
    )
}

/** 解析相位移字符串,如 "+pi"、"-pi/2"、"+3.14" */
private fun parsePhase(s: String): Double {
    if (s.isEmpty()) return 0.0
    val t = s.replace("pi", Math.PI.toString())
    // 支持分数形式 pi/2
    val frac = Regex("""^([+-]?)(\d*\.?\d*)/(\d*\.?\d*)$""").matchEntire(t)
    return if (frac != null) {
        val sign = if (frac.groupValues[1] == "-") -1.0 else 1.0
        val num = frac.groupValues[2].toDoubleOrNull() ?: 1.0
        val den = frac.groupValues[3].toDoubleOrNull() ?: 1.0
        sign * num / den
    } else {
        t.toDoubleOrNull() ?: 0.0
    }
}

/** 把相位(弧度)格式化为带 π 的字符串 */
private fun phaseToStr(p: Double): String {
    if (kotlin.math.abs(p) < 1e-9) return "0"
    val ratio = p / Math.PI
    val r = kotlin.math.round(ratio * 12.0) / 12.0  // 对齐到 π/12
    if (kotlin.math.abs(r) < 1e-9) return "0"
    val num = (r * 12).toInt()
    val den = 12
    val g = gcd(kotlin.math.abs(num), den)
    val n = num / g
    val d = den / g
    val sign = if (n < 0) "-" else ""
    val an = kotlin.math.abs(n)
    return when {
        d == 1 -> "${sign}${an}π"
        an == 1 -> "${sign}π/$d"
        else -> "${sign}${an}π/$d"
    }
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

/** 把系数 k 格式化为字符串(1 → 空, 2 → "2") */
private fun kToStr(k: Double): String {
    val ki = k.toInt()
    return if (kotlin.math.abs(k - ki) < 1e-9 && ki == 1) ""
    else if (kotlin.math.abs(k - ki) < 1e-9) ki.toString()
    else fmt(k)
}

/**
 * 格式化 x 表达式: sign * (inner - phase) / k
 * 当 k=1 且 phase=0 时,直接输出 inner(不带括号、不带除号)
 */
private fun fmtX(inner: String, phase: Double, k: Double): String {
    val ph = phaseToStr(phase)
    val a = kToStr(kotlin.math.abs(k))
    val sign = if (k < 0) "-" else ""
    val hasPhase = ph != "0"
    val hasDenom = a.isNotEmpty()

    val numerator = when {
        hasPhase -> "($inner - $ph)"
        else -> inner
    }
    return when {
        hasDenom && hasPhase -> "$sign$numerator/$a"
        hasDenom && !hasPhase -> "$sign$inner/$a"
        !hasDenom && hasPhase -> "$sign$numerator"
        else -> "$sign$inner"
    }
}

/** 分析 a*sin(k*x + phase) */
private fun analyzeSin(amp: Double, k: Double, phase: Double): FunctionAnalysis {
    val absAmp = kotlin.math.abs(amp)
    val period = if (kotlin.math.abs(k) < 1e-9) "无" else "${fmtK(2.0 / kotlin.math.abs(k))}π"

    val rootsExpr = "x = ${fmtX("πnᵢ", phase, k)}, nᵢ ∈ $Z"
    val minExpr = "(${fmtX("3π/2 + 2πnᵢ", phase, k)}, -${fmtAmp(absAmp)}), nᵢ ∈ $Z"
    val maxExpr = "(${fmtX("π/2 + 2πnᵢ", phase, k)}, ${fmtAmp(absAmp)}), nᵢ ∈ $Z"
    val inflExpr = "(${fmtX("πnᵢ", phase, k)}, 0), nᵢ ∈ $Z"

    val parity = when {
        kotlin.math.abs(phase % Math.PI) < 1e-6 -> "此函数为奇函数。"
        kotlin.math.abs((phase - Math.PI / 2) % Math.PI) < 1e-6 -> "此函数为偶函数。"
        else -> "此函数为非奇非偶函数。"
    }

    val decInterval = "(${fmtX("π/2 + 2πnᵢ", phase, k)}, ${fmtX("3π/2 + 2πnᵢ", phase, k)}), nᵢ ∈ $Z 下降"
    val incInterval = "(${fmtX("3π/2 + 2πnᵢ", phase, k)}, ${fmtX("5π/2 + 2πnᵢ", phase, k)}), nᵢ ∈ $Z 上升"
    val mono = if (amp < 0) "$incInterval\n$decInterval" else "$decInterval\n$incInterval"

    return FunctionAnalysis(
        domain = "x ∈ ℝ",
        range = "y ∈ [-${fmtAmp(absAmp)}, ${fmtAmp(absAmp)}]",
        xIntercept = rootsExpr,
        yIntercept = "y = ${fmt(amp * kotlin.math.sin(phase))}",
        minima = if (amp >= 0) minExpr else maxExpr,
        maxima = if (amp >= 0) maxExpr else minExpr,
        inflectionPoints = inflExpr,
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = parity,
        period = period,
        monotonicity = mono
    )
}

/** 分析 a*cos(k*x + phase) */
private fun analyzeCos(amp: Double, k: Double, phase: Double): FunctionAnalysis {
    val absAmp = kotlin.math.abs(amp)
    val period = if (kotlin.math.abs(k) < 1e-9) "无" else "${fmtK(2.0 / kotlin.math.abs(k))}π"

    val rootsExpr = "x = ${fmtX("π/2 + πnᵢ", phase, k)}, nᵢ ∈ $Z"
    val minExpr = "(${fmtX("π + 2πnᵢ", phase, k)}, -${fmtAmp(absAmp)}), nᵢ ∈ $Z"
    val maxExpr = "(${fmtX("2πnᵢ", phase, k)}, ${fmtAmp(absAmp)}), nᵢ ∈ $Z"
    val inflExpr = "(${fmtX("π/2 + πnᵢ", phase, k)}, 0), nᵢ ∈ $Z"

    val parity = when {
        kotlin.math.abs(phase % Math.PI) < 1e-6 -> "此函数为偶函数。"
        kotlin.math.abs((phase - Math.PI / 2) % Math.PI) < 1e-6 -> "此函数为奇函数。"
        else -> "此函数为非奇非偶函数。"
    }

    val decInterval = "(${fmtX("2πnᵢ", phase, k)}, ${fmtX("π + 2πnᵢ", phase, k)}), nᵢ ∈ $Z 下降"
    val incInterval = "(${fmtX("π + 2πnᵢ", phase, k)}, ${fmtX("2π + 2πnᵢ", phase, k)}), nᵢ ∈ $Z 上升"
    val mono = if (amp < 0) "$incInterval\n$decInterval" else "$decInterval\n$incInterval"

    return FunctionAnalysis(
        domain = "x ∈ ℝ",
        range = "y ∈ [-${fmtAmp(absAmp)}, ${fmtAmp(absAmp)}]",
        xIntercept = rootsExpr,
        yIntercept = "y = ${fmt(amp * kotlin.math.cos(phase))}",
        minima = if (amp >= 0) minExpr else maxExpr,
        maxima = if (amp >= 0) maxExpr else minExpr,
        inflectionPoints = inflExpr,
        verticalAsymptotes = "此函数没有任何垂直渐近线。",
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = parity,
        period = period,
        monotonicity = mono
    )
}

/** 分析 tan(k*x + phase) */
private fun analyzeTan(k: Double, phase: Double): FunctionAnalysis {
    val period = if (kotlin.math.abs(k) < 1e-9) "无" else "${fmtK(1.0 / kotlin.math.abs(k))}π"

    val rootsExpr = "x = ${fmtX("πnᵢ", phase, k)}, nᵢ ∈ $Z"
    val asymp = "x = ${fmtX("π/2 + πnᵢ", phase, k)}, nᵢ ∈ $Z"
    val domain = "x ≠ ${fmtX("π/2 + πnᵢ", phase, k)}, nᵢ ∈ $Z"

    return FunctionAnalysis(
        domain = domain,
        range = "y ∈ ℝ",
        xIntercept = rootsExpr,
        yIntercept = "y = ${fmt(kotlin.math.tan(phase))}",
        minima = "此函数没有极小值。",
        maxima = "此函数没有极大值。",
        inflectionPoints = "(${fmtX("πnᵢ", phase, k)}, 0), nᵢ ∈ $Z",
        verticalAsymptotes = asymp,
        horizontalAsymptotes = "此函数没有任何水平渐近线。",
        obliqueAsymptotes = "此函数没有任何倾斜渐近线。",
        parity = if (kotlin.math.abs(phase % Math.PI) < 1e-6) "此函数为奇函数。" else "此函数为非奇非偶函数。",
        period = period,
        monotonicity = "在每个连续区间内单调上升。"
    )
}

private fun fmtAmp(v: Double): String {
    val vi = v.toInt()
    return if (kotlin.math.abs(v - vi) < 1e-9) vi.toString() else fmt(v)
}

private fun fmtK(v: Double): String {
    val vi = v.toInt()
    return if (kotlin.math.abs(v - vi) < 1e-9) vi.toString() else fmt(v)
}

// =====================================================================
// 数值采样分析(符号匹配失败时的回退方案)
// =====================================================================
private fun analyzeNumerical(expr: String, evaluator: ExpressionEvaluator, angleUnit: AngleUnit): FunctionAnalysis {
    val angleMode = when (angleUnit) {
        AngleUnit.RADIANS -> ExpressionEvaluator.AngleMode.RAD
        AngleUnit.DEGREES -> ExpressionEvaluator.AngleMode.DEG
        AngleUnit.GRADIANS -> ExpressionEvaluator.AngleMode.GRAD
    }

    val N = 4000
    val xMin = -100.0
    val xMax = 100.0
    val dx = (xMax - xMin) / N

    data class Pt(val x: Double, val y: Double)
    val points = ArrayList<Pt>(N + 1)
    var prevY: Double? = null
    var prevX: Double? = null
    val discontinuities = mutableListOf<Double>()
    val roots = mutableListOf<Double>()
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    val yAtXList = ArrayList<Pt>()

    for (i in 0..N) {
        val x = xMin + i * dx
        val yBig = try { evaluator.evaluateWithVars(expr, mapOf("x" to x), angleMode) } catch (_: Exception) { null }
        val y = yBig?.toDouble()
        if (y == null || y.isNaN() || y.isInfinite()) {
            points.add(Pt(x, Double.NaN))
        } else {
            val yd = y.toDouble()
            points.add(Pt(x, yd))
            yAtXList.add(Pt(x, yd))
            if (yd < minY) minY = yd
            if (yd > maxY) maxY = yd
            if (prevY != null && !prevY.isNaN() && prevY * yd < 0.0) {
                val root = prevX!! + (0.0 - prevY) * (x - prevX!!) / (yd - prevY)
                roots.add(root)
            }
            if (prevY != null && !prevY.isNaN()) {
                val jump = kotlin.math.abs(yd - prevY)
                if (jump > 100.0 && kotlin.math.abs(prevY) > 1.0) {
                    discontinuities.add((prevX!! + x) / 2.0)
                }
            }
            prevY = yd
            prevX = x
        }
    }

    // 定义域
    val domain = if (discontinuities.isEmpty() && yAtXList.size == N + 1) {
        "x ∈ ℝ"
    } else if (discontinuities.isEmpty()) {
        "x ∈ ℝ(已采样区间)"
    } else {
        val parts = discontinuities.sorted().map { "x ≠ ${fmt(it)}" }
        "x ∈ ℝ, ${parts.joinToString(", ")}"
    }

    // 值域
    val range = if (minY.isInfinite() || maxY.isInfinite()) {
        "y ∈ ℝ"
    } else {
        "y ∈ [${fmt(minY)}, ${fmt(maxY)}]"
    }

    // X 轴截距
    val xIntercept = if (roots.isEmpty()) {
        "无"
    } else {
        val merged = mutableListOf<Double>()
        for (r in roots.sorted()) {
            if (merged.isEmpty() || kotlin.math.abs(r - merged.last()) > 0.01) merged.add(r)
        }
        if (merged.size <= 6) {
            "x = " + merged.joinToString(", ") { fmt(it) }
        } else {
            "x = ${merged.take(6).joinToString(", ") { fmt(it) }}, …"
        }
    }

    // Y 轴截距
    val y0Big = try { evaluator.evaluateWithVars(expr, mapOf("x" to 0.0), angleMode) } catch (_: Exception) { null }
    val y0 = y0Big?.toDouble()
    val yIntercept = if (y0 == null || y0.isNaN() || y0.isInfinite()) "无" else "y = ${fmt(y0)}"

    // 极值
    val minima = mutableListOf<Pt>()
    val maxima = mutableListOf<Pt>()
    for (i in 1 until yAtXList.size - 1) {
        val (x0, y0p) = yAtXList[i - 1]
        val (x1, y1p) = yAtXList[i]
        val (x2, y2p) = yAtXList[i + 1]
        if (y1p < y0p && y1p < y2p && y0p - y1p > 1e-6 && y2p - y1p > 1e-6) minima.add(Pt(x1, y1p))
        if (y1p > y0p && y1p > y2p && y1p - y0p > 1e-6 && y1p - y2p > 1e-6) maxima.add(Pt(x1, y1p))
    }

    fun fmtPts(list: List<Pt>): String {
        if (list.isEmpty()) return "无"
        val filtered = mutableListOf<Pt>()
        for (p in list) {
            if (filtered.isEmpty() || kotlin.math.abs(p.x - filtered.last().x) > 0.5) filtered.add(p)
        }
        return if (filtered.size <= 6) {
            filtered.joinToString(", ") { "(${fmt(it.x)}, ${fmt(it.y)})" }
        } else {
            filtered.take(6).joinToString(", ") { "(${fmt(it.x)}, ${fmt(it.y)})" } + ", …"
        }
    }

    // 拐点
    val inflections = mutableListOf<Double>()
    if (yAtXList.size > 4) {
        var prevConcavity = 0
        for (i in 2 until yAtXList.size - 2) {
            val d2 = yAtXList[i + 1].y - 2 * yAtXList[i].y + yAtXList[i - 1].y
            val concavity = when {
                d2 > 1e-9 -> 1
                d2 < -1e-9 -> -1
                else -> prevConcavity
            }
            if (prevConcavity != 0 && concavity != 0 && concavity != prevConcavity) {
                inflections.add(yAtXList[i].x)
            }
            if (concavity != 0) prevConcavity = concavity
        }
    }
    val inflectionPoints = if (inflections.isEmpty()) "无"
    else {
        val merged = mutableListOf<Double>()
        for (x in inflections.sorted()) {
            if (merged.isEmpty() || kotlin.math.abs(x - merged.last()) > 0.5) merged.add(x)
        }
        if (merged.size <= 6) merged.joinToString(", ") { "(${fmt(it)}, 0)" }
        else merged.take(6).joinToString(", ") { "(${fmt(it)}, 0)" } + ", …"
    }

    // 垂直渐近线
    val verticalAsymptotes = if (discontinuities.isEmpty()) "此函数没有任何垂直渐近线。"
    else {
        val merged = mutableListOf<Double>()
        for (x in discontinuities.sorted()) {
            if (merged.isEmpty() || kotlin.math.abs(x - merged.last()) > 0.5) merged.add(x)
        }
        "x = " + merged.joinToString(", ") { fmt(it) }
    }

    return FunctionAnalysis(
        domain = domain,
        range = range,
        xIntercept = xIntercept,
        yIntercept = yIntercept,
        minima = fmtPts(minima),
        maxima = fmtPts(maxima),
        inflectionPoints = inflectionPoints,
        verticalAsymptotes = verticalAsymptotes
    )
}

private fun fmt(v: Double): String {
    if (v.isNaN()) return "NaN"
    if (v.isInfinite()) return if (v > 0) "+∞" else "-∞"
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 100000.0 || (abs < 0.001 && abs > 0.0) -> String.format("%.2e", v)
        abs >= 100.0 -> String.format("%.1f", v)
        abs >= 1.0 -> String.format("%.3f", v).trimEnd('0').trimEnd('.')
        else -> String.format("%.4f", v).trimEnd('0').trimEnd('.')
    }
}
