package com.microsoft.calculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.calculator.ui.components.CalcButton
import com.microsoft.calculator.ui.theme.*
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.viewmodel.CalculatorViewModel

@Composable
fun ScientificScreen(
    vm: CalculatorViewModel,
    showHistory: Boolean,
    showMemory: Boolean
) {
    val display by vm.display.collectAsState()
    val expr by vm.expression.collectAsState()
    val isError by vm.isError.collectAsState()
    val angleMode by vm.angleMode.collectAsState()
    val memory by vm.memory.collectAsState()
    val history by vm.history.collectAsState()
    val isSecond by vm.isSecondFunction.collectAsState()
    val isHyp by vm.isHyp.collectAsState()
    val fixExp by vm.fixExp.collectAsState()

    Column(Modifier.fillMaxSize().background(DarkBg).padding(8.dp)) {
        // ===== 显示区:顶部表达式 + 底部数值 =====
        DisplayArea(expr, display, isError)

        // ===== RAD / F-E 按钮行 =====
        // ===== DEG/RAD/GRAD + F-E 按钮行(全部靠左,对标参考图) =====
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TextButton(onClick = { vm.toggleAngleMode() }) {
                Text(
                    when (angleMode) {
                        CalculatorViewModel.AngleMode.DEG -> "DEG"
                        CalculatorViewModel.AngleMode.RAD -> "RAD"
                        CalculatorViewModel.AngleMode.GRAD -> "GRAD"
                    },
                    color = AccentBlue, fontSize = 13.sp
                )
            }
            TextButton(onClick = { vm.toggleFixExp() }) {
                Text(
                    "F-E",
                    color = if (fixExp) AccentBlue else TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // ===== 内存按钮行 =====
        MemoryButtonRow(
            hasMemory = memory.isNotEmpty(),
            onMC = { vm.memoryClear() }, onMR = { vm.memoryRecall() },
            onMPlus = { vm.memoryAdd() }, onMMinus = { vm.memorySubtract() },
            onMS = { vm.memoryStore() }
        )
        Spacer(Modifier.height(6.dp))

        // ===== 主按钮区 + 侧边面板 =====
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                // 三角学 / 函数 下拉菜单行
                TrigAndFunctionMenuRow(vm, isSecond, isHyp)
                Spacer(Modifier.height(6.dp))
                // 按钮网格
                ScientificButtons(vm, isSecond, isHyp)
            }
            if (showHistory || showMemory) {
                SidePanel(
                    modifier = Modifier.width(180.dp),
                    showHistory = showHistory, showMemory = showMemory,
                    history = history, memory = memory,
                    onClearHistory = { vm.clearHistory() },
                    onMemoryClear = { vm.memoryClear() }
                )
            }
        }
    }
}

// ========== 三角学 / 函数 下拉菜单行 ==========
@Composable
private fun TrigAndFunctionMenuRow(
    vm: CalculatorViewModel,
    isSecond: Boolean,
    isHyp: Boolean
) {
    var trigExpanded by remember { mutableStateOf(false) }
    var funcExpanded by remember { mutableStateOf(false) }

    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // 三角学下拉
        Box(Modifier.weight(1f)) {
            MenuDropdownButton(
                label = s.trigonometry,
                expanded = trigExpanded,
                onClick = { trigExpanded = !trigExpanded; funcExpanded = false }
            )
            DropdownMenu(
                expanded = trigExpanded,
                onDismissRequest = { trigExpanded = false },
                containerColor = PanelBg
            ) {
                TrigDropdownContent(vm, isSecond, isHyp) { trigExpanded = false }
            }
        }
        // 函数下拉
        Box(Modifier.weight(1f)) {
            MenuDropdownButton(
                label = s.functions,
                expanded = funcExpanded,
                onClick = { funcExpanded = !funcExpanded; trigExpanded = false }
            )
            DropdownMenu(
                expanded = funcExpanded,
                onDismissRequest = { funcExpanded = false },
                containerColor = PanelBg
            ) {
                FunctionDropdownContent(vm) { funcExpanded = false }
            }
        }
    }
}

@Composable
private fun MenuDropdownButton(label: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ButtonBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}

/** 三角学下拉内容:sin cos tan / hyp sec csc cot(受 2ⁿᵈ 与 hyp 影响) */
@Composable
private fun TrigDropdownContent(
    vm: CalculatorViewModel,
    isSecond: Boolean,
    isHyp: Boolean,
    onDismiss: () -> Unit
) {
    // 根据 2ⁿᵈ 和 hyp 组合决定按钮标签与输入的函数名
    val second = isSecond
    val hyp = isHyp
    fun trigFn(base: String): Pair<String, String> {
        // base: sin, cos, tan, sec, csc, cot
        val label = when {
            second && hyp -> "a" + base + "h"  // asinh, acosh, ...
            second -> "a" + base                // asin, acos, ...
            hyp -> base + "h"                   // sinh, cosh, ...
            else -> base
        }
        return label to label  // 显示名与输入到表达式的函数名相同
    }

    val sinFn = trigFn("sin")
    val cosFn = trigFn("cos")
    val tanFn = trigFn("tan")
    val secFn = trigFn("sec")
    val cscFn = trigFn("csc")
    val cotFn = trigFn("cot")

    // 第一行:2ⁿᵈ, sin, cos, tan
    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DropdownCalcButton(
            "2ⁿᵈ",
            { vm.toggleSecondFunction(); },
            highlighted = isSecond,
            width = 60.dp
        )
        DropdownCalcButton(sinFn.first, { vm.inputOp(sinFn.second); onDismiss() })
        DropdownCalcButton(cosFn.first, { vm.inputOp(cosFn.second); onDismiss() })
        DropdownCalcButton(tanFn.first, { vm.inputOp(tanFn.second); onDismiss() })
    }
    // 第二行:hyp(开关) sec csc cot
    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DropdownCalcButton(
            "hyp",
            { vm.toggleHyp(); },
            highlighted = isHyp,
            width = 60.dp
        )
        DropdownCalcButton(secFn.first, { vm.inputOp(secFn.second); onDismiss() })
        DropdownCalcButton(cscFn.first, { vm.inputOp(cscFn.second); onDismiss() })
        DropdownCalcButton(cotFn.first, { vm.inputOp(cotFn.second); onDismiss() })
    }
}

/** 函数下拉内容:|x| ⌊x⌋ ⌈x⌉ / rand →dms →deg */
@Composable
private fun FunctionDropdownContent(vm: CalculatorViewModel, onDismiss: () -> Unit) {
    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DropdownCalcButton("|x|", { vm.inputOp("abs"); onDismiss() })
        DropdownCalcButton("⌊x⌋", { vm.inputOp("floor"); onDismiss() })
        DropdownCalcButton("⌈x⌉", { vm.inputOp("ceil"); onDismiss() })
    }
    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DropdownCalcButton("rand", { vm.inputOp("rand"); onDismiss() })
        DropdownCalcButton("→dms", { vm.inputOp("dms"); onDismiss() })
        DropdownCalcButton("→deg", { vm.inputOp("deg"); onDismiss() })
    }
}

@Composable
private fun DropdownCalcButton(
    text: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    width: androidx.compose.ui.unit.Dp = 56.dp
) {
    Box(
        Modifier
            .width(width)
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) AccentBlue else ButtonBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (highlighted) Color.White else TextPrimary,
            fontSize = 13.sp
        )
    }
}

// ========== 主按钮网格(5 列 × 7 行,对标图1) ==========
@Composable
private fun ScientificButtons(vm: CalculatorViewModel, isSecond: Boolean, isHyp: Boolean) {
    val gap = Arrangement.spacedBy(6.dp)
    Column(Modifier.fillMaxSize(), verticalArrangement = gap) {
        // 第1行:2ⁿᵈ  π  e  CE  ⌫
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
            CalcButton(
                "2ⁿᵈ", { vm.toggleSecondFunction() }, Modifier.weight(1f),
                bg = if (isSecond) AccentBlue else ButtonBg,
                fg = if (isSecond) Color.White else TextPrimary, fontSize = 13.sp
            )
            CalcButton("π", { vm.inputOp("pi") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("e", { vm.inputOp("e") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("CE", { vm.clearEntry() }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("⌫", { vm.backspace() }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
        }
        // 第2行:x²  ¹⁄ₓ  |x|  exp  mod
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
            CalcButton("x²", { vm.inputOp("sqr") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("¹⁄ₓ", { vm.inputOp("inv") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 12.sp)
            CalcButton("|x|", { vm.inputOp("abs") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("exp", { vm.inputOp("exp") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 13.sp)
            CalcButton("mod", { vm.inputOperator("mod") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 13.sp)
        }
        // 第3行:³√x  (  )  n!  ÷
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
            CalcButton("³√x", { vm.inputOp("cbrt") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 12.sp)
            CalcButton("(", { vm.inputOp("(") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton(")", { vm.inputOp(")") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("n!", { vm.inputOp("fact") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 13.sp)
            CalcButton("÷", { vm.inputOperator("÷") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        // 第4行:xʸ  7  8  9  ×
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
            CalcButton("xʸ", { vm.inputOperator("pow") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 13.sp)
            CalcButton("7", { vm.inputDigit("7") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("8", { vm.inputDigit("8") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("9", { vm.inputDigit("9") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("×", { vm.inputOperator("×") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        // 第5行:10ˣ  4  5  6  −
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
            CalcButton("10ˣ", { vm.inputOp("10x") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 12.sp)
            CalcButton("4", { vm.inputDigit("4") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("5", { vm.inputDigit("5") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("6", { vm.inputDigit("6") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("−", { vm.inputOperator("-") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        // 第6行:log  1  2  3  +
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
            CalcButton("log", { vm.inputOp("log") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 13.sp)
            CalcButton("1", { vm.inputDigit("1") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("2", { vm.inputDigit("2") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("3", { vm.inputDigit("3") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("+", { vm.inputOperator("+") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        // 第7行:ln  +/−  0  .  =
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
            CalcButton("ln", { vm.inputOp("ln") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary, fontSize = 13.sp)
            CalcButton("+/−", { vm.inputOp("neg") }, Modifier.weight(1f), bg = NumberBtnBg, fg = TextPrimary, fontSize = 12.sp)
            CalcButton("0", { vm.inputDigit("0") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton(".", { vm.inputDecimal() }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("=", { vm.equals() }, Modifier.weight(1f), bg = EqualsBtnBg, fg = EqualsBtnFg, fontWeight = FontWeight.Bold)
        }
    }
}
