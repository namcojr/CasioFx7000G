package com.retro.fx7000g.basic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramStoreTest {

    @Test
    fun initiallyEmpty() {
        val store = ProgramStore()
        for (i in 0..9) {
            assertFalse(store.isOccupied(i))
            assertTrue(store.getLines(i).isEmpty())
        }
        assertEquals("P0123456789", store.occupiedMask())
    }

    @Test
    fun insertAndSortLines() {
        val store = ProgramStore()
        store.setLine(0, 30, "END")
        store.setLine(0, 10, "PRINT \"HELLO\"")
        store.setLine(0, 20, "A=10")

        assertTrue(store.isOccupied(0))
        assertFalse(store.isOccupied(1))

        val lines = store.getLines(0)
        assertEquals(3, lines.size)
        assertEquals(10 to "PRINT \"HELLO\"", lines[0])
        assertEquals(20 to "A=10", lines[1])
        assertEquals(30 to "END", lines[2])
    }

    @Test
    fun replaceLine() {
        val store = ProgramStore()
        store.setLine(0, 10, "PRINT \"HELLO\"")
        store.setLine(0, 10, "PRINT \"WORLD\"")

        val lines = store.getLines(0)
        assertEquals(1, lines.size)
        assertEquals(10 to "PRINT \"WORLD\"", lines[0])
    }

    @Test
    fun deleteLine() {
        val store = ProgramStore()
        store.setLine(0, 10, "PRINT \"HELLO\"")
        store.setLine(0, 20, "END")
        store.deleteLine(0, 10)

        val lines = store.getLines(0)
        assertEquals(1, lines.size)
        assertEquals(20 to "END", lines[0])
    }

    @Test
    fun clearSlot() {
        val store = ProgramStore()
        store.setLine(2, 10, "A=1")
        assertTrue(store.isOccupied(2))

        store.clearSlot(2)
        assertFalse(store.isOccupied(2))
        assertTrue(store.getLines(2).isEmpty())
    }

    @Test
    fun occupiedMaskUpdates() {
        val store = ProgramStore()
        store.setLine(1, 10, "REM")
        store.setLine(5, 10, "REM")
        assertEquals("P0*234*6789", store.occupiedMask())
    }

    @Test
    fun parseAndCommitInsertsAndDeletes() {
        val store = ProgramStore()
        assertTrue(store.parseAndCommit(0, "10 PRINT \"HI\""))
        assertTrue(store.parseAndCommit(0, "20 GOTO 10"))
        assertEquals(2, store.getLines(0).size)

        // Empty statement deletes line
        assertTrue(store.parseAndCommit(0, "10"))
        assertEquals(1, store.getLines(0).size)
        assertEquals(20 to "GOTO 10", store.getLines(0)[0])

        // Invalid line format returns false
        assertFalse(store.parseAndCommit(0, "NO_LINE_NUMBER"))
        assertFalse(store.parseAndCommit(0, ""))
    }
}

