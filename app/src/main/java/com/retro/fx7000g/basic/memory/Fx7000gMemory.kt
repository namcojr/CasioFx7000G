package com.retro.fx7000g.basic.memory

/**
 * The single authoritative finite memory manager for the FX-7000G BASIC
 * environment.
 *
 * The simulator behaves as though the calculator has a fixed amount of user RAM
 * (21,456 bytes on a standard FX-880P, see [DEFAULT_CAPACITY_BYTES]). Program
 * text, DATA, variables and runtime stack frames **all** allocate from this one
 * pool, so no subsystem can pretend it owns an unlimited playground of its own.
 *
 * ```
 * val memory = Fx7000gMemory(21456)
 * val allocation = memory.allocate(size = 12, owner = MemoryOwner.PROGRAM)
 * ...
 * memory.release(allocation)
 * ```
 *
 * [MemoryOwner] categories are only a *view* over the one pool; they never add
 * capacity. Use [stats] (or the individual byte properties) to drive a future
 * `FRE()`/status display. The manager is deliberately **not** thread-safe: the
 * BASIC interpreter is effectively single-threaded, and deterministic, explicit
 * accounting matters more here than concurrency machinery.
 *
 * @param capacityBytes total user RAM modelled, in bytes. Defaults to the
 *   standard FX-880P's [DEFAULT_CAPACITY_BYTES]; pass a larger value for a
 *   future expanded-memory configuration.
 */
class Fx7000gMemory(
    val capacityBytes: Int = DEFAULT_CAPACITY_BYTES
) {
    init {
        require(capacityBytes > 0) { "capacityBytes must be positive, was $capacityBytes" }
    }

    /** Live allocations keyed by id, in allocation order (keeps iteration deterministic). */
    private val allocations = LinkedHashMap<Long, MemoryAllocation>()

    /** Bytes per [MemoryOwner], indexed by ordinal. */
    private val bytesByOwner = IntArray(MemoryOwner.entries.size)

    /** Next handle to hand out; a [Long] so it cannot realistically wrap. */
    private var nextId = 1L

    /** Sum of the sizes of all live allocations. */
    private var used = 0

    /** Total modelled capacity in bytes. */
    val totalBytes: Int get() = capacityBytes

    /** Bytes currently reserved by live allocations. */
    val usedBytes: Int get() = used

    /** Bytes still available for allocation. */
    val freeBytes: Int get() = capacityBytes - used

    /** Number of live allocations. */
    val allocationCount: Int get() = allocations.size

    /** Bytes currently held by [MemoryOwner.PROGRAM] allocations. */
    val programBytes: Int get() = bytesFor(MemoryOwner.PROGRAM)

    /** Bytes currently held by [MemoryOwner.DATA] allocations. */
    val dataBytes: Int get() = bytesFor(MemoryOwner.DATA)

    /** Bytes currently held by [MemoryOwner.VARIABLE] allocations. */
    val variableBytes: Int get() = bytesFor(MemoryOwner.VARIABLE)

    /** Bytes currently held by [MemoryOwner.RUNTIME_STACK] allocations. */
    val runtimeStackBytes: Int get() = bytesFor(MemoryOwner.RUNTIME_STACK)

    /** Bytes currently held by [MemoryOwner.TEMPORARY] allocations. */
    val temporaryBytes: Int get() = bytesFor(MemoryOwner.TEMPORARY)

    /** Bytes currently held by [owner]. */
    fun bytesFor(owner: MemoryOwner): Int = bytesByOwner[owner.ordinal]

    /**
     * Reserves [sizeBytes] bytes for [owner] and returns a handle to them.
     *
     * @param group optional opaque tag used by [releaseGroup].
     * @throws IllegalArgumentException if [sizeBytes] is not positive. (Zero-byte
     *   allocations are rejected consistently rather than half-supported.)
     * @throws Fx7000gOutOfMemoryException if the request does not fit; the
     *   memory state is left completely untouched in that case.
     */
    fun allocate(
        sizeBytes: Int,
        owner: MemoryOwner,
        group: Any? = null
    ): MemoryAllocation {
        require(sizeBytes > 0) { "Allocation size must be positive, was $sizeBytes" }
        // Overflow-safe comparison: freeBytes is always in 0..capacityBytes.
        if (sizeBytes > freeBytes) {
            throw Fx7000gOutOfMemoryException(sizeBytes, freeBytes, owner)
        }
        val allocation = MemoryAllocation(nextId++, sizeBytes, owner, group)
        allocations[allocation.id] = allocation
        bytesByOwner[owner.ordinal] += sizeBytes
        used += sizeBytes
        return allocation
    }

    /**
     * Releases [allocation], returning its bytes to the free pool.
     *
     * @return `true` if the allocation was live and has now been released, or
     *   `false` if it had already been released (or never belonged here). The
     *   `false` result makes a double release safe: it can never corrupt the
     *   accounting by subtracting the same block twice.
     * @throws IllegalArgumentException if [allocation] carries the id of a live
     *   allocation but is not actually that allocation (e.g. it came from a
     *   different [Fx7000gMemory] instance).
     */
    fun release(allocation: MemoryAllocation): Boolean {
        val stored = allocations[allocation.id] ?: return false
        require(stored == allocation) {
            "Allocation ${allocation.id} does not belong to this manager"
        }
        allocations.remove(allocation.id)
        bytesByOwner[stored.owner.ordinal] -= stored.sizeBytes
        used -= stored.sizeBytes
        return true
    }

    /** True while [allocation] is still reserved in this manager. */
    fun isAllocated(allocation: MemoryAllocation): Boolean =
        allocations[allocation.id] == allocation

    /**
     * Releases every live allocation tagged with [group] (see
     * [MemoryAllocation.group]) and returns how many were released.
     *
     * A future `ProgramStore` can use this to drop all lines of one program
     * (e.g. `releaseGroup(slot)`) in a single deterministic operation.
     */
    fun releaseGroup(group: Any?): Int {
        var released = 0
        val iterator = allocations.values.iterator()
        while (iterator.hasNext()) {
            val allocation = iterator.next()
            if (allocation.group == group) {
                iterator.remove()
                bytesByOwner[allocation.owner.ordinal] -= allocation.sizeBytes
                used -= allocation.sizeBytes
                released++
            }
        }
        return released
    }

    /**
     * Releases every live allocation owned by [owner] and returns the count.
     *
     * This is the primitive a future `CLEAR`/runtime reset will use to free the
     * variable area without disturbing programs.
     */
    fun releaseAll(owner: MemoryOwner): Int {
        var released = 0
        val iterator = allocations.values.iterator()
        while (iterator.hasNext()) {
            val allocation = iterator.next()
            if (allocation.owner == owner) {
                iterator.remove()
                bytesByOwner[owner.ordinal] -= allocation.sizeBytes
                used -= allocation.sizeBytes
                released++
            }
        }
        return released
    }

    /** Releases everything, returning the manager to its initial state. */
    fun releaseAll() {
        allocations.clear()
        bytesByOwner.fill(0)
        used = 0
    }

    /** All live allocations for [owner], in allocation order. */
    fun allocationsFor(owner: MemoryOwner): List<MemoryAllocation> =
        allocations.values.filter { it.owner == owner }

    /** A snapshot of the current state, for memory-usage/`FRE()`-style reporting. */
    fun stats(): MemoryStats = MemoryStats(
        totalBytes = totalBytes,
        usedBytes = used,
        freeBytes = freeBytes,
        allocationCount = allocations.size,
        programBytes = programBytes,
        dataBytes = dataBytes,
        variableBytes = variableBytes,
        runtimeStackBytes = runtimeStackBytes,
        temporaryBytes = temporaryBytes
    )

    // --- FX-880P FRE()-style views -------------------------------------------
    //
    // The allocator is unified, so with no CLEAR partition yet both logical
    // "areas" grow into the same free pool. These accessors exist - and are
    // derived from live allocations, never hard-coded constants - so the future
    // BASIC FRE() implementation will not force the manager to be redesigned.
    // Once CLEAR partitions the variable area, only these three functions
    // change; the allocator does not.

    /** Unused variable-area bytes, i.e. `FRE(0)`. */
    fun freeVariableBytes(): Int = freeBytes

    /** Free program/DATA-area bytes, i.e. `FRE(1)`. */
    fun freeProgramDataBytes(): Int = freeBytes

    /** Current variable-area capacity in the unified model, i.e. `FRE(2)`. */
    fun variableCapacity(): Int = capacityBytes

    companion object {
        /**
         * Standard FX-880P user-available RAM in bytes. Use a different value
         * for an expanded-memory configuration.
         */
        const val DEFAULT_CAPACITY_BYTES = 21456
    }
}
