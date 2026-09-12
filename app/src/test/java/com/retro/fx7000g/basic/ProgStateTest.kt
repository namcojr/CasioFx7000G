package com.retro.fx7000g.basic

import com.retro.fx7000g.basic.memory.Fx7000gMemory
import com.retro.fx7000g.basic.memory.MemoryOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Tests for the memory wiring on [ProgState]. These cover the value shown by the
 * PROG SELECT status line (`prog.freeMemoryLabel()`); the actual drawing is a
 * private UI concern in `LcdDisplay`.
 */
class ProgStateTest {

    @Test
    fun defaultsToFreshMemoryAtFullCapacity() {
        val prog = ProgState()
        assertEquals(Fx7000gMemory.DEFAULT_CAPACITY_BYTES, prog.memory.freeProgramDataBytes())
        assertEquals("21456 Free", prog.freeMemoryLabel())
    }

    @Test
    fun freeMemoryLabelReflectsInjectedMemoryModel() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(250, MemoryOwner.PROGRAM)

        val prog = ProgState(memory = memory)

        assertSame(memory, prog.memory)
        assertEquals(750, prog.memory.freeProgramDataBytes())
        assertEquals("750 Free", prog.freeMemoryLabel())
    }
}
