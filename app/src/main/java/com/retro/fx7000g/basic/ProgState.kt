package com.retro.fx7000g.basic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retro.fx7000g.basic.memory.Fx7000gMemory
import com.retro.fx7000g.calc.DisplayTokens

enum class ProgSubmode {
    SELECT, // Transient P0-P9 selector UI
    EDIT,   // Full-screen (8-row) program line editor
    RUN     // Full-screen (8-row) execution console
}

/**
 * Encapsulates the runtime, editor, and selection state for the FX-7000G PROG environment.
 */
class ProgState(
    val store: ProgramStore = ProgramStore(),
    /** Finite virtual RAM shared by the program, variable and runtime subsystems. */
    val memory: Fx7000gMemory = Fx7000gMemory()
) {
    var submode by mutableStateOf(ProgSubmode.SELECT)
    var selectedSlot by mutableStateOf(0)

    // EDIT mode state
    var editBuffer by mutableStateOf("")
    var editCursor by mutableStateOf(0)
    var scrollLineIdx by mutableStateOf(0)
    var alphaLock by mutableStateOf(false)

    // RUN mode state
    val runLines = mutableListOf<String>()
    var runHalted by mutableStateOf(false)
    var breakLine by mutableStateOf<Int?>(null)

    /** Selects a slot (0–9) and enters EDIT mode immediately. */
    fun selectSlot(slot: Int) {
        require(slot in 0..9) { "Slot must be in 0..9" }
        selectedSlot = slot
        submode = ProgSubmode.EDIT
        editBuffer = ""
        editCursor = 0
        scrollLineIdx = 0
    }

    /** Inserts text at the current cursor in EDIT mode. */
    fun insertText(text: String) {
        if (submode != ProgSubmode.EDIT) return
        val before = editBuffer.substring(0, editCursor)
        val after = editBuffer.substring(editCursor)
        editBuffer = before + text + after
        editCursor += text.length
    }

    /** Deletes the token immediately before the cursor in EDIT mode. */
    fun deleteChar() {
        if (submode != ProgSubmode.EDIT) return
        if (editCursor <= 0 || editBuffer.isEmpty()) return
        val beforeCursor = editBuffer.substring(0, editCursor)
        val len = DisplayTokens.trailingTokenLength(beforeCursor)
        val before = beforeCursor.dropLast(len)
        val after = editBuffer.substring(editCursor)
        editBuffer = before + after
        editCursor -= len
    }

    /** Commits the current edit buffer (line entry) into the selected program slot. */
    fun commitLine(): Boolean {
        if (submode != ProgSubmode.EDIT) return false
        if (editBuffer.isBlank()) return false
        val ok = store.parseAndCommit(selectedSlot, editBuffer)
        if (ok) {
            editBuffer = ""
            editCursor = 0
            // Keep scroll near the end if appropriate
            val totalLines = store.getLines(selectedSlot).size
            if (totalLines > 6) {
                scrollLineIdx = (totalLines - 6).coerceAtLeast(0)
            }
        }
        return ok
    }

    fun moveCursorLeft() {
        if (submode == ProgSubmode.EDIT) {
            editCursor = (editCursor - 1).coerceAtLeast(0)
        }
    }

    fun moveCursorRight() {
        if (submode == ProgSubmode.EDIT) {
            editCursor = (editCursor + 1).coerceAtMost(editBuffer.length)
        }
    }

    /** HOME: jumps the EDIT cursor to the start of the current line. */
    fun moveCursorHome() {
        if (submode == ProgSubmode.EDIT) {
            editCursor = 0
        }
    }

    /** END: jumps the EDIT cursor to the end of the current line. */
    fun moveCursorEnd() {
        if (submode == ProgSubmode.EDIT) {
            editCursor = editBuffer.length
        }
    }

    fun scrollUp() {
        if (submode == ProgSubmode.EDIT) {
            scrollLineIdx = (scrollLineIdx - 1).coerceAtLeast(0)
        }
    }

    fun scrollDown() {
        if (submode == ProgSubmode.EDIT) {
            val total = store.getLines(selectedSlot).size
            scrollLineIdx = (scrollLineIdx + 1).coerceAtMost((total - 1).coerceAtLeast(0))
        }
    }

    fun clearOrReturnToSelect(): Boolean {
        return when (submode) {
            ProgSubmode.SELECT -> false // handled by caller (exit to calc)
            ProgSubmode.EDIT -> {
                if (editBuffer.isNotEmpty()) {
                    editBuffer = ""
                    editCursor = 0
                    true
                } else {
                    submode = ProgSubmode.SELECT
                    true
                }
            }
            ProgSubmode.RUN -> {
                submode = ProgSubmode.EDIT
                true
            }
        }
    }

    /**
     * Free program/DATA memory for the PROG status area, e.g. `21456 Free`.
     *
     * The value comes from the authoritative [Fx7000gMemory] model. Until
     * `ProgramStore` allocates program lines from it (a later phase) this reports
     * the full capacity.
     */
    fun freeMemoryLabel(): String = "${memory.freeProgramDataBytes()} Free"
}

