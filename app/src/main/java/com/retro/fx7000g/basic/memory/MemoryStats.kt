package com.retro.fx7000g.basic.memory

/**
 * A read-only snapshot of the virtual memory state, suitable for a future
 * `FRE()` or memory-usage status display.
 *
 * Obtain one with [Fx7000gMemory.stats]. All figures are derived from the live
 * allocations at the moment the snapshot was taken.
 */
data class MemoryStats(
    val totalBytes: Int,
    val usedBytes: Int,
    val freeBytes: Int,
    val allocationCount: Int,
    val programBytes: Int,
    val dataBytes: Int,
    val variableBytes: Int,
    val runtimeStackBytes: Int,
    val temporaryBytes: Int
) {
    /** PROGRAM + DATA, i.e. the FX-880P-style "program/DATA" area usage. */
    val programDataBytes: Int get() = programBytes + dataBytes

    /** Bytes currently held by [owner]. */
    fun bytesFor(owner: MemoryOwner): Int = when (owner) {
        MemoryOwner.PROGRAM -> programBytes
        MemoryOwner.DATA -> dataBytes
        MemoryOwner.VARIABLE -> variableBytes
        MemoryOwner.RUNTIME_STACK -> runtimeStackBytes
        MemoryOwner.TEMPORARY -> temporaryBytes
    }
}
