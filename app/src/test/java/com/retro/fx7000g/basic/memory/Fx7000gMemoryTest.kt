package com.retro.fx7000g.basic.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Fx7000gMemory]. These are independent of the future parser/runtime:
 * allocations are driven directly through the public API.
 */
class Fx7000gMemoryTest {

    // --- capacity -----------------------------------------------------------

    @Test
    fun defaultCapacityIs21456() {
        assertEquals(21456, Fx7000gMemory().totalBytes)
        assertEquals(21456, Fx7000gMemory.DEFAULT_CAPACITY_BYTES)
    }

    @Test
    fun capacityIsConfigurable() {
        val expanded = Fx7000gMemory(54224)
        assertEquals(54224, expanded.totalBytes)
        assertEquals(54224, expanded.freeBytes)
        assertEquals(0, expanded.usedBytes)
    }

    @Test
    fun nonPositiveCapacityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Fx7000gMemory(0) }
        assertThrows(IllegalArgumentException::class.java) { Fx7000gMemory(-1) }
    }

    // --- basic allocation ---------------------------------------------------

    @Test
    fun allocateConsumesFreeMemory() {
        val memory = Fx7000gMemory(1000)
        val allocation = memory.allocate(sizeBytes = 100, owner = MemoryOwner.PROGRAM)

        assertEquals(100, allocation.sizeBytes)
        assertEquals(MemoryOwner.PROGRAM, allocation.owner)
        assertEquals(100, memory.usedBytes)
        assertEquals(900, memory.freeBytes)
        assertEquals(1, memory.allocationCount)
        assertTrue(memory.isAllocated(allocation))
    }

    @Test
    fun allocationsReceiveDistinctIdentities() {
        val memory = Fx7000gMemory(1000)
        val first = memory.allocate(10, MemoryOwner.PROGRAM)
        val second = memory.allocate(10, MemoryOwner.PROGRAM)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun multipleAllocationsSum() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(100, MemoryOwner.PROGRAM)
        memory.allocate(50, MemoryOwner.VARIABLE)
        memory.allocate(20, MemoryOwner.RUNTIME_STACK)

        assertEquals(170, memory.usedBytes)
        assertEquals(830, memory.freeBytes)
        assertEquals(3, memory.allocationCount)
    }

    // --- exhaustion ---------------------------------------------------------

    @Test
    fun fillingMemoryExactlyLeavesZeroFreeThenFails() {
        val memory = Fx7000gMemory(100)
        memory.allocate(100, MemoryOwner.PROGRAM)
        assertEquals(0, memory.freeBytes)

        val error = assertThrows(Fx7000gOutOfMemoryException::class.java) {
            memory.allocate(1, MemoryOwner.VARIABLE)
        }
        assertEquals(1, error.requestedBytes)
        assertEquals(0, error.availableBytes)
        assertEquals(MemoryOwner.VARIABLE, error.owner)
        // State is unchanged by the failed request.
        assertEquals(100, memory.usedBytes)
        assertEquals(1, memory.allocationCount)
    }

    @Test
    fun oversizedAllocationFailsWithoutChangingState() {
        val memory = Fx7000gMemory(100)
        val error = assertThrows(Fx7000gOutOfMemoryException::class.java) {
            memory.allocate(101, MemoryOwner.PROGRAM)
        }
        assertEquals(101, error.requestedBytes)
        assertEquals(100, error.availableBytes)
        assertEquals(0, memory.usedBytes)
        assertEquals(100, memory.freeBytes)
        assertEquals(0, memory.allocationCount)
    }

    @Test
    fun hugeAllocationDoesNotOverflow() {
        val memory = Fx7000gMemory(1000)
        assertThrows(Fx7000gOutOfMemoryException::class.java) {
            memory.allocate(Int.MAX_VALUE, MemoryOwner.PROGRAM)
        }
        assertEquals(0, memory.usedBytes)
    }

    @Test
    fun failedAllocationPreservesExistingAllocations() {
        val memory = Fx7000gMemory(100)
        val kept = memory.allocate(60, MemoryOwner.PROGRAM)

        assertThrows(Fx7000gOutOfMemoryException::class.java) {
            memory.allocate(50, MemoryOwner.VARIABLE)
        }

        assertEquals(60, memory.usedBytes)
        assertEquals(40, memory.freeBytes)
        assertEquals(60, memory.programBytes)
        assertEquals(0, memory.variableBytes)
        assertTrue(memory.isAllocated(kept))
    }

    // --- release ------------------------------------------------------------

    @Test
    fun releaseReturnsBytesToThePool() {
        val memory = Fx7000gMemory(1000)
        val allocation = memory.allocate(100, MemoryOwner.PROGRAM)

        assertTrue(memory.release(allocation))
        assertEquals(0, memory.usedBytes)
        assertEquals(1000, memory.freeBytes)
        assertEquals(0, memory.allocationCount)
        assertFalse(memory.isAllocated(allocation))
    }

    @Test
    fun doubleReleaseIsSafelyRejected() {
        val memory = Fx7000gMemory(1000)
        val allocation = memory.allocate(100, MemoryOwner.PROGRAM)

        assertTrue(memory.release(allocation))
        assertFalse(memory.release(allocation))
        assertEquals(0, memory.usedBytes)
        assertEquals(1000, memory.freeBytes)
    }

    @Test
    fun releasingAForeignAllocationWithACollidingIdIsRejected() {
        val a = Fx7000gMemory(1000)
        val b = Fx7000gMemory(1000)
        val fromA = a.allocate(100, MemoryOwner.PROGRAM)
        b.allocate(200, MemoryOwner.DATA) // same first id, different block

        assertThrows(IllegalArgumentException::class.java) { b.release(fromA) }
        assertEquals(200, b.usedBytes)
        assertEquals(100, a.usedBytes)
    }

    @Test
    fun invalidAllocationSizesAreRejected() {
        val memory = Fx7000gMemory(1000)
        assertThrows(IllegalArgumentException::class.java) {
            memory.allocate(0, MemoryOwner.PROGRAM)
        }
        assertThrows(IllegalArgumentException::class.java) {
            memory.allocate(-1, MemoryOwner.PROGRAM)
        }
        assertEquals(0, memory.usedBytes)
        assertEquals(1000, memory.freeBytes)
    }

    // --- category accounting ------------------------------------------------

    @Test
    fun categoryAccountingIsTrackedPerOwner() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(100, MemoryOwner.PROGRAM)
        memory.allocate(30, MemoryOwner.DATA)
        memory.allocate(50, MemoryOwner.VARIABLE)
        memory.allocate(20, MemoryOwner.RUNTIME_STACK)

        assertEquals(100, memory.programBytes)
        assertEquals(30, memory.dataBytes)
        assertEquals(50, memory.variableBytes)
        assertEquals(20, memory.runtimeStackBytes)
        assertEquals(0, memory.temporaryBytes)
        assertEquals(200, memory.usedBytes)
        assertEquals(800, memory.freeBytes)

        val stats = memory.stats()
        assertEquals(1000, stats.totalBytes)
        assertEquals(200, stats.usedBytes)
        assertEquals(800, stats.freeBytes)
        assertEquals(4, stats.allocationCount)
        assertEquals(130, stats.programDataBytes) // PROGRAM + DATA
        assertEquals(100, stats.bytesFor(MemoryOwner.PROGRAM))
        assertEquals(30, stats.bytesFor(MemoryOwner.DATA))
        assertEquals(50, stats.bytesFor(MemoryOwner.VARIABLE))
    }

    @Test
    fun releasingOneCategoryDoesNotAffectOthers() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(100, MemoryOwner.PROGRAM)
        val variable = memory.allocate(50, MemoryOwner.VARIABLE)

        assertTrue(memory.release(variable))

        assertEquals(100, memory.programBytes)
        assertEquals(0, memory.variableBytes)
        assertEquals(100, memory.usedBytes)
    }

    // --- replacement / atomicity --------------------------------------------

    @Test
    fun replacementScenarioKeepsAccountingConsistent() {
        val memory = Fx7000gMemory(1000)
        val old = memory.allocate(100, MemoryOwner.PROGRAM)
        memory.allocate(780, MemoryOwner.DATA) // leaves 120 free

        // The 150-byte replacement cannot fit yet: it must fail cleanly ...
        assertThrows(Fx7000gOutOfMemoryException::class.java) {
            memory.allocate(150, MemoryOwner.PROGRAM)
        }
        assertTrue(memory.isAllocated(old))
        assertEquals(880, memory.usedBytes)
        assertEquals(120, memory.freeBytes)

        // ... then, once the old block is released, it fits.
        assertTrue(memory.release(old))
        memory.allocate(150, MemoryOwner.PROGRAM)
        assertEquals(930, memory.usedBytes)
        assertEquals(70, memory.freeBytes)
    }

    // --- grouping / bulk release --------------------------------------------

    @Test
    fun releaseGroupReleasesOnlyMatchingAllocations() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(10, MemoryOwner.PROGRAM, group = 0)
        memory.allocate(20, MemoryOwner.PROGRAM, group = 0)
        memory.allocate(30, MemoryOwner.PROGRAM, group = 1)
        memory.allocate(40, MemoryOwner.PROGRAM) // ungrouped

        assertEquals(2, memory.releaseGroup(0))
        assertEquals(70, memory.usedBytes) // 30 + 40 remain
        assertEquals(930, memory.freeBytes)
        assertTrue(memory.allocationsFor(MemoryOwner.PROGRAM).none { it.group == 0 })
    }

    @Test
    fun releaseAllOfOwnerFreesThatCategory() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(10, MemoryOwner.VARIABLE)
        memory.allocate(20, MemoryOwner.VARIABLE)
        memory.allocate(30, MemoryOwner.PROGRAM)

        assertEquals(2, memory.releaseAll(MemoryOwner.VARIABLE))
        assertEquals(0, memory.variableBytes)
        assertEquals(30, memory.programBytes)
        assertEquals(30, memory.usedBytes)
    }

    @Test
    fun releaseAllResetsTheManager() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(10, MemoryOwner.PROGRAM)
        memory.allocate(20, MemoryOwner.VARIABLE)

        memory.releaseAll()

        assertEquals(0, memory.usedBytes)
        assertEquals(1000, memory.freeBytes)
        assertEquals(0, memory.allocationCount)
        val stats = memory.stats()
        assertEquals(0, stats.programBytes)
        assertEquals(0, stats.variableBytes)
        assertEquals(0, stats.programDataBytes)
    }

    // --- FRE()-style views --------------------------------------------------

    @Test
    fun freStyleViewsAreDerivedFromAllocations() {
        val memory = Fx7000gMemory(1000)
        memory.allocate(200, MemoryOwner.PROGRAM)
        memory.allocate(100, MemoryOwner.VARIABLE)

        assertEquals(700, memory.freeVariableBytes())
        assertEquals(700, memory.freeProgramDataBytes())
        assertEquals(1000, memory.variableCapacity())
    }
}

