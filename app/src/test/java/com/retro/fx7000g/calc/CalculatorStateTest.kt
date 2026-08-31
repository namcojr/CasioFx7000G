package com.retro.fx7000g.calc

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
    private val MINUS = "\u2212"

    private fun state() = CalculatorState()

    private fun ins(text: String) = CalcAction.Insert(text)

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
        assertEquals("S", s.indicator)
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
        assertEquals("A", s.indicator)
    }

    @Test
    fun pressingShiftTurnsAlphaOff() {
        val s = state()
        s.onAction(CalcAction.ToggleAlpha)
        s.onAction(CalcAction.ToggleShift)
        assertFalse(s.alpha)
        assertTrue(s.shift)
        assertEquals("S", s.indicator)
    }

    @Test
    fun hypToggles() {
        val s = state()
        s.onAction(CalcAction.ToggleHyp)
        assertTrue(s.hyp)
        assertEquals("h", s.indicator)
    }
    // endregion

    // region Angle mode
    @Test
    fun cycleModeRotatesThroughUnits() {
        val s = state()
        assertEquals("DEG", s.modeLabel)
        s.onAction(CalcAction.CycleMode)
        assertEquals("RAD", s.modeLabel)
        s.onAction(CalcAction.CycleMode)
        assertEquals("GRA", s.modeLabel)
        s.onAction(CalcAction.CycleMode)
        assertEquals("DEG", s.modeLabel)
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
}
