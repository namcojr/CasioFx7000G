package com.retro.fx7000g.calc

import com.retro.fx7000g.calc.NumberFormatter.DisplayFormat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [NumberFormatter] – Norm / Fix / Sci and BASE-n output formatting.
 */
class NumberFormatterTest {

    // region Norm mode
    @Test
    fun zeroFormatsAsPlainZero() = assertEquals("0", NumberFormatter.format(0.0))

    @Test
    fun smallIntegerHasNoDecimalPoint() = assertEquals("5", NumberFormatter.format(5.0))

    @Test
    fun negativeInteger() = assertEquals("-3", NumberFormatter.format(-3.0))

    @Test
    fun decimalTrimsTrailingZeros() = assertEquals("2.5", NumberFormatter.format(2.5))

    @Test
    fun tenSignificantDigits() = assertEquals("0.3333333333", NumberFormatter.format(1.0 / 3.0))

    @Test
    fun midRangeValueIsPlain() = assertEquals("123456.789", NumberFormatter.format(123456.789))

    @Test
    fun largeMagnitudeSwitchesToExponential() = assertEquals("1E10", NumberFormatter.format(1e10))

    @Test
    fun negativeLargeMagnitudeExponential() = assertEquals("-1E10", NumberFormatter.format(-1e10))

    @Test
    fun verySmallMagnitudeExponential() = assertEquals("1E-10", NumberFormatter.format(1e-10))

    @Test
    fun boundarySmallStaysPlain() = assertEquals("0.000000001", NumberFormatter.format(1e-9))
    // endregion

    // region DisplayFormat overload
    @Test
    fun normViaOverloadMatchesDefault() =
        assertEquals(NumberFormatter.format(42.0), NumberFormatter.format(42.0, DisplayFormat.Norm))

    @Test
    fun fixModeTwoDecimals() =
        assertEquals("3.14", NumberFormatter.format(3.14159, DisplayFormat.Fix(2)))

    @Test
    fun fixModeRoundsHalfUp() =
        assertEquals("4", NumberFormatter.format(3.9, DisplayFormat.Fix(0)))

    @Test
    fun fixModeAvoidsNegativeZero() =
        assertEquals("0.00", NumberFormatter.format(-0.0001, DisplayFormat.Fix(2)))

    @Test
    fun fixModePadsDecimals() =
        assertEquals("2.50", NumberFormatter.format(2.5, DisplayFormat.Fix(2)))

    @Test
    fun sciModeThreeSignificantDigits() =
        assertEquals("1.23E4", NumberFormatter.format(12345.0, DisplayFormat.Sci(3)))

    @Test
    fun sciModeNegative() =
        assertEquals("-1.20E1", NumberFormatter.format(-12.0, DisplayFormat.Sci(3)))

    @Test
    fun sciModeZeroWithDigits() =
        assertEquals("0.00E0", NumberFormatter.format(0.0, DisplayFormat.Sci(3)))

    @Test
    fun sciModeZeroSingleDigit() =
        assertEquals("0E0", NumberFormatter.format(0.0, DisplayFormat.Sci(1)))
    // endregion

    // region BASE-n (Long)
    @Test
    fun hexFromLong() = assertEquals("FF", NumberFormatter.formatBase(255L, 16))

    @Test
    fun binaryFromLong() = assertEquals("1010", NumberFormatter.formatBase(10L, 2))

    @Test
    fun octalFromLong() = assertEquals("10", NumberFormatter.formatBase(8L, 8))

    @Test
    fun decimalFromLong() = assertEquals("42", NumberFormatter.formatBase(42L, 10))

    @Test
    fun hexIsUpperCase() = assertEquals("ABCDEF", NumberFormatter.formatBase(0xABCDEFL, 16))
    // endregion

    // region BASE-n (Double – integer part only)
    @Test
    fun hexFromDoubleUsesIntegerPart() = assertEquals("FF", NumberFormatter.formatBase(255.9, 16))

    @Test
    fun decimalFromDouble() = assertEquals("7", NumberFormatter.formatBase(7.0, 10))
    // endregion

    // region Error handling
    @Test(expected = CalcError::class)
    fun nanThrows() {
        NumberFormatter.format(Double.NaN)
    }

    @Test(expected = CalcError::class)
    fun infinityThrows() {
        NumberFormatter.format(Double.POSITIVE_INFINITY)
    }

    @Test(expected = CalcError::class)
    fun overflowThrows() {
        NumberFormatter.format(1e100)
    }

    @Test(expected = CalcError::class)
    fun baseOverflowThrows() {
        NumberFormatter.formatBase(1e19, 16)
    }
    // endregion
}
