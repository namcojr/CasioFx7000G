# Phase 1 — FX-880P BASIC Tokenizer

Implement Phase 1 of the FX-880P BASIC subsystem: a dedicated BASIC tokenizer/lexer.

Before changing anything, inspect the existing project and understand the current calculator tokenizer/parser/evaluator, keyboard implementation, mode handling, program storage, and all existing `CalcAction`/input handling. Reuse existing low-level lexical functionality where appropriate, but do NOT force BASIC through the calculator tokenizer.

## Architecture

Create a dedicated BASIC tokenizer, separate from the calculator expression tokenizer.

The architecture should conceptually be:

```
Calculator input
    → calculator lexer/tokenizer
    → calculator parser/evaluator

BASIC input
    → BASIC lexer/tokenizer
    → (future) BASIC parser/executor
```

Share low-level helpers where useful, for example:

* numeric literal scanning
* identifier scanning
* string literal scanning
* common source-position/error handling
* character classification

Do not duplicate substantial lexical code unnecessarily.

However, BASIC must have its own token types and tokenizer because BASIC has different keywords, line numbers, statements, strings, operators and program syntax.

Do NOT prematurely merge the two token systems merely to reduce file count.

## BASIC tokens

Design an explicit `BasicToken` representation suitable for the future parser/interpreter.

It should support at minimum:

### Program structure

* line numbers
* identifiers/variables
* keywords
* numeric literals
* string literals
* end-of-line / end-of-input

### Operators

Support the operators currently exposed by the keyboard and required by the planned BASIC implementation, including:

* `+`
* `-`
* `*`
* `/`
* `=`
* `<`
* `>`
* and the appropriate combinations such as `<=`, `>=`, `<>` if supported by the language design

Do not invent operators that are not required.

### Punctuation

Support:

* `(`
* `)`
* `,`
* `;`
* `:`

and any other punctuation already represented by the current keyboard mapping.

### String handling

Support quoted strings:

```
"HELLO"
"HELLO WORLD"
"A=123"
```

Preserve the string contents appropriately for the future interpreter.

## BASIC keywords

Use the existing keyboard mapping as the authoritative list of commands currently planned for this simulator.

At minimum the tokenizer must recognize:

```
PRINT
INPUT
GOTO
IF
THEN
FOR
TO
NEXT
STEP
DRAW
PLOT
LET
ERASE
GOSUB
RETURN
END
STOP
READ
DATA
RESTORE
POKE
PEEK
INKEY$
LOCATE
DIM
SET
TAB
BEEP
REM
CLEAR
CLS
LIST
EDIT
RUN
CONT
FREE
NEW
```

Also recognize the string/numeric functions already exposed by the keyboard, including:

```
LEFT$
RIGHT$
MID$
CHR$
STR$
HEX$
SGN
ABS
INT
FRAC
MIN
MAX
FIX
ROUND
RAN#
POL
REC
```

and the mathematical functions/operators already supported by the calculator/BASIC design.

Do not blindly implement every keyword from the original FX-880P manual. This project intentionally implements a useful FX-880P-inspired BASIC subset rather than a 100% reproduction.

Where a keyword is already represented by a constant or existing symbol in the project, reuse it rather than introducing a second spelling.

## Case handling

BASIC keywords should be recognized case-insensitively.

For example:

```
PRINT
Print
print
```

must tokenize as the same BASIC keyword.

Identifiers should preserve whatever representation is appropriate for the future variable system.

## Program line numbers

A BASIC program line such as:

```
10 PRINT "HELLO"
```

must tokenize approximately as:

```
LineNumber(10)
Keyword(PRINT)
String("HELLO")
EndOfLine
```

Likewise:

```
100 FOR I=1 TO 10 STEP 1
```

must correctly identify the line number, keyword, identifier, operators, numeric literals and keywords.

Line numbers are syntactic BASIC constructs and should NOT be treated as ordinary calculator numbers.

Support the line-number format appropriate to the existing program-storage design. Do not impose arbitrary limits without checking the existing code/manual reference.

## Multiple statements

Support the existing BASIC statement separator:

```
:
```

For example:

```
10 A=10:PRINT A
```

must produce two statements separated by the appropriate punctuation token.

Do not execute either statement yet.

## REM

`REM` introduces a comment.

For example:

```
10 REM THIS IS A COMMENT
```

The tokenizer must handle the remainder of the line correctly according to the planned BASIC semantics.

Do not accidentally tokenize words inside a REM comment as executable BASIC keywords.

## Special identifiers

Handle identifiers correctly, including identifiers followed by `$` where required by the language.

For example:

```
A
X
COUNTER
NAME$
```

Do not confuse identifiers such as `INKEY$`, `LEFT$`, `CHR$`, etc. with arbitrary variables merely because `$` is present.

## Built-in constants

Support the constants already defined by the calculator/BASIC design, such as:

```
PI / π
E
```

Use the project's existing representation where possible.

## Existing keyboard integration

Do not redesign the keyboard.

The keyboard phase is considered closed.

The existing keyboard already inserts BASIC commands such as:

```
PRINT
INPUT
GOTO
IF
THEN
FOR
NEXT
DRAW
PLOT
...
```

The tokenizer must consume the text produced by that keyboard correctly.

Also support ordinary textual entry of commands even when a dedicated keyboard key does not exist.

For example:

```
DATA
```

must tokenize correctly even if the user enters it character-by-character.

## Calculator tokenizer relationship

Inspect the current calculator tokenizer carefully.

If it already contains robust implementations for:

* numbers
* identifiers
* strings
* character classification
* token positions
* lexical errors

extract genuinely reusable pieces into a small common helper where appropriate.

Do NOT create a giant universal tokenizer with dozens of mode-specific conditionals.

Prefer:

```
CommonLexerUtils
    ├── number scanning
    ├── identifier scanning
    └── string scanning

CalculatorTokenizer
    └── calculator-specific tokens/rules

BasicTokenizer
    └── BASIC-specific tokens/rules
```

The BASIC tokenizer should remain independently understandable.

## Immediate commands vs program statements

Do NOT make the tokenizer execute anything.

It should only produce tokens.

However, design the token types so that a future parser can distinguish between:

### Program statements

```
PRINT
INPUT
GOTO
IF
FOR
NEXT
GOTO
GOSUB
RETURN
END
STOP
READ
DATA
RESTORE
LET
DRAW
PLOT
...
```

and:

### Program-management/immediate commands

```
LIST
EDIT
RUN
CONT
FREE
NEW
CLEAR
...
```

The distinction between these categories belongs primarily to the future parser/command dispatcher, not the lexer.

## Important: do not implement program execution yet

This phase is ONLY lexical analysis.

Do NOT implement:

* FOR/NEXT execution
* IF evaluation
* GOTO
* GOSUB/RETURN stack
* INPUT
* PRINT output semantics
* variable storage
* arrays
* program execution
* program control flow
* RAM/program memory

Those belong to subsequent phases.

The tokenizer must make those phases easier.

## Immediate commands

If the project already has trivial mode-level implementations for commands such as:

```
LIST
EDIT
FREE
NEW
CLEAR
```

do not break them.

If implementing them requires the tokenizer, keep that integration minimal and clearly separated from tokenization.

Do not turn the tokenizer into a command executor.

## Testing

Create comprehensive tokenizer tests.

At minimum test:

```
10 PRINT "HELLO"

10 A=10

10 FOR I=1 TO 10 STEP 1
20 PRINT I
30 NEXT I

10 IF A>10 THEN 50

10 GOTO 100

10 A=10:PRINT A

10 INPUT A

10 DATA 1,2,3,4

10 READ A

10 REM THIS IS A COMMENT

10 DRAW 10,20

10 PLOT 30,40

10 CIRCLE(50,50,20)

10 PRINT LEFT$("HELLO",2)
```

Also test:

* lowercase/mixed-case keywords
* whitespace
* empty input
* malformed numbers
* unterminated strings
* unknown keywords/identifiers
* operators
* `<`, `>`, `<=`, `>=`, `<>`
* multiple statements separated by `:`
* `$` identifiers/functions
* comments
* line numbers

Tests should verify the token sequence, not program execution.

## Error handling

Provide useful lexical errors.

Errors should identify at least:

* unexpected character
* malformed number
* unterminated string
* invalid token

Where practical, include the source position.

Do not silently discard invalid input.

## UI / mode behavior

Do not redesign the UI in this phase.

Do not alter the keyboard layout.

Do not change CAL/PRG mode behavior except where required to route PRG input through the new BASIC tokenizer.

The recently agreed mode model is:

```
MODE → 7 = PRG
MODE → 8 = CAL
MODE alone = no mode change
```

PRG has program-management/editing/execution behavior, but there is no separate user-visible "RUN mode".

`BRK` replaces `AC` in PRG mode.

Do not reintroduce a separate RUN mode.

## Deliverable

At the end of this phase:

1. The project compiles.
2. Existing calculator behavior remains intact.
3. BASIC input can be tokenized independently.
4. Tokenizer tests pass.
5. Existing keyboard mappings remain intact.
6. No program execution is implemented yet.
7. The code is structured so the next phase can implement a BASIC parser/executor without rewriting the tokenizer.

Before making changes, inspect the actual project and adapt the implementation to its existing architecture rather than inventing parallel infrastructure.

Do not make unrelated refactors.
