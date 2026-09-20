package com.microsoft.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MathEngineTest {

    @Test
    fun basicArithmetic() {
        assertEquals("3", MathEngine.add(BigDecimal("1"), BigDecimal("2")).toPlainString())
        assertEquals("1", MathEngine.subtract(BigDecimal("3"), BigDecimal("2")).toPlainString())
        assertEquals("6", MathEngine.multiply(BigDecimal("2"), BigDecimal("3")).toPlainString())
        assertEquals("0.5", MathEngine.divide(BigDecimal("1"), BigDecimal("2")).stripTrailingZeros().toPlainString())
    }

    @Test
    fun divisionByZero() {
        try {
            MathEngine.divide(BigDecimal("1"), BigDecimal("0"))
            assert(false) { "Should throw" }
        } catch (e: ArithmeticException) { }
    }

    @Test
    fun infinitePrecisionDecimal() {
        // 1/3 应保留高精度小数
        val r = MathEngine.divide(BigDecimal("1"), BigDecimal("3"))
        assertTrue("1/3 should be 0.3333...", r.toPlainString().startsWith("0.3333"))
        // 0.1 + 0.2 == 0.3 (BigDecimal 不丢精度)
        val s = MathEngine.add(BigDecimal("0.1"), BigDecimal("0.2"))
        assertEquals("0.3", s.toPlainString())
    }

    @Test
    fun powerInt() {
        assertEquals("8", MathEngine.power(BigDecimal("2"), BigDecimal("3")).toPlainString())
        assertEquals("0.125", MathEngine.power(BigDecimal("2"), BigDecimal("-3")).stripTrailingZeros().toPlainString())
    }

    @Test
    fun powerFrac() {
        // 9 ^ 0.5 = 3
        val r = MathEngine.power(BigDecimal("9"), BigDecimal("0.5"))
        assertEquals("3", r.stripTrailingZeros().toPlainString().substring(0, 1))
    }

    @Test
    fun factorial() {
        assertEquals("120", MathEngine.fact(BigDecimal("5")).toPlainString())
        assertEquals("1", MathEngine.fact(BigDecimal("0")).toPlainString())
    }

    @Test
    fun reciprocal() {
        assertEquals("0.25", MathEngine.reciprocal(BigDecimal("4")).stripTrailingZeros().toPlainString())
    }

    @Test
    fun trig() {
        // sin(0)=0, cos(0)=1
        assertEquals(0.0, MathEngine.sinDeg(BigDecimal("0")).toDouble(), 1e-9)
        assertEquals(1.0, MathEngine.cosDeg(BigDecimal("0")).toDouble(), 1e-9)
        // sin(90)=1
        assertEquals(1.0, MathEngine.sinDeg(BigDecimal("90")).toDouble(), 1e-6)
    }

    @Test
    fun logFunctions() {
        assertEquals(0.0, MathEngine.ln(BigDecimal("1")).toDouble(), 1e-9)
        assertEquals(1.0, MathEngine.log10(BigDecimal("10")).toDouble(), 1e-9)
        assertEquals(2.0, MathEngine.log10(BigDecimal("100")).toDouble(), 1e-9)
    }

    @Test
    fun numberBaseConversion() {
        // 255 -> HEX FF, BIN 11111111
        assertEquals("FF", MathEngine.toBase(BigDecimal("255"), 16))
        assertEquals("11111111", MathEngine.toBase(BigDecimal("255"), 2))
        // 反向
        assertEquals("255", MathEngine.fromBase("FF", 16).toPlainString())
        assertEquals("255", MathEngine.fromBase("11111111", 2).toPlainString())
    }
}
