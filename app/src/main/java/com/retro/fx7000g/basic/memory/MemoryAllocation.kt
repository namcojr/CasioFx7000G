package com.retro.fx7000g.basic.memory

/**
 * A handle to a live block of virtual calculator RAM.
 *
 * An allocation is an explicit identity rather than a bare byte count, so every
 * subsystem can release exactly what it reserved (see [Fx7000gMemory.release]).
 * The [id] is unique for the lifetime of a [Fx7000gMemory] instance.
 *
 * [group] is an optional, opaque tag. A future `ProgramStore` can pass its slot
 * index so it can drop every allocation belonging to one program through
 * [Fx7000gMemory.releaseGroup] without tracking each line separately.
 */
data class MemoryAllocation(
    val id: Long,
    val sizeBytes: Int,
    val owner: MemoryOwner,
    val group: Any? = null
) {
    init {
        require(sizeBytes > 0) { "Allocation size must be positive, was $sizeBytes" }
    }
}
