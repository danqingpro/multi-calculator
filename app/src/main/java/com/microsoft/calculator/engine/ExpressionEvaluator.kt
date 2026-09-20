package com.microsoft.calculator.engine

import java.math.BigDecimal

/**
 * 表达式求值器:Shunting-yard 转后缀表达式,再求值。
 * 支持标准 + 科学模式的运算符优先级,对标 Windows 计算器科学模式。
 * 支持: + - * / % ^ ! sin cos tan asin acos atan sinh cosh tanh sec csc cot ln log sqrt abs exp floor ceil dms deg 以及括号与负号。
 * angleInDegrees=true 时,三角/反三角函数以度为单位输入输出;否则以弧度。
 */
class ExpressionEvaluator {

    private val functions = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
        "sec", "csc", "cot", "asec", "acsc", "acot",
        "sech", "csch", "coth", "asech", "acsch", "acoth",
        "ln", "log", "sqrt", "cbrt", "abs", "exp", "neg", "inv", "fact",
        "floor", "ceil", "dms", "deg", "rand"
    )

    private val constants = mapOf(
        "pi" to BigDecimal(Math.PI),
        "e" to BigDecimal(Math.E)
    )

    private var angleMode: AngleMode = AngleMode.RAD

    /** DEG=度, RAD=弧度, GRAD=梯度(400 grad = 360°) */
    enum class AngleMode { DEG, RAD, GRAD }

    fun evaluate(expression: String, angleMode: AngleMode = AngleMode.RAD): BigDecimal {
        this.angleMode = angleMode
        val tokens = tokenize(expression)
        val rpn = toRpn(tokens)
        return evalRpn(rpn)
    }

    /** 带变量绑定的求值,用于绘图模式。variables 如 mapOf("x" to 3.14) */
    fun evaluateWithVars(
        expression: String,
        variables: Map<String, Double>,
        angleMode: AngleMode = AngleMode.RAD
    ): BigDecimal {
        this.angleMode = angleMode
        // 在词法阶段,把变量 IDENT 替换成对应数值
        val tokens = tokenize(expression).map { tok ->
            if (tok.type == TokenType.IDENT && variables.containsKey(tok.value)) {
                Token(TokenType.NUMBER, variables[tok.value].toString())
            } else tok
        }
        val rpn = toRpn(tokens)
        return evalRpn(rpn)
    }

    // 角度输入 → 弧度;反三角函数输出从弧度转角度制
    private fun toRad(a: BigDecimal): BigDecimal = when (angleMode) {
        AngleMode.DEG -> MathEngine.toRadFromDeg(a)
        AngleMode.GRAD -> MathEngine.toRadFromGrad(a)
        AngleMode.RAD -> a
    }
    private fun fromRad(r: BigDecimal): BigDecimal = when (angleMode) {
        AngleMode.DEG -> MathEngine.toDegFromRad(r)
        AngleMode.GRAD -> MathEngine.toGradFromRad(r)
        AngleMode.RAD -> r
    }


    /** 词法分析 */
    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val sb = StringBuilder()
        var i = 0
        fun flushNumber() {
            if (sb.isNotEmpty()) {
                tokens.add(Token(TokenType.NUMBER, sb.toString()))
                sb.clear()
            }
        }
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> { i++ }
                c.isDigit() || c == '.' -> {
                    sb.append(c); i++
                }
                c.isLetter() -> {
                    flushNumber()
                    val name = StringBuilder()
                    while (i < expr.length && expr[i].isLetter()) {
                        name.append(expr[i]); i++
                    }
                    tokens.add(Token(TokenType.IDENT, name.toString().lowercase()))
                }
                c == '!' -> { flushNumber(); tokens.add(Token(TokenType.OP, "!")); i++ }
                c == '%' -> { flushNumber(); tokens.add(Token(TokenType.OP, "%")); i++ }
                c == '^' -> { flushNumber(); tokens.add(Token(TokenType.OP, "^")); i++ }
                c == '+' -> {
                    flushNumber()
                    // 一元正号当作为符号开头或在 ( 后
                    if (tokens.isEmpty() || tokens.last().type == TokenType.OP ||
                        tokens.last().value == "("
                    ) {
                        // 一元正,忽略
                    } else {
                        tokens.add(Token(TokenType.OP, "+"))
                    }
                    i++
                }
                c == '-' -> {
                    flushNumber()
                    if (tokens.isEmpty() || tokens.last().value == "(" || tokens.last().type == TokenType.OP) {
                        // 一元负,标记为 neg 函数
                        tokens.add(Token(TokenType.IDENT, "neg"))
                    } else {
                        tokens.add(Token(TokenType.OP, "-"))
                    }
                    i++
                }
                c == '*' || c == '×' -> { flushNumber(); tokens.add(Token(TokenType.OP, "*")); i++ }
                c == '/' || c == '÷' -> { flushNumber(); tokens.add(Token(TokenType.OP, "/")); i++ }
                c == '(' -> { flushNumber(); tokens.add(Token(TokenType.LPAREN, "(")); i++ }
                c == ')' -> { flushNumber(); tokens.add(Token(TokenType.RPAREN, ")")); i++ }
                c == ',' -> { flushNumber(); tokens.add(Token(TokenType.COMMA, ",")); i++ }
                else -> throw IllegalArgumentException("Unexpected character '$c'")
            }
        }
        flushNumber()
        return tokens
    }

    private fun precedence(op: String): Int = when (op) {
        "!", "fact" -> 5
        "^" -> 4
        "*", "/", "%" -> 3
        "+", "-" -> 2
        else -> 0
    }

    private fun isRightAssoc(op: String): Boolean = op == "^"

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()
        for (t in tokens) {
            when (t.type) {
                TokenType.NUMBER -> output.add(t)
                TokenType.IDENT -> {
                    if (t.value in constants) {
                        output.add(Token(TokenType.NUMBER, t.value)) // 标记为常量,evalRpn 处理
                        // 实际上我们把常量当作 NUMBER 但保留名字;用特殊处理
                        output.removeLast()
                        output.add(Token(TokenType.IDENT, t.value))
                    } else if (t.value in functions) {
                        stack.addLast(t)
                    } else throw IllegalArgumentException("Unknown identifier: ${t.value}")
                }
                TokenType.OP -> {
                    while (stack.isNotEmpty() && stack.last().type == TokenType.IDENT && stack.last().value !in listOf("(")) {
                        output.add(stack.removeLast())
                    }
                    while (stack.isNotEmpty() && stack.last().type == TokenType.OP &&
                        (precedence(stack.last().value) > precedence(t.value) ||
                            (precedence(stack.last().value) == precedence(t.value) && !isRightAssoc(t.value)))
                    ) {
                        output.add(stack.removeLast())
                    }
                    stack.addLast(t)
                }
                TokenType.LPAREN -> stack.addLast(t)
                TokenType.RPAREN -> {
                    while (stack.isNotEmpty() && stack.last().type != TokenType.LPAREN) {
                        output.add(stack.removeLast())
                    }
                    if (stack.isEmpty()) throw IllegalArgumentException("Mismatched parentheses")
                    stack.removeLast() // 弹出 (
                    // 如果栈顶是函数,弹出
                    if (stack.isNotEmpty() && stack.last().type == TokenType.IDENT && stack.last().value in functions) {
                        output.add(stack.removeLast())
                    }
                }
                TokenType.COMMA -> { /* 分隔符,处理多参数;此处单参数 */ }
            }
        }
        while (stack.isNotEmpty()) {
            val t = stack.removeLast()
            if (t.type == TokenType.LPAREN) throw IllegalArgumentException("Mismatched parentheses")
            output.add(t)
        }
        return output
    }

    private fun evalRpn(rpn: List<Token>): BigDecimal {
        val stack = ArrayDeque<BigDecimal>()
        for (t in rpn) {
            when {
                t.type == TokenType.NUMBER -> {
                    val v = BigDecimal(t.value.replace(",", ""))
                    stack.addLast(v)
                }
                t.type == TokenType.IDENT && t.value in constants -> {
                    stack.addLast(constants[t.value]!!)
                }
                t.type == TokenType.IDENT -> {
                    if (t.value == "rand") {
                        // 零参数函数:直接压入随机数
                        stack.addLast(MathEngine.rand())
                    } else {
                        val a = stack.removeLast()
                        stack.addLast(applyFunction(t.value, a))
                    }
                }
                t.type == TokenType.OP -> {
                    when (t.value) {
                        "!" -> {
                            val a = stack.removeLast()
                            stack.addLast(MathEngine.fact(a))
                        }
                        "fact" -> {
                            val a = stack.removeLast()
                            stack.addLast(MathEngine.fact(a))
                        }
                        else -> {
                            val b = stack.removeLast()
                            val a = stack.removeLast()
                            stack.addLast(applyOp(t.value, a, b))
                        }
                    }
                }
                else -> {}
            }
        }
        if (stack.size != 1) throw IllegalArgumentException("Invalid expression")
        return stack.last()
    }

    private fun applyOp(op: String, a: BigDecimal, b: BigDecimal): BigDecimal = when (op) {
        "+" -> MathEngine.add(a, b)
        "-" -> MathEngine.subtract(a, b)
        "*" -> MathEngine.multiply(a, b)
        "/" -> MathEngine.divide(a, b)
        "%" -> MathEngine.mod(a, b)
        "^" -> MathEngine.power(a, b)
        else -> throw IllegalArgumentException("Unknown operator $op")
    }

    private fun applyFunction(name: String, a: BigDecimal): BigDecimal = when (name) {
        // 三角函数:输入按角度模式转弧度,输出直接是弧度(正向函数都返回弧度域结果,无需反转换)
        "sin" -> MathEngine.sinRad(toRad(a))
        "cos" -> MathEngine.cosRad(toRad(a))
        "tan" -> MathEngine.tanRad(toRad(a))
        // 反三角函数:直接在弧度域计算,然后按角度模式输出
        "asin" -> fromRad(MathEngine.asinRad(a))
        "acos" -> fromRad(MathEngine.acosRad(a))
        "atan" -> fromRad(MathEngine.atanRad(a))
        // 正割/余割/余切
        "sec" -> MathEngine.secRad(toRad(a))
        "csc" -> MathEngine.cscRad(toRad(a))
        "cot" -> MathEngine.cotRad(toRad(a))
        "asec" -> fromRad(MathEngine.asecRad(a))
        "acsc" -> fromRad(MathEngine.acscRad(a))
        "acot" -> fromRad(MathEngine.acotRad(a))
        // 双曲函数(恒为弧度)
        "sinh" -> MathEngine.sinh(a)
        "cosh" -> MathEngine.cosh(a)
        "tanh" -> MathEngine.tanh(a)
        "asinh" -> MathEngine.asinh(a)
        "acosh" -> MathEngine.acosh(a)
        "atanh" -> MathEngine.atanh(a)
        "sech" -> MathEngine.sech(a)
        "csch" -> MathEngine.csch(a)
        "coth" -> MathEngine.coth(a)
        "asech" -> MathEngine.asech(a)
        "acsch" -> MathEngine.acsch(a)
        "acoth" -> MathEngine.acoth(a)
        // 对数/幂/其他
        "ln" -> MathEngine.ln(a)
        "log" -> MathEngine.log10(a)
        "sqrt" -> MathEngine.sqrt(a)
        "cbrt" -> MathEngine.cbrt(a)
        "abs" -> MathEngine.abs(a)
        "exp" -> MathEngine.exp(a)
        "neg" -> MathEngine.negate(a)
        "inv" -> MathEngine.reciprocal(a)
        "fact" -> MathEngine.fact(a)
        "floor" -> MathEngine.floor(a)
        "ceil" -> MathEngine.ceil(a)
        "dms" -> MathEngine.toDms(a)
        "deg" -> MathEngine.fromDms(a)
        else -> throw IllegalArgumentException("Unknown function $name")
    }
}

enum class TokenType { NUMBER, IDENT, OP, LPAREN, RPAREN, COMMA }
data class Token(val type: TokenType, val value: String)
