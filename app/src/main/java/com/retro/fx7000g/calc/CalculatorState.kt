package com.retro.fx7000g.calc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Everything a keypad button can ask the calculator to do. */
sealed interface CalcAction {
    /** Append literal display glyphs to the entry line (digits, operators, "sin(", "π", "Ans", "M"…). */
    data class Insert(val text: String) : CalcAction
    object Evaluate : CalcAction      // EXE
    object Clear : CalcAction         // AC
    object Delete : CalcAction        // DEL
    object ToggleShift : CalcAction   // SHIFT
    object ToggleAlpha : CalcAction   // ALPHA (letter layer)
    object ToggleHyp : CalcAction     // hyp prefix (sinh/cosh/tanh)
    object CycleMode : CalcAction     // MODE
    object MemoryAdd : CalcAction     // M+
    object MemorySubtract : CalcAction // SHIFT+M+ (M−)
    object ClearMemory : CalcAction   // SHIFT+DEL (Mcl – clear every value memory)
    object Round : CalcAction         // SHIFT+0 (Rnd – round to display precision)
    object Eng : CalcAction           // SHIFT+(−) (engineering notation)
    object Graph : CalcAction         // GRAPH
    object Range : CalcAction         // RANGE (graph window editor)
    object OpenModeMenu : CalcAction  // SHIFT+MODE (Norm/Fix/Sci setup)
    object OpenPresets : CalcAction   // SHIFT+Graph (built-in graph picker)
    object MoveLeft : CalcAction      // replay/cursor left
    object MoveRight : CalcAction     // replay/cursor right
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
    var alpha by mutableStateOf(false)
        private set
    var hyp by mutableStateOf(false)
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
    var displayFormat by mutableStateOf<NumberFormatter.DisplayFormat>(NumberFormatter.DisplayFormat.Norm)
        private set
    var modeMenu by mutableStateOf(false)
        private set
    var presetMenu by mutableStateOf(false)
        private set

    private var ans: Double = 0.0
    /** Named value memories (A–Z, plus M and the graph variable X). */
    private val vars = mutableMapOf<Char, Double>()
    private var justEvaluated = false
    private var error = false
    private var rangeFresh = false
    // Observable so the menu's prompt line ("Fix decimals?" / "Sci digits?")
    // recomposes on the first key press rather than requiring a second.
    private var modePrompt by mutableStateOf(0) // 0 none, 1 awaiting Fix decimals, 2 awaiting Sci digits

    /** Expressions currently drawn on the graph (for overlay + trace). */
    private val graphExprs = mutableListOf<String>()
    private var traceActive = false
    private var traceColIdx = 0
    private var traceRowIdx = -1
    private var traceLabel = ""

    // Graph window (editable via the RANGE key).
    private var xMin = -4.7
    private var xMax = 4.7
    private var xScl = 1.0
    private var yMin = -3.1
    private var yMax = 3.1
    private var yScl = 1.0

    /** True whenever a non-decimal integer base (hex/oct/bin) is active. */
    private val baseMode: Boolean get() = numberBase != 10

    /** The cursor underline is only drawn while the entry line is being edited. */
    val showCursor: Boolean get() = !justEvaluated && !error && graphBuffer == null && !rangeMode && !modeMenu && !presetMenu

    /** Text lines for the RANGE editor, or null when it is not open. */
    val rangeLines: List<String>?
        get() = if (!rangeMode) null else {
            listOf("XMIN", "XMAX", "XSCL", "YMIN", "YMAX", "YSCL").mapIndexed { i, label ->
                val v = if (i == rangeField) rangeBuffer else fmtRange(rangeFieldValue(i))
                "$label $v"
            }
        }
    val rangeCursorRow: Int get() = if (rangeMode) 2 + rangeField else -1
    val rangeCursorCol: Int get() = if (rangeMode) (5 + rangeBuffer.length).coerceIn(0, 15) else -1

    /** Lines for the Norm/Fix/Sci setup menu, or null when it is not open. */
    val modeLines: List<String>?
        get() = if (!modeMenu) null else listOf(
            "1Deg 2Rad 3Gra",
            "4Fix 5Sci 6Norm",
            when (modePrompt) {
                1 -> "Fix decimals?"
                2 -> "Sci digits?"
                else -> curFormatLabel()
            }
        )

    private fun curFormatLabel(): String = when (val f = displayFormat) {
        is NumberFormatter.DisplayFormat.Fix -> "now Fix " + f.decimals
        is NumberFormatter.DisplayFormat.Sci -> "now Sci " + f.digits
        else -> "now Norm"
    }

    val modeLabel: String
        get() = if (baseMode) when (numberBase) {
            16 -> "Hex"
            8 -> "Oct"
            2 -> "Bin"
            else -> "Dec"
        } else angleMode.label

    /** Small S / A / h prefix flags shown on the LCD status line. */
    val indicator: String
        get() = buildString {
            if (shift) append('S')
            if (alpha) append('A')
            if (hyp) append('h')
        }

    fun onAction(action: CalcAction) {
        if (rangeMode) {
            handleRangeAction(action)
            resetModifiers(action)
            return
        }
        if (modeMenu) {
            handleModeMenu(action)
            resetModifiers(action)
            return
        }
        if (presetMenu) {
            handlePresetMenu(action)
            resetModifiers(action)
            return
        }
        // Any key other than a re-plot dismisses the graph and returns to the calculator.
        if (graphBuffer != null && action !is CalcAction.Graph &&
            action != CalcAction.MoveLeft && action != CalcAction.MoveRight &&
            action != CalcAction.OpenPresets
        ) {
            graphBuffer = null
            traceActive = false
        }
        when (action) {
            is CalcAction.Insert -> insert(action.text)
            CalcAction.Evaluate -> evaluate()
            CalcAction.Clear -> clear()
            CalcAction.Delete -> delete()
            CalcAction.ToggleShift -> { shift = !shift; if (shift) alpha = false }
            CalcAction.ToggleAlpha -> { alpha = !alpha; if (alpha) shift = false }
            CalcAction.ToggleHyp -> hyp = !hyp
            CalcAction.CycleMode -> cycleMode()
            CalcAction.MemoryAdd -> memoryAdd()
            CalcAction.MemorySubtract -> memorySubtract()
            CalcAction.ClearMemory -> clearAllVariables()
            CalcAction.Round -> roundResult()
            CalcAction.Eng -> engResult()
            CalcAction.Graph -> plotGraph()
            CalcAction.Range -> enterRange()
            CalcAction.OpenModeMenu -> openModeMenu()
            CalcAction.OpenPresets -> openPresets()
            CalcAction.MoveLeft -> moveLeft()
            CalcAction.MoveRight -> moveRight()
            is CalcAction.ConvertBase -> convertBase(action.base)
        }
        resetModifiers(action)
    }

    /** SHIFT / ALPHA / hyp are one-shot prefixes cleared after the next real key. */
    private fun resetModifiers(action: CalcAction) {
        if (action == CalcAction.ToggleShift ||
            action == CalcAction.ToggleAlpha ||
            action == CalcAction.ToggleHyp
        ) return
        shift = false
        alpha = false
        hyp = false
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
                result = NumberFormatter.format(value.toDouble(), displayFormat)
            } else {
                val value = Evaluator.evaluate(entry, angleMode, ans, vars)
                ans = value
                result = NumberFormatter.format(value, displayFormat)
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
        graphExprs.clear()
        graphBuffer = null
        traceActive = false
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
        if (graphBuffer != null) { stepTrace(-1); return }
        // Pressing an arrow after EXE replays the last expression for editing.
        if (justEvaluated) { justEvaluated = false; cursor = entry.length }
        cursor = (cursor - 1).coerceAtLeast(0)
    }

    private fun moveRight() {
        if (error) return
        if (graphBuffer != null) { stepTrace(1); return }
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

    private fun memoryAdd() = memoryAccumulate(+1)

    private fun memorySubtract() = memoryAccumulate(-1)

    /** Adds (sign = +1) or subtracts (sign = -1) the current value to memory M. */
    private fun memoryAccumulate(sign: Int) {
        try {
            val value = currentValue()
            vars['M'] = (vars['M'] ?: 0.0) + sign * value
            result = if (baseMode) NumberFormatter.formatBase(value.toLong(), numberBase)
            else NumberFormatter.format(value, displayFormat)
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
                NumberFormatter.format(value, displayFormat)
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

    /** Plots the current entry as Y = f(X), overlaying it on any existing curves. */
    private fun plotGraph() {
        // Toggle: if the graph is already on screen, switch back to the
        // calculation buffer. The accumulated curves are kept so the next
        // Graph press can bring them straight back.
        if (graphBuffer != null) {
            graphBuffer = null
            traceActive = false
            return
        }
        // Nothing new to add (blank entry, or the entry is already the most
        // recent curve): just re-show the existing graph if there is one.
        if (entry.isBlank() || graphExprs.lastOrNull() == entry) {
            if (graphExprs.isNotEmpty()) {
                traceActive = false
                rebuildGraph()
            }
            return
        }
        // Built-in function graphs auto-set the optimum window (fx-7000G Bltin
        // behaviour): graphing sin(X)/cos(X)/tan(X) as the first curve fits a
        // full wave instead of the flat line the default ±4.7 window would show
        // for sin in degrees. Overlaid curves keep the first window.
        if (graphExprs.isEmpty()) builtinWindow(entry)?.let { applyWindow(it) }
        if (xMax <= xMin || yMax <= yMin) {
            result = "Ma ERROR"; error = true; graphBuffer = null; return
        }
        if (graphExprs.size >= 6) graphExprs.removeAt(0)
        graphExprs.add(entry)
        traceActive = false
        rebuildGraph()
    }

    /** A graph viewing window (the six RANGE values). */
    private data class GraphWindow(
        val xMin: Double, val xMax: Double, val xScl: Double,
        val yMin: Double, val yMax: Double, val yScl: Double
    )

    /**
     * The optimum window for a recognised built-in graph, or null when the
     * user's current window should be kept. On the real fx-7000G every built-in
     * (Bltin) graph auto-sets its ranges to optimum values before drawing, so
     * the curve always fills the screen regardless of the previous window. Here
     * we recognise the eight preset functions (and their common typed variants)
     * and hand back a window that shows the whole curve. Trig auto-range is
     * angle-mode aware; the rest use fixed windows chosen to frame the curve.
     */
    private fun builtinWindow(rawExpr: String): GraphWindow? {
        val e = rawExpr.replace(" ", "").removeSuffix(")")
        return when {
            e == "sin(X" || e == "cos(X" || e == "tan(X" ||
                e == "sinX" || e == "cosX" || e == "tanX" -> when (angleMode) {
                AngleMode.DEG -> GraphWindow(-360.0, 360.0, 180.0, -1.6, 1.6, 0.5)
                AngleMode.RAD -> GraphWindow(-2 * PI, 2 * PI, PI / 2, -1.6, 1.6, 0.5)
                AngleMode.GRA -> GraphWindow(-400.0, 400.0, 200.0, -1.6, 1.6, 0.5)
            }
            // Parabola y = X²
            e == "X\u00B2" || e == "X^2" ->
                GraphWindow(-4.7, 4.7, 1.0, -3.1, 15.5, 5.0)
            // Cubic y = X³
            e == "X^3" || e == "X\u00B3" ->
                GraphWindow(-4.7, 4.7, 1.0, -30.0, 30.0, 10.0)
            // Square root y = √X (only defined for X ≥ 0)
            e == "\u221A(X" || e == "\u221AX" ->
                GraphWindow(-1.0, 9.4, 1.0, -1.0, 3.1, 1.0)
            // Reciprocal y = 1/X
            e == "X\u207B\u00B9" || e == "1/X" || e == "1\u00F7X" ->
                GraphWindow(-4.7, 4.7, 1.0, -3.1, 3.1, 1.0)
            // Natural log y = ln X (only defined for X > 0)
            e == "ln(X" || e == "lnX" ->
                GraphWindow(-1.0, 8.4, 1.0, -3.1, 3.1, 1.0)
            // Common log y = log X (only defined for X > 0)
            e == "log(X" || e == "logX" ->
                GraphWindow(-1.0, 9.4, 1.0, -3.1, 1.6, 1.0)
            // Exponentials y = eˣ and y = 10ˣ
            e == "e^(X" || e == "e^X" ->
                GraphWindow(-4.7, 4.7, 1.0, -1.0, 20.0, 5.0)
            e == "10^(X" || e == "10^X" ->
                GraphWindow(-2.0, 2.0, 1.0, -1.0, 20.0, 5.0)
            else -> null
        }
    }

    private fun applyWindow(w: GraphWindow) {
        xMin = w.xMin; xMax = w.xMax; xScl = w.xScl
        yMin = w.yMin; yMax = w.yMax; yScl = w.yScl
    }

    /** Redraws the axes and every accumulated expression into the graph buffer. */
    private fun rebuildGraph() {
        try {
            val buf = BooleanArray(GRAPH_COLS * GRAPH_ROWS)
            drawAxes(buf)
            for (expr in graphExprs) {
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
            }
            graphBuffer = buf
            error = false
            justEvaluated = false
        } catch (e: Exception) {
            result = "Ma ERROR"
            error = true
        }
    }

    // --- Graph trace ----------------------------------------------------------

    /** Moves the trace cursor one dot-column along the most recent curve. */
    private fun stepTrace(delta: Int) {
        if (graphExprs.isEmpty()) return
        if (!traceActive) {
            traceActive = true
            // Start near the y-axis, or the left edge if it is off-screen.
            traceColIdx = if (0.0 in xMin..xMax)
                (-xMin / (xMax - xMin) * (GRAPH_COLS - 1)).roundToInt().coerceIn(0, GRAPH_COLS - 1)
            else 0
        } else {
            traceColIdx = (traceColIdx + delta).coerceIn(0, GRAPH_COLS - 1)
        }
        recomputeTrace()
    }

    private fun recomputeTrace() {
        val expr = graphExprs.lastOrNull() ?: return
        val x = xMin + (xMax - xMin) * traceColIdx / (GRAPH_COLS - 1)
        vars['X'] = x
        val y = try {
            Evaluator.evaluate(expr, angleMode, ans, vars)
        } catch (e: Exception) {
            traceRowIdx = -1
            traceLabel = "X" + traceNum(x)
            return
        }
        traceRowIdx = if (y.isNaN() || y.isInfinite()) -1
        else ((yMax - y) / (yMax - yMin) * (GRAPH_ROWS - 1)).roundToInt()
        traceLabel = "X" + traceNum(x) + " Y" + traceNum(y)
    }

    /** Compact number for the trace read-out (fits two on a 16-char row). */
    private fun traceNum(v: Double): String {
        if (v == 0.0) return "0"
        val s = try { NumberFormatter.format(v) } catch (e: Exception) { v.toString() }
        return if (s.length > 6) s.take(6) else s
    }

    val traceCol: Int get() = if (graphBuffer != null && traceActive) traceColIdx else -1
    val traceRow: Int get() = if (graphBuffer != null && traceActive) traceRowIdx else -1
    val traceText: String get() = if (graphBuffer != null && traceActive) traceLabel else ""

    // --- Built-in graph presets ----------------------------------------------

    /** Menu lines for the built-in graph picker, or null when it is closed. */
    val presetLines: List<String>?
        get() = if (!presetMenu) null else listOf(
            "1sinX  2cosX",
            "3tanX  4X\u00B2",
            "5X^3   6\u221AX",
            "7 1/X  8lnX"
        )

    private fun openPresets() {
        presetMenu = true
    }

    private fun handlePresetMenu(action: CalcAction) {
        when (action) {
            is CalcAction.Insert -> selectPreset(action.text)
            CalcAction.Clear -> presetMenu = false
            CalcAction.OpenPresets -> presetMenu = false
            CalcAction.ToggleShift -> shift = !shift
            else -> {}
        }
    }

    private fun selectPreset(text: String) {
        if (text.length != 1) return
        val expr = when (text[0]) {
            '1' -> "sin(X)"
            '2' -> "cos(X)"
            '3' -> "tan(X)"
            '4' -> "X\u00B2"
            '5' -> "X^3"
            '6' -> "\u221A(X)"
            '7' -> "X\u207B\u00B9"
            '8' -> "ln(X)"
            else -> return
        }
        presetMenu = false
        graphExprs.clear()
        entry = expr
        cursor = entry.length
        traceActive = false
        // Clear any graph already on screen so plotGraph draws the freshly
        // picked preset instead of toggling the previous one off.
        graphBuffer = null
        plotGraph()
    }

    private fun drawAxes(buf: BooleanArray) {
        val xAxisRow = if (0.0 in yMin..yMax)
            (yMax / (yMax - yMin) * (GRAPH_ROWS - 1)).roundToInt() else -1
        val yAxisCol = if (0.0 in xMin..xMax)
            (-xMin / (xMax - xMin) * (GRAPH_COLS - 1)).roundToInt() else -1

        if (xAxisRow in 0 until GRAPH_ROWS) {
            for (c in 0 until GRAPH_COLS) buf[xAxisRow * GRAPH_COLS + c] = true
        }
        if (yAxisCol in 0 until GRAPH_COLS) {
            for (r in 0 until GRAPH_ROWS) buf[r * GRAPH_COLS + yAxisCol] = true
        }

        // Scale marks: XSCL spaces the ticks along the x-axis, YSCL along the y-axis.
        drawXTicks(buf, xAxisRow)
        drawYTicks(buf, yAxisCol)
    }

    /** Short vertical ticks at every multiple of XSCL, sitting on the x-axis. */
    private fun drawXTicks(buf: BooleanArray, axisRow: Int) {
        if (axisRow !in 0 until GRAPH_ROWS || xScl <= 0.0) return
        val span = xMax - xMin
        if (span / xScl > 500) return
        var k = ceil(xMin / xScl).toInt()
        while (k * xScl <= xMax + 1e-9) {
            if (k != 0) {
                val col = ((k * xScl - xMin) / span * (GRAPH_COLS - 1)).roundToInt()
                if (col in 0 until GRAPH_COLS) {
                    for (dr in -1..1) {
                        val r = axisRow + dr
                        if (r in 0 until GRAPH_ROWS) buf[r * GRAPH_COLS + col] = true
                    }
                }
            }
            k++
        }
    }

    /** Short horizontal ticks at every multiple of YSCL, sitting on the y-axis. */
    private fun drawYTicks(buf: BooleanArray, axisCol: Int) {
        if (axisCol !in 0 until GRAPH_COLS || yScl <= 0.0) return
        val span = yMax - yMin
        if (span / yScl > 500) return
        var k = ceil(yMin / yScl).toInt()
        while (k * yScl <= yMax + 1e-9) {
            if (k != 0) {
                val row = ((yMax - k * yScl) / span * (GRAPH_ROWS - 1)).roundToInt()
                if (row in 0 until GRAPH_ROWS) {
                    for (dc in -1..1) {
                        val c = axisCol + dc
                        if (c in 0 until GRAPH_COLS) buf[row * GRAPH_COLS + c] = true
                    }
                }
            }
            k++
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
        if (rangeField < 5) enterField(rangeField + 1) else exitRange()
    }

    private fun rangeMove(delta: Int) {
        commitRangeField()
        enterField((rangeField + delta).coerceIn(0, 5))
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
        2 -> xScl
        3 -> yMin
        4 -> yMax
        else -> yScl
    }

    private fun setRangeFieldValue(i: Int, v: Double) {
        when (i) {
            0 -> xMin = v
            1 -> xMax = v
            2 -> xScl = v
            3 -> yMin = v
            4 -> yMax = v
            else -> yScl = v
        }
    }

    private fun fmtRange(v: Double): String =
        try { NumberFormatter.format(v) } catch (e: Exception) { v.toString() }

    // --- MODE setup menu (Norm / Fix / Sci and angle unit) --------------------

    private fun openModeMenu() {
        modeMenu = true
        modePrompt = 0
    }

    private fun handleModeMenu(action: CalcAction) {
        when (action) {
            is CalcAction.Insert -> modeMenuInput(action.text)
            CalcAction.Clear -> closeModeMenu()
            CalcAction.CycleMode -> closeModeMenu()
            CalcAction.OpenModeMenu -> closeModeMenu()
            CalcAction.ToggleShift -> shift = !shift
            else -> {}
        }
    }

    private fun modeMenuInput(text: String) {
        if (text.length != 1) return
        val c = text[0]
        if (modePrompt == 1 || modePrompt == 2) {
            if (c in '0'..'9') {
                val n = c - '0'
                displayFormat = if (modePrompt == 1) {
                    NumberFormatter.DisplayFormat.Fix(n)
                } else {
                    NumberFormatter.DisplayFormat.Sci(if (n == 0) 10 else n)
                }
                closeModeMenu()
            }
            return
        }
        when (c) {
            '1' -> { angleMode = AngleMode.DEG; closeModeMenu() }
            '2' -> { angleMode = AngleMode.RAD; closeModeMenu() }
            '3' -> { angleMode = AngleMode.GRA; closeModeMenu() }
            '4' -> modePrompt = 1 // Fix -> await decimal count
            '5' -> modePrompt = 2 // Sci -> await significant digits
            '6' -> { displayFormat = NumberFormatter.DisplayFormat.Norm; closeModeMenu() }
        }
    }

    /**
     * Clears every value memory (A–Z, the accumulation memory M and the graph
     * variable X). Mirrors the fx-7000G "Mcl" operation, reached with SHIFT DEL.
     */
    private fun clearAllVariables() {
        vars.clear()
        refreshMemoryFlag()
    }

    /**
     * Rounds the current value to the precision shown on the display (the Rnd
     * function, SHIFT 0). The rounded number becomes the new internal value, so a
     * following Ans-based calculation uses the shortened figure.
     */
    private fun roundResult() {
        if (baseMode) return
        try {
            val value = NumberFormatter.roundToFormat(currentValue(), displayFormat)
            ans = value
            result = NumberFormatter.format(value, displayFormat)
            justEvaluated = true
            error = false
        } catch (e: Exception) {
            result = "Ma ERROR"
            error = true
            justEvaluated = false
        }
    }

    /**
     * Reformats the current value in engineering notation (the ENG key, SHIFT of
     * the negation key): the exponent becomes a multiple of three. The value is
     * unchanged, only its display.
     */
    private fun engResult() {
        if (baseMode) return
        try {
            val value = currentValue()
            ans = value
            result = NumberFormatter.formatEng(value)
            justEvaluated = true
            error = false
        } catch (e: Exception) {
            result = "Ma ERROR"
            error = true
            justEvaluated = false
        }
    }

    private fun closeModeMenu() {
        modeMenu = false
        modePrompt = 0
    }

    /** Evaluates the current entry in the active mode, falling back to Ans. */
    private fun currentValue(): Double = when {
        // A blank entry means Ans only right after an evaluation/conversion;
        // once AC (or deleting everything) clears the display the working value
        // is 0, matching the real fx-7000G.
        entry.isBlank() -> if (justEvaluated) ans else 0.0
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
