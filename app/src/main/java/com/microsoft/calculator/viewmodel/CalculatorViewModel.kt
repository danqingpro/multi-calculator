package com.microsoft.calculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.calculator.engine.ExpressionEvaluator
import com.microsoft.calculator.engine.MathEngine
import com.microsoft.calculator.engine.NumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 标准/科学计算器 ViewModel。
 * 标准模式:即时求值链式输入(对标 Windows 计算器 Standard)。
 * 科学模式:表达式字符串,等号整体求值(对标 Windows 计算器 Scientific,支持优先级)。
 * 包含内存(M+/M-/MS/MR/MC)与历史记录。
 */
class CalculatorViewModel : ViewModel() {

    enum class Mode { STANDARD, SCIENTIFIC }
    enum class AngleMode { DEG, RAD, GRAD }

    private val evaluator = ExpressionEvaluator()

    private val _display = MutableStateFlow("0")
    val display: StateFlow<String> = _display.asStateFlow()

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _mode = MutableStateFlow(Mode.STANDARD)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _angleMode = MutableStateFlow(AngleMode.DEG)
    val angleMode: StateFlow<AngleMode> = _angleMode.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _memory = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memory: StateFlow<List<MemoryItem>> = _memory.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    // 科学模式:2ⁿᵈ 第二函数开关(反三角函数等)
    private val _isSecondFunction = MutableStateFlow(false)
    val isSecondFunction: StateFlow<Boolean> = _isSecondFunction.asStateFlow()

    // 科学模式:hyp 双曲函数开关
    private val _isHyp = MutableStateFlow(false)
    val isHyp: StateFlow<Boolean> = _isHyp.asStateFlow()

    // 科学模式:F-E 固定/指数计数法开关
    private val _fixExp = MutableStateFlow(false)
    val fixExp: StateFlow<Boolean> = _fixExp.asStateFlow()

    // 标准模式状态机
    private var current = BigDecimal.ZERO
    private var stored = BigDecimal.ZERO
    private var pendingOp: String? = null
    private var freshInput = true   // 是否刚按过运算符/等号(下一次数字输入应清空显示)
    private var lastOpRight: BigDecimal? = null // 用于等号重复

    // 内存
    private var memoryValue = BigDecimal.ZERO

    fun setMode(m: Mode) {
        _mode.value = m
        clear()
    }

    fun toggleAngleMode() {
        _angleMode.value = when (_angleMode.value) {
            AngleMode.DEG -> AngleMode.RAD
            AngleMode.RAD -> AngleMode.GRAD
            AngleMode.GRAD -> AngleMode.DEG
        }
    }

    /** 2ⁿᵈ 第二函数开关(切反三角等) */
    fun toggleSecondFunction() {
        _isSecondFunction.value = !_isSecondFunction.value
    }

    /** hyp 双曲函数开关 */
    fun toggleHyp() {
        _isHyp.value = !_isHyp.value
    }

    /** F-E 固定/指数计数法开关 */
    fun toggleFixExp() {
        _fixExp.value = !_fixExp.value
    }

    // ---------- 数字输入 ----------
    fun inputDigit(d: String) {
        if (_isError.value) clear()
        if (_mode.value == Mode.SCIENTIFIC) {
            // 等号后再次输入:清空旧表达式,重新开始
            if (freshInput) {
                _expression.value = ""
                freshInput = false
            }
            appendScientific(d)
            // 同步底部显示:把表达式末尾正在输入的数字提出来显示
            val num = extractTrailingNumber(_expression.value)
            if (num.isNotEmpty()) _display.value = num
            return
        }
        // 标准模式
        if (freshInput) {
            _display.value = if (d == "0") "0" else d
            freshInput = false
        } else {
            val cur = _display.value
            _display.value = if (cur == "0") d else cur + d
        }
        current = parseDisplay()
    }

    fun inputDecimal() {
        if (_isError.value) clear()
        if (_mode.value == Mode.SCIENTIFIC) {
            if (freshInput) {
                _expression.value = ""
                freshInput = false
            }
            appendScientific(".")
            val num = extractTrailingNumber(_expression.value)
            if (num.isNotEmpty()) _display.value = num
            return
        }
        if (freshInput) {
            _display.value = "0."
            freshInput = false
        } else if (!_display.value.contains(".")) {
            _display.value = _display.value + "."
        }
        current = parseDisplay()
    }

    private fun appendScientific(token: String) {
        val cur = _expression.value
        _expression.value = cur + token
    }

    // ---------- 通用符号/函数输入(科学模式附加到表达式,标准模式作为一元函数) ----------
    fun inputOp(token: String) {
        if (_isError.value) return
        if (_mode.value == Mode.SCIENTIFIC) {
            // 等号后再次操作:清空旧表达式
            if (freshInput) {
                _expression.value = ""
                freshInput = false
            }
            val toAppend = when (token) {
                "pi", "e", "(", ")", "," -> token
                "neg" -> "-"
                "inv" -> "(1)/"
                "exp" -> "exp("
                "rand" -> "rand"
                "sqr" -> "^2"             // x²
                "10x" -> "10^"           // 10^x
                "cbrt" -> "cbrt("        // ³√x
                "abs" -> "abs("
                "floor" -> "floor("
                "ceil" -> "ceil("
                "dms" -> "dms("
                "deg" -> "deg("
                else -> token + "("
            }
            _expression.value = _expression.value + toAppend
        } else {
            applyUnaryFunction(token)
        }
    }

    // ---------- 运算符 ----------
    fun inputOperator(op: String) {
        if (_isError.value) return
        if (_mode.value == Mode.SCIENTIFIC) {
            if (freshInput) {
                _expression.value = ""
                freshInput = false
            }
            // 映射到表达式符号
            val sym = when (op) {
                "+" -> "+"; "-" -> "-"; "×" -> "*"; "÷" -> "/"
                "mod" -> "%"; "pow" -> "^"; "yroot" -> "^"
                else -> op
            }
            _expression.value = _expression.value + sym
            return
        }
        // 标准模式状态机
        if (pendingOp != null && !freshInput) {
            // 先求值之前的运算
            compute()
        }
        stored = current
        pendingOp = op
        freshInput = true
        updateExpressionDisplay()
    }

    fun equals() {
        if (_isError.value) return
        if (_mode.value == Mode.SCIENTIFIC) {
            val expr = _expression.value
            if (expr.isBlank()) return
            try {
                val angleMode = when (_angleMode.value) {
                    AngleMode.DEG -> ExpressionEvaluator.AngleMode.DEG
                    AngleMode.RAD -> ExpressionEvaluator.AngleMode.RAD
                    AngleMode.GRAD -> ExpressionEvaluator.AngleMode.GRAD
                }
                val result = evaluator.evaluate(expr, angleMode)
                val displayResult = NumberFormatter.format(result, scientific = _fixExp.value)
                _history.value = _history.value + HistoryItem(
                    expr + " =",
                    displayResult,
                    System.currentTimeMillis()
                )
                _display.value = displayResult
                current = result
                // 表达式保留在顶部作为历史,不立即清空(对标图4)
                freshInput = true
            } catch (e: Exception) {
                _isError.value = true
                _display.value = "Error"
            }
            return
        }
        // 标准模式等号
        if (pendingOp != null) {
            val right = current
            lastOpRight = right
            compute()
            _history.value = _history.value + HistoryItem(
                buildExprString(stored, pendingOp!!, right) + " =",
                _display.value,
                System.currentTimeMillis()
            )
            pendingOp = null
            freshInput = true
            _expression.value = ""
        }
    }

    private fun compute() {
        val op = pendingOp ?: return
        try {
            val result = when (op) {
                "+" -> MathEngine.add(stored, current)
                "-" -> MathEngine.subtract(stored, current)
                "×" -> MathEngine.multiply(stored, current)
                "÷" -> MathEngine.divide(stored, current)
                "mod" -> MathEngine.mod(stored, current)
                "pow" -> MathEngine.power(stored, current)
                else -> throw IllegalArgumentException("Unknown op")
            }
            stored = result
            _display.value = NumberFormatter.format(result)
            current = result
        } catch (e: Exception) {
            _isError.value = true
            _display.value = "Error"
        }
    }

    // ---------- 一元函数 ----------
    fun applyUnaryFunction(name: String) {
        if (_isError.value) return
        if (_mode.value == Mode.SCIENTIFIC) {
            // 科学模式追加函数名与括号
            _expression.value = _expression.value + name + "("
            return
        }
        // 标准模式立即应用
        try {
            current = when (name) {
                "sqrt" -> MathEngine.sqrt(current)
                "inv" -> MathEngine.reciprocal(current)
                "neg" -> MathEngine.negate(current)
                "sqr" -> MathEngine.power(current, BigDecimal(2))
                "cube" -> MathEngine.power(current, BigDecimal(3))
                "abs" -> MathEngine.abs(current)
                "fact" -> MathEngine.fact(current)
                "ln" -> MathEngine.ln(current)
                "log" -> MathEngine.log10(current)
                "exp" -> MathEngine.exp(current)
                "sin", "cos", "tan", "asin", "acos", "atan" -> applyTrig(name)
                "ceil" -> current.setScale(0, RoundingMode.CEILING)
                "floor" -> current.setScale(0, RoundingMode.FLOOR)
                else -> current
            }
            _display.value = NumberFormatter.format(current)
            freshInput = true
        } catch (e: Exception) {
            _isError.value = true
            _display.value = "Error"
        }
    }

    private fun applyTrig(name: String): BigDecimal {
        // 角度模式处理
        val v = current
        return when (name) {
            "sin" -> MathEngine.sinDeg(v)
            "cos" -> MathEngine.cosDeg(v)
            "tan" -> MathEngine.tanDeg(v)
            "asin" -> MathEngine.asinDeg(v)
            "acos" -> MathEngine.acosDeg(v)
            "atan" -> MathEngine.atanDeg(v)
            else -> v
        }
    }

    // ---------- 百分比 ----------
    fun percent() {
        if (_isError.value) return
        if (_mode.value == Mode.SCIENTIFIC) {
            _expression.value = _expression.value + "%"
            return
        }
        if (pendingOp != null) {
            // 百分比 = stored 的百分比
            current = MathEngine.divide(stored.multiply(current), BigDecimal(100))
        } else {
            current = MathEngine.divide(current, BigDecimal(100))
        }
        _display.value = NumberFormatter.format(current)
        freshInput = true
    }

    // ---------- 清除类 ----------
    fun clear() {
        current = BigDecimal.ZERO
        stored = BigDecimal.ZERO
        pendingOp = null
        freshInput = true
        lastOpRight = null
        _display.value = "0"
        _expression.value = ""
        _isError.value = false
    }

    fun clearEntry() {
        if (_isError.value) { clear(); return }
        if (_mode.value == Mode.SCIENTIFIC) {
            _expression.value = ""
            _display.value = "0"
            freshInput = true
        } else {
            current = BigDecimal.ZERO
            _display.value = "0"
            freshInput = true
        }
    }

    fun backspace() {
        if (_isError.value) { clear(); return }
        if (_mode.value == Mode.SCIENTIFIC) {
            val e = _expression.value
            if (e.isNotEmpty()) {
                _expression.value = e.dropLast(1)
                // 同步底部显示
                _display.value = extractTrailingNumber(_expression.value)
            }
            return
        }
        val cur = _display.value
        if (freshInput || cur == "0") return
        val newStr = if (cur.length == 1) "0" else cur.dropLast(1)
        _display.value = newStr
        current = parseDisplay()
    }

    // ---------- 内存操作 ----------
    fun memoryAdd() {
        memoryValue = MathEngine.add(memoryValue, parseOrDisplay())
        refreshMemoryList()
    }

    fun memorySubtract() {
        memoryValue = MathEngine.subtract(memoryValue, parseOrDisplay())
        refreshMemoryList()
    }

    fun memoryStore() {
        memoryValue = parseOrDisplay()
        refreshMemoryList()
    }

    fun memoryRecall() {
        if (_mode.value == Mode.SCIENTIFIC) {
            _expression.value = _expression.value + NumberFormatter.format(memoryValue, false)
        } else {
            current = memoryValue
            _display.value = NumberFormatter.format(memoryValue)
            freshInput = true
        }
    }

    fun memoryClear() {
        memoryValue = BigDecimal.ZERO
        refreshMemoryList()
    }

    private fun refreshMemoryList() {
        _memory.value = if (memoryValue.compareTo(BigDecimal.ZERO) == 0) emptyList()
        else listOf(MemoryItem(NumberFormatter.format(memoryValue), System.currentTimeMillis()))
    }

    // ---------- 历史 ----------
    fun clearHistory() { _history.value = emptyList() }

    // ---------- 辅助 ----------
    private fun parseDisplay(): BigDecimal {
        return try {
            BigDecimal(_display.value.replace(",", ""))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun parseOrDisplay(): BigDecimal =
        if (_mode.value == Mode.SCIENTIFIC) {
            try { evaluator.evaluate(_expression.value) } catch (e: Exception) { parseDisplay() }
        } else parseDisplay()

    private fun updateExpressionDisplay() {
        val op = pendingOp ?: return
        _expression.value = NumberFormatter.format(stored) + " " + opSym(op)
    }

    private fun opSym(op: String) = when (op) {
        "+" -> "+"; "-" -> "-"; "×" -> "×"; "÷" -> "÷"
        "mod" -> "mod"; "pow" -> "^"; else -> op
    }

    private fun buildExprString(a: BigDecimal, op: String, b: BigDecimal): String {
        val sym = opSym(op)
        return "${NumberFormatter.format(a)} $sym ${NumberFormatter.format(b)}"
    }

    /**
     * 从表达式末尾提取正在输入的数字(用于科学模式底部实时显示)。
     * 例如 "log(10" -> "10"; "3.14+" -> "" (末尾不是数字则返回空,显示保持上一次结果);
     * "100" -> "100"; "sin(0.5" -> "0.5"。
     */
    private fun extractTrailingNumber(expr: String): String {
        if (expr.isEmpty()) return ""
        // 从末尾向前扫描连续的数字/小数点
        val sb = StringBuilder()
        var i = expr.length - 1
        while (i >= 0 && (expr[i].isDigit() || expr[i] == '.')) {
            sb.append(expr[i])
            i--
        }
        if (sb.isEmpty()) return ""
        return sb.reverse().toString()
    }
}

data class HistoryItem(val expr: String, val result: String, val timestamp: Long)
data class MemoryItem(val value: String, val timestamp: Long)
