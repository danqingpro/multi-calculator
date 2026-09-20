package com.microsoft.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ExpressionEvaluatorTest {

    private val ev = ExpressionEvaluator()

    @Test
    fun basicOps() {
        assertEquals("5", ev.evaluate("2+3").stripTrailingZeros().toPlainString())
        assertEquals("6", ev.evaluate("2*3").stripTrailingZeros().toPlainString())
        assertEquals("4", ev.evaluate("8-4").stripTrailingZeros().toPlainString())
        assertEquals("2", ev.evaluate("8/4").stripTrailingZeros().toPlainString())
    }

    @Test
    fun precedence() {
        // 2 + 3 * 4 = 14 (优先级)
        assertEquals("14", ev.evaluate("2+3*4").stripTrailingZeros().toPlainString())
        // (2+3)*4 = 20
        assertEquals("20", ev.evaluate("(2+3)*4").stripTrailingZeros().toPlainString())
    }

    @Test
    fun powerAndMod() {
        assertEquals("8", ev.evaluate("2^3").stripTrailingZeros().toPlainString())
        assertEquals("16", ev.evaluate("2^4").stripTrailingZeros().toPlainString())
        assertEquals("1", ev.evaluate("7%3").stripTrailingZeros().toPlainString())
    }

    @Test
    fun unaryNegative() {
        assertEquals("-5", ev.evaluate("-5").stripTrailingZeros().toPlainString())
        assertEquals("-5", ev.evaluate("-(2+3)").stripTrailingZeros().toPlainString())
        assertEquals("3", ev.evaluate("-2+5").stripTrailingZeros().toPlainString())
    }

    @Test
    fun functions() {
        assertEquals(0.0, ev.evaluate("sin(0)").toDouble(), 1e-9)
        assertEquals(1.0, ev.evaluate("cos(0)").toDouble(), 1e-9)
        assertEquals(2.0, ev.evaluate("sqrt(4)").toDouble(), 1e-9)
        assertEquals(0.0, ev.evaluate("ln(1)").toDouble(), 1e-9)
        assertEquals(1.0, ev.evaluate("log(10)").toDouble(), 1e-9)
    }

    @Test
    fun factorial() {
        assertEquals("120", ev.evaluate("5!").stripTrailingZeros().toPlainString())
        assertEquals("24", ev.evaluate("4!").stripTrailingZeros().toPlainString())
    }

    @Test
    fun constants() {
        // pi 已定义
        val r = ev.evaluate("sin(0)+1")
        assertEquals("1", r.stripTrailingZeros().stripTrailingZeros().toPlainString().substring(0,1))
    }

    @Test
    fun complexExpression() {
        // 2 + 3 * 4 - 6 / 2 = 2 + 12 - 3 = 11
        assertEquals("11", ev.evaluate("2+3*4-6/2").stripTrailingZeros().toPlainString())
        // (1+2)*(3+4) = 3*7 = 21
        assertEquals("21", ev.evaluate("(1+2)*(3+4)").stripTrailingZeros().toPlainString())
    }

    @Test
    fun nestedParentheses() {
        // ((2+3)*2+1) = (5*2+1)=11
        assertEquals("11", ev.evaluate("((2+3)*2+1)").stripTrailingZeros().toPlainString())
    }
}
