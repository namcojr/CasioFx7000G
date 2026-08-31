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

    /** Selectable output style, mirroring the FX-7000G's Norm / Fix / Sci modes. */
    sealed interface DisplayFormat {
        object Norm : DisplayFormat
        /** Fixed number of decimal places (0–9). */
        data class Fix(val decimals: Int) : DisplayFormat
        /** Scientific notation with a fixed number of significant digits (1–10). */
        data class Sci(val digits: Int) : DisplayFormat
    }

    fun format(value: Double, format: DisplayFormat = DisplayFormat.Norm): String {
        if (value.isNaN() || value.isInfinite()) throw CalcError("range")
        if (abs(value) >= 1e100) throw CalcError("overflow")
        return when (format) {
            is DisplayFormat.Fix -> fixed(value, format.decimals)
            is DisplayFormat.Sci -> scientific(value, format.digits)
            DisplayFormat.Norm -> format(value)
        }
    }

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

    /** Fix mode: exactly [decimals] digits after the point. */
    private fun fixed(value: Double, decimals: Int): String {
        val d = decimals.coerceIn(0, 9)
        val bd = BigDecimal(value).setScale(d, RoundingMode.HALF_UP)
        // Avoid an ugly "-0.00".
        val s = bd.toPlainString()
        return if (bd.signum() == 0 && s.startsWith("-")) s.substring(1) else s
    }

    /** Sci mode: scientific notation with [digits] significant figures. */
    private fun scientific(value: Double, digits: Int): String {
        if (value == 0.0) {
            val frac = (digits.coerceIn(1, 10) - 1)
            return if (frac == 0) "0E0" else "0." + "0".repeat(frac) + "E0"
        }
        val n = digits.coerceIn(1, 10)
        val rounded = BigDecimal(value).round(MathContext(n, RoundingMode.HALF_UP))
        val negative = rounded.signum() < 0
        var mantissa = rounded.abs()
        var exp = 0
        while (mantissa >= BigDecimal.TEN) { mantissa = mantissa.movePointLeft(1); exp++ }
        while (mantissa > BigDecimal.ZERO && mantissa < BigDecimal.ONE) { mantissa = mantissa.movePointRight(1); exp-- }
        val m = mantissa.setScale(n - 1, RoundingMode.HALF_UP).toPlainString()
        val sign = if (negative) "-" else ""
        return "$sign${m}E$exp"
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
