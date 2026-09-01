package com.retro.fx7000g.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/** Logical dot-matrix resolution of the FX-7000G LCD. */
private const val COLS = 96
private const val ROWS = 64
private const val CELL_W = 6 // 5px glyph + 1px gap  -> 16 chars per line
private const val CELL_H = 8 // 7px glyph + 1px gap  -> 8 lines

/**
 * The green dot-matrix LCD. Renders a status line (angle mode + memory flag),
 * the wrapped entry line and the right-aligned result onto a real 96x64 pixel
 * grid, exactly like the original hardware.
 */
@Composable
fun LcdDisplay(
    entry: String,
    result: String,
    modeLabel: String,
    memorySet: Boolean,
    cursor: Int = 0,
    showCursor: Boolean = false,
    graph: BooleanArray? = null,
    rangeLines: List<String>? = null,
    rangeCursorRow: Int = -1,
    rangeCursorCol: Int = -1,
    modeLines: List<String>? = null,
    presetLines: List<String>? = null,
    indicator: String = "",
    traceCol: Int = -1,
    traceRow: Int = -1,
    traceText: String = "",
    modifier: Modifier = Modifier
) {
    val buffer = remember(
        entry, result, modeLabel, memorySet, cursor, showCursor,
        graph, rangeLines, rangeCursorRow, rangeCursorCol, modeLines,
        presetLines, indicator, traceCol, traceRow, traceText
    ) {
        when {
            presetLines != null -> buildMenuBuffer("GRAPH", presetLines)
            modeLines != null -> buildMenuBuffer("MODE", modeLines)
            rangeLines != null -> buildRangeBuffer(rangeLines, rangeCursorRow, rangeCursorCol)
            graph != null -> overlayTrace(graph, traceCol, traceRow, traceText)
            else -> buildBuffer(entry, result, modeLabel, memorySet, cursor, showCursor, indicator)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(COLS.toFloat() / ROWS.toFloat())
            .clip(RoundedCornerShape(6.dp))
            .background(Fx7000gColors.LcdBackground)
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMatrix(buffer)
        }
    }
}

private fun DrawScope.drawMatrix(buffer: BooleanArray) {
    val dotW = size.width / COLS
    val dotH = size.height / ROWS
    val radius = minOf(dotW, dotH) * 0.42f
    for (y in 0 until ROWS) {
        for (x in 0 until COLS) {
            val on = buffer[y * COLS + x]
            val center = Offset(x * dotW + dotW / 2f, y * dotH + dotH / 2f)
            drawCircle(
                color = if (on) Fx7000gColors.LcdDotOn else Fx7000gColors.LcdDotOff,
                radius = radius,
                center = center
            )
        }
    }
}

// --- Framebuffer construction -------------------------------------------------

private fun buildBuffer(
    entry: String,
    result: String,
    modeLabel: String,
    memorySet: Boolean,
    cursor: Int,
    showCursor: Boolean,
    indicator: String
): BooleanArray {
    val buf = BooleanArray(COLS * ROWS)

    // Status row (char-row 0): mode label at far left, S/A/h prefix flags and
    // the memory flag at the right.
    drawText(buf, modeLabel, col = 0, charRow = 0)
    var rightCol = 16
    if (memorySet) { drawText(buf, "M", 15, 0); rightCol = 15 }
    if (indicator.isNotEmpty()) {
        val startCol = (rightCol - indicator.length).coerceAtLeast(0)
        drawText(buf, indicator, startCol, 0)
    }

    // Entry occupies char-rows 2 and 3, left aligned, showing a 32-char window
    // that keeps the cursor in view.
    val maxChars = 32
    val cur = cursor.coerceIn(0, entry.length)
    val start = if (entry.length <= maxChars) 0
    else (cur - maxChars + 1).coerceIn(0, entry.length - maxChars + 1)
    val end = minOf(entry.length, start + maxChars)
    val window = entry.substring(start, end)
    for (idx in window.indices) {
        drawGlyph(buf, window[idx], idx % 16, 2 + idx / 16)
    }
    if (showCursor) {
        val local = cur - start
        drawCursor(buf, local % 16, 2 + local / 16)
    }

    // Result on char-row 6, right aligned. On the fx-7000G every BASE-n value
    // fits one 16-char line (binary is 16-bit, octal/decimal/hex are 32-bit).
    if (result.isNotEmpty()) {
        val shown = if (result.length <= 16) result else result.takeLast(16)
        drawTextRight(buf, shown, charRow = 6)
    }

    return buf
}

/** Draws a text-cursor underline in the cell at (charCol, charRow). */
private fun drawCursor(buf: BooleanArray, charCol: Int, charRow: Int) {
    if (charCol !in 0 until 16) return
    val originX = charCol * CELL_W
    val y = charRow * CELL_H + 7 // separator row just under the 7px glyph
    for (gx in 0 until DotFont.WIDTH) {
        val x = originX + gx
        if (x in 0 until COLS && y in 0 until ROWS) buf[y * COLS + x] = true
    }
}

/** Renders the RANGE editor: a header plus one line per window bound. */
private fun buildRangeBuffer(
    lines: List<String>,
    cursorRow: Int,
    cursorCol: Int
): BooleanArray {
    val buf = BooleanArray(COLS * ROWS)
    drawText(buf, "RANGE", col = 0, charRow = 0)
    lines.forEachIndexed { i, line ->
        drawText(buf, line, col = 0, charRow = 2 + i)
    }
    if (cursorRow in 0 until (ROWS / CELL_H)) drawCursor(buf, cursorCol, cursorRow)
    return buf
}

/** Renders a simple header + option lines menu (Norm/Fix/Sci or graph presets). */
private fun buildMenuBuffer(header: String, lines: List<String>): BooleanArray {
    val buf = BooleanArray(COLS * ROWS)
    drawText(buf, header, col = 0, charRow = 0)
    lines.forEachIndexed { i, line ->
        drawText(buf, line, col = 0, charRow = 2 + i)
    }
    return buf
}

/** Copies the graph and overlays the trace cursor column, marker and read-out. */
private fun overlayTrace(
    graph: BooleanArray,
    traceCol: Int,
    traceRow: Int,
    traceText: String
): BooleanArray {
    if (traceCol < 0) return graph
    val buf = graph.copyOf()

    // Dotted vertical cursor line at the trace column.
    if (traceCol in 0 until COLS) {
        for (y in 0 until ROWS) {
            if (y % 2 == 0) buf[y * COLS + traceCol] = true
        }
    }
    // Solid 3x3 marker at the curve point.
    if (traceRow in 0 until ROWS && traceCol in 0 until COLS) {
        for (dy in -1..1) for (dx in -1..1) {
            val x = traceCol + dx
            val y = traceRow + dy
            if (x in 0 until COLS && y in 0 until ROWS) buf[y * COLS + x] = true
        }
    }
    // Coordinate read-out on the bottom char-row, cleared for legibility.
    if (traceText.isNotEmpty()) {
        val top = 7 * CELL_H
        for (y in top until ROWS) for (x in 0 until COLS) buf[y * COLS + x] = false
        drawText(buf, traceText, col = 0, charRow = 7)
    }
    return buf
}

private fun drawText(buf: BooleanArray, text: String, col: Int, charRow: Int) {
    var c = col
    for (ch in text) {
        if (c >= 16) break
        drawGlyph(buf, ch, c, charRow)
        c++
    }
}

private fun drawTextRight(buf: BooleanArray, text: String, charRow: Int) {
    val clipped = if (text.length <= 16) text else text.takeLast(16)
    val startCol = 16 - clipped.length
    drawText(buf, clipped, startCol, charRow)
}

private fun drawGlyph(buf: BooleanArray, ch: Char, charCol: Int, charRow: Int) {
    val glyph = DotFont.glyph(ch)
    val originX = charCol * CELL_W
    val originY = charRow * CELL_H
    for (gy in 0 until DotFont.HEIGHT) {
        val row = glyph[gy]
        for (gx in 0 until DotFont.WIDTH) {
            if (gx < row.length && row[gx] == '#') {
                val x = originX + gx
                val y = originY + gy
                if (x in 0 until COLS && y in 0 until ROWS) {
                    buf[y * COLS + x] = true
                }
            }
        }
    }
}
