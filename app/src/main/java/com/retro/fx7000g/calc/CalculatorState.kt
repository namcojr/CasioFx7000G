package com.retro.fx7000g.calc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/** Everything a keypad button can ask the calculator to do. */
sealed interface CalcAction {
    /** Append literal display glyphs to the entry line (digits, operators, "sin(", "π", "Ans", "M"…). */
    data class Insert(val text: String) : CalcAction
    object Evaluate : CalcAction      // EXE
    object Clear : CalcAction         // AC
    object Delete : CalcAction        // DEL
    object ToggleShift : CalcAction   // SHIFT
    object CycleMode : CalcAction     // MODE
    object MemoryAdd : CalcAction     // M+
    object Graph : CalcAction         // GRAPH
    object Range : CalcAction         // RANGE (graph window editor)
    object MoveLeft : CalcAction      // ◄ replay/cursor
    object MoveRight : CalcAction     // ► replay/cursor
    data class ConvertBase(val base: Int) : CalcAction // DEC / HEX / BIN / OCT
}

/**
 * Holds the live calculator state and applies key presses. Kept as a plain
 * Compose state holder (survives rotation via the activity's configChanges).
 */
class CalculatorState {

    var entry by mutableStateOf("")
        private set
    var result by mutableStateOf("")
        private set
    var cursor by mutableStateOf(0)
        private set
    var angleMode by mutableStateOf(AngleMode.DEG)
        private set
    var numberBase by mutableStateOf(10)
        private set
    var shift by mutableStateOf(false)
        private set
    var memorySet by mutableStateOf(false)
        private set
    var graphBuffer by mutableStateOf<BooleanArray?>(null)
        private set
    var rangeMode by mutableStateOf(false)
        private set
    var rangeField by mutableStateOf(0)
        private set
    var rangeBuffer by mutableStateOf("")
        private set

    private var ans: Double = 0.0
    /** Named value memories (A–Z, plus M and the graph variable X). */
    private val vars = mutableMapOf<Char, Double>()
    private var justEvaluated = false
    private var error = false
    private var rangeFresh = false

    // Graph window (editable via the RANGE key).
    private var xMin = -4.7
    private var xMax = 4.7
    private var yMin = -3.1
    private var yMax = 3.1

    /** True whenever a non-decimal integer base (hex/oct/bin) is active. */
    private val baseMode: Boolean get() = numberBase != 10

    /** The cursor underline is only drawn while the entry line is being edited. */
    val showCursor: Boolean get() = !justEvaluated && !error && graphBuffer == null && !rangeMode

    /** Text lines for the RANGE editor, or null when it is not open. */
    val rangeLines: List<String>?
        get() = if (!rangeMode) null else {
            listOf("XMIN", "XMAX", "YMIN", "YMAX").mapIndexed { i, label ->
                val v = if (i == rangeField) rangeBuffer else fmtRange(rangeFieldValue(i))
                "$label $v"
            }
        }
    val rangeCursorRow: Int get() = if (rangeMode) 2 + rangeField else -1
    val rangeCursorCol: Int get() = if (rangeMode) (5 + rangeBuffer.length).coerceIn(0, 15) else -1

    val modeLabel: String
        get() = if (baseMode) when (numberBase) {
            16 -> "Hex"
            8 -> "Oct"
            2 -> "Bin"
            else -> "Dec"
        } else angleMode.label

    fun onAction(action: CalcAction) {
        if (rangeMode) {
            handleRangeAction(action)
            if (action != CalcAction.ToggleShift) shift = false
            return
        }
        // Any key other than a re-plot dismisses the graph and returns to the calculator.
        if (graphBuffer != null && action !is CalcAction.Graph) graphBuffer = null
        when (action) {
            is CalcAction.Insert -> insert(action.text)
            CalcAction.Evaluate -> evaluate()
            CalcAction.Clear -> clear()
            CalcAction.Delete -> delete()
            CalcAction.ToggleShift -> shift = !shift
            CalcAction.CycleMode -> cycleMode()
            CalcAction.MemoryAdd -> memoryAdd()
            CalcAction.Graph -> plotGraph()
            CalcAction.Range -> enterRange()
            CalcAction.MoveLeft -> moveLeft()
            CalcAction.MoveRight -> moveRight()
            is CalcAction.ConvertBase -> convertBase(action.base)
        }
        // SHIFT is a one-shot modifier that clears after the next key.
        if (action != CalcAction.ToggleShift) shift = false
    }

    private fun insert(text: String) {
        if (baseMode && text.length == 1 && !isValidBaseChar(text[0])) return
        if (error) {
            error = false
            result = ""
            entry = ""
            cursor = 0
        }
        if (justEvaluated) {
            justEvaluated = false
            entry = if (isOperator(text)) "Ans" else ""
            cursor = entry.length
        }
        entry = entry.substring(0, cursor) + text + entry.substring(cursor)
        cursor += text.length
    }

    /** In BASE-n only valid digits/letters, operators and logical words are allowed. */
    private fun isValidBaseChar(c: Char): Boolean = when {
        c == '.' || c == 'e' || c == '\u03C0' || c == '\u2192' -> false
        c in '0'..'9' -> (c - '0') < numberBase
        c in 'A'..'F' -> numberBase == 16
        c in 'G'..'Z' -> false
        else -> true
    }

    private fun evaluate() {
        if (entry.isBlank()) return
        try {
            if (baseMode) {
                val value = Evaluator.evaluateBase(entry, numberBase, ans.toLong())
                ans = value.toDouble()
                result = NumberFormatter.formatBase(value, numberBase)
            } else if (hasLogicalOp(entry)) {
                // Logical words (and/or/xor/Not) use integer semantics even in DEC.
                val value = Evaluator.evaluateBase(entry, 10, ans.toLong())
                ans = value.toDouble()
                result = NumberFormatter.format(value.toDouble())
            } else {
                val value = Evaluator.evaluate(entry, angleMode, ans, vars)
                ans = value
                result = NumberFormatter.format(value)
            }
            justEvaluated = true
            error = false
            refreshMemoryFlag()
        } catch (e: CalcError) {
            result = "Ma ERROR"
            error = true
            justEvaluated = false
        } catch (e: Exception) {
            result = "Ma ERROR"
            error = true
            justEvaluated = false
        }
    }

    private fun clear() {
        entry = ""
        result = ""
        cursor = 0
        error = false
        justEvaluated = false
    }

    private fun delete() {
        if (justEvaluated || error) {
            error = false
            justEvaluated = false
            return
        }
        if (cursor == 0) return
        val before = entry.substring(0, cursor)
        val token = TRAILING_TOKENS.firstOrNull { before.endsWith(it) }
        val len = token?.length ?: 1
        entry = before.dropLast(len) + entry.substring(cursor)
        cursor -= len
    }

    private fun moveLeft() {
        if (error) return
        // Pressing an arrow after EXE replays the last expression for editing.
        if (justEvaluated) { justEvaluated = false; cursor = entry.length }
        cursor = (cursor - 1).coerceAtLeast(0)
    }

    private fun moveRight() {
        if (error) return
        if (justEvaluated) { justEvaluated = false; cursor = entry.length; return }
        cursor = (cursor + 1).coerceAtMost(entry.length)
    }

    private fun cycleMode() {
        angleMode = when (angleMode) {
            AngleMode.DEG -> AngleMode.RAD
            AngleMode.RAD -> AngleMode.GRA
            AngleMode.GRA -> AngleMode.DEG
        }
    }

    private fun memoryAdd() {
        try {
            val value = currentValue()
            vars['M'] = (vars['M'] ?: 0.0) + value
            result = if (baseMode) NumberFormatter.formatBase(value.toLong(), numberBase)
            else NumberFormatter.format(value)
            ans = value
            justEvaluated = true
            error = false
            refreshMemoryFlag()
        } catch (e: Exception) {
            result = "Ma ERROR"
            error = true
        }
    }

    private fun convertBase(base: Int) {
        try {
            val value = currentValue()
            numberBase = base
            ans = value
            entry = ""
            cursor = 0
            result = if (base == 10) {
                NumberFormatter.format(value)
            } else {
                NumberFormatter.formatBase(value.toLong(), base)
            }
            justEvaluated = true
            error = false
        } catch (e: Exception) {
            result = "Ma ERROR"
            error = true
            justEvaluated = false
        }
    }

    /** Plots the current entry as Y = f(X) onto the 96×64 dot-matrix buffer. */
    private fun plotGraph() {
        if (entry.isBlank()) return
        if (xMax <= xMin || yMax <= yMin) {
            result = "Ma ERROR"; error = true; graphBuffer = null; return
        }
        try {
            val expr = entry
            val buf = BooleanArray(GRAPH_COLS * GRAPH_ROWS)
            drawAxes(buf)
            for (col in 0 until GRAPH_COLS) {
                val x = xMin + (xMax - xMin) * col / (GRAPH_COLS - 1)
                vars['X'] = x
                val y = try {
                    Evaluator.evaluate(expr, angleMode, ans, vars)
                } catch (e: CalcError) {
                    continue
                }
                if (y.isNaN() || y.isInfinite()) continue
                val row = ((yMax - y) / (yMax - yMin) * (GRAPH_ROWS - 1)).roundToInt()
                if (row in 0 until GRAPH_ROWS) buf[row * GRAPH_COLS + col] = true
            }
            graphBuffer = buf
            error = false
            justEvaluated = false
        } catch (e: Exception) {
            result = "Ma ERROR"
            error = true
        }
    }

    private fun drawAxes(buf: BooleanArray) {
        if (0.0 in yMin..yMax) {
            val row = (yMax / (yMax - yMin) * (GRAPH_ROWS - 1)).roundToInt()
            if (row in 0 until GRAPH_ROWS) {
                for (c in 0 until GRAPH_COLS) buf[row * GRAPH_COLS + c] = true
            }
        }
        if (0.0 in xMin..xMax) {
            val col = (-xMin / (xMax - xMin) * (GRAPH_COLS - 1)).roundToInt()
            if (col in 0 until GRAPH_COLS) {
                for (r in 0 until GRAPH_ROWS) buf[r * GRAPH_COLS + col] = true
            }
        }
    }

    // --- RANGE editor ---------------------------------------------------------

    private fun enterRange() {
        rangeMode = true
        enterField(0)
    }

    private fun enterField(i: Int) {
        rangeField = i
        rangeBuffer = fmtRange(rangeFieldValue(i))
        rangeFresh = true
    }

    private fun handleRangeAction(action: CalcAction) {
        when (action) {
            is CalcAction.Insert -> rangeInsert(action.text)
            CalcAction.Delete -> rangeDelete()
            CalcAction.Evaluate -> rangeNext()
            CalcAction.MoveLeft -> rangeMove(-1)
            CalcAction.MoveRight -> rangeMove(1)
            CalcAction.Clear -> exitRange()
            CalcAction.Graph -> { commitRangeField(); exitRange(); plotGraph() }
            CalcAction.Range -> exitRange()
            CalcAction.ToggleShift -> shift = !shift
            else -> {}
        }
    }

    private fun rangeInsert(text: String) {
        if (text.length != 1) return
        val c = text[0]
        val mapped = when {
            c in '0'..'9' -> c
            c == '.' -> '.'
            c == '\u2212' || c == '-' -> '-'
            c == 'E' || c == 'e' -> 'E'
            else -> return
        }
        if (rangeFresh) { rangeBuffer = ""; rangeFresh = false }
        if (rangeBuffer.length < 12) rangeBuffer += mapped
    }

    private fun rangeDelete() {
        rangeFresh = false
        if (rangeBuffer.isNotEmpty()) rangeBuffer = rangeBuffer.dropLast(1)
    }

    private fun rangeNext() {
        commitRangeField()
        if (rangeField < 3) enterField(rangeField + 1) else exitRange()
    }

    private fun rangeMove(delta: Int) {
        commitRangeField()
        enterField((rangeField + delta).coerceIn(0, 3))
    }

    private fun exitRange() {
        commitRangeField()
        rangeMode = false
    }

    private fun commitRangeField() {
        val v = rangeBuffer.replace('\u2212', '-').toDoubleOrNull() ?: return
        setRangeFieldValue(rangeField, v)
    }

    private fun rangeFieldValue(i: Int): Double = when (i) {
        0 -> xMin
        1 -> xMax
        2 -> yMin
        else -> yMax
    }

    private fun setRangeFieldValue(i: Int, v: Double) {
        when (i) {
            0 -> xMin = v
            1 -> xMax = v
            2 -> yMin = v
            else -> yMax = v
        }
    }

    private fun fmtRange(v: Double): String =
        try { NumberFormatter.format(v) } catch (e: Exception) { v.toString() }

    /** Evaluates the current entry in the active mode, falling back to Ans. */
    private fun currentValue(): Double = when {
        entry.isBlank() -> ans
        baseMode -> Evaluator.evaluateBase(entry, numberBase, ans.toLong()).toDouble()
        hasLogicalOp(entry) -> Evaluator.evaluateBase(entry, 10, ans.toLong()).toDouble()
        else -> Evaluator.evaluate(entry, angleMode, ans, vars)
    }

    private fun refreshMemoryFlag() { memorySet = (vars['M'] ?: 0.0) != 0.0 }

    /** True when the entry uses a bitwise logical word (and / or / xor / Not). */
    private fun hasLogicalOp(text: String): Boolean =
        text.contains("and") || text.contains("or") || text.contains("Not")

    private fun isOperator(text: String): Boolean =
        text.length == 1 && text[0] in OPERATORS

    private companion object {
        const val GRAPH_COLS = 96
        const val GRAPH_ROWS = 64

        val OPERATORS = charArrayOf('+', '\u2212', '\u00D7', '\u00F7', '^')

        // Multi-character glyph groups DEL should remove in one press.
        val TRAILING_TOKENS = listOf(
            "sin\u207B\u00B9(", "cos\u207B\u00B9(", "tan\u207B\u00B9(",
            "sin(", "cos(", "tan(", "log(", "ln(",
            "Abs(", "Int(", "Frac(", "and", "xor", "Not", "or",
            "10^(", "e^(", "\u221A(", "Ans"
        )
    }
}
