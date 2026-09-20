package com.microsoft.calculator.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/**
 * 高精度数学引擎,对标 Windows 计算器 Ratpack 的无限精度算术能力。
 * 基础四则运算使用 BigDecimal,保证不丢失精度。
 */
object MathEngine {
    const val DEFAULT_SCALE = 32
    val MATH_CONTEXT = MathContext(DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    val MATH_CONTEXT_HALF_UP = MathContext(DEFAULT_SCALE, RoundingMode.HALF_UP)

    fun add(a: BigDecimal, b: BigDecimal): BigDecimal = a.add(b, MATH_CONTEXT)
    fun subtract(a: BigDecimal, b: BigDecimal): BigDecimal = a.subtract(b, MATH_CONTEXT)
    fun multiply(a: BigDecimal, b: BigDecimal): BigDecimal = a.multiply(b, MATH_CONTEXT)

    /** 除法使用高精度刻度,避免无限循环小数报错 */
    fun divide(a: BigDecimal, b: BigDecimal): BigDecimal {
        if (b.signum() == 0) throw ArithmeticException("Divide by zero")
        return a.divide(b, DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    fun mod(a: BigDecimal, b: BigDecimal): BigDecimal {
        if (b.signum() == 0) throw ArithmeticException("Modulo by zero")
        return a.remainder(b, MATH_CONTEXT)
    }

    /** 幂运算:整数幂用精确乘法,小数幂用 exp/ln */
    fun power(base: BigDecimal, exp: BigDecimal): BigDecimal {
        // 尝试整数幂
        val scaled = exp.setScale(0, RoundingMode.DOWN)
        val isIntExp = scaled.compareTo(exp) == 0
        if (isIntExp) {
            try {
                val n = scaled.intValueExact()
                return base.pow(n, MATH_CONTEXT).stripTrailingZeros()
            } catch (_: ArithmeticException) {
                // 溢出,转小数幂
            }
        }
        // 负底数 + 整数次幂已处理;其余走 exp/ln
        if (base.signum() < 0) {
            throw ArithmeticException("Negative base with fractional exponent")
        }
        return exp(ln(base).multiply(exp)).stripTrailingZeros()
    }

    fun negate(a: BigDecimal): BigDecimal = a.negate(MATH_CONTEXT)

    fun exp(a: BigDecimal): BigDecimal {
        return BigDecimal(Math.exp(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    fun ln(a: BigDecimal): BigDecimal {
        if (a.signum() <= 0) throw ArithmeticException("ln of non-positive")
        return BigDecimal(Math.log(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    fun log10(a: BigDecimal): BigDecimal {
        if (a.signum() <= 0) throw ArithmeticException("log of non-positive")
        return BigDecimal(Math.log10(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    fun sqrt(a: BigDecimal): BigDecimal {
        if (a.signum() < 0) throw ArithmeticException("sqrt of negative")
        return BigDecimal(Math.sqrt(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    fun cbrt(a: BigDecimal): BigDecimal =
        BigDecimal(Math.cbrt(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    fun sinDeg(a: BigDecimal): BigDecimal =
        BigDecimal(Math.sin(Math.toRadians(a.toDouble()))).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    fun cosDeg(a: BigDecimal): BigDecimal =
        BigDecimal(Math.cos(Math.toRadians(a.toDouble()))).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    fun tanDeg(a: BigDecimal): BigDecimal =
        BigDecimal(Math.tan(Math.toRadians(a.toDouble()))).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    fun asinDeg(a: BigDecimal): BigDecimal {
        val v = Math.toDegrees(Math.asin(a.toDouble()))
        return BigDecimal(v).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    fun acosDeg(a: BigDecimal): BigDecimal {
        val v = Math.toDegrees(Math.acos(a.toDouble()))
        return BigDecimal(v).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    fun atanDeg(a: BigDecimal): BigDecimal {
        val v = Math.toDegrees(Math.atan(a.toDouble()))
        return BigDecimal(v).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    // ---------- 弧度版三角函数(科学模式 RAD) ----------
    fun sinRad(a: BigDecimal): BigDecimal =
        BigDecimal(Math.sin(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun cosRad(a: BigDecimal): BigDecimal =
        BigDecimal(Math.cos(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun tanRad(a: BigDecimal): BigDecimal =
        BigDecimal(Math.tan(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun asinRad(a: BigDecimal): BigDecimal =
        BigDecimal(Math.asin(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun acosRad(a: BigDecimal): BigDecimal =
        BigDecimal(Math.acos(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun atanRad(a: BigDecimal): BigDecimal =
        BigDecimal(Math.atan(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    // ---------- 双曲函数 ----------
    fun sinh(a: BigDecimal): BigDecimal =
        BigDecimal(Math.sinh(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun cosh(a: BigDecimal): BigDecimal =
        BigDecimal(Math.cosh(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun tanh(a: BigDecimal): BigDecimal =
        BigDecimal(Math.tanh(a.toDouble())).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun asinh(a: BigDecimal): BigDecimal {
        val x = a.toDouble()
        return BigDecimal(Math.log(x + Math.sqrt(x * x + 1.0))).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }
    fun acosh(a: BigDecimal): BigDecimal {
        val x = a.toDouble()
        return BigDecimal(Math.log(x + Math.sqrt(x * x - 1.0))).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }
    fun atanh(a: BigDecimal): BigDecimal {
        val x = a.toDouble()
        return BigDecimal(0.5 * Math.log((1.0 + x) / (1.0 - x))).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    }

    // ---------- 正割/余割/余切(弧度版,按角度模式外层转换) ----------
    fun secRad(a: BigDecimal): BigDecimal = reciprocal(cosRad(a))
    fun cscRad(a: BigDecimal): BigDecimal = reciprocal(sinRad(a))
    fun cotRad(a: BigDecimal): BigDecimal = divide(cosRad(a), sinRad(a))
    fun asecRad(a: BigDecimal): BigDecimal = acosRad(reciprocal(a))
    fun acscRad(a: BigDecimal): BigDecimal = asinRad(reciprocal(a))
    fun acotRad(a: BigDecimal): BigDecimal = atanRad(reciprocal(a))

    // 双曲版 sec/csc/cot
    fun sech(a: BigDecimal): BigDecimal = reciprocal(cosh(a))
    fun csch(a: BigDecimal): BigDecimal = reciprocal(sinh(a))
    fun coth(a: BigDecimal): BigDecimal = divide(cosh(a), sinh(a))
    fun asech(a: BigDecimal): BigDecimal = acosh(reciprocal(a))
    fun acsch(a: BigDecimal): BigDecimal = asinh(reciprocal(a))
    fun acoth(a: BigDecimal): BigDecimal = atanh(reciprocal(a))

    // ---------- 取整 ----------
    fun floor(a: BigDecimal): BigDecimal = a.setScale(0, RoundingMode.FLOOR)
    fun ceil(a: BigDecimal): BigDecimal = a.setScale(0, RoundingMode.CEILING)

    // ---------- 度 ↔ 度分秒 ----------
    // 小数度 → DMS 表示(把小数部分转成分秒,返回一个数如 30.5° → 30.30 = 30°30')
    fun toDms(a: BigDecimal): BigDecimal {
        val deg = a.setScale(0, RoundingMode.DOWN)
        val minFrac = a.subtract(deg).abs().multiply(BigDecimal(60))
        val min = minFrac.setScale(0, RoundingMode.DOWN)
        val secFrac = minFrac.subtract(min).multiply(BigDecimal(60))
        val sec = secFrac.setScale(2, RoundingMode.HALF_EVEN)
        // 结果: deg + min/100 + sec/10000
        return deg.add(min.movePointLeft(2)).add(sec.movePointLeft(4))
    }
    // DMS 表示 → 小数度
    fun fromDms(a: BigDecimal): BigDecimal {
        val deg = a.setScale(0, RoundingMode.DOWN)
        val frac = a.subtract(deg).abs()
        val min = frac.multiply(BigDecimal(100)).setScale(0, RoundingMode.DOWN)
        val sec = frac.multiply(BigDecimal(100)).subtract(min).multiply(BigDecimal(100))
        return deg.add(min.divide(BigDecimal(60), DEFAULT_SCALE, RoundingMode.HALF_EVEN))
            .add(sec.divide(BigDecimal(3600), DEFAULT_SCALE, RoundingMode.HALF_EVEN))
    }

    // ---------- 随机数 [0,1) ----------
    fun rand(): BigDecimal = BigDecimal(Math.random()).setScale(DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    /** 弧度 ↔ 度/梯度互转(表达式求值器用) */
    fun toRadFromDeg(a: BigDecimal): BigDecimal =
        a.multiply(BigDecimal(Math.PI)).divide(BigDecimal(180), DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun toDegFromRad(r: BigDecimal): BigDecimal =
        r.multiply(BigDecimal(180)).divide(BigDecimal(Math.PI), DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun toRadFromGrad(a: BigDecimal): BigDecimal =
        a.multiply(BigDecimal(Math.PI)).divide(BigDecimal(200), DEFAULT_SCALE, RoundingMode.HALF_EVEN)
    fun toGradFromRad(r: BigDecimal): BigDecimal =
        r.multiply(BigDecimal(200)).divide(BigDecimal(Math.PI), DEFAULT_SCALE, RoundingMode.HALF_EVEN)

    fun fact(a: BigDecimal): BigDecimal {
        val n = a.setScale(0, RoundingMode.DOWN)
        if (n.signum() < 0) throw ArithmeticException("Factorial of negative")
        if (n > BigDecimal(5000)) throw ArithmeticException("Factorial too large")
        var result = BigInteger.ONE
        var i = BigInteger.valueOf(2)
        val max = n.toBigInteger()
        while (i <= max) {
            result = result.multiply(i)
            i = i.add(BigInteger.ONE)
        }
        return BigDecimal(result)
    }

    fun reciprocal(a: BigDecimal): BigDecimal = divide(BigDecimal.ONE, a)

    fun abs(a: BigDecimal): BigDecimal = a.abs(MATH_CONTEXT)

    /** 十进制转任意进制(2/8/16) */
    fun toBase(value: BigDecimal, radix: Int): String {
        val intPart = value.toBigInteger()
        val frac = value.subtract(BigDecimal(intPart))
        val intStr = if (intPart.signum() == 0) "0" else intPart.toString(radix).uppercase()
        if (frac.signum() == 0) return intStr
        // 小数部分
        val sb = StringBuilder()
        var f = frac
        repeat(16) {
            f = f.multiply(BigDecimal(radix))
            val d = f.toBigInteger()
            sb.append(d.toString(radix).uppercase())
            f = f.subtract(BigDecimal(d))
            if (f.signum() == 0) return@repeat
        }
        return "$intStr.${sb}"
    }

    /** 任意进制转十进制 BigDecimal */
    fun fromBase(text: String, radix: Int): BigDecimal {
        val cleaned = text.trim().uppercase().replace(" ", "")
        if (cleaned.isEmpty()) return BigDecimal.ZERO
        val (intPartRaw, fracPartRaw) = if (cleaned.contains('.')) {
            val p = cleaned.split('.')
            p[0] to p[1]
        } else cleaned to ""
        val intPart = if (intPartRaw.isEmpty()) BigInteger.ZERO else BigInteger(intPartRaw, radix)
        var result = BigDecimal(intPart)
        if (fracPartRaw.isNotEmpty()) {
            var f = BigDecimal.ZERO
            var weight = BigDecimal.ONE.divide(BigDecimal(radix), DEFAULT_SCALE, RoundingMode.HALF_EVEN)
            for (c in fracPartRaw) {
                val d = Character.digit(c, radix)
                if (d < 0) break
                f = f.add(BigDecimal(d).multiply(weight))
                weight = weight.divide(BigDecimal(radix), DEFAULT_SCALE, RoundingMode.HALF_EVEN)
            }
            result = result.add(f)
        }
        return result
    }
}
