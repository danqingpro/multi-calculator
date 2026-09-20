package com.microsoft.calculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.calculator.ui.components.CalcButton
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.ui.theme.*
import com.microsoft.calculator.viewmodel.CalculatorViewModel

@Composable
fun StandardScreen(
    vm: CalculatorViewModel,
    showHistory: Boolean,
    showMemory: Boolean
) {
    val display by vm.display.collectAsState()
    val expr by vm.expression.collectAsState()
    val isError by vm.isError.collectAsState()
    val memory by vm.memory.collectAsState()
    val history by vm.history.collectAsState()

    Column(Modifier.fillMaxSize().background(DarkBg).padding(8.dp)) {
        // 显示区
        DisplayArea(expr, display, isError)
        Spacer(Modifier.height(8.dp))
        // 内存按钮行
        MemoryButtonRow(
            hasMemory = memory.isNotEmpty(),
            onMC = { vm.memoryClear() },
            onMR = { vm.memoryRecall() },
            onMPlus = { vm.memoryAdd() },
            onMMinus = { vm.memorySubtract() },
            onMS = { vm.memoryStore() }
        )
        Spacer(Modifier.height(8.dp))
        // 主按钮区 + 侧边面板
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                StandardButtons(vm)
            }
            if (showHistory || showMemory) {
                SidePanel(
                    modifier = Modifier.width(180.dp),
                    showHistory = showHistory,
                    showMemory = showMemory,
                    history = history,
                    memory = memory,
                    onClearHistory = { vm.clearHistory() },
                    onMemoryClear = { vm.memoryClear() }
                )
            }
        }
    }
}

@Composable
fun DisplayArea(expr: String, display: String, isError: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            expr,
            color = TextSecondary,
            fontSize = 18.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        val displayColor = if (isError) ErrorColor else TextPrimary
        Text(
            display,
            color = displayColor,
            fontSize = 46.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MemoryButtonRow(
    hasMemory: Boolean,
    onMC: () -> Unit, onMR: () -> Unit, onMPlus: () -> Unit, onMMinus: () -> Unit, onMS: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val memChipBg = MemoryChipBg
        val disabledColor = TextMuted
        listOf(
            "MC" to (onMC),
            "MR" to (onMR),
            "M+" to (onMPlus),
            "M-" to (onMMinus),
            "MS" to (onMS)
        ).forEachIndexed { i, (label, action) ->
            val enabled = hasMemory || label == "MS" || label == "M+"
            CalcButton(
                text = label,
                onClick = { if (enabled) action() },
                modifier = Modifier.weight(1f).height(40.dp),
                bg = memChipBg,
                fg = if (enabled) TextSecondary else disabledColor,
                fontSize = 13.sp
            )
        }
        if (hasMemory) {
            Box(Modifier.weight(1f)) {
                Text("M", color = AccentBlue, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun StandardButtons(vm: CalculatorViewModel) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("%", { vm.percent() }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("CE", { vm.clearEntry() }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("C", { vm.clear() }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("⌫", { vm.backspace() }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("¹⁄ₓ", { vm.applyUnaryFunction("inv") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("x²", { vm.applyUnaryFunction("sqr") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("√", { vm.applyUnaryFunction("sqrt") }, Modifier.weight(1f), bg = ButtonBg, fg = TextPrimary)
            CalcButton("÷", { vm.inputOperator("÷") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("7", { vm.inputDigit("7") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("8", { vm.inputDigit("8") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("9", { vm.inputDigit("9") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("×", { vm.inputOperator("×") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("4", { vm.inputDigit("4") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("5", { vm.inputDigit("5") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("6", { vm.inputDigit("6") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("-", { vm.inputOperator("-") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("1", { vm.inputDigit("1") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("2", { vm.inputDigit("2") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("3", { vm.inputDigit("3") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("+", { vm.inputOperator("+") }, Modifier.weight(1f), bg = OperatorBtnBg, fg = AccentBlue)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("±", { vm.applyUnaryFunction("neg") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("0", { vm.inputDigit("0") }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton(".", { vm.inputDecimal() }, Modifier.weight(1f), bg = NumberBtnBg)
            CalcButton("=", { vm.equals() }, Modifier.weight(1f), bg = EqualsBtnBg, fg = EqualsBtnFg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SidePanel(
    modifier: Modifier = Modifier,
    showHistory: Boolean,
    showMemory: Boolean,
    history: List<com.microsoft.calculator.viewmodel.HistoryItem>,
    memory: List<com.microsoft.calculator.viewmodel.MemoryItem>,
    onClearHistory: () -> Unit,
    onMemoryClear: () -> Unit
) {
    Column(modifier.fillMaxHeight().background(PanelBg, RoundedCornerShape(8.dp))) {
        val s = LocalStrings.current
        if (showHistory) {
            Text(s.history, color = TextSecondary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(12.dp, 8.dp))
            if (history.isEmpty()) {
                Text(s.noHistory, color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp, 4.dp))
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(history.reversed()) { item ->
                        Column(Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(item.expr, color = TextMuted, fontSize = 11.sp)
                            Text(item.result, color = TextPrimary, fontSize = 16.sp,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
                TextButton(onClick = onClearHistory) {
                    Text(s.clearAll, color = AccentBlue, fontSize = 12.sp)
                }
            }
        }
        if (showMemory) {
            Divider(color = Divider)
            Text(s.memory, color = TextSecondary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(12.dp, 8.dp))
            if (memory.isEmpty()) {
                Text(s.noMemory, color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp, 4.dp))
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(memory) { item ->
                        Text(item.value, color = TextPrimary, fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp))
                    }
                }
                TextButton(onClick = onMemoryClear) {
                    Text(s.clear, color = AccentBlue, fontSize = 12.sp)
                }
            }
        }
    }
}
