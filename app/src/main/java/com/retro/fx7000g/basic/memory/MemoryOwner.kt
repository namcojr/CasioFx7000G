package com.retro.fx7000g.basic.memory

/**
 * Logical ownership category for a [MemoryAllocation].
 *
 * These are accounting/ownership categories over *one* finite pool of
 * calculator RAM ([Fx7000gMemory]) - they are **not** independent memory pools.
 * Every category consumes bytes from the single authoritative allocator, and
 * the categories exist only so statistics (and a future `FRE()` implementation)
 * can report how that one pool is being used.
 */
enum class MemoryOwner {
    /** Tokenized BASIC program lines for the P0..P9 slots. */
    PROGRAM,

    /** DATA statement payloads; shares the program side of the memory map. */
    DATA,

    /** BASIC variable table entries and values (CALC variables are separate). */
    VARIABLE,

    /** Active runtime stack frames (FOR/NEXT, GOSUB/RETURN, ...). */
    RUNTIME_STACK,

    /** Short-lived runtime/editor scratch storage. */
    TEMPORARY
}
