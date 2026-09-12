package com.retro.fx7000g.calc

import com.retro.fx7000g.basic.ProgSubmode
import com.retro.fx7000g.ui.DisplayGlyphs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [CalculatorState] – the key-press reducer that drives the UI.
 *
 * These exercise the state holder directly (no Compose composition needed); the
 * `mutableStateOf`-backed properties read and write through the global snapshot.
 */
class CalculatorStateTest {
    private val TIMES = "\u00D7"
    private val DIVIDE = "\u00F7"

    private fun state() = CalculatorState()
    private fun ins(text: String) = CalcAction.Insert(text)

    /** Reads one RANGE editor line (0=XMIN..5=YSCL) without altering the window. */
    private fun rangeLine(s: CalculatorState, idx: Int): String {
        s.onAction(CalcAction.Range)
        val line = s.rangeLines!![idx]
        s.onAction(CalcAction.Clear)
        return line
    }

    /**
     * Counts lit pixels that lie clear of the two axes (and their tick bands),
     * i.e. actual curve dots. A blank or axis-hugging flat line scores ~0.
     */
    private fun offAxisPixels(buf: BooleanArray): Int {
        val cols = 96
        val rows = 64
        val rowCounts = IntArray(rows)
        val colCounts = IntArray(cols)
        for (r in 0 until rows) for (c in 0 until cols) if (buf[r * cols + c]) {
            rowCounts[r]++; colCounts[c]++
        }
        val axisRow = rowCounts.indices.maxByOrNull { rowCounts[it] } ?: -1
        val axisCol = colCounts.indices.maxByOrNull { colCounts[it] } ?: -1
        var count = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            if (!buf[r * cols + c]) continue
            if (kotlin.math.abs(r - axisRow) <= 1) continue
            if (kotlin.math.abs(c - axisCol) <= 1) continue
            count++
        }
        return count
    }


    // region Insertion & cursor
    @Test
    fun insertBuildsEntryAndAdvancesCursor() {
        val s = state()
        s.onAction(ins("12"))
        assertEquals("12", s.entry)
        assertEquals(2, s.cursor)
    }

    @Test
    fun insertAtCursorPosition() {
        val s = state()
        s.onAction(ins("12"))
        s.onAction(CalcAction.MoveLeft)
        s.onAction(ins("5"))
        assertEquals("152", s.entry)
        assertEquals(2, s.cursor)
    }

    @Test
    fun moveLeftClampsAtZero() {
        val s = state()
        s.onAction(ins("1"))
        s.onAction(CalcAction.MoveLeft)
        s.onAction(CalcAction.MoveLeft)
        assertEquals(0, s.cursor)
    }

    @Test
    fun moveRightClampsAtEnd() {
        val s = state()
        s.onAction(ins("1"))
        s.onAction(CalcAction.MoveRight)
        s.onAction(CalcAction.MoveRight)
        assertEquals(1, s.cursor)
    }

    @Test
    fun moveHomeJumpsToStartOfEntry() {
        val s = state()
        s.onAction(ins("12345"))
        s.onAction(CalcAction.MoveHome)
        assertEquals(0, s.cursor)
    }

    @Test
    fun moveEndJumpsToEndOfEntry() {
        val s = state()
        s.onAction(ins("12345"))
        s.onAction(CalcAction.MoveHome)
        assertEquals(0, s.cursor)
        s.onAction(CalcAction.MoveEnd)
        assertEquals(5, s.cursor)
    }

    @Test
    fun moveHomeAfterEvaluateReopensEntryForEditing() {
        val s = state()
        s.onAction(ins("1+2"))
        s.onAction(CalcAction.Evaluate)
        assertFalse(s.showCursor) // result is on screen, not the entry cursor

        // HOME makes the last entry editable again with the cursor at position 0.
        s.onAction(CalcAction.MoveHome)
        assertEquals(0, s.cursor)
        assertTrue(s.showCursor)

        // Inserting there prepends to the recalled expression.
        s.onAction(ins("9"))
        assertEquals("91+2", s.entry)
    }

    @Test
    fun moveEndAfterEvaluateReopensEntryForAppending() {
        val s = state()
        s.onAction(ins("1+2"))
        s.onAction(CalcAction.Evaluate)

        s.onAction(CalcAction.MoveEnd)
        assertEquals(3, s.cursor)
        assertTrue(s.showCursor)

        s.onAction(ins("+4"))
        assertEquals("1+2+4", s.entry)
    }

    @Test
    fun moveHomeAndEndClampOnEmptyEntry() {
        val s = state()
        s.onAction(CalcAction.MoveHome)
        assertEquals(0, s.cursor)
        s.onAction(CalcAction.MoveEnd)
        assertEquals(0, s.cursor)
    }
    // endregion

    // region Evaluation
    @Test
    fun evaluatesSimpleExpression() {
        val s = state()
        s.onAction(ins("1+2"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("3", s.result)
    }

    @Test
    fun evaluateOnBlankEntryDoesNothing() {
        val s = state()
        s.onAction(CalcAction.Evaluate)
        assertEquals("", s.result)
    }

    @Test
    fun digitAfterEvaluateStartsFresh() {
        val s = state()
        s.onAction(ins("1+2"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(ins("5"))
        assertEquals("5", s.entry)
    }

    @Test
    fun operatorAfterEvaluateChainsAns() {
        val s = state()
        s.onAction(ins("1+2"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(ins("+"))
        assertEquals("Ans+", s.entry)
    }

    @Test
    fun ansCarriesPreviousResult() {
        val s = state()
        s.onAction(ins("6"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(ins("$TIMES"))
        s.onAction(ins("7"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("42", s.result)
    }

    @Test
    fun invalidExpressionShowsError() {
        val s = state()
        s.onAction(ins("1$DIVIDE" + "0"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("Ma ERROR", s.result)
    }

    @Test
    fun toggleSignChangesTheCurrentEntry() {
        val s = state()
        s.onAction(ins("12"))
        s.onAction(CalcAction.ToggleSign)
        assertEquals("-12", s.entry)
        s.onAction(CalcAction.ToggleSign)
        assertEquals("12", s.entry)
    }

    @Test
    fun dmssFormatsCurrentValue() {
        val s = state()
        s.onAction(ins("1.5"))
        s.onAction(CalcAction.Dmss)
        assertEquals("1\u00B030\u20320\u2033", s.result)
    }

    @Test
    fun typingAfterErrorClearsIt() {
        val s = state()
        s.onAction(ins("1$DIVIDE" + "0"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(ins("5"))
        assertEquals("5", s.entry)
        assertEquals("", s.result)
    }
    // endregion

    // region Clear & delete
    @Test
    fun clearResetsEverything() {
        val s = state()
        s.onAction(ins("1+2"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(CalcAction.Clear)
        assertEquals("", s.entry)
        assertEquals("", s.result)
        assertEquals(0, s.cursor)
    }

    @Test
    fun deleteRemovesSingleCharacter() {
        val s = state()
        s.onAction(ins("12"))
        s.onAction(CalcAction.Delete)
        assertEquals("1", s.entry)
        assertEquals(1, s.cursor)
    }

    @Test
    fun deleteRemovesMultiCharTokenAtOnce() {
        val s = state()
        s.onAction(ins("sin("))
        s.onAction(CalcAction.Delete)
        assertEquals("", s.entry)
        assertEquals(0, s.cursor)
    }

    @Test
    fun deleteRemovesMissingTokensAtOnce() {
        val s = state()
        s.onAction(ins("HEX$("))
        s.onAction(CalcAction.Delete)
        assertEquals("", s.entry)
        assertEquals(0, s.cursor)
    }

    @Test
    fun progDeleteRemovesTokenAtOnce() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("0"))
        assertEquals(ProgSubmode.EDIT, s.progState?.submode)
        s.onAction(ins("STR$("))
        s.onAction(CalcAction.Delete)
        assertEquals("", s.progState?.editBuffer)
        assertEquals(0, s.progState?.editCursor)
    }

    @Test
    fun deleteAtStartDoesNothing() {
        val s = state()
        s.onAction(ins("1"))
        s.onAction(CalcAction.MoveLeft)
        s.onAction(CalcAction.Delete)
        assertEquals("1", s.entry)
    }
    // endregion

    // region Modifier prefixes (SHIFT / ALPHA / hyp)
    @Test
    fun toggleShiftSetsFlagAndIndicator() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        assertTrue(s.shift)
        assertEquals(DisplayGlyphs.SHIFT.toString(), s.indicator)
    }

    @Test
    fun shiftIsClearedAfterNextKey() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(ins("1"))
        assertFalse(s.shift)
    }

    @Test
    fun pressingAlphaTurnsShiftOff() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleAlpha)
        assertFalse(s.shift)
        assertTrue(s.alpha)
        assertEquals(DisplayGlyphs.ALPHA.toString(), s.indicator)
    }

    @Test
    fun pressingShiftTurnsAlphaOff() {
        val s = state()
        s.onAction(CalcAction.ToggleAlpha)
        s.onAction(CalcAction.ToggleShift)
        assertFalse(s.alpha)
        assertTrue(s.shift)
        assertEquals(DisplayGlyphs.SHIFT.toString(), s.indicator)
    }

    @Test
    fun hypToggles() {
        val s = state()
        s.onAction(CalcAction.ToggleHyp)
        assertTrue(s.hyp)
        assertEquals("h", s.indicator)
    }

    @Test
    fun doublePressShiftLatchesShift() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleShift)
        assertTrue(s.shift)
        assertTrue(s.shiftLock)
        assertEquals(DisplayGlyphs.SHIFT_LOCK.toString(), s.indicator)
    }

    @Test
    fun latchedShiftSurvivesKeyPresses() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleShift)
        s.onAction(ins("1"))
        s.onAction(ins("2"))
        assertTrue(s.shift)
        assertTrue(s.shiftLock)
        assertEquals(DisplayGlyphs.SHIFT_LOCK.toString(), s.indicator)
    }

    @Test
    fun latchedShiftReleasedByShiftPress() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleShift)
        assertFalse(s.shift)
        assertFalse(s.shiftLock)
        assertEquals("", s.indicator)
    }

    @Test
    fun latchedShiftReleasedByAlphaPress() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleAlpha)
        assertFalse(s.shift)
        assertFalse(s.shiftLock)
        assertTrue(s.alpha)
        assertFalse(s.alphaLock)
        assertEquals(DisplayGlyphs.ALPHA.toString(), s.indicator)
    }

    @Test
    fun doublePressAlphaLatchesAlpha() {
        val s = state()
        s.onAction(CalcAction.ToggleAlpha)
        s.onAction(CalcAction.ToggleAlpha)
        assertTrue(s.alpha)
        assertTrue(s.alphaLock)
        assertEquals(DisplayGlyphs.ALPHA_LOCK.toString(), s.indicator)
    }

    @Test
    fun latchedAlphaSurvivesKeyPresses() {
        val s = state()
        s.onAction(CalcAction.ToggleAlpha)
        s.onAction(CalcAction.ToggleAlpha)
        s.onAction(ins("7"))
        assertTrue(s.alpha)
        assertTrue(s.alphaLock)
        assertEquals(DisplayGlyphs.ALPHA_LOCK.toString(), s.indicator)
    }

    @Test
    fun latchedAlphaReleasedByShiftPress() {
        val s = state()
        s.onAction(CalcAction.ToggleAlpha)
        s.onAction(CalcAction.ToggleAlpha)
        s.onAction(CalcAction.ToggleShift)
        assertFalse(s.alpha)
        assertFalse(s.alphaLock)
        assertTrue(s.shift)
        assertFalse(s.shiftLock)
    }

    @Test
    fun hypPressDropsLatchedPrefixes() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleHyp)
        assertFalse(s.shift)
        assertFalse(s.shiftLock)
        assertTrue(s.hyp)
        assertEquals("h", s.indicator)
    }

    @Test
    fun openingModeMenuDropsLatchedShift() {
        val s = state()
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.ToggleShift)
        s.onAction(CalcAction.OpenModeMenu)
        assertFalse(s.shift)
        assertFalse(s.shiftLock)
    }
    // endregion

    // region Angle mode
    @Test
    fun cycleModeRotatesThroughUnits() {
        val s = state()
        assertEquals("DEG", s.modeLabel)
        s.onAction(CalcAction.CycleMode())
        assertEquals("RAD", s.modeLabel)
        s.onAction(CalcAction.CycleMode())
        assertEquals("GRA", s.modeLabel)
        s.onAction(CalcAction.CycleMode())
        assertEquals("DEG", s.modeLabel)
    }

    @Test
    fun cycleModeCanSetSpecificUnit() {
        val s = state()
        s.onAction(CalcAction.CycleMode(2))
        assertEquals("GRA", s.modeLabel)
        s.onAction(CalcAction.CycleMode(1))
        assertEquals("RAD", s.modeLabel)
        s.onAction(CalcAction.CycleMode(0))
        assertEquals("DEG", s.modeLabel)
    }

    @Test
    fun cycleModeIgnoresUnknownTarget() {
        val s = state()
        s.onAction(CalcAction.CycleMode(9))
        assertEquals("DEG", s.modeLabel)
    }
    // endregion

    // region String-like numeric display helpers
    @Test
    fun hexFunctionFormatsArgumentAsHex() {
        val s = state()
        s.onAction(ins("HEX$(255"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("FF", s.result)
    }

    @Test
    fun strFunctionFormatsArgumentAsDecimal() {
        val s = state()
        s.onAction(ins("STR$(255"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("255", s.result)
    }
    // endregion

    // region Memory
    @Test
    fun memoryAddAccumulatesAndSetsFlag() {
        val s = state()
        s.onAction(ins("5"))
        s.onAction(CalcAction.MemoryAdd)
        assertTrue(s.memorySet)
        assertEquals("5", s.result)
    }

    @Test
    fun memoryFlagStartsUnset() {
        assertFalse(state().memorySet)
    }
    // endregion

    // region Base conversion
    @Test
    fun convertToHexFormatsResult() {
        val s = state()
        s.onAction(ins("255"))
        s.onAction(CalcAction.ConvertBase(16))
        assertEquals("FF", s.result)
        assertEquals("Hex", s.modeLabel)
    }

    @Test
    fun convertToBinary() {
        val s = state()
        s.onAction(ins("10"))
        s.onAction(CalcAction.ConvertBase(2))
        assertEquals("1010", s.result)
        assertEquals("Bin", s.modeLabel)
    }

    @Test
    fun baseModeRejectsInvalidCharacters() {
        val s = state()
        s.onAction(ins("1"))
        s.onAction(CalcAction.ConvertBase(2))
        s.onAction(ins("2")) // not a valid binary digit
        assertEquals("", s.entry)
    }

    @Test
    fun baseModeEvaluatesInRadix() {
        val s = state()
        s.onAction(ins("0"))
        s.onAction(CalcAction.ConvertBase(16))
        s.onAction(ins("F"))
        s.onAction(ins("F"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("FF", s.result)
    }

    @Test
    fun logicalWordUsesIntegerSemanticsInDecimal() {
        val s = state()
        s.onAction(ins("12and8"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("8", s.result)
    }
    // endregion

    // region Display format via MODE menu
    @Test
    fun openModeMenuShowsLines() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        assertNotNull(s.modeLines)
        assertTrue(s.modeMenu)
    }

    @Test
    fun modeMenuSelectsRadians() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("2"))
        assertEquals("RAD", s.modeLabel)
        assertFalse(s.modeMenu)
    }

    @Test
    fun fixModeAppliesToResults() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("4")) // Fix
        s.onAction(ins("2")) // 2 decimals
        s.onAction(ins("1$DIVIDE" + "3"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("0.33", s.result)
    }

    @Test
    fun normModeRestoresDefault() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("6")) // Norm
        s.onAction(ins("1$DIVIDE" + "3"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("0.3333333333", s.result)
    }
    // endregion

    // region Clear all variables (Mcl)
    @Test
    fun clearMemoryClearsAllVariables() {
        val s = state()
        // Store 5 -> A, confirm it recalls.
        s.onAction(ins("5\u2192A"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(CalcAction.Clear)
        s.onAction(ins("A"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("5", s.result)
        // Mcl (SHIFT DEL) wipes every value memory; A now reads back as 0.
        s.onAction(CalcAction.ClearMemory)
        s.onAction(CalcAction.Clear)
        s.onAction(ins("A"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("0", s.result)
    }

    @Test
    fun clearMemoryClearsMemoryFlag() {
        val s = state()
        s.onAction(ins("7"))
        s.onAction(CalcAction.MemoryAdd) // M = 7
        assertTrue(s.memorySet)
        s.onAction(CalcAction.ClearMemory)
        assertFalse(s.memorySet)
    }
    // endregion

    // region Rnd (round to display precision)
    @Test
    fun rndRoundsToFixPrecision() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("4")) // Fix
        s.onAction(ins("2")) // 2 decimals
        s.onAction(ins("2$DIVIDE" + "3"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("0.67", s.result)
        // Rnd fixes the internal value to 0.67, so Ans*3 = 2.01 (not 2.00).
        s.onAction(CalcAction.Round)
        assertEquals("0.67", s.result)
        s.onAction(ins("Ans$TIMES" + "3"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("2.01", s.result)
    }
    // endregion

    // region ENG (engineering notation)
    @Test
    fun engUsesMultipleOfThreeExponent() {
        val s = state()
        s.onAction(ins("1234"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(CalcAction.Eng)
        assertEquals("1.234E3", s.result)
    }

    @Test
    fun engHandlesSmallValues() {
        val s = state()
        s.onAction(ins("0.0001234"))
        s.onAction(CalcAction.Evaluate)
        s.onAction(CalcAction.Eng)
        assertEquals("123.4E-6", s.result)
    }
    // endregion

    // region Cube root
    @Test
    fun cubeRootEvaluates() {
        val s = state()
        s.onAction(ins("\u00B3\u221A(27"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("3", s.result)
    }

    @Test
    fun cubeRootOfNegative() {
        val s = state()
        s.onAction(ins("\u00B3\u221A(-8"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("-2", s.result)
    }
    // endregion

    // region RANGE editor
    @Test
    fun enterRangeShowsSixFields() {
        val s = state()
        s.onAction(CalcAction.Range)
        assertTrue(s.rangeMode)
        val lines = s.rangeLines
        assertNotNull(lines)
        assertEquals(6, lines!!.size)
        assertTrue(lines[0].startsWith("XMIN"))
    }

    @Test
    fun rangeEditCommitsValue() {
        val s = state()
        s.onAction(CalcAction.Range)
        s.onAction(ins("1")) // fresh field replaced with "1"
        s.onAction(CalcAction.Evaluate) // commit XMIN and advance
        assertTrue(s.rangeLines!![0].startsWith("XMIN 1"))
    }

    @Test
    fun clearExitsRange() {
        val s = state()
        s.onAction(CalcAction.Range)
        s.onAction(CalcAction.Clear)
        assertFalse(s.rangeMode)
        assertNull(s.rangeLines)
    }
    // endregion

    // region Graphing
    @Test
    fun graphProducesBuffer() {
        val s = state()
        s.onAction(ins("X"))
        s.onAction(CalcAction.Graph)
        assertNotNull(s.graphBuffer)
    }

    @Test
    fun graphIsDismissedByOtherKeys() {
        val s = state()
        s.onAction(ins("X"))
        s.onAction(CalcAction.Graph)
        s.onAction(ins("5"))
        assertNull(s.graphBuffer)
    }

    @Test
    fun traceActivatesOnArrowAfterGraph() {
        val s = state()
        s.onAction(ins("X"))
        s.onAction(CalcAction.Graph)
        s.onAction(CalcAction.MoveRight)
        assertTrue(s.traceCol >= 0)
        assertTrue(s.traceText.isNotEmpty())
    }
    // endregion

    // region Preset menu
    @Test
    fun openPresetsShowsLines() {
        val s = state()
        s.onAction(CalcAction.OpenPresets)
        assertNotNull(s.presetLines)
        assertTrue(s.presetMenu)
    }

    @Test
    fun selectingPresetPlotsGraph() {
        val s = state()
        s.onAction(CalcAction.OpenPresets)
        s.onAction(ins("1")) // sinX
        assertFalse(s.presetMenu)
        assertNotNull(s.graphBuffer)
        assertEquals("sin(X)", s.entry)
    }

    /** Every built-in preset must draw a visible curve, not a blank/flat screen. */
    @Test
    fun everyPresetDrawsACurve() {
        val entries = mapOf(
            "1" to "sin(X)", "2" to "cos(X)", "3" to "tan(X)", "4" to "X\u00B2",
            "5" to "X^3", "6" to "\u221A(X)", "7" to "X\u207B\u00B9", "8" to "ln(X)"
        )
        for ((key, expr) in entries) {
            val s = state()
            s.onAction(CalcAction.OpenPresets)
            s.onAction(ins(key))
            assertEquals(expr, s.entry)
            val buf = s.graphBuffer
            assertNotNull("preset $key produced no graph", buf)
            assertTrue("preset $key drew no curve", offAxisPixels(buf!!) > 20)
        }
    }

    /**
     * Regression: selecting ln or 1/X after a trig graph used to keep the trig
     * window (±360 / ±1.6), leaving no line or a flat line. Each preset must now
     * auto-set its own optimum window.
     */
    @Test
    fun lnPresetAutoRangesAfterTrig() {
        val s = state()
        s.onAction(CalcAction.OpenPresets); s.onAction(ins("1")) // sinX → ±360 window
        s.onAction(CalcAction.OpenPresets); s.onAction(ins("8")) // lnX
        assertEquals("ln(X)", s.entry)
        assertNotNull(s.graphBuffer)
        val xmax = rangeLine(s, 1)
        assertTrue("XMAX was $xmax", xmax.contains("8.4"))
        assertFalse("window still trig-sized: $xmax", xmax.contains("360"))
    }

    @Test
    fun reciprocalPresetAutoRangesAfterTrig() {
        val s = state()
        s.onAction(CalcAction.OpenPresets); s.onAction(ins("1")) // sinX → ±360 window
        s.onAction(CalcAction.OpenPresets); s.onAction(ins("7")) // 1/X
        assertEquals("X\u207B\u00B9", s.entry)
        assertNotNull(s.graphBuffer)
        assertTrue(offAxisPixels(s.graphBuffer!!) > 20)
        assertTrue(rangeLine(s, 1).contains("4.7")) // XMAX back to ±4.7
    }

    /** Selecting a second preset while a graph is shown must redraw it, not clear it. */
    @Test
    fun secondPresetRedrawsInsteadOfToggling() {
        val s = state()
        s.onAction(CalcAction.OpenPresets); s.onAction(ins("8")) // lnX
        assertNotNull(s.graphBuffer)
        s.onAction(CalcAction.OpenPresets); s.onAction(ins("4")) // X²
        assertEquals("X\u00B2", s.entry)
        assertNotNull(s.graphBuffer)
    }
    // endregion

    // region Cursor visibility
    @Test
    fun cursorVisibleWhileEditing() {
        val s = state()
        s.onAction(ins("1"))
        assertTrue(s.showCursor)
    }

    @Test
    fun cursorHiddenAfterEvaluate() {
        val s = state()
        s.onAction(ins("1+1"))
        s.onAction(CalcAction.Evaluate)
        assertFalse(s.showCursor)
    }

    @Test
    fun cursorHiddenInRangeMode() {
        val s = state()
        s.onAction(CalcAction.Range)
        assertFalse(s.showCursor)
    }
    // endregion

    // region PROG mode
    @Test
    fun modeMenuIncludesProgOption() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        assertNotNull(s.modeLines)
        assertTrue(s.modeLines!!.any { it.contains("7Prg") })
    }

    @Test
    fun modeMenuIncludesCalculatorOption() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        assertNotNull(s.modeLines)
        assertTrue(s.modeLines!!.any { it.contains("8Cal") })
    }

    /** MODE inside PROG opens the shared mode menu without leaving PROG. */
    @Test
    fun modeInProgOpensModeMenuOverlay() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7")) // enter 7Prg
        assertNotNull(s.progState)

        s.onAction(CalcAction.OpenModeMenu)
        assertNotNull(s.progState) // still in PROG underneath
        assertNotNull(s.modeLines)
    }

    /** The 8Cal option in the mode menu returns to the normal calculator. */
    @Test
    fun calOptionReturnsToCalculatorFromProg() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("1")) // slot 1, EDIT
        assertEquals(ProgSubmode.EDIT, s.progState!!.submode)

        s.onAction(CalcAction.OpenModeMenu) // open menu from PROG
        s.onAction(ins("8")) // 8Cal
        assertNull(s.progState)
        assertNull(s.modeLines)
    }

    /** Re-selecting 7Prg from the overlay keeps the existing PROG slot. */
    @Test
    fun progOptionKeepsSelectedSlot() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("4")) // slot 4, EDIT
        s.onAction(CalcAction.OpenModeMenu) // open menu from PROG
        s.onAction(ins("7")) // back to 7Prg (SELECT)
        assertEquals(4, s.progState!!.selectedSlot)
        assertEquals(ProgSubmode.SELECT, s.progState!!.submode)
        assertNull(s.modeLines)
    }

    @Test
    fun enterProgModeOpensSelectSubmode() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7")) // select 7Prg
        assertNotNull(s.progState)
        assertEquals(ProgSubmode.SELECT, s.progState!!.submode)
        assertFalse(s.showCursor) // selection UI has no cursor
    }

    @Test
    fun selectSlotTransitionsToEditMode() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("2")) // select slot 2
        assertEquals(2, s.progState!!.selectedSlot)
        assertEquals(ProgSubmode.EDIT, s.progState!!.submode)
    }

    @Test
    fun editModeInsertsAndCommitsLine() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("0")) // slot 0
        s.onAction(ins("10 PRINT \"HI\""))
        s.onAction(CalcAction.Evaluate) // commit line

        val lines = s.progState!!.store.getLines(0)
        assertEquals(1, lines.size)
        assertEquals(10 to "PRINT \"HI\"", lines[0])
        assertEquals("", s.progState!!.editBuffer)
    }

    @Test
    fun exitingProgPreservesCalculatorState() {
        val s = state()
        s.onAction(ins("42"))
        s.onAction(CalcAction.Evaluate)
        assertEquals("42", s.result)

        // Enter PROG mode and edit slot 1
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("1"))
        s.onAction(ins("10 REM TEST"))
        s.onAction(CalcAction.Evaluate)

        // Press MODE to open the mode menu (as an overlay over PROG),
        // then 8Cal to return to the calculator.
        s.onAction(CalcAction.OpenModeMenu)
        assertNotNull(s.progState)
        assertNotNull(s.modeLines)
        s.onAction(ins("8"))
        assertNull(s.progState)

        // Calculator result and entry are preserved
        assertEquals("42", s.result)

        // Re-enter PROG mode: slot 1 is occupied
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        assertNotNull(s.progState)
        assertEquals("P0*23456789", s.progState!!.store.occupiedMask())
    }

    @Test
    fun progModeAlphaLockToggle() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("0")) // EDIT mode
        assertFalse(s.alpha)
        assertFalse(s.progState!!.alphaLock)

        // First press of ALPHA: alpha = true, alphaLock = false
        s.onAction(CalcAction.ToggleAlpha)
        assertTrue(s.alpha)
        assertFalse(s.progState!!.alphaLock)

        // Second press of ALPHA: activates alphaLock
        s.onAction(CalcAction.ToggleAlpha)
        assertTrue(s.progState!!.alphaLock)

        // Typing a character does not reset alpha when alphaLock is active
        s.onAction(ins("A"))
        assertEquals(DisplayGlyphs.ALPHA.toString(), s.indicator) // indicator retains the alpha flag
    }

    @Test
    fun clearInEditModeWithEmptyBufferReturnsToSelect() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("3"))
        assertEquals(ProgSubmode.EDIT, s.progState!!.submode)

        // AC when buffer is empty returns to SELECT
        s.onAction(CalcAction.Clear)
        assertEquals(ProgSubmode.SELECT, s.progState!!.submode)

        // AC in SELECT exits PROG mode entirely
        s.onAction(CalcAction.Clear)
        assertNull(s.progState)
    }

    @Test
    fun homeAndEndNavigateProgramEditor() {
        val s = state()
        s.onAction(CalcAction.OpenModeMenu)
        s.onAction(ins("7"))
        s.onAction(ins("0")) // slot 0 -> EDIT mode
        s.onAction(ins("10 PRINT \"HI\""))

        // HOME jumps the edit cursor to the start of the line.
        s.onAction(CalcAction.MoveHome)
        assertEquals(0, s.progState!!.editCursor)

        // END jumps it to the end of the line.
        s.onAction(CalcAction.MoveEnd)
        assertEquals("10 PRINT \"HI\"".length, s.progState!!.editCursor)

        // Inserting at HOME prepends to the current line.
        s.onAction(CalcAction.MoveHome)
        s.onAction(ins("5"))
        assertEquals("510 PRINT \"HI\"", s.progState!!.editBuffer)
    }
    // endregion
}
