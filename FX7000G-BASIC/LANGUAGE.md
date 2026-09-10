# FX-7000G BASIC — Language Definition

## Status

This document defines the **initial/core language**, not the final language.

The FX-880P BASIC manual is a reference for command selection and historical syntax. Exact compatibility is not required.

## 1. Program lines

Programs are line-numbered.

    10 PRINT "HELLO"
    20 A=10
    30 PRINT A

Line numbers are positive integers. The implementation may choose a practical range, initially suggested as 1-9999.

Entering an existing line number replaces that line.

Entering a line number without a statement deletes that line.

## 2. Assignment

The initial syntax supports direct assignment:

    A=10
    B=A+5

`LET` may be accepted as an optional compatibility spelling:

    LET A=10

Internally both forms should produce the same assignment operation.

## 3. Numeric expressions

Initial operators:

    +   -   *   /   ^

Parentheses:

    ( )

Expressions must follow normal mathematical precedence.

Examples:

    A+B*2
    (A+B)*2
    X^2
    -A

The implementation should use a real expression parser rather than evaluating expressions by ad-hoc string manipulation.

## 4. Comparisons

Initial comparison operators:

    =
    <>
    <
    >
    <=
    >=

They produce a BASIC truth value suitable for IF.

## 5. Variables

Initial numeric variables use single-letter names:

    A-Z

The implementation may internally support a larger identifier space, but the public language should initially retain the compact calculator feel.

Variables are numeric by default.

Example:

    A=25
    B=A*2

## 6. Strings

String literals use double quotes:

    "HELLO"
    "FX-7000G"

PRINT must support string literals.

String variables may use a BASIC-style suffix if adopted by the implementation:

    A$="HELLO"
    PRINT A$

String variables are part of the planned core, but their complete operation set can be implemented incrementally.

## 7. PRINT

Examples:

    PRINT "HELLO"
    PRINT A
    PRINT A+B
    PRINT "VALUE=";A

The exact separator semantics should be defined before implementing complex formatting.

PRINT is a Tier-1 keyboard token.

## 8. INPUT

Examples:

    INPUT A
    INPUT "VALUE";A

INPUT pauses program execution and returns control to the calculator's input subsystem.

INPUT is a Tier-1 keyboard token.

## 9. GOTO

    GOTO 100

Execution continues at the specified line.

GOTO is a Tier-1 keyboard token.

## 10. IF / THEN

Initial form:

    IF A>10 THEN GOTO 100

A compact form may also be accepted if appropriate:

    IF A>10 THEN 100

The parser/runtime should make the distinction explicit rather than relying on textual tricks.

IF and THEN are Tier-1 keyboard tokens.

## 11. FOR / NEXT

Initial form:

    FOR I=1 TO 10
    NEXT I

Optional STEP:

    FOR I=10 TO 0 STEP -1
    NEXT I

FOR/NEXT is a core feature and should be implemented early.

FOR and NEXT are Tier-1 keyboard tokens.

## 12. GOSUB / RETURN

    GOSUB 500
    ...
    500 RETURN

GOSUB pushes a return address onto a runtime stack.

RETURN pops it.

Both are core control-flow commands.

## 13. END

    END

Terminates the current program.

END should leave the calculator in a stable state and make the program ready to RUN again.

## 14. STOP

    STOP

Stops execution without deleting program state.

The exact difference between STOP and END should be kept simple initially:

- END = normal program termination
- STOP = explicit execution stop/break

## 15. LIST

LIST displays the current program.

Initial behaviour:

    LIST

Optional future forms may include line ranges.

The editor/display layer is responsible for scrolling.

## 16. RUN

RUN executes the selected program slot.

Example UI operation:

    P0 → RUN

A future direct BASIC command may also support RUN if useful.

## 17. CLS

    CLS

Clears the BASIC output/graphics area.

This is an FX-7000G-oriented command and need not mimic an FX-880P implementation exactly.

## 18. LOCATE

LOCATE is intended for display-aware text positioning.

Conceptual form:

    LOCATE X,Y
    PRINT "HELLO"

Coordinates refer to the logical BASIC text grid, not raw pixels.

The logical text grid should be defined by the display subsystem rather than hard-coded into the parser.

## 19. Graphics commands

Planned, but initially outside the minimum executable subset:

    PLOT X,Y
    LINE X1,Y1,X2,Y2
    DRAW X,Y
    BOX X1,Y1,X2,Y2
    CIRCLE X,Y,R

These are proposed FX-7000G BASIC extensions.

They should operate on a logical graphics coordinate system defined by DISPLAY.md.

## 20. Mathematical functions

The existing FX-7000G calculator functions should be exposed to BASIC where practical.

Likely initial candidates:

    SIN
    COS
    TAN
    ASIN
    ACOS
    ATAN
    LOG
    LN
    EXP
    SQRT
    ABS
    INT
    RND

The exact function set should be checked against the existing calculator implementation before coding.

## 21. Errors

The BASIC subsystem should eventually report concise calculator-style errors, for example:

    SYNTAX ERROR
    UNDEFINED LINE
    DIVIDE BY 0
    TYPE ERROR
    FOR ERROR
    RETURN ERROR

Error handling should be implemented after the execution core works.

## 22. Tokenization

Every keyword should have a unique internal token.

Example:

    PRINT A+5

must not be stored internally as five independent characters for PRINT.

Conceptually:

    TOKEN_PRINT
    VARIABLE(A)
    OP_PLUS
    NUMBER(5)

This makes editing, parsing, execution and storage substantially simpler.
