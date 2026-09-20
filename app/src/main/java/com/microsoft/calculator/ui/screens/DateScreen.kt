@file:OptIn(ExperimentalMaterial3Api::class)

package com.microsoft.calculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.ui.theme.*
import com.microsoft.calculator.viewmodel.DateCalculatorViewModel
import java.time.LocalDate

@Composable
fun DateScreen(vm: DateCalculatorViewModel) {
    val subMode by vm.subMode.collectAsState()
    val addSubDir by vm.addSubDir.collectAsState()
    val startDate by vm.startDate.collectAsState()
    val endDate by vm.endDate.collectAsState()
    val diffYears by vm.diffYears.collectAsState()
    val diffMonths by vm.diffMonths.collectAsState()
    val diffWeeks by vm.diffWeeks.collectAsState()
    val diffDays by vm.diffDays.collectAsState()
    val totalDays by vm.totalDays.collectAsState()
    val addYears by vm.addYears.collectAsState()
    val addMonths by vm.addMonths.collectAsState()
    val addDays by vm.addDays.collectAsState()
    val resultDate by vm.resultDate.collectAsState()
    val s = LocalStrings.current

    Column(Modifier.fillMaxSize().background(DarkBg).padding(12.dp)) {
        // ===== 模式下拉(替换原来的两个按钮) =====
        ModeDropdown(
            current = subMode,
            onSelect = { vm.setSubMode(it) }
        )
        Spacer(Modifier.height(12.dp))

        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (subMode == DateCalculatorViewModel.SubMode.DIFFERENCE) {
                    // ---- 日期差值模式 ----
                    DateRow(
                        date = startDate,
                        label = null,
                        lang = s.langCode,
                        onPick = { vm.setStart(it) }
                    )
                    DateRow(
                        date = endDate,
                        label = s.endDate,
                        lang = s.langCode,
                        onPick = { vm.setEnd(it) }
                    )
                    HorizontalDivider(color = Divider)
                    DifferenceResult(
                        diffYears, diffMonths, diffWeeks, diffDays,
                        totalDays, s
                    )
                } else {
                    // ---- 添加/减去模式 ----
                    DateRow(
                        date = startDate,
                        label = s.startDate,
                        lang = s.langCode,
                        onPick = { vm.setStart(it) }
                    )
                    // RadioButton 行:添加 / 减去
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = addSubDir == DateCalculatorViewModel.AddSubtractDir.ADD,
                                onClick = { vm.setAddSubDir(DateCalculatorViewModel.AddSubtractDir.ADD) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentBlue,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Text(s.add, color = TextPrimary, fontSize = 15.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = addSubDir == DateCalculatorViewModel.AddSubtractDir.SUBTRACT,
                                onClick = { vm.setAddSubDir(DateCalculatorViewModel.AddSubtractDir.SUBTRACT) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentBlue,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Text(s.subtract, color = TextPrimary, fontSize = 15.sp)
                        }
                    }
                    // 三列数字下拉:年 | 月 | 天
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumberDropdown(
                            label = s.years,
                            value = addYears,
                            range = 0..99,
                            onSelect = { vm.setAddYears(it) },
                            modifier = Modifier.weight(1f)
                        )
                        NumberDropdown(
                            label = s.months,
                            value = addMonths,
                            range = 0..12,
                            onSelect = { vm.setAddMonths(it) },
                            modifier = Modifier.weight(1f)
                        )
                        NumberDropdown(
                            label = s.days,
                            value = addDays,
                            range = 0..31,
                            onSelect = { vm.setAddDays(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = Divider)
                    // 结果日期
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.date, color = TextSecondary, fontSize = 14.sp,
                            modifier = Modifier.padding(end = 12.dp))
                        Text(
                            vm.formatDate(resultDate, s.langCode),
                            color = TextPrimary, fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ========================================================================
// 子组件
// ========================================================================

/** 顶部模式下拉:显示当前模式名 + ▼,点击弹出深色下拉菜单 */
@Composable
private fun ModeDropdown(
    current: DateCalculatorViewModel.SubMode,
    onSelect: (DateCalculatorViewModel.SubMode) -> Unit
) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (current == DateCalculatorViewModel.SubMode.DIFFERENCE)
                        s.diffBetweenDates else s.addSubtract,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = PanelBg
        ) {
            DropdownMenuItem(
                text = { Text(s.diffBetweenDates, color = TextPrimary) },
                onClick = { onSelect(DateCalculatorViewModel.SubMode.DIFFERENCE); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(s.addSubtract, color = TextPrimary) },
                onClick = { onSelect(DateCalculatorViewModel.SubMode.ADDSUBTRACT); expanded = false }
            )
        }
    }
}

/** 日期行:可选标签 + 日期文本 + 日历图标按钮 */
@Composable
private fun DateRow(
    date: LocalDate,
    label: String?,
    lang: String,
    onPick: (LocalDate) -> Unit
) {
    val fmt = if (lang == "zh") "yyyy年M月d日" else "MMM d, yyyy"
    val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86400000L)
    var showPicker by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 日期文本(点此也能弹出)
        TextButton(onClick = { showPicker = true }) {
            Text(
                date.format(java.time.format.DateTimeFormatter.ofPattern(fmt)),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.weight(1f))
        // 日历图标按钮
        IconButton(onClick = { showPicker = true }) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = AccentBlue
            )
        }
    }

    // 标签(在 Row 之后显示,如果有)
    if (label != null) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
    }

    // 深色 DatePicker 对话框(用 Dialog + 自定义宽度,避免 AlertDialog 裁剪周日列)
    if (showPicker) {
        Dialog(
            onDismissRequest = { showPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = PanelBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.padding(bottom = 8.dp)) {
                    MaterialTheme(
                        colorScheme = darkColorScheme(
                            surface = PanelBg,
                            onSurface = TextPrimary,
                            onSurfaceVariant = TextSecondary,
                            primary = AccentBlue,
                            onPrimary = Color.White,
                            surfaceVariant = SurfaceBg,
                            outline = Divider
                        )
                    ) {
                        DatePicker(state = state, modifier = Modifier.fillMaxWidth())
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPicker = false }) {
                            Text(LocalStrings.current.cancel, color = TextSecondary)
                        }
                        TextButton(onClick = {
                            state.selectedDateMillis?.let {
                                onPick(LocalDate.ofEpochDay(it / 86400000L))
                            }
                            showPicker = false
                        }) { Text(LocalStrings.current.ok, color = AccentBlue) }
                    }
                }
            }
        }
    }
}

/** 数字下拉:标签 + 当前值 + ▼,点击弹出深色下拉列表 */
@Composable
private fun NumberDropdown(
    label: String,
    value: Long,
    range: IntRange,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            color = SurfaceBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Column(
                Modifier.fillMaxWidth().padding(10.dp, 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$value",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = TextSecondary, fontSize = 12.sp)
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = PanelBg
        ) {
            range.forEach { n ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "$n",
                            color = if (n.toLong() == value) AccentBlue else TextPrimary,
                            fontSize = 15.sp
                        )
                    },
                    onClick = { onSelect(n.toLong()); expanded = false }
                )
            }
        }
    }
}

/** 差值结果显示 */
@Composable
private fun DifferenceResult(
    years: Long, months: Long, weeks: Long, days: Long,
    totalDays: Long,
    s: com.microsoft.calculator.ui.i18n.Strings
) {
    if (years == 0L && months == 0L && weeks == 0L && days == 0L) {
        Text(s.sameDate, color = AccentBlue, fontSize = 15.sp)
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val parts = mutableListOf<String>()
            if (years != 0L) parts.add("${years}${s.years}")
            if (months != 0L) parts.add("${months}${s.months}")
            if (weeks != 0L) parts.add("${weeks}${if (s.langCode == "zh") "周" else "wk"}")
            if (days != 0L) parts.add("${days}${s.days}")
            Text(parts.joinToString(" "), color = TextPrimary, fontSize = 15.sp,
                modifier = Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(s.totalDays, color = TextSecondary, fontSize = 13.sp)
            Text(": ", color = TextSecondary, fontSize = 13.sp)
            Text("$totalDays", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
