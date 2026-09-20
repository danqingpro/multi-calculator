package com.microsoft.calculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.calculator.ui.components.CalcButton
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.ui.theme.*
import com.microsoft.calculator.viewmodel.ProgrammerViewModel

@Composable
fun ProgrammerScreen(vm: ProgrammerViewModel) {
    val radix by vm.radix.collectAsState()
    val width by vm.width.collectAsState()
    val shiftMode by vm.shiftMode.collectAsState()
    val mainDisplay by vm.mainDisplay.collectAsState()
    val hexView by vm.hexView.collectAsState()
    val decView by vm.decView.collectAsState()
    val octView by vm.octView.collectAsState()
    val binView by vm.binView.collectAsState()
    val memoryNotEmpty by vm.memoryNotEmpty.collectAsState()
    // 顶部表达式:左操作数(按当前进制) + 运算符,例如 "10 +";无待运算时为 null
    val pendingExpression by vm.pendingExpression.collectAsState()
    val s = LocalStrings.current

    // 面板切换:键盘面板 / 位面板
    var showBitPanel by remember { mutableStateOf(false) }

    // 下拉菜单展开状态
    var bitwiseMenuExpanded by remember { mutableStateOf(false) }
    var shiftMenuExpanded by remember { mutableStateOf(false) }
    var widthMenuExpanded by remember { mutableStateOf(false) }
    var memoryMenuExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(DarkBg)) {
        // -------- 区域1:主显示区(独立右侧大图) --------
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1.15f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // 顶部区域:上一次的结果 + 当前输入的运算符(如 "10 +"),对标 Windows 计算器。
                // 无待执行运算时留空(不显示任何占位),与下方大数字形成两区域。
                if (pendingExpression != null) {
                    Text(
                        pendingExpression!!,
                        color = TextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                    )
                }
                AutoSizeText(
                    text = mainDisplay,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    minFontSize = 18.sp,
                    maxFontSize = 50.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // -------- 区域2:4 进制显示列(HEX/DEC/OCT/BIN,每条横条) --------
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            RadixBar("HEX", hexView, radix == ProgrammerViewModel.Radix.HEX,
                onClick = { vm.setRadix(ProgrammerViewModel.Radix.HEX) })
            RadixBar("DEC", decView, radix == ProgrammerViewModel.Radix.DEC,
                onClick = { vm.setRadix(ProgrammerViewModel.Radix.DEC) })
            RadixBar("OCT", octView, radix == ProgrammerViewModel.Radix.OCT,
                onClick = { vm.setRadix(ProgrammerViewModel.Radix.OCT) })
            RadixBar("BIN", binView, radix == ProgrammerViewModel.Radix.BIN,
                onClick = { vm.setRadix(ProgrammerViewModel.Radix.BIN) })
        }

        Divider(color = Divider, thickness = 0.7.dp, modifier = Modifier.padding(vertical = 4.dp))

        // -------- 区域3:工具栏(图标 + 宽度 + 内存) --------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 键盘面板图标
            IconTab(
                selected = !showBitPanel,
                onClick = { showBitPanel = false },
                icon = Icons.Default.Keyboard
            )
            // 位面板图标
            IconTab(
                selected = showBitPanel,
                onClick = { showBitPanel = true },
                icon = Icons.Default.Storage,
                underlineColor = AccentPurple
            )
            Spacer(Modifier.weight(1f))
            // 宽度下拉
            Box {
                CalcButton(
                    text = width.label,
                    onClick = { widthMenuExpanded = true },
                    modifier = Modifier.height(28.dp).width(96.dp),
                    bg = SurfaceBg, fg = TextPrimary, fontSize = 11.sp
                )
                DropdownMenu(
                    expanded = widthMenuExpanded,
                    onDismissRequest = { widthMenuExpanded = false },
                    containerColor = PanelBg
                ) {
                    ProgrammerViewModel.Width.entries.forEach { w ->
                        DropdownMenuItem(
                            text = {
                                Text(w.label, color = if (width == w) AccentBlue else TextPrimary)
                            },
                            onClick = { vm.setWidth(w); widthMenuExpanded = false }
                        )
                    }
                }
            }
            // MS (Memory Store)
            CalcButton(
                text = "MS",
                onClick = { vm.memoryStore() },
                modifier = Modifier.height(28.dp).width(44.dp),
                bg = ButtonBg,
                fg = if (memoryNotEmpty) TextPrimary else TextMuted,
                fontSize = 11.sp
            )
            // M^ 下拉
            Box {
                CalcButton(
                    text = "M~",
                    onClick = { memoryMenuExpanded = true },
                    modifier = Modifier.height(28.dp).width(44.dp),
                    bg = ButtonBg,
                    fg = if (memoryNotEmpty) TextPrimary else TextMuted,
                    fontSize = 11.sp
                )
                DropdownMenu(
                    expanded = memoryMenuExpanded,
                    onDismissRequest = { memoryMenuExpanded = false },
                    containerColor = PanelBg
                ) {
                    DropdownMenuItem(
                        text = { Text(s.mrRecall, color = TextPrimary) },
                        onClick = { vm.memoryRecall(); memoryMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(s.mAdd, color = TextPrimary) },
                        onClick = { vm.memoryAdd(); memoryMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(s.mSubtract, color = TextPrimary) },
                        onClick = { vm.memorySubtract(); memoryMenuExpanded = false }
                    )
                }
            }
        }

        // -------- 区域4:按位/位移菜单行 + C图标/复制图标 行(无论是否显示位面板,都显示这行) --------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 「按位」下拉:左侧有"带下划线的D"图标效果
            Box(Modifier.weight(2f)) {
                ButtonWithLeadingIcon(
                    text = s.bitwise,
                    onClick = { bitwiseMenuExpanded = true },
                    leadingText = "D̲"
                )
                DropdownMenu(
                    expanded = bitwiseMenuExpanded,
                    onDismissRequest = { bitwiseMenuExpanded = false },
                    containerColor = PanelBg
                ) {
                    val bitOps = listOf("AND", "OR", "NOT", "NAND", "NOR", "XOR")
                    bitOps.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { op ->
                                CalcButton(
                                    text = op,
                                    onClick = {
                                        if (op == "NOT") vm.applyNot()
                                        else vm.applyBinaryOp(op.lowercase())
                                        bitwiseMenuExpanded = false
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    bg = SurfaceBg, fg = TextPrimary, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            // 「位移位」下拉:左侧有"《》"图标
            Box(Modifier.weight(2f)) {
                ButtonWithLeadingIcon(
                    text = "${s.shift} ${shiftModeArrowLabel(shiftMode)}",
                    onClick = { shiftMenuExpanded = true },
                    leadingText = "» "
                )
                DropdownMenu(
                    expanded = shiftMenuExpanded,
                    onDismissRequest = { shiftMenuExpanded = false },
                    containerColor = PanelBg
                ) {
                    val modes = listOf(
                        ProgrammerViewModel.ShiftMode.ARITHMETIC to s.arithmeticShift,
                        ProgrammerViewModel.ShiftMode.LOGICAL to s.logicalShift,
                        ProgrammerViewModel.ShiftMode.ROTATE to s.rotateShift,
                        ProgrammerViewModel.ShiftMode.ROTATE_CARRY to s.rotateCarryShift
                    )
                    modes.forEach { (m, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = if (shiftMode == m) AccentBlue else TextPrimary) },
                            onClick = { vm.setShiftMode(m); shiftMenuExpanded = false },
                            leadingIcon = {
                                RadioButton(
                                    selected = shiftMode == m,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                                )
                            }
                        )
                    }
                    Divider(color = Divider)
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp, 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalcButton(
                            text = "<<",
                            onClick = { vm.applyBinaryOp("shl"); shiftMenuExpanded = false },
                            modifier = Modifier.weight(1f).height(40.dp),
                            bg = SurfaceBg, fg = AccentBlue, fontSize = 15.sp
                        )
                        CalcButton(
                            text = ">>",
                            onClick = { vm.applyBinaryOp("shr"); shiftMenuExpanded = false },
                            modifier = Modifier.weight(1f).height(40.dp),
                            bg = SurfaceBg, fg = AccentBlue, fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // -------- 区域5:位面板 / 键盘面板(根据 showBitPanel 切换) --------
        if (showBitPanel) {
            Box(Modifier.weight(4.5f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
                BitPanelV2(vm = vm)
            }
        } else {
            Box(Modifier.weight(5f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
                KeyboardGridV2(vm)
            }
        }
    }
}

// ============ 组件 ============

/** 图标 Tab(选中时可选底部下划线高亮) */
@Composable
private fun IconTab(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    underlineColor: Color = AccentBlue
) {
    val density = LocalDensity.current
    Box(
        Modifier
            .size(30.dp)
            .background(
                if (selected) SurfaceBg else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .drawBehind {
                if (selected) {
                    val stroke = with(density) { 2.dp.toPx() }
                    val y = size.height - stroke / 2
                    drawLine(
                        color = underlineColor,
                        start = Offset(size.width * 0.2f, y),
                        end = Offset(size.width * 0.8f, y),
                        strokeWidth = stroke
                    )
                }
            }
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (selected) AccentBlue else TextMuted,
            modifier = Modifier.size(18.dp).align(Alignment.Center)
        )
    }
}

/** 「按位 / 位移位」按钮:左侧有前缀字符(D带下划线 / » 等) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ButtonWithLeadingIcon(
    text: String,
    onClick: () -> Unit,
    leadingText: String
) {
    Surface(
        onClick = onClick,
        color = ButtonBg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp).fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            // 前导图标字符(D̲ 或 » )
            Text(
                leadingText,
                color = AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Text(text, color = TextPrimary, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text("˅", color = TextMuted, fontSize = 10.sp)
        }
    }
}

/** 进制横条:左侧蓝色短竖线(选中) + 标签 + 值 */
@Composable
private fun RadixBar(
    label: String,
    value: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 蓝色短竖线
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .background(
                    if (active) AccentBlue else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (active) AccentBlue else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(40.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            color = if (active) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 自适应字号文本:根据 maxFontSize 和 minFontSize 动态缩小。
 * 参考图中 HEX 超长(774A AAAA...)不会被截断,而是自动变小。
 */
@Composable
private fun AutoSizeText(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    textAlign: TextAlign,
    maxLines: Int,
    minFontSize: TextUnit,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    val overflowReady = remember { mutableStateOf(false) }
    // 简化:按照字符长度估算字号
    val len = text.length
    val estimated = when {
        len <= 8 -> maxFontSize
        len <= 14 -> 42.sp
        len <= 20 -> 34.sp
        len <= 26 -> 28.sp
        len <= 32 -> 23.sp
        len <= 42 -> 19.sp
        else -> minFontSize
    }
    if (estimated != fontSize) fontSize = estimated
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** 位移模式箭头短标签 */
private fun shiftModeArrowLabel(m: ProgrammerViewModel.ShiftMode): String = when (m) {
    ProgrammerViewModel.ShiftMode.ARITHMETIC -> ""
    ProgrammerViewModel.ShiftMode.LOGICAL -> "L"
    ProgrammerViewModel.ShiftMode.ROTATE -> "R"
    ProgrammerViewModel.ShiftMode.ROTATE_CARRY -> "C"
}

// ============ 键盘布局 V2 ============

@Composable
private fun KeyboardGridV2(vm: ProgrammerViewModel) {
    // 关键:直接在函数体内订阅 radix / width,确保此 @Composable 在这些状态变化时一定被重绘,
    // 从而重新计算每个 CellDigit 的 avail = isDigitAvailable,刷新 禁用/启用 的 bg+fg。
    // 如果不写这两行,Compose 可能认为 vm(稳定单例)未变,跳过函数体,导致进制切换时按键灰阶不刷新。
    val radix by vm.radix.collectAsState()
    val width by vm.width.collectAsState()
    // 显式读取(避免被编译器优化掉)
    @Suppress("UNUSED_VARIABLE") val _key = radix to width

    // 严格对标参考图
    // Row 1: A   <<   >>   CE   ⧉   ← 按位和位移位菜单已在上半部分,这里改为: A  <<  >>  CE  ⧉
    // Row 2: B   (    )    %    ÷
    // Row 3: C   7    8    9    ×
    // Row 4: D   4    5    6    −
    // Row 5: E   1    2    3    +
    // Row 6: F  +/-   0    .    =  (紫色 =)
    val rows = listOf(
        listOf(D("A"), O("<<"), O(">>"), CLR("C"), BS("⌫")),
        listOf(D("B"), P("("), P(")"), O2("%"), O("÷")),
        listOf(D("C"), D("7"), D("8"), D("9"), O("×")),
        listOf(D("D"), D("4"), D("5"), D("6"), O("−")),
        listOf(D("E"), D("1"), D("2"), D("3"), O("+")),
        listOf(D("F"), NG("+/−"), D("0"), DT("."), EQ("="))
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { cell ->
                    when (cell) {
                        is CellDigit -> {
                            val avail = vm.isDigitAvailable(cell.d)
                            CalcButton(
                                text = cell.d,
                                onClick = { if (avail) vm.inputDigit(cell.d) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                bg = if (avail) NumberBtnBg else ButtonBg,
                                fg = if (avail) TextPrimary else TextMuted,
                                fontSize = 18.sp
                            )
                        }
                        is CellOp -> CalcButton(
                            text = cell.s,
                            onClick = {
                                when (cell.s) {
                                    "<<" -> vm.applyBinaryOp("shl")
                                    ">>" -> vm.applyBinaryOp("shr")
                                    "×" -> vm.applyBinaryOp("*")
                                    "÷" -> vm.applyBinaryOp("/")
                                    "−" -> vm.applyBinaryOp("-")
                                    "+" -> vm.applyBinaryOp("+")
                                    else -> {}
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = OperatorBtnBg, fg = AccentBlue, fontSize = 20.sp
                        )
                        is CellOp2 -> CalcButton(
                            text = cell.s,
                            onClick = { vm.applyBinaryOp("%") },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = OperatorBtnBg, fg = AccentBlue, fontSize = 18.sp
                        )
                        is CellParen -> CalcButton(
                            text = cell.s,
                            onClick = { /* 程序员模式不支持括号表达式 */ },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = ButtonBg, fg = TextMuted, fontSize = 18.sp
                        )
                        is CellClear -> CalcButton(
                            text = cell.s,
                            onClick = {
                                if (cell.s == "C") vm.clear()
                                else vm.clearEntry()
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = ButtonBg, fg = AccentRed, fontSize = 13.sp
                        )
                        is CellBackspace -> CalcButton(
                            text = cell.s,
                            onClick = { vm.backspace() },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = ButtonBg, fg = AccentRed, fontSize = 18.sp
                        )
                        is CellCopy -> CalcButton(
                            text = "⧉",
                            onClick = { /* 复制到剪贴板,预留 */ },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = ButtonBg, fg = TextSecondary, fontSize = 15.sp
                        )
                        is CellNeg -> CalcButton(
                            text = cell.s,
                            onClick = { vm.negate() },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = NumberBtnBg, fg = TextPrimary, fontSize = 13.sp
                        )
                        is CellDot -> CalcButton(
                            text = cell.s,
                            onClick = { /* 程序员模式无小数,禁用 */ },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = DisabledDotBg, fg = TextMuted, fontSize = 18.sp
                        )
                        is CellEq -> CalcButton(
                            text = cell.s,
                            onClick = { vm.equals() },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            bg = EqualsBtnBg, fg = EqualsBtnFg,
                            fontWeight = FontWeight.SemiBold, fontSize = 22.sp
                        )
                    }
                }
            }
        }
    }
}

// ============ Cell 类型 ============
private sealed class KeyCell
private data class CellDigit(val d: String) : KeyCell()
private data class CellOp(val s: String) : KeyCell()
private data class CellOp2(val s: String) : KeyCell()
private data class CellParen(val s: String) : KeyCell()
private data class CellClear(val s: String) : KeyCell()  // C / CE
private data class CellBackspace(val s: String) : KeyCell()  // ⌫
private data class CellCopy(val s: String = "⧉") : KeyCell()
private data class CellNeg(val s: String) : KeyCell()
private data class CellDot(val s: String) : KeyCell()
private data class CellEq(val s: String) : KeyCell()

private fun D(d: String) = CellDigit(d)
private fun O(s: String) = CellOp(s)
private fun O2(s: String) = CellOp2(s)
private fun P(s: String) = CellParen(s)
private fun CL(s: String) = CellClear(s)
// C = Clear all
private fun CLR(s: String) = CellClear(s)
// ⌫ = Backspace 删除最后一位
private fun BS(s: String) = CellBackspace(s)
private fun COPY() = CellCopy()
private fun NG(s: String) = CellNeg(s)
private fun DT(s: String) = CellDot(s)
private fun EQ(s: String) = CellEq(s)

// ============ 位面板 V2:每位显示 0/1 文本 ============

/**
 * 位面板 V2 —— 修复「必须先切顶栏图标后点击才生效」的根因方案:
 *
 * 原根因:内层 BitPanelV2Content 通过 vm.bitAt(pos) 读取 ViewModel 的普通 var value(非 StateFlow),
 * Compose 编译器把 vm 视为 @Stable 单例 → 没有读取到任何 snapshot state → 函数体被当作无需重绘。
 * 只有 showBitPanel 切换(真的销毁+重建整个 BitPanelV2)才会重新跑 vm.bitAt()。
 *
 * 修复:
 * 1) ViewModel 新增 bitPanelBits StateFlow,每次 sync() 生成新 BooleanArray(64) 并赋值,
 *    确保 toggleBit / inputDigit / setWidth 等所有修改 value 的路径都有明确状态推送。
 * 2) 这里直接 collectAsState 拿到 bits 数组,每个 cell 用 bits[pos] 读值(纯数据类/数组),
 *    Compose 肯定能识别它是参数变化,重绘不再依赖任何顶栏切换或 key() 黑魔法。
 * 3) 保留 width 也当参数,因为宽度决定 isAvailable 高位灰显。
 */
@Composable
private fun BitPanelV2(vm: ProgrammerViewModel) {
    // 关键订阅:只订阅真正驱动 UI 的两个状态——宽度 & 64 位数组。
    // 两者任一变化 → 此 @Composable 必定重组 → 重新传入 BitPanelV2Content 作为新参数。
    val width by vm.width.collectAsState()
    val bits by vm.bitPanelBits.collectAsState()

    BitPanelV2Content(
        onToggle = { pos -> vm.toggleBit(pos) },
        width = width,
        bits = bits
    )
}

/**
 * 纯渲染层:不持有任何 ViewModel 引用,只靠参数驱动。
 * 这样 100% 不受 Compose「Stable 对象未读状态」优化影响。
 */
@Composable
private fun BitPanelV2Content(
    onToggle: (Int) -> Unit,
    width: ProgrammerViewModel.Width,
    bits: BooleanArray
) {
    // 固定 4 行 × 16 位 = 64 位布局
    val validBits = width.bits

    val rows = 4
    val cols = 16
    val bitsPerGroup = 4
    val groupsPerRow = cols / bitsPerGroup // 4 组

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (r in 0 until rows) {
            // ---- 位行 ----
            Row(Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (g in 0 until groupsPerRow) {
                    // 每组 4 位,内部再紧密排布
                    Row(Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (b in 0 until bitsPerGroup) {
                            // 位置:行 0 = 最高位(63~48);行 3 = 最低位(15~0)
                            // group 0 = 该行左起最高 4 位
                            val colIndexInRow = g * bitsPerGroup + b // 0..15
                            val globalBitPos = 63 - (r * cols + colIndexInRow)
                            val isAvailable = globalBitPos < validBits
                            // 直接从 bits 数组读,不再走 vm.bitAt()
                            val on = isAvailable && bits[globalBitPos]
                            // 颜色:1=紫色(AccentPurple),0=灰色;高位区整体更灰(透明化)
                            val borderColor = when {
                                !isAvailable -> TextMuted.copy(alpha = 0.33f)
                                on -> AccentPurple
                                else -> TextMuted.copy(alpha = 0.6f)
                            }
                            val textColor = when {
                                !isAvailable -> TextMuted.copy(alpha = 0.38f)
                                on -> AccentPurple
                                else -> TextMuted
                            }
                            // 单层 Box:weight 给水平空间 + fillMaxHeight 撑满行高,
                            // clip(CircleShape) 把矩形裁成圆形,
                            // border(CircleShape) 画圆环,
                            // clickable 直接挂在 Box 上。
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(1.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .border(
                                        width = 0.7.dp,
                                        color = borderColor,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .then(
                                        if (isAvailable) Modifier.clickable {
                                            onToggle(globalBitPos)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (on) "1" else "0",
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
            // ---- 组标签行(60 56 52 48 等):只有属于可用位宽的标签才亮,否则同样变灰 ----
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (g in 0 until groupsPerRow) {
                    // 每组最末位(最右)的 bit position(参考图:60/56/52/48)
                    val pos = 63 - (r * cols + g * bitsPerGroup + 3)
                    val labelAvailable = pos in 0 until validBits
                    Box(Modifier.weight(1f)) {
                        Text(
                            "$pos",
                            color = if (labelAvailable) TextMuted else TextMuted.copy(alpha = 0.38f),
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
}

