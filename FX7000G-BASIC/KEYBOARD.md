# FX-7000G BASIC — Keyboard Specification

## 1. Goal

The existing FX-7000G tribute has 49 physical keys.

Three keys are already fundamental modifiers/control keys:

- SHIFT
- ALPHA
- EXE

The BASIC environment must therefore use **context-sensitive keyboard layers** rather than attempting to reproduce a larger physical keyboard.

## 2. Core rule

The physical keyboard never changes.

Its interpretation changes according to the current calculator state.

Initial conceptual layers:

    NORMAL
    SHIFT
    ALPHA

BASIC adds:

    PROG_INPUT
    PROG_INPUT_SHIFT
    PROG_INPUT_ALPHA
    PROG_RUN
    PROG_RUN_SHIFT
    PROG_RUN_ALPHA

The implementation should use a data-driven keymap rather than hard-coded key-condition chains.

## 3. PROG mode

Entering PROG mode changes the status display.

Normal calculator status:

    DEG

Programming status:

    PRG

The third status line (one line of interval from PRG) should eventually display the ten program slots:

    P0123456789

An occupied program slot replaces its number with `*`.

Example:

    P0*234*6789

This is a design target, not an immediate implementation requirement.

## 4. PROG INPUT

PROG INPUT is the program editing environment.

The keyboard should prioritize frequently typed BASIC constructs.

### Tier 1 — direct keys

The initial target is to dedicate physical keys to common commands such as:

    PRINT
    INPUT
    GOTO
    IF
    THEN
    FOR
    NEXT
    GOSUB
    RETURN
    END
    STOP
    LIST

Not every command must receive a dedicated key if the final 49-key layout becomes crowded. The exact mapping is intentionally left open until the physical FX-7000G key layout is audited.

### Tier 2 — modifier keys

Less frequent commands can use SHIFT combinations.

Candidates:

    DIM
    READ
    DATA
    RESTORE
    STEP
    LET
    RUN
    CLS
    LOCATE
    PLOT
    LINE
    DRAW

This list is provisional.

### Tier 3 — typed identifiers/keywords

Any command without a shortcut must remain typeable through ALPHA character input.

## 5. Numbers and operators

Numbers should remain directly accessible.

Operators should remain directly accessible wherever the existing FX-7000G layout permits:

    +
    -
    *
    /
    ^
    =
    <
    >
    (
    )
    .
    ,
    "

Programming should not feel like text entry through a phone keypad.

## 6. ALPHA layer

ALPHA is the character-entry layer.

At minimum it must provide:

    A-Z
    0-9 where necessary
    punctuation required by BASIC

The exact letter arrangement should be derived from the existing FX-7000G keyboard and optimized around programming frequency.

## 7. Token insertion

A dedicated BASIC command key inserts a token, not five independent characters.

Example:

Press PRINT key:

    editor.insertToken(TOKEN_PRINT)

The renderer displays:

    PRINT

Backspace should remove the token as one logical unit.

This is essential to making the small keyboard practical.

## 8. EXE

EXE retains its central role.

In PROG INPUT it may:

- commit the current program line
- confirm program selection
- confirm a menu/action
- execute the current editor operation where appropriate

The exact behaviour should be state-specific but consistent.

## 9. RUN mode

RUN mode is intentionally distinct from INPUT mode.

The keyboard may expose:

    RUN
    STOP/BREAK
    CONT
    RESET
    program selection

The normal calculator keys may regain mathematical meaning where appropriate.

The first implementation can keep RUN mode minimal and add convenience mappings later.

## 10. Keyboard usability rule

Do not optimize solely for the number of commands that fit.

Optimize for:

1. frequency of use
2. discoverability
3. muscle memory
4. consistency
5. ability to type ordinary BASIC quickly

A theoretically complete mapping that is unpleasant to use is a failed mapping.

## 11. Mapping process

Before implementing the keyboard:

1. Inventory all 49 physical keys.
2. Record their existing NORMAL/SHIFT/ALPHA meanings.
3. Inventory the initial BASIC language.
4. Rank BASIC tokens by expected frequency.
5. Allocate Tier-1 tokens.
6. Allocate Tier-2 tokens.
7. Verify that all remaining BASIC syntax is typeable.
8. Check collisions with existing calculator functions.
9. Produce a printable/visual keymap for testing.

## 12. Future keyboard layers

Additional layers should only be introduced if necessary.

Avoid an explosion of modifier combinations merely to fit obscure commands.

The user should be able to learn the keyboard without consulting a manual for every character.
