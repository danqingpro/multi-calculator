package com.microsoft.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate

class ProgrammerAndDateTest {

    @Test
    fun radixConversion() {
        // 255 dec -> FF hex
        assertEquals("FF", ProgrammerMath.toBase(BigInteger("255"), ProgrammerMath.Radix.HEX))
        // 255 dec -> 11111111 bin
        assertEquals("11111111", ProgrammerMath.toBase(BigInteger("255"), ProgrammerMath.Radix.BIN))
        // 8 oct
        assertEquals("10", ProgrammerMath.toBase(BigInteger("8"), ProgrammerMath.Radix.OCT))
    }

    @Test
    fun parseFromHex() {
        assertEquals(BigInteger("255"), ProgrammerMath.parseFromBase("FF", ProgrammerMath.Radix.HEX))
        assertEquals(BigInteger("255"), ProgrammerMath.parseFromBase("11111111", ProgrammerMath.Radix.BIN))
    }

    @Test
    fun bitwiseAndOrXor() {
        // 12(1100) AND 10(1010) = 8(1000)
        assertEquals(BigInteger("8"), ProgrammerMath.and(BigInteger("12"), BigInteger("10")))
        // 12 | 10 = 14
        assertEquals(BigInteger("14"), ProgrammerMath.or(BigInteger("12"), BigInteger("10")))
        // 12 ^ 10 = 6
        assertEquals(BigInteger("6"), ProgrammerMath.xor(BigInteger("12"), BigInteger("10")))
    }

    @Test
    fun shift() {
        // 1 << 4 = 16
        assertEquals(BigInteger("16"), ProgrammerMath.shl(BigInteger("1"), 4))
        // 256 >> 3 = 32
        assertEquals(BigInteger("32"), ProgrammerMath.shr(BigInteger("256"), 3))
    }

    @Test
    fun not() {
        // ~0 = -1
        assertEquals(BigInteger("-1"), ProgrammerMath.not(BigInteger("0")))
    }

    @Test
    fun dateDifference() {
        val s = LocalDate.of(2023, 1, 1)
        val e = LocalDate.of(2023, 2, 1)
        // 31 days
        assertEquals(31L, DateCalculator.totalDaysBetween(s, e))
        val diff = DateCalculator.difference(s, e)
        assertEquals(0L, diff.years)
        assertEquals(1L, diff.months)
    }

    @Test
    fun dateAdd() {
        val s = LocalDate.of(2023, 1, 1)
        val r = DateCalculator.add(s, 0, 1, 0, 0)
        assertEquals(LocalDate.of(2023, 2, 1), r)
        // 加 10 天
        val r2 = DateCalculator.add(s, 0, 0, 0, 10)
        assertEquals(LocalDate.of(2023, 1, 11), r2)
    }
}
