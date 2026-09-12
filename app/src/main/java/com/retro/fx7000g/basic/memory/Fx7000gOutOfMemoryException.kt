package com.retro.fx7000g.basic.memory

/**
 * Calculator-style `Om ERROR`: the virtual calculator RAM is exhausted.
 *
 * This is deliberately *not* the JVM's [OutOfMemoryError]. The simulator models
 * the finite RAM of a real calculator, so running out of virtual memory is a
 * normal, deterministic, user-visible condition that the future BASIC runtime
 * is expected to catch and display.
 *
 * @param requestedBytes bytes the caller asked for.
 * @param availableBytes bytes that were actually free at the time.
 * @param owner the category the failed allocation belonged to.
 */
class Fx7000gOutOfMemoryException(
    val requestedBytes: Int,
    val availableBytes: Int,
    val owner: MemoryOwner
) : Exception(
    "Om ERROR - cannot allocate $requestedBytes byte(s) for $owner; " +
        "only $availableBytes byte(s) free"
)
