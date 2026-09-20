package com.microsoft.calculator.viewmodel

import androidx.lifecycle.ViewModel
import com.microsoft.calculator.engine.NumberFormatter
import com.microsoft.calculator.engine.UnitConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal

/**
 * 单位换算 ViewModel,对标 Windows 计算器 Converter 模式。
 * 每个类别在导航栏点击后直接进入对应类别,不再在界面内切换类别。
 */
class UnitConverterViewModel : ViewModel() {

    private val _category = MutableStateFlow(UnitConverter.Category.LENGTH)
    val category: StateFlow<UnitConverter.Category> = _category.asStateFlow()

    private val _fromIdx = MutableStateFlow(0)
    val fromIdx: StateFlow<Int> = _fromIdx.asStateFlow()

    private val _toIdx = MutableStateFlow(1)
    val toIdx: StateFlow<Int> = _toIdx.asStateFlow()

    private val _fromValue = MutableStateFlow("1")
    val fromValue: StateFlow<String> = _fromValue.asStateFlow()

    private val _toValue = MutableStateFlow("")
    val toValue: StateFlow<String> = _toValue.asStateFlow()

    init { convert() }

    /** 每个类别默认的 (fromIdx, toIdx) */
    private val defaults: Map<UnitConverter.Category, Pair<Int, Int>> = mapOf(
        UnitConverter.Category.LENGTH to (0 to 1),
        UnitConverter.Category.AREA to (0 to 1),
        UnitConverter.Category.VOLUME to (1 to 9),
        UnitConverter.Category.MASS to (3 to 6),
        UnitConverter.Category.TEMPERATURE to (0 to 1),
        UnitConverter.Category.TIME to (3 to 2),       // 小时 -> 分钟
        UnitConverter.Category.SPEED to (1 to 2),
        UnitConverter.Category.DATA to (4 to 3),       // GB -> MB
        UnitConverter.Category.ANGLE to (0 to 1),      // 度 -> 弧度
        UnitConverter.Category.PRESSURE to (4 to 3),   // 大气压 -> 巴
        UnitConverter.Category.ENERGY to (1 to 5),
        UnitConverter.Category.POWER to (1 to 3),      // 千瓦 -> 马力(美制)
        UnitConverter.Category.CURRENCY to (0 to 1)
    )

    /**
     * 「约等于」行展示的额外单位(单位名 -> 标签)。
     * 显示输入值换算到这些单位的结果。
     */
    private val supplementary: Map<UnitConverter.Category, List<Pair<String, String>>> = mapOf(
        UnitConverter.Category.ANGLE to listOf("grad" to "梯度"),
        UnitConverter.Category.POWER to listOf(
            "btumin" to "BTU/分钟",
            "ftlbmin" to "磅英尺/分钟",
            "w" to "瓦"
        )
    )

    fun setCategory(c: UnitConverter.Category) {
        _category.value = c
        val d = defaults[c] ?: (0 to 1)
        _fromIdx.value = d.first
        _toIdx.value = d.second
        if (_fromValue.value.isEmpty()) _fromValue.value = "1"
        convert()
    }

    fun setFromIdx(i: Int) { _fromIdx.value = i; convert() }
    fun setToIdx(i: Int) { _toIdx.value = i; convert() }

    fun inputDigit(d: String) {
        val cur = _fromValue.value
        _fromValue.value = if (cur == "0" && d != ".") d else cur + d
        convert()
    }

    fun inputDecimal() {
        if (!_fromValue.value.contains(".")) {
            _fromValue.value = _fromValue.value + "."
            convert()
        }
    }

    fun backspace() {
        val cur = _fromValue.value
        _fromValue.value = if (cur.length <= 1) "0" else cur.dropLast(1)
        convert()
    }

    fun clear() {
        _fromValue.value = "0"
        convert()
    }

    fun negate() {
        val cur = _fromValue.value
        _fromValue.value = if (cur.startsWith("-")) cur.drop(1) else "-$cur"
        convert()
    }

    fun swap() {
        val f = _fromIdx.value
        _fromIdx.value = _toIdx.value
        _toIdx.value = f
        convert()
    }

    fun fromUnitLabels(lang: String): List<Pair<String, String>> {
        val c = _category.value
        return if (c == UnitConverter.Category.TEMPERATURE) {
            UnitConverter.temperatureUnits(lang)
        } else {
            UnitConverter.units[c]!!.map { it.name to it.label(lang) }
        }
    }

    /** 返回当前类别「约等于」行的数据:List of (数值文本, 单位标签) */
    fun supplementaryValues(lang: String): List<Pair<String, String>> {
        val c = _category.value
        val extras = supplementary[c] ?: return emptyList()
        val list = UnitConverter.units[c] ?: return emptyList()
        val v = _fromValue.value.replace(",", "").toDoubleOrNull() ?: return emptyList()
        val fromIdx = _fromIdx.value
        return extras.mapNotNull { (name, labelZh) ->
            val idx = list.indexOfFirst { it.name == name }
            if (idx < 0) null else {
                val result = UnitConverter.convert(v, c, fromIdx, idx)
                formatDouble(result) to labelZh
            }
        }
    }

    private fun convert() {
        try {
            val v = _fromValue.value.replace(",", "").toDouble()
            val result = UnitConverter.convert(
                v, _category.value, _fromIdx.value, _toIdx.value
            )
            _toValue.value = formatDouble(result)
        } catch (e: Exception) {
            _toValue.value = "Error"
        }
    }

    private fun formatDouble(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return "Error"
        return NumberFormatter.format(BigDecimal.valueOf(d))
    }
}
