package com.microsoft.calculator.viewmodel

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 4 张参考图对应的 4 套功能测试:
 * 图1(HEX 模式)→testFigure1_HexRadix 可用性边界
 * 图2(DEC 模式,红框 A-F)→testFigure2_DecRadix A-F禁用、0-9启用
 * 图3(OCT 模式)→testFigure3_OctRadix 0-7启用 8-9禁用 A-F禁用
 * 图4(BIN 模式)→testFigure4_BinRadix 仅0-1启用
 * 另加:禁用按键点击不生效;C 清除;⌫ 删除最后一位
 */
class ProgrammerRadixKeysTest {

    private lateinit var vm: ProgrammerViewModel

    @Before fun setup() { vm = ProgrammerViewModel() }

    // ---------- 图1:HEX 模式(参考图1) ----------
    @Test fun `图1 HEX模式 0-9 A-F 全可用`() {
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        // 0-9 全可用
        for (c in "0123456789") assertTrue("数字 $c 应在HEX下可用", vm.isDigitAvailable(c.toString()))
        // A-F 全可用
        for (c in "ABCDEF") assertTrue("字母 $c 应在HEX下可用", vm.isDigitAvailable(c.toString()))
    }

    @Test fun `图1 HEX模式 输入A-F得到正确十进制`() {
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        // 输入 774A = 7*16^3 + 7*16^2 + 4*16 + 10 = 30538
        vm.clear()
        vm.inputDigit("7"); vm.inputDigit("7"); vm.inputDigit("4"); vm.inputDigit("A")
        assertEquals(16, vm.radix.value.base)
        assertEquals("30,538", vm.decView.value)
        assertEquals("774A", vm.hexView.value) // 4 位=1 组,无空格分隔
    }

    @Test fun `图1 HEX模式 点击774A AAAA 切换进制左侧同显正确`() {
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        // 774A AAAA AAAA = 774A = 30538,但超长数字测试分组显示
        vm.clear()
        "774A".forEach { vm.inputDigit(it.toString()) }
        // 切到DEC,同显应有千分位
        vm.setRadix(ProgrammerViewModel.Radix.DEC)
        assertTrue("DEC视图应包含千分位或至少等于30538", vm.decView.value.replace(",", "") == "30538")
        // HEX视图仍显示 774A(4 位一组时 4 个字符刚好一组,无分隔)
        assertEquals("774A", vm.hexView.value)
        // 切回HEX
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        assertEquals("774A", vm.hexView.value)
    }

    // ---------- 图2:DEC 模式(参考图2,红框A-F禁用) ----------
    @Test fun `图2 DEC模式 A-F全禁用 0-9全启用`() {
        vm.setRadix(ProgrammerViewModel.Radix.DEC)
        for (c in "0123456789") assertTrue("数字 $c 应在DEC下可用", vm.isDigitAvailable(c.toString()))
        for (c in "ABCDEF") assertFalse("字母 $c 应在DEC下禁用", vm.isDigitAvailable(c.toString()))
    }

    @Test fun `图2 DEC模式 禁用按键A点击无效 不改变值`() {
        vm.setRadix(ProgrammerViewModel.Radix.DEC)
        // 先输入1908 建立状态
        "1908".forEach { vm.inputDigit(it.toString()) }
        val before = vm.mainDisplay.value
        // 连续点击禁用 A
        vm.inputDigit("A")
        val after = vm.mainDisplay.value
        assertEquals("禁用按键A不应改变值", before, after)
        assertEquals("1,908", before) // DEC 千分位分隔用逗号,不是空格
    }

    // ---------- 图3:OCT 模式(参考图3,0-7可用,8-9禁用 A-F禁用) ----------
    @Test fun `图3 OCT模式 0-7可用 8-9禁用 A-F禁用`() {
        vm.setRadix(ProgrammerViewModel.Radix.OCT)
        for (c in "01234567") assertTrue("$c 应在OCT下可用", vm.isDigitAvailable(c.toString()))
        for (c in "89") assertFalse("$c 应在OCT下禁用", vm.isDigitAvailable(c.toString()))
        for (c in "ABCDEF") assertFalse("$c 应在OCT下禁用", vm.isDigitAvailable(c.toString()))
    }

    @Test fun `图3 OCT模式 禁用按键8和9点击无效`() {
        vm.setRadix(ProgrammerViewModel.Radix.OCT)
        "3564".forEach { vm.inputDigit(it.toString()) }
        val before = vm.mainDisplay.value
        vm.inputDigit("8")
        assertEquals("8无效", before, vm.mainDisplay.value)
        vm.inputDigit("9")
        assertEquals("9无效", before, vm.mainDisplay.value)
    }

    // ---------- 图4:BIN 模式(参考图4,仅0-1可用) ----------
    @Test fun `图4 BIN模式 仅0-1可用 2-9禁用 A-F禁用`() {
        vm.setRadix(ProgrammerViewModel.Radix.BIN)
        assertTrue(vm.isDigitAvailable("0"))
        assertTrue(vm.isDigitAvailable("1"))
        for (c in "23456789") assertFalse("$c 应在BIN下禁用", vm.isDigitAvailable(c.toString()))
        for (c in "ABCDEF") assertFalse("$c 应在BIN下禁用", vm.isDigitAvailable(c.toString()))
    }

    @Test fun `图4 BIN模式 输入011101110100 得到分组显示`() {
        vm.setRadix(ProgrammerViewModel.Radix.BIN)
        vm.clear()
        // 输入 0 开头时 BigInteger 忽略前导 0,实际存储为 11101110100(11位)。
        // Windows 计算器 BIN 输入 0111 也只显示 111,所以与原版一致。
        "011101110100".forEach { vm.inputDigit(it.toString()) }
        // 11 位,4 位分组(从右向左):111 0111 0100
        assertEquals("111 0111 0100", vm.binView.value)
        assertEquals("1,908", vm.decView.value) // 0b011101110100 = 1908 (DEC千分位用逗号)
    }

    @Test fun `图4 BIN模式 点击禁用按键2无效`() {
        vm.setRadix(ProgrammerViewModel.Radix.BIN)
        vm.inputDigit("1"); vm.inputDigit("0")
        val before = vm.mainDisplay.value
        vm.inputDigit("2")
        assertEquals(before, vm.mainDisplay.value)
    }

    // ---------- C 清除; ⌫ 删除 ----------
    @Test fun `C 清除所有 包括待执行运算和左操作数`() {
        vm.setRadix(ProgrammerViewModel.Radix.DEC)
        vm.inputDigit("1"); vm.inputDigit("2")
        vm.applyBinaryOp("+")  // 待执行 12 +
        vm.inputDigit("3")     // 12 + 3
        vm.clear()
        assertEquals("0", vm.mainDisplay.value)
        assertEquals(null, vm.pendingExpression.value)
    }

    @Test fun `⌫ 退格删除最后一位`() {
        vm.setRadix(ProgrammerViewModel.Radix.DEC)
        "1234".forEach { vm.inputDigit(it.toString()) }
        assertEquals("1,234", vm.mainDisplay.value) // DEC千分位用逗号
        vm.backspace() // 123
        assertEquals("123", vm.mainDisplay.value.replace(",", ""))
        vm.backspace() // 12
        assertEquals("12", vm.mainDisplay.value.replace(",", ""))
        vm.backspace() // 1
        assertEquals("1", vm.mainDisplay.value)
        vm.backspace() // 0
        assertEquals("0", vm.mainDisplay.value)
    }

    // ---------- 回归:点击进制条(调用setRadix)后,isDigitAvailable 必须立刻正确刷新 ----------
    @Test fun `回归-切换进制后isDigitAvailable即时正确_HEX_DEC_OCT_BIN全回路`() {
        // HEX -> DEC -> OCT -> BIN -> HEX,每一步立即断言按键可用性边界
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        assertTrue(vm.isDigitAvailable("A"))
        assertTrue(vm.isDigitAvailable("0"))

        vm.setRadix(ProgrammerViewModel.Radix.DEC)
        assertFalse("DEC 后 A 应立刻禁用", vm.isDigitAvailable("A"))
        assertTrue("DEC 后 9 仍可用", vm.isDigitAvailable("9"))

        vm.setRadix(ProgrammerViewModel.Radix.OCT)
        assertFalse("OCT 后 8 立刻禁用", vm.isDigitAvailable("8"))
        assertTrue("OCT 后 7 仍可用", vm.isDigitAvailable("7"))
        assertFalse("OCT 后 A 禁用", vm.isDigitAvailable("A"))

        vm.setRadix(ProgrammerViewModel.Radix.BIN)
        assertFalse("BIN 后 2 立刻禁用", vm.isDigitAvailable("2"))
        assertTrue("BIN 后 1 仍可用", vm.isDigitAvailable("1"))

        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        assertTrue("切回 HEX 后 A 立刻恢复", vm.isDigitAvailable("A"))
        assertTrue("切回 HEX 后 9 仍可用", vm.isDigitAvailable("9"))
    }

    @Test fun `回归-切换进制后禁用按键点击立刻不生效`() {
        // 从 HEX 输入 ABCD,切 DEC 立刻尝试点 A —— 不能改变值
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        "ABCD".forEach { vm.inputDigit(it.toString()) }
        val hexStr = vm.hexView.value
        vm.setRadix(ProgrammerViewModel.Radix.DEC)
        val decStrBefore = vm.mainDisplay.value
        // 尝试点击禁用字母 A-F
        "ABCDEF".forEach { vm.inputDigit(it.toString()) }
        assertEquals("切 DEC 后禁用 A-F 点击应无效", decStrBefore, vm.mainDisplay.value)

        // 切回 HEX —— A 立刻恢复可用
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        assertTrue("切回 HEX A 立刻可用", vm.isDigitAvailable("A"))
    }

    // ---------- 回归:宽度切换 QWORD->DWORD->WORD->BYTE 位面板位数立刻正确 ----------
    @Test fun `回归-切换宽度后bitAt和toggleBit的有效位数立即同步`() {
        vm.setWidth(ProgrammerViewModel.Width.QWORD)
        // QWORD 第 63 位(MSB)有效
        assertFalse(vm.bitAt(63))
        vm.toggleBit(63)
        assertTrue("QWORD 第 63 位能被翻转", vm.bitAt(63))
        vm.toggleBit(63); assertFalse(vm.bitAt(63))

        vm.setWidth(ProgrammerViewModel.Width.BYTE)
        // BYTE 第 63 位不再存在(有效范围 0-7)
        // 但内部 value 可能仍保留高位,因为 maskToWidth 截断
        assertEquals(0, vm.bitPanel().count { it }) // 所有高位被 mask 后,8 位全0
        assertEquals(8, vm.bitPanel().size)

        vm.setWidth(ProgrammerViewModel.Width.QWORD)
        // 切回 QWORD,第 63 位仍是 0(BYTE mask 时被截断,无法恢复)
        assertFalse(vm.bitAt(63))

        vm.setWidth(ProgrammerViewModel.Width.WORD)
        assertEquals(16, vm.bitPanel().size)

        vm.setWidth(ProgrammerViewModel.Width.DWORD)
        assertEquals(32, vm.bitPanel().size)
    }

    @Test fun `回归-切换进制后重新输入的字符立刻正确`() {
        vm.setRadix(ProgrammerViewModel.Radix.BIN); vm.clear()
        vm.inputDigit("1"); vm.inputDigit("0"); vm.inputDigit("1")
        assertEquals("5", vm.decView.value)

        vm.setRadix(ProgrammerViewModel.Radix.HEX); vm.clear()
        vm.inputDigit("A"); vm.inputDigit("B"); vm.inputDigit("C"); vm.inputDigit("D")
        assertEquals("ABCD", vm.hexView.value)

        vm.setRadix(ProgrammerViewModel.Radix.OCT); vm.clear()
        vm.inputDigit("7"); vm.inputDigit("7")
        assertEquals("77", vm.octView.value)
        vm.inputDigit("8"); vm.inputDigit("9")
        assertEquals("77", vm.octView.value)
    }

    // ---------- 字节宽度下输入字符限制测试 ----------
    @Test fun `BYTE 8位 HEX 最多2字符 F后再输A丢弃`() {
        vm.setWidth(ProgrammerViewModel.Width.BYTE)
        vm.setRadix(ProgrammerViewModel.Radix.HEX); vm.clear()
        vm.inputDigit("F") // 第1个字符
        vm.inputDigit("F") // 第2个字符,FF
        assertEquals("FF", vm.hexView.value)
        // 继续点 A,应丢弃
        vm.inputDigit("A")
        assertEquals("FF", vm.hexView.value)
        vm.inputDigit("F")
        assertEquals("FF", vm.hexView.value)
    }

    @Test fun `WORD 16位 HEX 最多4字符`() {
        vm.setWidth(ProgrammerViewModel.Width.WORD)
        vm.setRadix(ProgrammerViewModel.Radix.HEX); vm.clear()
        "FFFF".forEach { vm.inputDigit(it.toString()) }
        assertEquals("FFFF", vm.hexView.value)
        vm.inputDigit("A") // 第5个,丢弃
        assertEquals("FFFF", vm.hexView.value)
    }

    @Test fun `DWORD 32位 HEX 最多8字符 QWORD 最多16字符`() {
        vm.setWidth(ProgrammerViewModel.Width.DWORD)
        vm.setRadix(ProgrammerViewModel.Radix.HEX); vm.clear()
        "FFFFFFFF".forEach { vm.inputDigit(it.toString()) }
        assertEquals("FFFF FFFF", vm.hexView.value)
        vm.inputDigit("A")
        assertEquals("FFFF FFFF", vm.hexView.value)

        vm.setWidth(ProgrammerViewModel.Width.QWORD)
        vm.clear()
        "FFFFFFFFFFFFFFFF".forEach { vm.inputDigit(it.toString()) }
        assertEquals("FFFF FFFF FFFF FFFF", vm.hexView.value)
        vm.inputDigit("A")
        assertEquals("FFFF FFFF FFFF FFFF", vm.hexView.value)
    }

    @Test fun `BYTE 8位 BIN 最多8字符 11111111后再输丢弃`() {
        vm.setWidth(ProgrammerViewModel.Width.BYTE)
        vm.setRadix(ProgrammerViewModel.Radix.BIN); vm.clear()
        "11111111".forEach { vm.inputDigit(it.toString()) } // 8个1
        assertEquals("1111 1111", vm.binView.value)
        vm.inputDigit("1"); assertEquals("1111 1111", vm.binView.value)
        vm.inputDigit("0"); assertEquals("1111 1111", vm.binView.value)
    }

    @Test fun `WORD 16位 BIN 16字符 DWORD 32字符`() {
        vm.setWidth(ProgrammerViewModel.Width.WORD)
        vm.setRadix(ProgrammerViewModel.Radix.BIN); vm.clear()
        "1010101010101010".forEach { vm.inputDigit(it.toString()) } // 16个
        assertEquals("1010 1010 1010 1010", vm.binView.value)
        vm.inputDigit("1"); assertEquals("1010 1010 1010 1010", vm.binView.value)

        vm.setWidth(ProgrammerViewModel.Width.DWORD); vm.clear()
        repeat(32) { vm.inputDigit("1") }
        assertEquals("1111 1111 1111 1111 1111 1111 1111 1111", vm.binView.value)
        vm.inputDigit("1") // 第33个,丢弃
        assertEquals("1111 1111 1111 1111 1111 1111 1111 1111", vm.binView.value)
    }

    @Test fun `BYTE DEC 255后输6丢弃(位宽校验)`() {
        // BYTE DEC:字符上限 3,但 "255" → 再输 "6" = "2556" 超过 255 应丢弃
        vm.setWidth(ProgrammerViewModel.Width.BYTE)
        vm.setRadix(ProgrammerViewModel.Radix.DEC); vm.clear()
        vm.inputDigit("2"); vm.inputDigit("5"); vm.inputDigit("5")
        assertEquals("255", vm.decView.value)
        vm.inputDigit("6") // 2556 > 255,丢弃
        assertEquals("255", vm.decView.value)
    }

    @Test fun `BYTE OCT 377后输7丢弃`() {
        vm.setWidth(ProgrammerViewModel.Width.BYTE)
        vm.setRadix(ProgrammerViewModel.Radix.OCT); vm.clear()
        vm.inputDigit("3"); vm.inputDigit("7"); vm.inputDigit("7") // 377 = FF
        assertEquals("377", vm.octView.value)
        vm.inputDigit("7") // 第4个,字符数超限,丢弃
        assertEquals("377", vm.octView.value)
    }

    // ---------- 位面板可用性区/不可用区 bitAt 正确测试 ----------
    @Test fun `BYTE 模式 高位bitAt全返回false 低位正常`() {
        vm.setWidth(ProgrammerViewModel.Width.BYTE)
        vm.clear()
        // 低 8 位(0..7)返回正常;高位 8..63 返回 false
        for (i in 0..7) assertFalse(vm.bitAt(i))
        for (i in 8..63) assertFalse(vm.bitAt(i))
        vm.toggleBit(0)
        assertTrue(vm.bitAt(0))
        vm.toggleBit(7)
        assertTrue(vm.bitAt(7))
        // 尝试 toggle 高位:应返回 false(无效位)
        vm.toggleBit(8); vm.toggleBit(63)
        for (i in 8..63) assertFalse(vm.bitAt(i))
    }

    @Test fun `DWORD 模式 位面板32位可用以上高位全部false`() {
        vm.setWidth(ProgrammerViewModel.Width.DWORD)
        vm.clear()
        for (i in 0..31) assertFalse(vm.bitAt(i))
        for (i in 32..63) assertFalse(vm.bitAt(i))
        vm.toggleBit(31)
        assertTrue(vm.bitAt(31))
        vm.toggleBit(32) // 高位,无效
        assertFalse(vm.bitAt(32))
    }

    // ---------- 顶部表达式(左操作数 + 运算符)显示 ----------
    @Test fun `DEC 模式 按+后顶部显示 12 + 且主显示归0`() {
        vm.setRadix(ProgrammerViewModel.Radix.DEC); vm.clear()
        vm.inputDigit("1"); vm.inputDigit("2")
        vm.applyBinaryOp("+")
        // 顶部表达式 = 左操作数(DEC) + 运算符符号
        assertEquals("12 +", vm.pendingExpression.value)
        // 主显示归零,等待输入右操作数
        assertEquals("0", vm.mainDisplay.value)
    }

    @Test fun `HEX 模式 按AND后顶部按十六进制显示左操作数`() {
        vm.setRadix(ProgrammerViewModel.Radix.HEX); vm.clear()
        vm.inputDigit("F"); vm.inputDigit("F")
        vm.applyBinaryOp("and")
        assertEquals("FF AND", vm.pendingExpression.value)
    }

    @Test fun `按等号后顶部表达式清空`() {
        vm.setRadix(ProgrammerViewModel.Radix.DEC); vm.clear()
        vm.inputDigit("1"); vm.inputDigit("2")
        vm.applyBinaryOp("+")
        assertEquals("12 +", vm.pendingExpression.value)
        vm.inputDigit("3")
        vm.equals()
        assertEquals(null, vm.pendingExpression.value)
        assertEquals("15", vm.mainDisplay.value)
    }

    @Test fun `切换进制后顶部表达式按新进制重算左操作数`() {
        vm.setRadix(ProgrammerViewModel.Radix.DEC); vm.clear()
        vm.inputDigit("2"); vm.inputDigit("5"); vm.inputDigit("5") // 255 = 0xFF
        vm.applyBinaryOp("+")
        assertEquals("255 +", vm.pendingExpression.value)
        // 切到 HEX,左操作数 255 应显示为 FF
        vm.setRadix(ProgrammerViewModel.Radix.HEX)
        assertEquals("FF +", vm.pendingExpression.value)
    }
}
