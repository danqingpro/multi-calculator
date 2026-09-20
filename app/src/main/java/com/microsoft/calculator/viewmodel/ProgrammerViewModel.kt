package com.microsoft.calculator.viewmodel

import androidx.lifecycle.ViewModel
import com.microsoft.calculator.engine.ProgrammerMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigInteger
import kotlin.math.min

/**
 * 程序员模式 ViewModel —— 对齐原版 Windows 计算器 Programmer。
 *
 * 核心能力:
 * 1. 4 进制同显(HEX/DEC/OCT/BIN),当前选中高亮
 * 2. 字节宽度:BYTE(8)/WORD(16)/DWORD(32)/QWORD(64),决定掩码范围
 * 3. 按位运算:AND / OR / XOR / NOT / NAND / NOR
 * 4. 位移位 4 种模式:
 *    - Arithmetic (算术):左移补 0,右移补符号位(SAR)
 *    - Logical    (逻辑):左/右移都补 0(SHL/SHR)
 *    - Rotate     (循环):循环移位(ROL/ROR)
 *    - RotateC    (带进位循环):RCL/RCR(这里简化用 CF=0)
 * 5. 位面板(Bit Panel):每一位可点击翻转
 * 6. 数字可用性禁位:在 BIN 时只能输 0-1,在 OCT 时只能输 0-7 等等,A-F 只有 HEX 才可用
 * 7. 内存:MS / M^ (调用父计算器共享的内存,简化实现为独立内存)
 * 8. 符号位处理:有符号负数以补码形式显示
 */
class ProgrammerViewModel : ViewModel() {

    enum class Radix(val base: Int, val label: String) {
        HEX(16, "HEX"),
        DEC(10, "DEC"),
        OCT(8, "OCT"),
        BIN(2, "BIN")
    }

    enum class Width(val bits: Int, val label: String) {
        BYTE(8, "BYTE"),
        WORD(16, "WORD"),
        DWORD(32, "DWORD"),
        QWORD(64, "QWORD")
    }

    enum class ShiftMode { ARITHMETIC, LOGICAL, ROTATE, ROTATE_CARRY }

    // ---------- UI 暴露状态 ----------
    private val _radix = MutableStateFlow(Radix.DEC)
    val radix: StateFlow<Radix> = _radix.asStateFlow()

    private val _width = MutableStateFlow(Width.QWORD)
    val width: StateFlow<Width> = _width.asStateFlow()

    private val _shiftMode = MutableStateFlow(ShiftMode.ARITHMETIC)
    val shiftMode: StateFlow<ShiftMode> = _shiftMode.asStateFlow()

    // 当前值的十进制(内部存储为 BigInteger,始终非负,用于显示补码)
    private var value: BigInteger = BigInteger.ZERO

    // 当前输入(以当前 _radix 为基数的字符串)
    private var currentInput = StringBuilder("0")

    // 主显示区:当前进制下的大数字显示(对应参考图右上角的 0)
    private val _mainDisplay = MutableStateFlow("0")
    val mainDisplay: StateFlow<String> = _mainDisplay.asStateFlow()

    // 左侧 4 进制同显
    private val _hexView = MutableStateFlow("0")
    val hexView: StateFlow<String> = _hexView.asStateFlow()
    private val _decView = MutableStateFlow("0")
    val decView: StateFlow<String> = _decView.asStateFlow()
    private val _octView = MutableStateFlow("0")
    val octView: StateFlow<String> = _octView.asStateFlow()
    private val _binView = MutableStateFlow("0")
    val binView: StateFlow<String> = _binView.asStateFlow()

    // 位面板 64 位的显式状态流(每个 sync 都更新),用于 Compose 精准追踪每一位的变化。
    // 相比 bitAt() 读 var value(非状态),收集此 Flow 后 Compose 能 100% 知道需要重绘。
    private val _bitPanelBits = MutableStateFlow(BooleanArray(64))
    val bitPanelBits: StateFlow<BooleanArray> = _bitPanelBits.asStateFlow()

    // 内存
    private var memory: BigInteger = BigInteger.ZERO
    private val _memoryNotEmpty = MutableStateFlow(false)
    val memoryNotEmpty: StateFlow<Boolean> = _memoryNotEmpty.asStateFlow()

    // 待执行操作(二元:保存左操作数)
    private var pendingOp: String? = null
    private var pendingLeft: BigInteger = BigInteger.ZERO

    // 顶部表达式显示:左操作数(按当前进制格式化) + 运算符符号,例如 "10 +"、"FF AND"、"3 <<";
    // 没有待执行运算时为 null(顶部区域留空)。由 sync() 统一维护,确保进制/宽度切换时也会重算。
    private val _pendingExpression = MutableStateFlow<String?>(null)
    val pendingExpression: StateFlow<String?> = _pendingExpression.asStateFlow()

    // ---------- 基础方法 ----------

    fun setRadix(r: Radix) {
        _radix.value = r
        // 切换进制时,把当前 value 格式化为新进制
        currentInput = StringBuilder(valueToCurrentRadix())
        sync()
    }

    fun setWidth(w: Width) {
        _width.value = w
        // 按新宽度截断 value(补码截断)
        value = maskToWidth(value)
        currentInput = StringBuilder(valueToCurrentRadix())
        sync()
    }

    fun setShiftMode(m: ShiftMode) {
        _shiftMode.value = m
    }

    /**
     * 按进制×位宽,计算输入字符串的最大允许长度。
     * 超出长度时继续点击数字不应改变当前值(与Windows行为一致)。
     *
     *    位宽 | HEX(4bit/char) | OCT(3bit/char) | DEC(约3.32bit) | BIN(1bit/char)
     *   BYTE 8 | 2 (FF)        | 3 (377)         | 3 (255)        | 8
     *   WORD 16| 4 (FFFF)      | 6 (177777)      | 5 (65535)      | 16
     *  DWORD 32| 8 (FFFFFFFF)  | 11 (37777777777)| 10 (4294967295)| 32
     *  QWORD 64| 16            | 22              | 20             | 64
     */
    private fun maxCharsForRadixAndWidth(r: Int, bits: Int): Int {
        return when (r) {
            16 -> bits / 4
            8  -> (bits * 10 + 9) / 10  // ceil(bits * ln(2) / ln(8)) = ceil(bits/3)
            10 -> when (bits) {
                8  -> 3   // 255
                16 -> 5   // 65535
                32 -> 10  // 4294967295
                64 -> 20  // 18446744073709551615
                else -> (bits * 0.301029995f).toInt() + 1
            }
            2  -> bits
            else -> Int.MAX_VALUE
        }
    }

    /** 数字按键输入;如当前进制不支持该字符或超出位宽字符上限则忽略 */
    fun inputDigit(ch: String) {
        if (ch.length != 1) return
        val c = ch.uppercase()[0]
        val r = _radix.value.base
        val d = Character.digit(c, r)
        if (d < 0) return
        val cur = currentInput.toString()
        val maxChars = maxCharsForRadixAndWidth(r, _width.value.bits)
        val next = if (cur == "0") c.toString() else cur + c
        // 1) 字符数上限检查:达到上限后不再追加
        if (next.length > maxChars) {
            // 超限,丢弃输入
            return
        }
        // 2) 数值 bitLength 检查(字符数未超限,但数值仍可能超出,如 BYTE+DEC "255" 后再加 6 → 2556 > 255)
        try {
            val newVal = BigInteger(next, r)
            if (newVal.bitLength() > _width.value.bits) {
                // 超过位宽,拒绝最后一位
                return
            }
            currentInput = StringBuilder(next)
            value = maskToWidth(newVal)
        } catch (_: Exception) {
            return
        }
        sync()
    }

    /** 输入是否在当前进制可用(用于 UI 灰化按钮) */
    fun isDigitAvailable(ch: String): Boolean {
        if (ch.length != 1) return false
        val r = _radix.value.base
        val d = Character.digit(ch[0].uppercaseChar(), r)
        return d in 0 until r
    }

    fun backspace() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)
            if (currentInput.isEmpty() || currentInput.toString() == "-") {
                currentInput = StringBuilder("0")
            }
            value = try {
                maskToWidth(BigInteger(currentInput.toString(), _radix.value.base))
            } catch (_: Exception) { BigInteger.ZERO }
        }
        sync()
    }

    fun clear() {
        value = BigInteger.ZERO
        currentInput = StringBuilder("0")
        pendingOp = null; pendingLeft = BigInteger.ZERO
        sync()
    }

    fun clearEntry() {
        currentInput = StringBuilder("0")
        value = BigInteger.ZERO
        sync()
    }

    fun negate() {
        // 在有符号范围内取负
        val mask = maskBig()
        if (value.signum() == 0) return
        // 补码取负:~x+1, 并 mask
        val neg = value.not().add(BigInteger.ONE).and(mask)
        value = maskToWidth(neg)
        currentInput = StringBuilder(valueToCurrentRadix())
        sync()
    }

    // ---------- 一元位运算 ----------

    fun applyNot() {
        val mask = maskBig()
        value = value.not().and(mask)
        currentInput = StringBuilder(valueToCurrentRadix())
        sync()
    }

    // ---------- 二元运算(+ - * / AND OR XOR NAND NOR << >>) ----------

    fun applyBinaryOp(op: String) {
        pendingOp = op
        pendingLeft = value
        // 清空当前输入以便输入右操作数
        currentInput = StringBuilder("0")
        value = BigInteger.ZERO
        sync()
    }

    fun equals() {
        val op = pendingOp ?: return
        val left = pendingLeft
        val right = value
        val result: BigInteger = try {
            when (op) {
                "+" -> left.add(right)
                "-" -> left.subtract(right)
                "*" -> left.multiply(right)
                "/" -> if (right.signum() == 0) BigInteger.ZERO else left.divide(right)
                "%" -> if (right.signum() == 0) BigInteger.ZERO else left.remainder(right)
                "and" -> left.and(right)
                "or" -> left.or(right)
                "xor" -> left.xor(right)
                "nand" -> left.and(right).not()
                "nor" -> left.or(right).not()
                "shl" -> shiftLeft(left, right.toIntCoerce())
                "shr" -> shiftRight(left, right.toIntCoerce())
                else -> right
            }
        } catch (_: Exception) {
            BigInteger.ZERO
        }
        value = maskToWidth(result)
        currentInput = StringBuilder(valueToCurrentRadix())
        pendingOp = null; pendingLeft = BigInteger.ZERO
        sync()
    }

    /** 位面板点击:翻转某个位,position 0 = LSB, max = bits-1 */
    fun toggleBit(position: Int) {
        if (position < 0 || position >= _width.value.bits) return
        val bit = BigInteger.ONE.shiftLeft(position)
        value = if (value.and(bit).signum() != 0) value.and(bit.not()) else value.or(bit)
        value = maskToWidth(value)
        currentInput = StringBuilder(valueToCurrentRadix())
        sync()
    }

    /** 查询某个位是否为 1 */
    fun bitAt(position: Int): Boolean {
        if (position < 0 || position >= _width.value.bits) return false
        return value.and(BigInteger.ONE.shiftLeft(position)).signum() != 0
    }

    /** 返回当前宽度下的位面板数据(从 MSB 到 LSB,共 width.bits 位) */
    fun bitPanel(): List<Boolean> {
        val bits = _width.value.bits
        val list = ArrayList<Boolean>(bits)
        for (i in bits - 1 downTo 0) {
            list.add(bitAt(i))
        }
        return list
    }

    /** 根据宽度返回每个位的标签(参考图:60 56 52...0,用于 UI 标识每组位的最高位) */
    fun bitPanelRowLabels(): List<List<Int>> {
        val totalBits = _width.value.bits
        val cols = 16
        val rows = (totalBits + cols - 1) / cols
        val labels = ArrayList<List<Int>>(rows)
        for (r in 0 until rows) {
            val rowLabels = ArrayList<Int>(cols / 4)
            // 每组 4 位,显示最高位的位置号;16 位一行 = 4 组
            for (c in 0 until cols step 4) {
                val idx = (rows - 1 - r) * cols + (cols - 1 - c - 3)
                // 从高位到低位排列:顶部是 QWORD 的高位
                val actual = (totalBits - 1) - (r * cols + c + 3)
                if (actual in 0 until totalBits) rowLabels.add(actual)
            }
            labels.add(rowLabels)
        }
        return labels
    }

    // ---------- 内存 ----------
    fun memoryStore() { memory = value; _memoryNotEmpty.value = true }
    fun memoryRecall() {
        value = maskToWidth(memory)
        currentInput = StringBuilder(valueToCurrentRadix())
        sync()
    }
    fun memoryAdd() { memory = maskToWidth(memory.add(value)); _memoryNotEmpty.value = true }
    fun memorySubtract() { memory = maskToWidth(memory.subtract(value)); _memoryNotEmpty.value = true }

    // ---------- 私有方法 ----------

    private fun maskToWidth(v: BigInteger): BigInteger {
        val bits = _width.value.bits
        // 负数补码截断:取低 bits 位
        val mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE)
        return v.and(mask)
    }

    private fun maskBig(): BigInteger {
        return BigInteger.ONE.shiftLeft(_width.value.bits).subtract(BigInteger.ONE)
    }

    /** 对 BigInteger 安全转 int,避免溢出 */
    private fun BigInteger.toIntCoerce(): Int = when {
        this > Int.MAX_VALUE.toBigInteger() -> Int.MAX_VALUE
        this < Int.MIN_VALUE.toBigInteger() -> Int.MIN_VALUE
        else -> this.toInt()
    }

    private fun valueToCurrentRadix(): String {
        val r = _radix.value.base
        return value.toString(r).uppercase().ifEmpty { "0" }
    }

    private fun shiftLeft(x: BigInteger, n: Int): BigInteger {
        if (n < 0) return shiftRight(x, -n)
        val bits = _width.value.bits
        return when (_shiftMode.value) {
            ShiftMode.ARITHMETIC, ShiftMode.LOGICAL -> {
                x.shiftLeft(n).and(maskBig())
            }
            ShiftMode.ROTATE -> rotateLeft(x, n)
            ShiftMode.ROTATE_CARRY -> rotateLeft(x, n) // 简化(单操作数 CF=0 效果相同)
        }
    }

    private fun shiftRight(x: BigInteger, n: Int): BigInteger {
        if (n < 0) return shiftLeft(x, -n)
        val bits = _width.value.bits
        return when (_shiftMode.value) {
            ShiftMode.ARITHMETIC -> {
                // 算术右移:按高位补符号位(判断最高位是否1)
                val signed = signBit(x)
                var r = x.shiftRight(n)
                if (signed) {
                    // 在高位补 n 个 1
                    val ones = (BigInteger.ONE.shiftLeft(n).subtract(BigInteger.ONE)).shiftLeft(bits - n)
                    r = r.or(ones)
                }
                r.and(maskBig())
            }
            ShiftMode.LOGICAL -> {
                // 逻辑右移:高位补 0
                val shifted = x.shiftRight(n)
                shifted.and(maskBig().shiftRight(n))
            }
            ShiftMode.ROTATE -> rotateRight(x, n)
            ShiftMode.ROTATE_CARRY -> rotateRight(x, n)
        }
    }

    private fun rotateLeft(x: BigInteger, n: Int): BigInteger {
        val bits = _width.value.bits
        val k = ((n % bits) + bits) % bits
        if (k == 0) return x
        val mask = maskBig()
        val hi = x.shiftLeft(k).and(mask)
        val lo = x.shiftRight(bits - k).and(mask)
        return hi.or(lo).and(mask)
    }

    private fun rotateRight(x: BigInteger, n: Int): BigInteger {
        val bits = _width.value.bits
        val k = ((n % bits) + bits) % bits
        if (k == 0) return x
        val mask = maskBig()
        val lo = x.shiftRight(k).and(mask)
        val hi = x.shiftLeft(bits - k).and(mask)
        return lo.or(hi).and(mask)
    }

    /** 当前值在当前位宽下的最高位(符号位)是否为 1 */
    private fun signBit(x: BigInteger): Boolean {
        val msbPos = _width.value.bits - 1
        return x.and(BigInteger.ONE.shiftLeft(msbPos)).signum() != 0
    }

    // ---------- 初始化:用初始 value(=0) 同步一次所有显示 + 位数组,保证冷启动时状态一致 ----------
    init {
        sync()
    }

    /** 同步所有显示:主显示、4 进制显示、位面板数组、顶部待运算表达式 */
    private fun sync() {
        val radixStr = valueToCurrentRadix()
        _mainDisplay.value = formatByRadix(radixStr, _radix.value.base)
        _hexView.value = formatByRadix(value.toString(16).uppercase(), 16)
        _decView.value = formatByRadix(value.toString(10), 10)
        _octView.value = formatByRadix(value.toString(8), 8)
        _binView.value = formatByRadix(value.toString(2), 2)
        // 同步更新 64 位面板数组(每次新建数组,确保 StateFlow 视为新值)
        val bits = BooleanArray(64)
        val one = BigInteger.ONE
        for (i in 0 until 64) {
            if (value.and(one.shiftLeft(i)).signum() != 0) bits[i] = true
        }
        _bitPanelBits.value = bits
        // 顶部表达式:左操作数(按当前进制格式化) + 运算符符号;无待运算则置空
        // 这样切换进制/宽度时左操作数会按新进制重新显示,与下方主显示保持一致。
        _pendingExpression.value = if (pendingOp != null) {
            val leftStr = formatByRadix(
                pendingLeft.toString(_radix.value.base).uppercase(),
                _radix.value.base
            )
            "$leftStr ${opSymbol(pendingOp!!)}"
        } else null
    }

    /** 运算符 → 显示符号,与标准计算器一致 */
    private fun opSymbol(op: String): String = when (op) {
        "+" -> "+"
        "-" -> "−"
        "*" -> "×"
        "/" -> "÷"
        "%" -> "mod"
        "and" -> "AND"
        "or" -> "OR"
        "xor" -> "XOR"
        "nand" -> "NAND"
        "nor" -> "NOR"
        "shl" -> "<<"
        "shr" -> ">>"
        else -> op
    }

    /**
     * 按进制做 4 位分组空格(参考图 Windows 格式):
     *  HEX: 774A AAAA AAAA AAAA
     *  BIN: 0111 0111 0100 1010 ...
     *  OCT: 每 3-4 位,简化同样 4 位
     *  DEC: 千分位(3位分组)
     */
    private fun formatByRadix(s: String, base: Int): String {
        if (s == "0") return "0"
        return when (base) {
            10 -> {
                // 千分位 3 位分组
                val neg = s.startsWith("-")
                val str = if (neg) s.drop(1) else s
                val sb = StringBuilder(str).reverse()
                val groups = sb.chunked(3).joinToString(",").reversed()
                (if (neg) "-" else "") + groups
            }
            else -> {
                // 4 位分组
                val sb = StringBuilder(s).reverse()
                val groups = sb.chunked(4).joinToString(" ").reversed()
                groups
            }
        }
    }
}
