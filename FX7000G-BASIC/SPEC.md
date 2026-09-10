# FX-7000G BASIC — Project Specification

## 1. Purpose

This project adds a hypothetical, native programming environment to the FX-7000G tribute/simulation.

It is **not an FX-880P emulator**. The FX-880P and its BASIC manual are reference material for language design and historical calculator usability, but the resulting language and environment are defined specifically for the FX-7000G tribute.

The guiding idea is:

> What might a programmable FX-7000G have looked and felt like if Casio had given the graphing calculator a compact BASIC programming environment?

The implementation should preserve the character of a small 1980s/early-1990s programmable calculator while taking advantage of the tribute's modern display and software architecture.

## 2. Design principles

1. BASIC must feel like a calculator language, not a desktop programming language.
2. Common commands should be available as single-key tokens.
3. Less common commands may be entered through SHIFT/ALPHA layers.
4. The language must be tokenized internally.
5. Program editing is line-oriented.
6. Programs are stored in numbered slots P0-P9.
7. The display is modernized where useful, but the programming model remains deliberately compact.
8. Historical FX-880P BASIC syntax may be reused where it makes sense, but it is not a compatibility target.
9. The existing FX-7000G mathematical vocabulary should be reused wherever practical.
10. Extensions should be clearly identified as FX-7000G BASIC extensions rather than pretending to be historical commands.

## 3. Initial implementation target

The first usable BASIC subset should support:

- program slots P0-P9
- PROG mode
- INPUT mode
- RUN mode
- line numbers
- program editing
- LIST
- RUN
- STOP
- PRINT
- INPUT
- LET/implicit assignment
- numeric variables
- string literals
- basic string variables if practical
- arithmetic expressions
- comparison operators
- IF ... THEN
- GOTO
- GOSUB
- RETURN
- FOR ... NEXT
- END
- CLS
- LOCATE

Graphics are part of the architecture from the beginning but may initially be implemented after the core text BASIC is operational.

## 4. Non-goals for the first release

Do not initially attempt:

- complete historical FX-880P compatibility
- every obscure BASIC command
- machine-code facilities
- cassette/tape compatibility
- exact historical memory limitations
- every historical graphics quirk
- desktop-style file management
- a full modern BASIC dialect

Those can be considered after the basic system is enjoyable and stable.

## 5. Internal architecture

The BASIC subsystem should be independent from the normal calculator expression/UI subsystem.

Recommended pipeline:

    Physical key
        ↓
    Key layer / action
        ↓
    Program editor
        ↓
    Token stream
        ↓
    Parser / validator
        ↓
    Executable representation
        ↓
    BASIC runtime

The editor should retain token identity rather than converting everything back into ordinary text.

## 6. Program model

Programs consist of ordered numbered lines.

Example:

    10 PRINT "HELLO"
    20 FOR I=1 TO 10
    30 PRINT I
    40 NEXT I
    50 END

A program slot is independent of the others:

    P0 ... P9

The occupied-slot indicator is a UI concern. In PROG mode the status area should eventually show:

    PRG
    P0123456789

An occupied slot replaces its number with `*`, e.g.:

    P0*23456789

The exact typography/layout is subject to the existing display implementation.

## 7. Future compatibility philosophy

When adding a command inspired by the FX-880P manual, decide in this order:

1. Is the command useful on the FX-7000G?
2. Does its semantics fit the proposed BASIC?
3. Can it be implemented cleanly?
4. Does it conflict with existing calculator functionality?
5. Is there a better display-aware implementation?

Only then add it.
