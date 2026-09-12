# FX-880P BASIC Simulator — Phase 3a: Unified Virtual Memory

## Objective

Implement the finite virtual memory model for the FX-880P BASIC simulator. You might see fx7000g referred occasionally because we are simulating an fx7000g but adding BASIC programming capabilities based on FX-880P BASIC.

This phase comes **after the BASIC tokenizer and listing formatter** and **before the BASIC parser**.

The goal is NOT to implement the parser, executor, or full BASIC semantics yet.

The goal is to establish the memory architecture that all later BASIC components will use.

The simulator must behave as if it has a finite amount of calculator RAM. Program text, variables, DATA, and runtime stack allocations must ultimately consume space from this same finite virtual memory budget. CALC mode doesn't care about this PROG memory map. CALC variables are independent and should stay implemented as they are.

Do NOT use unlimited JVM collections and merely report a fake "free memory" value.

---

# 1. Important design principle

The simulator is emulating a calculator with finite RAM.

Therefore:

- BASIC programs consume RAM.
- Variables consume RAM.
- Runtime stacks consume RAM.
- DATA consumes RAM.
- Temporary/runtime allocations may consume RAM where appropriate.
- Releasing/deleting programs must return memory.
- Clearing variables must return variable memory.
- Runtime stack frames must consume memory while active.
- Memory exhaustion must be detectable and deterministic.

The implementation may use normal Kotlin data structures internally, but their logical storage must be backed by the virtual memory allocator.

For example:

    10 PRINT B

must consume program memory.

If the program executes:

    A=10

then the variable storage for A must consume variable memory.

If a FOR loop creates a runtime stack frame, that stack frame must consume runtime memory while the loop is active.

All of this belongs to the same finite virtual RAM model.

---

# 2. Reference hardware target

The standard FX-880P has approximately:

    21,456 bytes

of user-available RAM.

Use:

    21456

as the default virtual user RAM capacity.

Do NOT hard-code assumptions that prevent a future expanded-memory configuration.

The architecture should allow:

    Fx7000gMemory(capacity = 21456)

and potentially:

    Fx7000gMemory(capacity = 54224)

or another capacity later.

The exact physical memory map does not need to be emulated byte-for-byte at the machine level in this phase.

However, the externally observable allocation model must remain finite.

---

# 3. Memory architecture

Create a dedicated memory subsystem.

Suggested package:

    com.retro.fx7000g.basic.memory

Suggested components:

    Fx7000gMemory
    MemoryRegion
    MemoryAllocation
    MemoryException / MemoryError
    MemoryOwner
    MemoryStats

Names may be adjusted to fit the existing project conventions.

Do not blindly create every class listed above if a simpler design is cleaner.

The important requirement is separation of responsibilities.

---

# 4. Unified memory

There must be ONE authoritative memory manager.

Conceptually:

                    FX-7000G USER RAM
                   ┌─────────────────┐
                   │                 │
                   │  Program P0     │
                   │  Program P1     │
                   │  ...            │
                   │  Program P9     │
                   │                 │
                   │  DATA           │
                   │                 │
                   │  Variables      │
                   │                 │
                   │  Runtime stack  │
                   │                 │
                   │  Other runtime  │
                   │                 │
                   └─────────────────┘

All consumers ultimately allocate from the same finite capacity.

Do NOT create independent unlimited pools such as:

    programMemory = unlimited
    variableMemory = unlimited
    stackMemory = unlimited

That would defeat the purpose.

---

# 5. Logical regions vs physical allocation

The implementation may expose logical categories such as:

    PROGRAM
    DATA
    VARIABLES
    RUNTIME_STACK
    TEMPORARY

but these are accounting/ownership categories over the same finite memory.

The allocator is authoritative.

Example:

    capacity = 21456

If programs occupy:

    12000

variables occupy:

    4000

and runtime stack occupies:

    1000

then:

    used = 17000
    free = 4456

There must never be a situation where each subsystem independently believes it has the full 21456 bytes available.

---

# 6. Allocation API

Provide a clean API for allocating and releasing memory.

For example:

    val allocation = memory.allocate(
        size = 128,
        owner = MemoryOwner.PROGRAM
    )

and:

    memory.release(allocation)

The exact API is up to the implementation.

Requirements:

- Allocation must fail when insufficient memory exists.
- Failure must be deterministic.
- Allocation must never silently exceed capacity.
- Releasing an allocation must return the bytes to the free pool.
- Releasing the same allocation twice must be detected or safely prevented.
- Negative allocation sizes must be rejected.
- Zero-byte allocations should either be explicitly supported or rejected consistently.
- Integer overflow must not be possible.

---

# 7. Allocation identity

An allocation should have an identity/token/handle rather than simply subtracting bytes from a counter.

For example:

    MemoryAllocation(
        id = ...,
        size = ...,
        owner = ...
    )

This allows later components to correctly release exactly what they allocated.

Do not implement:

    used -= 100

without tracking what the 100 bytes represented.

---

# 8. Memory statistics

The memory subsystem must expose enough information for future BASIC commands such as:

    FRE(0)
    FRE(1)
    FRE(2)

At minimum expose:

    totalBytes
    usedBytes
    freeBytes

Also expose category usage, e.g.:

    programBytes
    dataBytes
    variableBytes
    runtimeStackBytes
    temporaryBytes

Do not yet implement the BASIC `FRE()` function itself unless doing so is trivial.

This phase provides the memory information that the later BASIC runtime will use.

---

# 9. FRE semantics

The real FX-880P distinguishes memory categories.

The important conceptual meanings are:

    FRE(0)
        currently unused variable area

    FRE(1)
        free program/DATA area

    FRE(2)
        total variable-area capacity

The exact historical implementation involves a more complicated memory layout and CLEAR-dependent variable area.

We do NOT need to reproduce the physical memory map yet.

However, design the API so that the future BASIC implementation can ask:

    memory.freeVariableBytes()
    memory.freeProgramDataBytes()
    memory.variableCapacity()

without rewriting the memory subsystem.

Do not fake these values with constants.

They must be derived from actual allocations.

---

# 10. Program memory

The future `ProgramStore` will store programs P0 through P9.

This memory phase must provide the accounting foundation for it.

Each stored BASIC line consumes virtual RAM.

For example:

    10 PRINT B

must have a calculable memory cost.

Do NOT simply use:

    source.length

as the definitive storage size.

The FX-880P stores tokenized BASIC and commands/functions have different storage costs.

The known accounting rules include approximately:

- line number: 2 bytes
- command/token: 2 bytes
- function/token: 2 bytes
- ordinary alphanumeric characters/spaces: 1 byte
- additional line termination/control information

The exact accounting rules must be isolated in a dedicated component so they can be refined against the FX-880P manual without rewriting `Fx880pMemory`.

Suggested concept:

    BasicProgramMemorySizer

or:

    BasicTokenMemorySizer

This component should accept the tokenized representation produced by `BasicTokenizer`.

Example conceptual API:

    val bytes = programMemorySizer.size(tokens)

Do NOT duplicate memory-size calculations in ProgramStore.

---

# 11. Important: tokenizer vs memory sizing

The tokenizer is responsible for lexical representation.

The memory sizer is responsible for calculating how much FX-880P-style storage that representation requires.

Do NOT put memory accounting logic inside:

    BasicTokenizer

Do NOT modify the tokenizer merely to make memory accounting easier.

Keep:

    source
      ↓
    tokenizer
      ↓
    BasicToken[]
      ↓
    memory sizer
      ↓
    byte count

as separate responsibilities.

---

# 12. Program allocation model

The future ProgramStore should be able to do something conceptually like:

    programStore.addLine(
        program = P0,
        line = 10,
        tokens = ...
    )

and internally:

1. calculate the required storage size;
2. reserve that amount from `Fx7000gMemory`;
3. store the logical representation;
4. associate the allocation with the program line.

Deleting/replacing a line must release the old allocation.

For example:

    10 PRINT "HELLO"

is replaced by:

    10 PRINT "HELLO WORLD"

The old line allocation must be released before or as part of replacing it, with correct handling if the new allocation cannot fit.

Avoid leaving "ghost" allocations.

---

# 13. Atomic replacement

Program-line replacement should eventually be safe.

If the old line uses:

    100 bytes

and the replacement needs:

    150 bytes

the operation must not leave the program corrupted if only 120 bytes are available.

Prefer a transactional approach:

1. calculate new size;
2. determine additional memory required;
3. verify that allocation is possible;
4. perform the replacement;
5. release the old allocation at the appropriate point.

The exact implementation can be deferred to ProgramStore, but the memory API must make this possible.

---

# 14. Program deletion

Deleting a program line must eventually release its allocation.

Deleting an entire (active) program:

    NEW

or equivalent program clearing must eventually release all allocations belonging to that program.

Deleting P0 must NOT affect P1–P9 allocations.

The memory subsystem should therefore support ownership/grouping well enough for a future ProgramStore to release all allocations belonging to one program.

Deleting ALL programs:

    NEW ALL

Same as above but it will affect P0-P9. Does not ask for confirmation.
---

# 15. Variables

Variables must eventually consume virtual RAM.

This phase does NOT implement BASIC variables.

However, the memory model must support variable allocations.

For example:

    A = 10

may require storage for:

- variable table entry
- variable value

and:

    A$

may require appropriate string-variable storage.

The exact FX-880P variable representation should be implemented later.

For now provide a generic allocation category:

    MemoryOwner.VARIABLE

or equivalent.

Do not invent a detailed variable byte layout unless it is required by the allocator architecture.

---

# 16. Runtime stack

The future BASIC executor will require runtime stack allocations for things such as:

- FOR/NEXT
- GOSUB/RETURN
- possibly other control-flow state

These allocations must eventually consume virtual RAM.

The memory subsystem therefore needs a:

    RUNTIME_STACK

category.

The real FX-880P has documented stack costs, including examples such as:

- FOR stack entry: approximately 26 bytes
- GOSUB stack entry: approximately 8 bytes

Do not hard-code these values into the generic allocator.

Later runtime-stack components should request the required number of bytes.

For example:

    val frame = memory.allocate(
        size = 26,
        owner = MemoryOwner.RUNTIME_STACK
    )

Then:

    memory.release(frame)

when the FOR frame disappears.

---

# 17. Runtime stack exhaustion

The future executor must be able to receive a memory error when nested control flow exhausts available RAM.

Example:

    FOR I=1 TO ...
        GOSUB ...
    NEXT I

must eventually fail if the runtime stack cannot allocate another frame.

The memory subsystem must make this straightforward.

Do not silently grow a Kotlin stack/list beyond the virtual RAM limit.

---

# 18. DATA

DATA storage is part of the program/data memory side of the calculator.

The architecture should therefore support:

    MemoryOwner.DATA

or allow DATA to share:

    PROGRAM_DATA

as appropriate.

The exact DATA representation belongs to the parser/runtime/program-storage phases.

Do not implement DATA parsing here.

Just make sure the memory model can distinguish DATA from variable memory for future FRE semantics.

---

# 19. Fragmentation

Do not prematurely implement a byte-perfect physical allocator unless required.

For this phase, the primary requirement is correct finite capacity and accounting.

However, choose an architecture that does not make fragmentation impossible to model later.

There are two acceptable approaches:

### Option A — logical allocation accounting

Track allocations and total used bytes.

This is acceptable for the first implementation if the real FX-880P does not require physical address-level behavior for any current feature.

### Option B — virtual address allocator

Track contiguous virtual blocks:

    start + size

This is more faithful and can support fragmentation.

Either is acceptable now.

If implementing Option B, do not expose unnecessary complexity to callers.

The public API should remain simple.

---

# 20. Do not implement a fake garbage collector

Do not make memory automatically disappear merely because a Kotlin object is no longer referenced.

Virtual calculator memory is explicit.

The owning subsystem must explicitly release its allocations.

This is important because later BASIC behavior depends on deterministic memory usage.

---

# 21. CLEAR

Do NOT implement the BASIC `CLEAR` command in this phase.

However, the memory API must make it possible for the future implementation to:

- release variable allocations;
- reset runtime state;
- potentially change the variable-area allocation/capacity according to FX-880P semantics.

Do not hard-code `CLEAR` behavior into the generic memory manager.

---

# 22. Program vs variable capacity

The real calculator has a concept of variable-area allocation and program/DATA area.

Do not reduce everything to a single:

    freeBytes

field and stop there.

The underlying allocator is unified, but the logical API should support future region semantics.

Conceptually:

    total RAM
       |
       +-- variable area
       |
       +-- program/DATA area

while recognizing that both ultimately belong to the same finite RAM.

The exact dynamic partitioning behavior can be implemented in the later `CLEAR`/runtime phase.

For now expose enough information to support it.

---

# 23. Thread safety

The BASIC interpreter is currently effectively single-threaded.

Do NOT add unnecessary concurrency complexity.

A simple non-thread-safe memory manager is acceptable unless the existing architecture already requires synchronization.

Prioritize deterministic behavior and testability.

---

# 24. Errors

Create a specific memory exception/error type.

For example:

    Fx7000gOutOfMemoryException

or an equivalent sealed error model.

It should contain useful information such as:

    requested
    available
    owner/category

Example conceptual message:

    Om ERROR

Do not expose JVM `OutOfMemoryError`, use actual FX-880P errors where possible.

This is simulated calculator memory, not host JVM memory.

---

# 25. Tests

This phase must be heavily unit-tested.

At minimum test:

### Basic allocation

- allocate 100 bytes;
- used becomes 100;
- free decreases accordingly.

### Multiple allocations

- allocate several categories;
- total used equals sum;
- free equals capacity minus used.

### Full memory

Allocate exactly the total capacity.

Then:

    free == 0

A further allocation must fail.

### Oversized allocation

Request more than total capacity.

Must fail without changing memory state.

### Release

Allocate 100 bytes.

Release it.

Memory must return to the original state.

### Double release

Releasing the same allocation twice must be detected or safely ignored according to the chosen contract.

Test the chosen behavior.

### Invalid sizes

Test:

    -1

and any other invalid values.

### Category accounting

Allocate:

    PROGRAM = 100
    VARIABLE = 50
    RUNTIME_STACK = 20

Verify:

    programBytes == 100
    variableBytes == 50
    runtimeStackBytes == 20
    usedBytes == 170

### Program memory sizing

Create token sequences representing simple BASIC lines and verify the calculated storage size.

Do NOT merely test Kotlin string length.

Test commands/functions separately from ordinary characters.

### Replacement scenario

Simulate:

    old allocation = 100
    new allocation = 150

Verify that memory accounting remains correct.

### Memory failure preservation

Attempt an allocation that cannot fit.

Verify that:

- existing allocations remain intact;
- usedBytes does not change;
- freeBytes does not change.

---

# 26. Tests should not depend on the future parser

The memory phase must be independently testable.

Do NOT wait for:

    BasicParser

or:

    BasicExecutor

to exist.

Construct token lists directly in tests where necessary.

The memory subsystem should compile and pass its tests before the parser exists.

---

# 27. Integration constraints

This phase must NOT:

- implement BasicParser;
- implement BasicExecutor;
- implement BASIC variable semantics;
- implement RUN;
- implement LIST;
- implement EDIT;
- implement CLEAR;
- modify CalculatorState unnecessarily;
- modify keyboard mappings;
- modify CAL behavior;
- introduce a second BASIC tokenizer;
- put parsing logic into the memory subsystem.

Keep the phase isolated.

---

# 28. Existing tokenizer

The project already contains:

    BasicTokenizer

and the BASIC token hierarchy.

Use the existing token types.

Do NOT create duplicate token types merely for memory accounting.

If the current token model is insufficient for an exact storage calculation, report the limitation rather than redesigning the tokenizer unnecessarily.

---

# 29. Existing formatter

The project also contains the BASIC listing formatter.

Do NOT make the formatter responsible for memory accounting.

LIST formatting and memory storage are different concerns.

The pipeline should remain conceptually:

    BASIC source
        ↓
    BasicTokenizer
        ↓
    BasicToken[]
        ├── BasicListingFormatter
        ├── BasicProgramMemorySizer
        └── future BasicParser

---

# 30. Suggested architecture

A reasonable architecture is:

    Fx880pMemory
        │
        ├── allocations
        │
        ├── totalBytes
        ├── usedBytes
        ├── freeBytes
        └── category accounting
              │
              ├── PROGRAM
              ├── DATA
              ├── VARIABLE
              ├── RUNTIME_STACK
              └── TEMPORARY


    BasicProgramMemorySizer
        │
        └── BasicToken[] → byte count


Future:

    ProgramStore
        │
        └── Fx7000gMemory.allocate(...)

Future:

    VariableStore
        │
        └── Fx7000gMemory.allocate(...)

Future:

    BasicRuntimeStack
        │
        └── Fx7000gMemory.allocate(...)


Everything ultimately shares:

    Fx7000gMemory

---

# 31. Important architectural distinction

Do NOT interpret "unified memory" as requiring every subsystem to become one giant class.

The correct design is:

    unified physical/virtual memory
    +
    independent logical subsystems

For example:

    ProgramStore
    VariableStore
    RuntimeStack

are still separate classes because they have different semantics.

They simply cannot allocate outside the shared:

    Fx7000gMemory

---

# 32. Memory sizing accuracy

Be conservative about claims of exact FX-880P byte accounting.

Where the manual specifies a rule, implement that rule.

Where the exact rule is unclear, isolate it in the memory-sizing component rather than spreading assumptions throughout the code.

The architecture must make it easy to refine:

    BasicProgramMemorySizer

later when comparing against actual FX-880P behavior.

Do NOT "optimize" memory usage.

The objective is calculator authenticity, not JVM efficiency.

---

# 33. Default memory instance

Do not create global mutable singleton state unless the existing application architecture already requires it.

Prefer dependency injection / ownership.

For example:

    val memory = Fx7000gMemory(21456)

Then the future BASIC environment can own:

    memory
    programStore
    variableStore
    runtime

This will also make tests deterministic.

---

# 34. No parser yet

STOP after completing the memory subsystem.

Do not proceed into:

- AST
- expressions
- statements
- IF parsing
- FOR parsing
- GOTO resolution
- variable evaluation
- execution

Those belong to the next phase.

---

# 35. Deliverables

Implement:

1. Unified finite `Fx7000gMemory`.
2. Allocation/release mechanism.
3. Allocation ownership/category tracking.
4. Memory statistics.
5. Calculator-specific out-of-memory error.
6. Program/token memory sizing component.
7. Unit tests for all of the above.
8. Minimal documentation/comments explaining the memory model.

The implementation should compile cleanly.

Run the relevant unit tests.

If there are unrelated pre-existing failures, identify them separately rather than changing unrelated code.

---

# 36. Acceptance criteria

The phase is complete when all of the following are true:

- [ ] Default capacity is 21,456 bytes.
- [ ] Capacity is configurable.
- [ ] One authoritative finite memory manager exists.
- [ ] All allocations consume from the same total capacity.
- [ ] Allocations have identities/handles.
- [ ] Allocations can be released deterministically.
- [ ] Memory exhaustion produces a calculator-specific error.
- [ ] Category accounting works.
- [ ] Program memory can be sized from `BasicToken[]`.
- [ ] Program-memory accounting is separate from tokenization.
- [ ] Variable allocation can be represented.
- [ ] Runtime-stack allocation can be represented.
- [ ] DATA allocation can be represented.
- [ ] `FRE()`-style information can be obtained later without redesigning the memory manager.
- [ ] No parser or executor has been implemented.
- [ ] No unrelated calculator behavior has been modified.
- [ ] Unit tests cover normal allocation, exhaustion, release, category accounting and program-memory sizing.
- [ ] Existing tokenizer/formatter tests remain passing.

---

# 37. Final architectural goal

After this phase, the BASIC subsystem should have the following foundation:

    source
      │
      ▼
    BasicTokenizer
      │
      ▼
    BasicToken[]
      │
      ├───────────────┐
      ▼               ▼
    Formatter      MemorySizer
      │               │
      ▼               ▼
     LIST         byte requirement
                      │
                      ▼
                Fx7000gMemory
                      ▲
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
      Program      Variables    Runtime
       Store                     Stack

The parser will be built on top of this foundation in the next phase.

Do not implement the parser in this phase.
An FX-880P manual was added inside /docs in PDF format for reference
docs/Casio fx-880P Owners Manual.pdf
