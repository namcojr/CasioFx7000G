package com.retro.fx7000g.calc

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.math.truncate

/** Angle unit, mirroring the FX-7000G's Deg / Rad / Gra modes. */
enum class AngleMode(val label: String) {
    DEG("DEG"),
    RAD("RAD"),
    GRA("GRA")
}

/** Thrown for any math/syntax problem; surfaced on the LCD as "Ma ERROR". */
class CalcError(message: String) : Exception(message)

/**
 * Tokenizer + recursive-descent evaluator for the calculator entry line.
 *
 * The strings handed in here use the exact display glyphs the keypad inserts
 * (× ÷ − √ π ², the superscript "⁻¹" for inverse trig, "Ans", "M", "E" for the
 * EXP key, etc.), so the same text is both shown on the LCD and evaluated.
 */
object Evaluator {

    fun evaluate(expression: String, mode: AngleMode, ans: Double, vars: MutableMap<Char, Double>): Double {
        val tokens = tokenize(expression)
        val parser = Parser(tokens, mode, ans, vars)
        val value = parser.parse()
        if (value.isNaN() || value.isInfinite()) throw CalcError("domain")
        return value
    }

    // region Tokens
    private sealed interface Token {
        data class Num(val value: Double) : Token
        data class Fn(val name: String) : Token
        data class Const(val name: String) : Token
        object Plus : Token
        object Minus : Token
        object Times : Token
        object Divide : Token
        object LParen : Token
        object RParen : Token
        object Caret : Token
        object Square : Token
        object Factorial : Token
        object Sqrt : Token
        object Root : Token          // ˣ√  (index-th root)
        object Reciprocal : Token    // ⁻¹ (1/x)
        object Percent : Token       // %
        object Comma : Token         // , (Pol/Rec argument separator)
        object Perm : Token          // nPr
        object Comb : Token          // nCr
        object Deg : Token           // ° (sexagesimal degrees)
        object Min : Token           // ′ (arc-minutes)
        object Sec : Token           // ″ (arc-seconds)
        data class Var(val name: Char) : Token
        object Store : Token
    }

    private const val INV = "\u207B\u00B9" // superscript "-1"

    private fun tokenize(src: String): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c == ' ' -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < src.length && (src[i].isDigit() || src[i] == '.')) i++
                    // Scientific "E" exponent produced by the EXP key.
                    if (i < src.length && src[i] == 'E') {
                        i++
                        if (i < src.length && (src[i] == '+' || src[i] == '-' || src[i] == '\u2212')) i++
                        while (i < src.length && src[i].isDigit()) i++
                    }
                    val text = src.substring(start, i)
                        .replace('\u2212', '-')
                        .replace('E', 'e')
                    val value = text.toDoubleOrNull() ?: throw CalcError("num")
                    tokens += Token.Num(value)
                }
                src.startsWith("sinh$INV", i) -> { tokens += Token.Fn("asinh"); i += 4 + INV.length }
                src.startsWith("cosh$INV", i) -> { tokens += Token.Fn("acosh"); i += 4 + INV.length }
                src.startsWith("tanh$INV", i) -> { tokens += Token.Fn("atanh"); i += 4 + INV.length }
                src.startsWith("sinh", i) -> { tokens += Token.Fn("sinh"); i += 4 }
                src.startsWith("cosh", i) -> { tokens += Token.Fn("cosh"); i += 4 }
                src.startsWith("tanh", i) -> { tokens += Token.Fn("tanh"); i += 4 }
                src.startsWith("sin$INV", i) -> { tokens += Token.Fn("asin"); i += 3 + INV.length }
                src.startsWith("cos$INV", i) -> { tokens += Token.Fn("acos"); i += 3 + INV.length }
                src.startsWith("tan$INV", i) -> { tokens += Token.Fn("atan"); i += 3 + INV.length }
                src.startsWith("sin", i) -> { tokens += Token.Fn("sin"); i += 3 }
                src.startsWith("cos", i) -> { tokens += Token.Fn("cos"); i += 3 }
                src.startsWith("tan", i) -> { tokens += Token.Fn("tan"); i += 3 }
                src.startsWith("log", i) -> { tokens += Token.Fn("log"); i += 3 }
                src.startsWith("ln", i) -> { tokens += Token.Fn("ln"); i += 2 }
                src.startsWith("Abs", i) -> { tokens += Token.Fn("abs"); i += 3 }
                src.startsWith("Int", i) -> { tokens += Token.Fn("int"); i += 3 }
                src.startsWith("Frac", i) -> { tokens += Token.Fn("frac"); i += 4 }
                src.startsWith("Pol", i) -> { tokens += Token.Fn("pol"); i += 3 }
                src.startsWith("Rec", i) -> { tokens += Token.Fn("rec"); i += 3 }
                src.startsWith("Ran#", i) -> { tokens += Token.Const("ran"); i += 4 }
                src.startsWith("nPr", i) -> { tokens += Token.Perm; i += 3 }
                src.startsWith("nCr", i) -> { tokens += Token.Comb; i += 3 }
                src.startsWith("Ans", i) -> { tokens += Token.Const("ans"); i += 3 }
                c in 'A'..'Z' -> { tokens += Token.Var(c); i++ }
                c == '\u2192' -> { tokens += Token.Store; i++ } // → (store)
                c == '\u03C0' -> { tokens += Token.Const("pi"); i++ } // π
                c == 'e' -> { tokens += Token.Const("e"); i++ }
                c == '+' -> { tokens += Token.Plus; i++ }
                c == '-' || c == '\u2212' -> { tokens += Token.Minus; i++ }
                c == '*' || c == '\u00D7' -> { tokens += Token.Times; i++ }
                c == '/' || c == '\u00F7' -> { tokens += Token.Divide; i++ }
                c == '(' -> { tokens += Token.LParen; i++ }
                c == ')' -> { tokens += Token.RParen; i++ }
                c == '^' -> { tokens += Token.Caret; i++ }
                c == '\u00B2' -> { tokens += Token.Square; i++ } // ²
                c == '!' -> { tokens += Token.Factorial; i++ }
                src.startsWith("\u02E3\u221A", i) -> { tokens += Token.Root; i += 2 } // ˣ√
                src.startsWith("\u207B\u00B9", i) -> { tokens += Token.Reciprocal; i += 2 } // ⁻¹
                src.startsWith("\u00B3\u221A", i) -> { tokens += Token.Fn("cbrt"); i += 2 } // ³√ cube root
                c == '\u221A' -> { tokens += Token.Sqrt; i++ } // √
                c == '%' -> { tokens += Token.Percent; i++ }
                c == ',' -> { tokens += Token.Comma; i++ }
                c == '\u00B0' -> { tokens += Token.Deg; i++ } // °
                c == '\u2032' -> { tokens += Token.Min; i++ } // ′
                c == '\u2033' -> { tokens += Token.Sec; i++ } // ″
                else -> throw CalcError("char")
            }
        }
        return tokens
    }
    // endregion

    // region Parser
    private class Parser(
        private val tokens: List<Token>,
        private val mode: AngleMode,
        private val ans: Double,
        private val vars: MutableMap<Char, Double>
    ) {
        private var pos = 0

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun next(): Token = tokens[pos++]

        fun parse(): Double {
            if (tokens.isEmpty()) throw CalcError("empty")
            val value = additive()
            // A trailing "→ VAR" chain stores the value into one or more variables.
            while (peek() == Token.Store) {
                next()
                val target = next()
                if (target !is Token.Var) throw CalcError("store")
                vars[target.name] = value
            }
            if (pos != tokens.size) throw CalcError("trailing")
            return value
        }

        private fun additive(): Double {
            var acc = permutation()
            while (true) {
                when (peek()) {
                    Token.Plus -> { next(); acc += permutation() }
                    Token.Minus -> { next(); acc -= permutation() }
                    else -> return acc
                }
            }
        }

        // nPr / nCr bind tighter than +− but looser than ×÷, like the hardware.
        private fun permutation(): Double {
            var acc = multiplicative()
            while (true) {
                when (peek()) {
                    Token.Perm -> { next(); acc = nPr(acc, multiplicative()) }
                    Token.Comb -> { next(); acc = nCr(acc, multiplicative()) }
                    else -> return acc
                }
            }
        }

        private fun multiplicative(): Double {
            var acc = unary()
            while (true) {
                when (peek()) {
                    Token.Times -> { next(); acc *= unary() }
                    Token.Divide -> {
                        next()
                        val d = unary()
                        if (d == 0.0) throw CalcError("div0")
                        acc /= d
                    }
                    // Implicit multiplication: 2π, 2(3), )(, 2sin(…), 2X
                    is Token.Num, is Token.Const, is Token.Var, is Token.Fn,
                    Token.LParen, Token.Sqrt -> acc *= unary()
                    else -> return acc
                }
            }
        }

        private fun unary(): Double = when (peek()) {
            Token.Minus -> { next(); -unary() }
            Token.Plus -> { next(); unary() }
            else -> power()
        }

        private fun power(): Double {
            val base = postfix()
            return when (peek()) {
                Token.Caret -> { next(); base.pow(unary()) } // right associative
                // index ˣ√ radicand  =  radicand^(1/index)
                Token.Root -> { next(); val y = unary(); if (base == 0.0) throw CalcError("root"); y.pow(1.0 / base) }
                else -> base
            }
        }

        private fun postfix(): Double {
            var value = atom()
            while (true) {
                when (peek()) {
                    Token.Square -> { next(); value *= value }
                    Token.Factorial -> { next(); value = factorial(value) }
                    Token.Reciprocal -> { next(); if (value == 0.0) throw CalcError("div0"); value = 1.0 / value }
                    Token.Percent -> { next(); value /= 100.0 }
                    Token.Deg -> { next(); value = readDms(value) }
                    else -> return value
                }
            }
        }

        /** Combines a "D° [M′ [S″]]" sexagesimal entry into decimal degrees. */
        private fun readDms(degrees: Double): Double {
            var total = degrees
            val m = peek()
            if (m is Token.Num && tokens.getOrNull(pos + 1) == Token.Min) {
                next(); next()
                total += m.value / 60.0
                val s = peek()
                if (s is Token.Num && tokens.getOrNull(pos + 1) == Token.Sec) {
                    next(); next()
                    total += s.value / 3600.0
                }
            }
            return total
        }

        private fun atom(): Double = when (val t = peek()) {
            is Token.Num -> { next(); t.value }
            is Token.Var -> { next(); vars[t.name] ?: 0.0 }
            is Token.Const -> { next(); constant(t.name) }
            Token.LParen -> {
                next()
                val v = additive()
                if (peek() == Token.RParen) next() // closing paren optional, like the FX-7000G
                v
            }
            Token.Sqrt -> { next(); val v = unary(); if (v < 0) throw CalcError("sqrt"); sqrt(v) }
            is Token.Fn -> {
                next()
                if (t.name == "pol" || t.name == "rec") twoArgFn(t.name) else applyFn(t.name, unary())
            }
            else -> throw CalcError("expected value")
        }

        /** Pol(x,y) → r (θ→J) and Rec(r,θ) → x (y→J); both also mirror into I. */
        private fun twoArgFn(name: String): Double {
            if (peek() == Token.LParen) next()
            val a = additive()
            if (peek() != Token.Comma) throw CalcError("args")
            next()
            val b = additive()
            if (peek() == Token.RParen) next()
            return if (name == "pol") {
                val r = hypot(a, b)
                vars['I'] = r
                vars['J'] = fromRadians(atan2(b, a))
                r
            } else {
                val x = a * cos(toRadians(b))
                val y = a * sin(toRadians(b))
                vars['I'] = x
                vars['J'] = y
                x
            }
        }

        private fun constant(name: String): Double = when (name) {
            "pi" -> PI
            "e" -> E
            "ans" -> ans
            "ran" -> Math.random()
            else -> throw CalcError("const")
        }

        private fun applyFn(name: String, arg: Double): Double = when (name) {
            "sin" -> sin(toRadians(arg))
            "cos" -> cos(toRadians(arg))
            "tan" -> tan(toRadians(arg))
            "asin" -> { if (arg < -1 || arg > 1) throw CalcError("domain"); fromRadians(asin(arg)) }
            "acos" -> { if (arg < -1 || arg > 1) throw CalcError("domain"); fromRadians(acos(arg)) }
            "atan" -> fromRadians(atan(arg))
            "sinh" -> sinh(arg)
            "cosh" -> cosh(arg)
            "tanh" -> tanh(arg)
            "asinh" -> asinh(arg)
            "acosh" -> { if (arg < 1) throw CalcError("domain"); acosh(arg) }
            "atanh" -> { if (arg <= -1 || arg >= 1) throw CalcError("domain"); atanh(arg) }
            "log" -> { if (arg <= 0) throw CalcError("domain"); log10(arg) }
            "ln" -> { if (arg <= 0) throw CalcError("domain"); ln(arg) }
            "abs" -> abs(arg)
            "int" -> truncate(arg)
            "frac" -> arg - truncate(arg)
            "cbrt" -> Math.cbrt(arg)
            else -> throw CalcError("fn")
        }

        private fun toRadians(v: Double): Double = when (mode) {
            AngleMode.DEG -> v * PI / 180.0
            AngleMode.RAD -> v
            AngleMode.GRA -> v * PI / 200.0
        }

        private fun fromRadians(v: Double): Double = when (mode) {
            AngleMode.DEG -> v * 180.0 / PI
            AngleMode.RAD -> v
            AngleMode.GRA -> v * 200.0 / PI
        }

        private fun factorial(v: Double): Double {
            val n = v.roundToLong()
            if (n < 0 || abs(v - n) > 1e-9) throw CalcError("factorial")
            if (n > 69) throw CalcError("overflow")
            var acc = 1.0
            for (k in 2..n) acc *= k
            return acc
        }

        private fun nPr(n: Double, r: Double): Double {
            val nn = n.roundToLong(); val rr = r.roundToLong()
            if (nn < 0 || rr < 0 || rr > nn ||
                abs(n - nn) > 1e-9 || abs(r - rr) > 1e-9
            ) throw CalcError("domain")
            var acc = 1.0
            for (k in 0 until rr) acc *= (nn - k)
            return acc
        }

        private fun nCr(n: Double, r: Double): Double {
            val rr = r.roundToLong()
            val perm = nPr(n, r)
            var f = 1.0
            for (k in 2..rr) f *= k
            return perm / f
        }
    }
    // endregion

    // region BASE-n (integer) evaluation
    /**
     * Integer evaluator for the FX-7000G's BASE-n mode. Numbers are read in the
     * given radix (2/8/10/16, hex digits A-F upper-case), and support the four
     * arithmetic operators plus the bitwise logical words and / or / xor / Not.
     */
    fun evaluateBase(expression: String, base: Int, ans: Long = 0L): Long {
        val tokens = tokenizeBase(expression, base, ans)
        if (tokens.isEmpty()) throw CalcError("empty")
        val parser = BaseParser(tokens)
        val raw = parser.parse()
        // The fx-7000G stores BASE-n values as fixed-width signed integers:
        // 16-bit for binary, 32-bit for octal/decimal/hex. Wrapping here makes
        // negatives round-trip as two's complement (e.g. -1 → 1111111111111111).
        return if (base == 2) raw.toShort().toLong() else raw.toInt().toLong()
    }

    private sealed interface BToken {
        data class Num(val value: Long) : BToken
        object Plus : BToken
        object Minus : BToken
        object Times : BToken
        object Divide : BToken
        object And : BToken
        object Or : BToken
        object Xor : BToken
        object Not : BToken
        object LParen : BToken
        object RParen : BToken
    }

    private fun baseDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }

    private fun tokenizeBase(src: String, base: Int, ans: Long): List<BToken> {
        val out = ArrayList<BToken>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            val digit = baseDigit(c)
            when {
                c == ' ' -> i++
                // "Ans" must be matched before the digit branch, since 'A' is a hex digit.
                src.startsWith("Ans", i) -> { out += BToken.Num(ans); i += 3 }
                digit in 0 until base -> {
                    val start = i
                    while (i < src.length && baseDigit(src[i]).let { it in 0 until base }) i++
                    val value = src.substring(start, i).toLongOrNull(base) ?: throw CalcError("num")
                    out += BToken.Num(value)
                }
                src.startsWith("and", i) -> { out += BToken.And; i += 3 }
                src.startsWith("xor", i) -> { out += BToken.Xor; i += 3 }
                src.startsWith("or", i) -> { out += BToken.Or; i += 2 }
                src.startsWith("Not", i) -> { out += BToken.Not; i += 3 }
                c == '+' -> { out += BToken.Plus; i++ }
                c == '-' || c == '\u2212' -> { out += BToken.Minus; i++ }
                c == '*' || c == '\u00D7' -> { out += BToken.Times; i++ }
                c == '/' || c == '\u00F7' -> { out += BToken.Divide; i++ }
                c == '(' -> { out += BToken.LParen; i++ }
                c == ')' -> { out += BToken.RParen; i++ }
                else -> throw CalcError("char")
            }
        }
        return out
    }

    /** Precedence (loosest → tightest): or < xor < and < +,- < *,/ < unary(Not,-). */
    private class BaseParser(private val tokens: List<BToken>) {
        private var pos = 0
        private fun peek(): BToken? = tokens.getOrNull(pos)
        private fun next(): BToken = tokens[pos++]

        fun parse(): Long {
            val value = orExpr()
            if (pos != tokens.size) throw CalcError("trailing")
            return value
        }

        private fun orExpr(): Long {
            var acc = xorExpr()
            while (peek() == BToken.Or) { next(); acc = acc or xorExpr() }
            return acc
        }

        private fun xorExpr(): Long {
            var acc = andExpr()
            while (peek() == BToken.Xor) { next(); acc = acc xor andExpr() }
            return acc
        }

        private fun andExpr(): Long {
            var acc = additive()
            while (peek() == BToken.And) { next(); acc = acc and additive() }
            return acc
        }

        private fun additive(): Long {
            var acc = multiplicative()
            while (true) {
                when (peek()) {
                    BToken.Plus -> { next(); acc += multiplicative() }
                    BToken.Minus -> { next(); acc -= multiplicative() }
                    else -> return acc
                }
            }
        }

        private fun multiplicative(): Long {
            var acc = unary()
            while (true) {
                when (peek()) {
                    BToken.Times -> { next(); acc *= unary() }
                    BToken.Divide -> {
                        next()
                        val d = unary()
                        if (d == 0L) throw CalcError("div0")
                        acc /= d
                    }
                    else -> return acc
                }
            }
        }

        private fun unary(): Long = when (peek()) {
            BToken.Not -> { next(); unary().inv() }
            BToken.Minus -> { next(); -unary() }
            BToken.Plus -> { next(); unary() }
            else -> atom()
        }

        private fun atom(): Long = when (val t = peek()) {
            is BToken.Num -> { next(); t.value }
            BToken.LParen -> {
                next()
                val v = orExpr()
                if (peek() == BToken.RParen) next() // closing paren optional, like the hardware
                v
            }
            else -> throw CalcError("expected value")
        }
    }
    // endregion
}
