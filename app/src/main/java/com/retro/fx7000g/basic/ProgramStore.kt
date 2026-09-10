package com.retro.fx7000g.basic

import java.util.TreeMap

/**
 * Storage for the FX-7000G's ten program slots (P0 to P9).
 *
 * Each slot contains line-numbered BASIC source lines, kept sorted by line number.
 * Line numbers are positive integers (typically 1–9999).
 */
class ProgramStore {

    /** 10 program slots, each holding a sorted map of lineNumber -> statement. */
    private val slots: Array<TreeMap<Int, String>> = Array(10) { TreeMap() }

    /** Returns true if the specified slot index (0–9) contains at least one line. */
    fun isOccupied(slot: Int): Boolean {
        require(slot in 0..9) { "Slot must be in 0..9" }
        return slots[slot].isNotEmpty()
    }

    /** Clears all lines in the specified slot. */
    fun clearSlot(slot: Int) {
        require(slot in 0..9) { "Slot must be in 0..9" }
        slots[slot].clear()
    }

    /**
     * Sets or replaces a line in [slot]. If [text] is blank, the line is removed.
     */
    fun setLine(slot: Int, lineNum: Int, text: String) {
        require(slot in 0..9) { "Slot must be in 0..9" }
        require(lineNum > 0) { "Line number must be positive" }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            slots[slot].remove(lineNum)
        } else {
            slots[slot][lineNum] = trimmed
        }
    }

    /** Removes [lineNum] from [slot]. */
    fun deleteLine(slot: Int, lineNum: Int) {
        require(slot in 0..9) { "Slot must be in 0..9" }
        slots[slot].remove(lineNum)
    }

    /** Returns all lines in [slot] sorted by line number. */
    fun getLines(slot: Int): List<Pair<Int, String>> {
        require(slot in 0..9) { "Slot must be in 0..9" }
        return slots[slot].map { (k, v) -> Pair(k, v) }
    }

    /** Returns the text of [lineNum] in [slot], or null if not found. */
    fun getLine(slot: Int, lineNum: Int): String? {
        require(slot in 0..9) { "Slot must be in 0..9" }
        return slots[slot][lineNum]
    }

    /**
     * Parses a raw entered line like "10 PRINT \"HI\"" or "20" and applies it to [slot].
     * - "10 PRINT \"HI\"" -> sets line 10 to "PRINT \"HI\""
     * - "10" -> deletes line 10
     * Returns true if a valid line number was recognized and applied, false otherwise.
     */
    fun parseAndCommit(slot: Int, raw: String): Boolean {
        require(slot in 0..9) { "Slot must be in 0..9" }
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false

        var i = 0
        while (i < trimmed.length && trimmed[i].isDigit()) i++
        if (i == 0) return false // No line number

        val lineNum = trimmed.substring(0, i).toIntOrNull() ?: return false
        if (lineNum <= 0) return false

        val stmt = trimmed.substring(i).trim()
        if (stmt.isEmpty()) {
            deleteLine(slot, lineNum)
        } else {
            setLine(slot, lineNum, stmt)
        }
        return true
    }

    /**
     * Returns the 11-character status string `P0123456789` where occupied slots
     * have their digit replaced with `*` (e.g. `P0*234*6789`).
     */
    fun occupiedMask(): String = buildString(11) {
        append('P')
        for (i in 0..9) {
            if (isOccupied(i)) append('*') else append(i)
        }
    }
}

