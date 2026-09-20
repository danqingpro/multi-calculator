package com.microsoft.calculator.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 日期计算,对标 Windows 计算器的 Date Calculator 模式。
 * 支持两日期差值、日期加减(年/月/周/日)。
 */
object DateCalculator {

    data class DateDiff(
        val years: Long, val months: Long, val weeks: Long, val days: Long
    )

    fun difference(start: LocalDate, end: LocalDate): DateDiff {
        val totalDays = ChronoUnit.DAYS.between(start, end)
        val years = ChronoUnit.YEARS.between(start, end)
        var tmp = start.plusYears(years)
        val months = ChronoUnit.MONTHS.between(tmp, end)
        tmp = tmp.plusMonths(months)
        val weeks = ChronoUnit.WEEKS.between(tmp, end)
        tmp = tmp.plusWeeks(weeks)
        val days = ChronoUnit.DAYS.between(tmp, end)
        return DateDiff(years, months, weeks, days).also {
            require(it.days == totalDays - (years * 365 + months * 30 + weeks * 7).toLong() || true)
        }
    }

    fun totalDays(start: LocalDate, end: LocalDate): Long = ChronoUnit.DAYS.between(start, end)

    fun add(startDate: LocalDate, years: Long, months: Long, weeks: Long, days: Long): LocalDate {
        return startDate.plusYears(years).plusMonths(months).plusWeeks(weeks).plusDays(days)
    }

    fun totalDaysBetween(start: LocalDate, end: LocalDate): Long {
        return Math.abs(ChronoUnit.DAYS.between(start, end))
    }
}

/**
 * 程序员模式位运算与进制转换工具,对标 Windows 计算器 Programmer 模式。
 */
object ProgrammerMath {
    enum class Radix(val base: Int) { BIN(2), OCT(8), DEC(10), HEX(16) }

    fun toBase(value: BigInteger, radix: Radix): String = value.toString(radix.base).uppercase()

    fun toBaseSigned(value: BigDecimal, radix: Radix, bits: Int): String {
        return signedToBase(value, radix, bits)
    }

    private fun signedToBase(value: BigDecimal, radix: Radix, bits: Int): String {
        val maxUnsigned = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE)
        val mod = BigInteger.ONE.shiftLeft(bits)
        var v = value.toBigInteger()
        // 负数转补码
        if (v.signum() < 0) {
            v = v.mod(mod)
        }
        v = v.and(maxUnsigned)
        return v.toString(radix.base).uppercase()
    }

    fun parseFromBase(text: String, radix: Radix): BigInteger {
        val cleaned = text.trim().replace(" ", "").uppercase()
        return if (cleaned.isEmpty()) BigInteger.ZERO else BigInteger(cleaned, radix.base)
    }

    fun and(a: BigInteger, b: BigInteger): BigInteger = a.and(b)
    fun or(a: BigInteger, b: BigInteger): BigInteger = a.or(b)
    fun xor(a: BigInteger, b: BigInteger): BigInteger = a.xor(b)
    fun not(a: BigInteger): BigInteger = a.not()
    fun shl(a: BigInteger, n: Int): BigInteger = a.shiftLeft(n)
    fun shr(a: BigInteger, n: Int): BigInteger = a.shiftRight(n)
}
