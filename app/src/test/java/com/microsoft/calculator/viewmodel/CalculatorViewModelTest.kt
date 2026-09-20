package com.microsoft.calculator.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorViewModelTest {

    private fun newVm() = CalculatorViewModel().also { it.setMode(CalculatorViewModel.Mode.STANDARD) }

    @Test
    fun standardSimpleAddition() {
        val vm = newVm()
        vm.inputDigit("1")
        vm.inputDigit("2")
        vm.inputOperator("+")
        vm.inputDigit("3")
        vm.inputDigit("4")
        vm.equals()
        assertEquals("46", vm.display.value)
    }

    @Test
    fun standardChaining() {
        // 5 + 5 = 10, 然后 = 重复 = 15
        val vm = newVm()
        vm.inputDigit("5")
        vm.inputOperator("+")
        vm.inputDigit("5")
        vm.equals()
        assertEquals("10", vm.display.value)
        vm.inputOperator("+")  // 把 10 作为左操作数
        vm.inputDigit("5")
        vm.equals()
        assertEquals("15", vm.display.value)
    }

    @Test
    fun standardDecimal() {
        val vm = newVm()
        vm.inputDigit("0")
        vm.inputDecimal()
        vm.inputDigit("5")
        assertEquals("0.5", vm.display.value)
    }

    @Test
    fun standardUnarySqrt() {
        val vm = newVm()
        vm.inputDigit("9")
        vm.applyUnaryFunction("sqrt")
        assertEquals("3", vm.display.value)
    }

    @Test
    fun standardPercent() {
        // 100 + 10% = 110
        val vm = newVm()
        vm.inputDigit("1"); vm.inputDigit("0"); vm.inputDigit("0")
        vm.inputOperator("+")
        vm.inputDigit("1"); vm.inputDigit("0")
        vm.percent()
        vm.equals()
        assertEquals("110", vm.display.value)
    }

    @Test
    fun standardDivisionByZero() {
        val vm = newVm()
        vm.inputDigit("5")
        vm.inputOperator("÷")
        vm.inputDigit("0")
        vm.equals()
        assertEquals("Error", vm.display.value)
        assertTrue(vm.isError.value)
    }

    @Test
    fun standardBackspace() {
        val vm = newVm()
        vm.inputDigit("1"); vm.inputDigit("2"); vm.inputDigit("3")
        vm.backspace()
        assertEquals("12", vm.display.value)
    }

    @Test
    fun memoryOperations() {
        val vm = newVm()
        vm.inputDigit("5")
        vm.memoryStore()
        // 清空后 recall
        vm.clear()
        vm.memoryRecall()
        assertEquals("5", vm.display.value)
        // M+ 3 => 8
        vm.clear(); vm.inputDigit("3"); vm.memoryAdd()
        vm.clear(); vm.memoryRecall()
        assertEquals("8", vm.display.value)
        // M- 2 => 6
        vm.clear(); vm.inputDigit("2"); vm.memorySubtract()
        vm.clear(); vm.memoryRecall()
        assertEquals("6", vm.display.value)
    }

    @Test
    fun historyRecorded() {
        val vm = newVm()
        vm.inputDigit("2"); vm.inputOperator("+"); vm.inputDigit("3"); vm.equals()
        assertEquals(1, vm.history.value.size)
        assertTrue(vm.history.value[0].result == "5")
    }

    @Test
    fun scientificPrecedence() {
        val vm = CalculatorViewModel().also { it.setMode(CalculatorViewModel.Mode.SCIENTIFIC) }
        // 2 + 3 * 4 = 14
        listOf("2", "+", "3", "*", "4").forEach { t ->
            if (t.length == 1 && t[0].isDigit()) vm.inputDigit(t) else vm.inputOperator(t)
        }
        vm.equals()
        assertEquals("14", vm.display.value)
    }

    @Test
    fun scientificFunctions() {
        val vm = CalculatorViewModel().also { it.setMode(CalculatorViewModel.Mode.SCIENTIFIC) }
        // sqrt(9) = 3
        vm.inputOp("sqrt")
        vm.inputDigit("9")
        vm.inputOp(")")
        vm.equals()
        assertEquals("3", vm.display.value)
    }
}
