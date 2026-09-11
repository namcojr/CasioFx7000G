# FX-7000G BASIC — Implementation Roadmap

## Phase 0 — Specification - THIS IS DONE!

Status: PLANNING

Documents:

- SPEC.md
- LANGUAGE.md
- KEYBOARD.md
- DISPLAY.md
- TESTS.md

Tasks:

- audit existing FX-7000G keyboard
- confirm existing calculator modes
- inventory existing expression evaluator
- inventory existing display/font system
- compare proposed BASIC vocabulary with the FX-880P manual
- identify commands worth adopting
- finalize Tier-1 keyboard tokens

No BASIC implementation should begin until the keyboard architecture is understood.

## Phase 1 — BASIC mode/state infrastructure

Implement:

- PROG mode
- INPUT/RUN submodes
- program slot selection
- P0-P9 state
- occupied-slot tracking
- PRG status indicator
- keyboard layer abstraction

At the end of Phase 1, BASIC does not need to execute programs.

## Phase 2 — Program storage and editor

Implement:

- P0-P9 program objects
- numbered lines
- insertion
- replacement
- deletion
- cursor movement
- backspace/delete
- scrolling
- LIST

Target:

    10 PRINT "HELLO"
    20 A=10
    30 PRINT A

can be entered, edited and listed.

## Phase 3 — Tokenizer

Implement:

- keyword tokens
- numeric literals
- string literals
- identifiers
- operators
- punctuation
- line numbers

Dedicated keyboard commands must insert tokens directly.

## Phase 4 — Expression parser

Implement:

- numbers
- variables
- parentheses
- unary minus
- + - * / ^
- comparisons

Use a proper precedence-aware parser.

## Phase 5 — Minimal runtime

Implement in this order:

1. assignment
2. PRINT
3. END
4. GOTO
5. IF/THEN
6. FOR/NEXT
7. GOSUB/RETURN
8. STOP

First success criterion:

    10 A=10
    20 B=A*2
    30 PRINT B
    40 END

produces:

    20

## Phase 6 — INPUT and strings

Implement:

- INPUT
- string literals
- string variables
- basic string printing
- type checking

## Phase 7 — Display-aware commands

Implement:

- CLS
- LOCATE

Ensure they use the display abstraction rather than direct UI manipulation.

## Phase 8 — Graphics

Implement:

1. PLOT
2. LINE
3. DRAW
4. BOX
5. CIRCLE

Graphics should use the logical coordinate system in DISPLAY.md.

## Phase 9 — Mathematical functions

Expose existing FX-7000G mathematical functions to BASIC.

Avoid duplicating mathematical implementations unnecessarily.

## Phase 10 — Extended language

Only after the core system is enjoyable:

- DIM
- arrays
- DATA/READ/RESTORE
- additional string functions
- additional mathematical functions
- additional control-flow features
- more graphics
- compatibility conveniences

## Phase 11 — Polish

Implement:

- calculator-style errors
- BREAK handling
- CONT if useful
- program renaming if useful
- persistence
- memory usage display if useful
- improved editing
- documentation/help

## Agent workflow

### Planning/review agent

Use a lower-cost model for:

- keyboard layout review
- command categorization
- architecture criticism
- test-case generation
- finding inconsistencies

### Implementation agent

Use the stronger coding model for:

- actual code changes
- refactoring
- parser/runtime implementation
- integration with the existing project

### Human validation

After each milestone:

1. build
2. run existing tests
3. manually test the new feature
4. record regressions
5. update TESTS.md
6. only then move to the next phase

## Important rule

Do not implement the entire BASIC language in one pass.

Every phase should leave the calculator in a usable/buildable state.
