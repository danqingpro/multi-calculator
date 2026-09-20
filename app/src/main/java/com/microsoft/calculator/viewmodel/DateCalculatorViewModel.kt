package com.microsoft.calculator.viewmodel

import androidx.lifecycle.ViewModel
import com.microsoft.calculator.engine.DateCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 日期计算 ViewModel,对标 Windows 计算器 Date Calculator 模式。
 * 支持两日期差值 与 日期加减(可切换 添加/减去 方向)。
 */
class DateCalculatorViewModel : ViewModel() {

    enum class SubMode { DIFFERENCE, ADDSUBTRACT }
    enum class AddSubtractDir { ADD, SUBTRACT }

    private val fmtIso = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ---- 模式 ----
    private val _subMode = MutableStateFlow(SubMode.DIFFERENCE)
    val subMode: StateFlow<SubMode> = _subMode.asStateFlow()

    private val _addSubDir = MutableStateFlow(AddSubtractDir.ADD)
    val addSubDir: StateFlow<AddSubtractDir> = _addSubDir.asStateFlow()

    // ---- 差值模式 ----
    private val _startDate = MutableStateFlow(LocalDate.now())
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(LocalDate.now().plusDays(1))
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _diffYears = MutableStateFlow(0L)
    val diffYears: StateFlow<Long> = _diffYears.asStateFlow()
    private val _diffMonths = MutableStateFlow(0L)
    val diffMonths: StateFlow<Long> = _diffMonths.asStateFlow()
    private val _diffWeeks = MutableStateFlow(0L)
    val diffWeeks: StateFlow<Long> = _diffWeeks.asStateFlow()
    private val _diffDays = MutableStateFlow(0L)
    val diffDays: StateFlow<Long> = _diffDays.asStateFlow()
    private val _totalDays = MutableStateFlow(0L)
    val totalDays: StateFlow<Long> = _totalDays.asStateFlow()

    // ---- 加/减模式(只有年/月/天,匹配 Windows 计算器) ----
    private val _addYears = MutableStateFlow(0L)
    val addYears: StateFlow<Long> = _addYears.asStateFlow()
    private val _addMonths = MutableStateFlow(0L)
    val addMonths: StateFlow<Long> = _addMonths.asStateFlow()
    private val _addDays = MutableStateFlow(0L)
    val addDays: StateFlow<Long> = _addDays.asStateFlow()
    private val _resultDate = MutableStateFlow(LocalDate.now())
    val resultDate: StateFlow<LocalDate> = _resultDate.asStateFlow()

    init { compute() }

    fun setSubMode(m: SubMode) { _subMode.value = m; compute() }
    fun setStart(date: LocalDate) { _startDate.value = date; compute() }
    fun setEnd(date: LocalDate) { _endDate.value = date; compute() }
    fun setAddSubDir(d: AddSubtractDir) { _addSubDir.value = d; compute() }

    fun setAddYears(v: Long) { _addYears.value = v; compute() }
    fun setAddMonths(v: Long) { _addMonths.value = v; compute() }
    fun setAddDays(v: Long) { _addDays.value = v; compute() }

    private fun compute() {
        if (_subMode.value == SubMode.DIFFERENCE) {
            val s = _startDate.value
            val e = _endDate.value
            val diff = DateCalculator.difference(s, e)
            _diffYears.value = diff.years
            _diffMonths.value = diff.months
            _diffWeeks.value = diff.weeks
            _diffDays.value = diff.days
            _totalDays.value = DateCalculator.totalDaysBetween(s, e)
        } else {
            val sign = if (_addSubDir.value == AddSubtractDir.SUBTRACT) -1 else 1
            _resultDate.value = DateCalculator.add(
                _startDate.value,
                _addYears.value * sign, _addMonths.value * sign, 0L,
                _addDays.value * sign
            )
        }
    }

    /** 日期格式化:zh → "2026年9月5日",en → "Sep 5, 2026" */
    fun formatDate(date: LocalDate, lang: String): String {
        return if (lang == "zh") {
            val fmt = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
            date.format(fmt)
        } else {
            val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
            date.format(fmt)
        }
    }
}
