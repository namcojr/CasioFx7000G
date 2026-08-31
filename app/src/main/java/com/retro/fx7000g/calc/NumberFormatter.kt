package com.retro.fx7000g.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Formats a result the way the FX-7000G does: a 10 significant-digit mantissa,
 * switching to exponential notation for very large / very small magnitudes.
 */
object NumberFormatter {

    private const val SIG_DIGITS = 10

    fun format(value: Double): String {
        if (value == 0.0) return "0"
        if (value.isNaN() || value.isInfinite()) throw CalcError("range")

        val abs = abs(value)
        // The real calculator overflows around 1e100.
        if (abs >= 1e100) throw CalcError("overflow")

        val rounded = BigDecimal(value).round(MathContext(SIG_DIGITS, RoundingMode.HALF_UP))

        return if (abs >= 1e10 || abs < 1e-9) {
            exponential(rounded)
        } else {
            plain(rounded)
        }
    }

    /**
     * Formats an integer value in the given radix (16/2/8/10) for BASE-n mode.
     * Hex digits are upper-case; the active base is shown on the LCD status line
     * rather than as a suffix, matching the original hardware.
     */
    fun formatBase(value: Double, base: Int): String {
        if (value.isNaN() || value.isInfinite()) throw CalcError("range")
        if (abs(value) >= 9.2e18) throw CalcError("overflow")
        val n = value.toLong() // BASE-n operates on the integer part only
        return formatBase(n, base)
    }

    fun formatBase(n: Long, base: Int): String = when (base) {
        16 -> n.toString(16).uppercase()
        2 -> n.toString(2)
        8 -> n.toString(8)
        else -> n.toString(10)
    }

    private fun plain(bd: BigDecimal): String {
        var s = bd.toPlainString()
        if (s.contains('.')) {
            s = s.trimEnd('0').trimEnd('.')
        }
        return s
    }

    private fun exponential(bd: BigDecimal): String {
        val negative = bd.signum() < 0
        var mantissa = bd.abs()
        var exp = 0
        while (mantissa >= BigDecimal.TEN) {
            mantissa = mantissa.movePointLeft(1); exp++
        }
        while (mantissa > BigDecimal.ZERO && mantissa < BigDecimal.ONE) {
            mantissa = mantissa.movePointRight(1); exp--
        }
        mantissa = mantissa.round(MathContext(SIG_DIGITS, RoundingMode.HALF_UP))
        var m = mantissa.toPlainString()
        if (m.contains('.')) m = m.trimEnd('0').trimEnd('.')
        val sign = if (negative) "-" else ""
        return "${sign}${m}E$exp"
    }
}
