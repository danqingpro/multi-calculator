package com.microsoft.calculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.calculator.ui.components.CalcButton
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.ui.theme.*
import com.microsoft.calculator.viewmodel.UnitConverterViewModel

/**
 * 单位换算界面。
 * 导航栏点击某个类别后直接进入该类别对应的换算界面,
 * 不再在界面内显示所有类别的选择器,每个类别都是独立的界面。
 */
@Composable
fun ConverterScreen(vm: UnitConverterViewModel) {
    val fromIdx by vm.fromIdx.collectAsState()
    val toIdx by vm.toIdx.collectAsState()
    val fromValue by vm.fromValue.collectAsState()
    val toValue by vm.toValue.collectAsState()

    val s = LocalStrings.current
    val units = vm.fromUnitLabels(s.langCode)
    val supplementary = vm.supplementaryValues(s.langCode)

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 输入值 + 单位
        ConverterValueRow(
            value = fromValue,
            units = units,
            selectedIdx = fromIdx,
            onUnitSelected = { vm.setFromIdx(it) }
        )

        Spacer(Modifier.height(8.dp))

        // 结果值 + 单位
        ConverterValueRow(
            value = toValue,
            units = units,
            selectedIdx = toIdx,
            onUnitSelected = { vm.setToIdx(it) }
        )

        // 「约等于」补充单位行(角度/功率等)
        if (supplementary.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.approximately, color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                supplementary.forEachIndexed { i, (v, label) ->
                    Text(v, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(3.dp))
                    Text(label, color = TextSecondary, fontSize = 13.sp)
                    if (i < supplementary.size - 1) Spacer(Modifier.width(10.dp))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // 数字键盘
        NumericKeypad(
            onDigit = { vm.inputDigit(it) },
            onDecimal = { vm.inputDecimal() },
            onBackspace = { vm.backspace() },
            onClear = { vm.clear() },
            onNegate = { vm.negate() }
        )
    }
}

/**
 * 单行:大号数值 + 单位下拉按钮。
 */
@Composable
private fun ConverterValueRow(
    value: String,
    units: List<Pair<String, String>>,
    selectedIdx: Int,
    onUnitSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val unitName = units.getOrNull(selectedIdx)?.second ?: ""

    Column(Modifier.fillMaxWidth()) {
        Text(
            value.ifEmpty { "0" },
            color = TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Box {
            Surface(
                color = ButtonBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Row(
                    Modifier
                        .clickableNoRipple { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(unitName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = PanelBg
            ) {
                units.forEachIndexed { i, (_, lbl) ->
                    DropdownMenuItem(
                        text = { Text(lbl, color = TextPrimary) },
                        onClick = { onUnitSelected(i); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onNegate: () -> Unit
) {
    val btnH = 56.dp
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("CE", { onClear() }, Modifier.weight(1f).height(btnH), bg = ButtonBg, fg = AccentRed, fontSize = 18.sp)
            CalcButton("⌫", { onBackspace() }, Modifier.weight(1f).height(btnH), bg = ButtonBg, fg = TextPrimary, fontSize = 20.sp)
        }
        listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    CalcButton(d, { onDigit(d) }, Modifier.weight(1f).height(btnH), bg = NumberBtnBg, fg = TextPrimary)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("+/−", { onNegate() }, Modifier.weight(1f).height(btnH), bg = NumberBtnBg, fg = TextPrimary, fontSize = 18.sp)
            CalcButton("0", { onDigit("0") }, Modifier.weight(1f).height(btnH), bg = NumberBtnBg, fg = TextPrimary)
            CalcButton(".", { onDecimal() }, Modifier.weight(1f).height(btnH), bg = NumberBtnBg, fg = TextPrimary)
        }
    }
}

/** 无 ripple 的点击 */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        Modifier.clickableNoRippleInner(interactionSource, onClick)
    )
}

@Composable
private fun Modifier.clickableNoRippleInner(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    onClick: () -> Unit
): Modifier {
    return this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    )
}
