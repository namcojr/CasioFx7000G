package com.retro.fx7000g.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E
import kotlin.math.PI

/**
 * Unit tests for [Evaluator] – the tokenizer + recursive-descent expression engine.
 *
 * Expressions use the exact display glyphs the keypad inserts (× ÷ − √ π ² ⁻¹ ˣ√ …),
 * so the constants below reference those Unicode code points explicitly.
 */
class EvaluatorTest {

    // Glyphs used by the keypad / evaluator.
    private val TIMES = "\u00D7"
    private val DIVIDE = "\u00F7"
    private val MINUS = "\u2212"
    private val SQRT = "\u221A"
    private val PI_SIGN = "\u03C0"
    private val SQUARE = "\u00B2"
    private val INV = "\u207B\u00B9"      // ⁻¹ reciprocal / inverse-trig suffix
    private val ROOT = "\u02E3\u221A"      // ˣ√ index-th root
    private val DEG = "\u00B0"
    private val MIN = "\u2032"
    private val SEC = "\u2033"
    private val STORE = "\u2192"

    private val DELTA = 1e-9

    private fun eval(
        expr: String,
        mode: AngleMode = AngleMode.DEG,
        ans: Double = 0.0,
        vars: MutableMap<Char, Double> = mutableMapOf()
    ): Double = Evaluator.evaluate(expr, mode, ans, vars)

    // region Basic arithmetic & precedence
    @Test
    fun addition() = assertEquals(5.0, eval("2+3"), DELTA)

    @Test
    fun subtractionWithUnicodeMinus() = assertEquals(1.0, eval("3${MINUS}2"), DELTA)

    @Test
    fun multiplicationWithUnicodeTimes() = assertEquals(12.0, eval("4${TIMES}3"), DELTA)

    @Test
    fun divisionWithUnicodeDivide() = assertEquals(4.0, eval("8${DIVIDE}2"), DELTA)

    @Test
    fun operatorPrecedence() = assertEquals(14.0, eval("2+3${TIMES}4"), DELTA)

    @Test
    fun parenthesesOverridePrecedence() = assertEquals(20.0, eval("(2+3)${TIMES}4"), DELTA)

    @Test
    fun optionalClosingParen() = assertEquals(14.0, eval("(2+3${TIMES}4"), DELTA)

    @Test
    fun negativeNumber() = assertEquals(-5.0, eval("${MINUS}5"), DELTA)

    @Test
    fun doubleNegative() = assertEquals(5.0, eval("${MINUS}${MINUS}5"), DELTA)

    @Test
    fun decimalNumbers() = assertEquals(3.5, eval("1.25+2.25"), DELTA)

    @Test
    fun scientificExponentEntry() = assertEquals(1500.0, eval("1.5E3"), DELTA)
    // endregion

    // region Implicit multiplication
    @Test
    fun implicitMultiplyNumberAndConstant() = assertEquals(2 * PI, eval("2$PI_SIGN"), DELTA)

    @Test
    fun implicitMultiplyNumberAndParen() = assertEquals(10.0, eval("2(3+2)"), DELTA)

    @Test
    fun implicitMultiplyParenAndParen() = assertEquals(6.0, eval("(2)(3)"), DELTA)
    // endregion

    // region Powers, roots, postfix
    @Test
    fun caretPower() = assertEquals(1024.0, eval("2^10"), DELTA)

    @Test
    fun caretIsRightAssociative() = assertEquals(512.0, eval("2^3^2"), DELTA)

    @Test
    fun square() = assertEquals(9.0, eval("3$SQUARE"), DELTA)

    @Test
    fun squareRoot() = assertEquals(3.0, eval("${SQRT}9"), DELTA)

    @Test
    fun indexRoot() = assertEquals(2.0, eval("3${ROOT}8"), DELTA)

    @Test
    fun reciprocal() = assertEquals(0.25, eval("4$INV"), DELTA)

    @Test
    fun factorial() = assertEquals(120.0, eval("5!"), DELTA)

    @Test
    fun percent() = assertEquals(0.5, eval("50%"), DELTA)
    // endregion

    // region Combinatorics
    @Test
    fun permutation() = assertEquals(20.0, eval("5nPr2"), DELTA)

    @Test
    fun combination() = assertEquals(10.0, eval("5nCr2"), DELTA)
    // endregion

    // region Trigonometry (angle modes)
    @Test
    fun sinDegrees() = assertEquals(0.5, eval("sin30", AngleMode.DEG), 1e-9)

    @Test
    fun cosDegrees() = assertEquals(0.5, eval("cos60", AngleMode.DEG), 1e-9)

    @Test
    fun tanDegrees() = assertEquals(1.0, eval("tan45", AngleMode.DEG), 1e-9)

    @Test
    fun sinRadians() = assertEquals(1.0, eval("sin($PI_SIGN${DIVIDE}2)", AngleMode.RAD), 1e-9)

    @Test
    fun sinGradians() = assertEquals(1.0, eval("sin100", AngleMode.GRA), 1e-9)

    @Test
    fun inverseSinDegrees() = assertEquals(30.0, eval("sin${INV}0.5", AngleMode.DEG), 1e-9)

    @Test
    fun inverseCosDegrees() = assertEquals(60.0, eval("cos${INV}0.5", AngleMode.DEG), 1e-9)

    @Test
    fun inverseTanDegrees() = assertEquals(45.0, eval("tan${INV}1", AngleMode.DEG), 1e-9)
    // endregion

    // region Hyperbolic
    @Test
    fun sinh() = assertEquals(kotlin.math.sinh(1.0), eval("sinh1"), DELTA)

    @Test
    fun cosh() = assertEquals(kotlin.math.cosh(1.0), eval("cosh1"), DELTA)

    @Test
    fun tanh() = assertEquals(kotlin.math.tanh(1.0), eval("tanh1"), DELTA)

    @Test
    fun inverseSinh() = assertEquals(1.0, eval("sinh${INV}${eval("sinh1")}"), 1e-6)
    // endregion

    // region Logarithms
    @Test
    fun log10() = assertEquals(2.0, eval("log100"), DELTA)

    @Test
    fun naturalLog() = assertEquals(1.0, eval("ln($PI_SIGN${DIVIDE}$PI_SIGN${TIMES}e)"), DELTA)

    @Test
    fun lnOfE() = assertEquals(1.0, eval("lne"), DELTA)
    // endregion

    // region Constants
    @Test
    fun piConstant() = assertEquals(PI, eval(PI_SIGN), DELTA)

    @Test
    fun eConstant() = assertEquals(E, eval("e"), DELTA)

    @Test
    fun ansConstant() = assertEquals(43.0, eval("Ans+1", ans = 42.0), DELTA)
    // endregion

    // region Functions Abs/Int/Frac
    @Test
    fun absValue() = assertEquals(7.0, eval("Abs(${MINUS}7)"), DELTA)

    @Test
    fun intPart() = assertEquals(3.0, eval("Int3.9"), DELTA)

    @Test
    fun fracPart() = assertEquals(0.9, eval("Frac3.9"), 1e-9)
    // endregion

    // region Variables & store
    @Test
    fun storeIntoVariable() {
        val vars = mutableMapOf<Char, Double>()
        assertEquals(5.0, eval("5${STORE}A", vars = vars), DELTA)
        assertEquals(5.0, vars['A'])
    }

    @Test
    fun readStoredVariable() {
        val vars = mutableMapOf('A' to 6.0)
        assertEquals(12.0, eval("A${TIMES}2", vars = vars), DELTA)
    }

    @Test
    fun unsetVariableIsZero() = assertEquals(0.0, eval("B", vars = mutableMapOf()), DELTA)
    // endregion

    // region DMS (sexagesimal)
    @Test
    fun degreesMinutes() = assertEquals(1.5, eval("1${DEG}30$MIN"), 1e-9)

    @Test
    fun degreesMinutesSeconds() = assertEquals(1.0 + 1.0 / 60.0 + 30.0 / 3600.0, eval("1${DEG}1${MIN}30$SEC"), 1e-9)
    // endregion

    // region Pol / Rec
    @Test
    fun polReturnsMagnitude() = assertEquals(5.0, eval("Pol(3,4)"), 1e-9)

    @Test
    fun polStoresAngleInJ() {
        val vars = mutableMapOf<Char, Double>()
        eval("Pol(1,1)", AngleMode.DEG, vars = vars)
        assertEquals(45.0, vars['J']!!, 1e-9)
    }

    @Test
    fun recReturnsX() = assertEquals(0.0, eval("Rec(1,90)", AngleMode.DEG), 1e-9)
    // endregion

    // region Error handling
    @Test(expected = CalcError::class)
    fun divisionByZeroThrows() {
        eval("1${DIVIDE}0")
    }

    @Test(expected = CalcError::class)
    fun sqrtOfNegativeThrows() {
        eval("${SQRT}${MINUS}1")
    }

    @Test(expected = CalcError::class)
    fun asinOutOfDomainThrows() {
        eval("sin${INV}2")
    }

    @Test(expected = CalcError::class)
    fun logOfNonPositiveThrows() {
        eval("log0")
    }

    @Test(expected = CalcError::class)
    fun emptyExpressionThrows() {
        eval("")
    }

    @Test(expected = CalcError::class)
    fun trailingTokenThrows() {
        eval("2)3")
    }

    @Test(expected = CalcError::class)
    fun unknownCharacterThrows() {
        eval("2@3")
    }

    @Test(expected = CalcError::class)
    fun negativeFactorialThrows() {
        eval("(${MINUS}1)!")
    }
    // endregion

    // region BASE-n evaluation
    @Test
    fun binaryNumber() = assertEquals(10L, Evaluator.evaluateBase("1010", 2))

    @Test
    fun octalNumber() = assertEquals(8L, Evaluator.evaluateBase("10", 8))

    @Test
    fun hexNumber() = assertEquals(255L, Evaluator.evaluateBase("FF", 16))

    @Test
    fun decimalAddition() = assertEquals(30L, Evaluator.evaluateBase("12+18", 10))

    @Test
    fun baseSubtraction() = assertEquals(5L, Evaluator.evaluateBase("12-7", 10))

    @Test
    fun baseMultiplication() = assertEquals(42L, Evaluator.evaluateBase("6*7", 10))

    @Test
    fun baseIntegerDivision() = assertEquals(3L, Evaluator.evaluateBase("10/3", 10))

    @Test
    fun bitwiseAnd() = assertEquals(8L, Evaluator.evaluateBase("12 and 10", 10))

    @Test
    fun bitwiseOr() = assertEquals(14L, Evaluator.evaluateBase("12 or 2", 10))

    @Test
    fun bitwiseXor() = assertEquals(5L, Evaluator.evaluateBase("6 xor 3", 10))

    @Test
    fun bitwiseNot() = assertEquals(-1L, Evaluator.evaluateBase("Not 0", 10))

    @Test
    fun basePrecedenceAndBeforeOr() =
        assertEquals(5L, Evaluator.evaluateBase("4 or 1 and 3", 10))

    @Test
    fun baseAnsSubstitution() = assertEquals(16L, Evaluator.evaluateBase("Ans+1", 16, ans = 15L))

    @Test
    fun baseParentheses() = assertEquals(20L, Evaluator.evaluateBase("(2+3)*4", 10))

    @Test(expected = CalcError::class)
    fun baseDivisionByZeroThrows() {
        Evaluator.evaluateBase("5/0", 10)
    }

    @Test(expected = CalcError::class)
    fun baseEmptyThrows() {
        Evaluator.evaluateBase("", 10)
    }

    @Test
    fun distinctModesGiveDistinctResults() {
        // sin(90) differs by angle interpretation.
        assertNotEquals(eval("sin90", AngleMode.DEG), eval("sin90", AngleMode.RAD), DELTA)
    }

    @Test
    fun infiniteResultThrows() {
        var threw = false
        try {
            eval("1E300${TIMES}1E300")
        } catch (e: CalcError) {
            threw = true
        }
        assertTrue(threw)
    }
    // endregion
}
