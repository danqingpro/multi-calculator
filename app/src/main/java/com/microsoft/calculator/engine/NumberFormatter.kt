package com.microsoft.calculator.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 数字格式化,对标 Windows 计算器的数字显示格式化逻辑。
 * 整数千分位分组,科学计数法上限,小数去尾零。
 */
object NumberFormatter {

    private const val MAX_DIGITS = 16
    private val EXP_THRESHOLD = BigDecimal("1E16")
    private val EXP_THRESHOLD_SMALL = BigDecimal("1E-4")

    /**
     * @param value 数值
     * @param grouping 是否千分位分组
     * @param scientific F-E 模式:true 时强制科学计数法显示
     */
    fun format(value: BigDecimal, grouping: Boolean = true, scientific: Boolean = false): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return if (scientific) "0.E+0" else "0"

        val stripped = value.stripTrailingZeros()

        // F-E 模式:强制科学计数法
        if (scientific) {
            return formatScientific(stripped)
        }

        val absVal = stripped.abs()

        // 极大数或极小数 -> 科学计数法
        if (absVal.compareTo(EXP_THRESHOLD) >= 0 || absVal.compareTo(EXP_THRESHOLD_SMALL) < 0) {
            return formatScientific(stripped)
        }

        // 普通格式
        val scale = stripped.scale()
        val intPart = if (scale < 0) stripped.setScale(0, RoundingMode.DOWN) else stripped
        // 如果整数部分过长,也用科学计数法
        if (intPart.precision() - intPart.scale() > MAX_DIGITS) {
            return formatScientific(stripped)
        }

        val pattern = if (grouping) "#,##0.##########" else "0.##########"
        val df = DecimalFormat(pattern)
        return df.format(stripped)
    }

    private fun formatScientific(value: BigDecimal): String {
        val d = value.toDouble()
        val s = String.format("%.10E", d)
        // 1.2345000000E+10 -> 1.2345E+10
        val cleaned = s
            .replace(Regex("0+E"), "E")
            .replace(Regex("\\.E"), "E")
        return cleaned
    }

    fun format(value: Double, grouping: Boolean = true): String =
        format(BigDecimal.valueOf(value), grouping)
}
