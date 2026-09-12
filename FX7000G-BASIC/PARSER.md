# FX-880P BASIC — PHASE 4 - Parser Implementation Specification

## 1. Purpose

The tokenizer and memory foundation are now implemented.

The next phase is to implement the **BASIC parser**.

The parser is the component that transforms:

```text
source text
    ↓
BasicTokenizer
    ↓
BasicToken[]
    ↓
BasicParser
    ↓
parsed BASIC representation
```

The parser must **not execute BASIC**.

It must not evaluate expressions, modify variables, manipulate RAM, perform graphics, perform I/O, or implement runtime behavior.

Its responsibility is only:

> Take the token stream produced by `BasicTokenizer` and turn it into a structurally valid representation of an FX-880P BASIC program or BASIC statement.

This phase establishes the syntax foundation required by the future interpreter.

---

# 2. Architectural goal

The resulting architecture should be:

```text
                     source
                       │
                       ▼
                BasicTokenizer
                       │
                       ▼
                  BasicToken[]
                       │
                       ▼
                  BasicParser
                       │
                       ▼
              Parsed BASIC structure
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
         Formatter            Interpreter
             │                   │
             ▼                   ▼
            LIST               Runtime
                                 │
                  ┌──────────────┼──────────────┐
                  ▼              ▼              ▼
               Memory         Variables       Stack
```

The parser must remain independent of the runtime.

---

# 3. Do NOT redesign existing components

Before implementing anything:

1. Inspect the existing tokenizer.
2. Inspect the existing token definitions.
3. Inspect the existing memory implementation.
4. Inspect the existing BASIC command/key definitions.
5. Inspect the existing formatter/LIST implementation.
6. Inspect existing tests.

Do not replace existing architecture merely because a different parser architecture would be convenient.

The parser must consume the token types that already exist.

If a small tokenizer change is genuinely required to make parsing possible, make the smallest compatible change and document it.

Do not rewrite the tokenizer as part of this phase.

---

# 4. Parser responsibilities

The parser is responsible for:

* program line structure
* statement structure
* command recognition
* argument structure
* assignment structure
* expression structure
* operator precedence
* parentheses
* function calls
* string expressions
* numeric expressions
* variable references
* array references
* separators
* statement chaining
* syntactic validation
* useful parse errors

The parser is NOT responsible for:

* evaluating expressions
* variable lookup
* variable creation
* RAM allocation
* program execution
* GOTO/GOSUB execution
* FOR/NEXT execution
* IF execution
* PRINT output
* INPUT behavior
* graphics
* device I/O
* floating-point calculation
* random number generation
* runtime stack manipulation

Those belong to later phases.

---

# 5. Parsed representation

Use an explicit parsed representation rather than keeping only the original token stream.

The exact class/type names may follow the project's existing conventions, but the conceptual structure should be equivalent to:

```text
BasicProgram
    lines: BasicProgramLine[]

BasicProgramLine
    lineNumber: number
    statements: BasicStatement[]

BasicStatement
    type
    source/token information
    arguments / child nodes
```

Expressions should similarly have an explicit structure:

```text
BasicExpression
```

with concrete forms such as:

```text
NumberLiteral
StringLiteral
VariableReference
ArrayReference
UnaryExpression
BinaryExpression
FunctionCall
ParenthesizedExpression
```

Do not evaluate these nodes while parsing.

---

# 6. Program lines

A program line consists of:

```text
[line number] [statement] [: statement] [: statement] ...
```

Example:

```basic
10 A=10
20 B=A+5
30 PRINT B
40 GOTO 20
```

A line number is part of the program structure.

The parser must preserve it.

For a program source containing:

```basic
10 A=10
20 PRINT A
```

the parser should produce conceptually:

```text
Program
 ├── Line 10
 │    └── Assignment
 │         ├── A
 │         └── 10
 │
 └── Line 20
      └── PRINT
           └── A
```

Line numbers must not be treated as ordinary expressions.

---

# 7. Immediate-mode statements

The parser must also support parsing a statement without a program line number.

For example:

```basic
A=10
PRINT A
FOR I=1 TO 10
```

Therefore:

```text
BasicParser.parseLine(...)
```

and/or

```text
BasicParser.parseStatement(...)
```

must support both:

```text
10 PRINT A
```

and:

```text
PRINT A
```

The exact public API should follow the existing project conventions.

---

# 8. Statement separators

A colon separates BASIC statements on the same line.

Example:

```basic
10 A=10: B=20: PRINT A+B
```

This must produce three statements:

```text
Line 10
 ├── Assignment A=10
 ├── Assignment B=20
 └── PRINT A+B
```

The colon is structural.

It must not become part of an expression.

Empty statements should not be silently created.

For example, malformed input such as:

```basic
10 A=10::PRINT A
```

should produce a parser error unless the existing FX-880P syntax explicitly permits empty statements.

Do not invent permissive behavior merely to make parsing easier.

---

# 9. Statements

The parser should be designed around a statement dispatcher.

Conceptually:

```text
parseStatement()
    │
    ├── assignment?
    ├── PRINT?
    ├── INPUT?
    ├── IF?
    ├── FOR?
    ├── NEXT?
    ├── GOTO?
    ├── GOSUB?
    ├── RETURN?
    ├── END?
    ├── STOP?
    ├── DIM?
    ├── REM?
    ├── DATA?
    ├── READ?
    ├── RESTORE?
    ├── graphics command?
    ├── other known command?
    └── error
```

Do not create one enormous `switch` containing all expression parsing logic.

Keep statement parsing and expression parsing separate.

---

# 10. Assignment

Assignment must be recognized structurally.

Examples:

```basic
A=10
A=B+1
A=(B+C)*2
A$="HELLO"
```

The left-hand side must be a valid assignable target.

Conceptually:

```text
Assignment
    target
    value
```

The target may eventually include:

```text
VariableReference
ArrayReference
```

Do not perform type checking or variable existence checking during parsing unless the existing architecture explicitly requires syntax-level type checking.

For example:

```basic
A=10
```

is syntactically valid regardless of whether `A` currently exists in RAM.

---

# 11. Variable references

Support the variable syntax already established by the tokenizer and FX-880P command model.

At minimum the parser must distinguish:

```text
numeric variable
string variable
array reference
```

Examples:

```basic
A
B
A$
B$
A(10)
A(I)
```

Do not assume that an identifier appearing in an expression is a function.

The parser must use the tokenizer's token classification and the language's known function/command definitions to distinguish them.

---

# 12. Expressions

Expression parsing is one of the most important parts of this phase.

Implement a real precedence-aware expression parser.

A Pratt parser or precedence-climbing parser is preferred.

Do NOT parse expressions using a flat left-to-right algorithm.

For example:

```basic
A=2+3*4
```

must produce:

```text
    +
   / \
  2   *
     / \
    3   4
```

and NOT:

```text
    *
   / \
  +   4
 / \
2   3
```

The result must respect BASIC operator precedence.

---

# 13. Operator precedence

Use the operator definitions already established by the tokenizer/language model.

At minimum the parser should provide distinct precedence levels for:

```text
parentheses
unary operators
multiplication/division
addition/subtraction
relational operators
logical operators
```

The exact precedence and available operators must follow the project's FX-880P language specification.

Do not invent modern-language operators that the calculator does not support.

If the tokenizer already has explicit operator metadata, reuse it.

---

# 14. Unary operators

Unary operators must be parsed separately from binary operators.

Examples:

```basic
A=-10
B=-(A+5)
C=+A
```

Conceptually:

```text
UnaryExpression
    operator: -
    operand:
        NumberLiteral(10)
```

Do not interpret unary minus as:

```text
0 - 10
```

The AST/parsed structure should retain the distinction.

---

# 15. Parentheses

Parentheses must produce correct expression grouping.

Examples:

```basic
A=(2+3)*4
B=((A+B)*2)
C=(A)
```

Parsing must reject unmatched parentheses:

```basic
A=(2+3
A=2+3)
```

Errors should identify the location of the problem whenever possible.

---

# 16. Numeric literals

Numeric literals must be represented as literals, not evaluated.

For example:

```basic
A=123
```

produces conceptually:

```text
Assignment
 ├── A
 └── NumberLiteral(123)
```

Do not convert the value into a runtime-specific representation prematurely unless the existing numeric architecture requires it.

The parser should preserve sufficient source/token information for later formatting/debugging.

---

# 17. String literals

String literals must be represented separately from numeric literals.

Example:

```basic
PRINT "HELLO"
```

Conceptually:

```text
PRINT
 └── StringLiteral("HELLO")
```

String concatenation or other string operations should be parsed according to the existing BASIC operator definitions.

Do not evaluate strings during parsing.

---

# 18. Functions

Functions such as the existing BASIC mathematical functions must be parsed as expressions rather than statements.

Examples:

```basic
SIN(X)
COS(X)
ABS(X)
```

Conceptually:

```text
FunctionCall
    name: SIN
    arguments:
        VariableReference(X)
```

The parser must support function arguments as full expressions where the language permits them.

For example:

```basic
SIN(A+B)
```

must parse as:

```text
SIN
 └── +
     ├── A
     └── B
```

Do not execute the function.

---

# 19. Function vs variable ambiguity

The parser must distinguish:

```basic
A
```

from:

```basic
SIN(A)
```

and:

```basic
A(10)
```

according to the language grammar.

Do not blindly classify every identifier followed by `(` as a function.

An identifier followed by `(` may be an array reference.

Use the existing BASIC symbol/command/function definitions.

---

# 20. PRINT

`PRINT` must be parsed as a statement with one or more printable expressions/items.

Examples:

```basic
PRINT A
PRINT A+B
PRINT "HELLO"
PRINT A;B
```

The parser should preserve separators such as:

```text
;
,
```

if they are valid in the existing BASIC dialect.

The parser must not produce output.

It only constructs the PRINT statement.

---

# 21. INPUT

`INPUT` must parse its arguments without performing input.

Examples:

```basic
INPUT A
INPUT A$
```

The resulting node should identify the target(s).

Runtime behavior belongs to the interpreter.

---

# 22. IF

`IF` is syntactically important because it introduces conditional structure.

At minimum support the syntax defined by the project's FX-880P BASIC specification.

Conceptually:

```text
IF expression THEN statement
```

or whatever exact form the existing language definition specifies.

The condition must be represented as an expression.

Example:

```basic
10 IF A>10 THEN PRINT A
```

Conceptually:

```text
IF
 ├── condition
 │    └── A > 10
 │
 └── body
      └── PRINT A
```

Do not evaluate the condition.

Do not execute the body.

If the actual FX-880P syntax does not use `THEN`, follow the established tokenizer/language specification instead.

---

# 23. FOR

Parse FOR structurally.

Example:

```basic
FOR I=1 TO 10
```

Conceptually:

```text
FOR
 ├── variable: I
 ├── start: 1
 ├── limit: 10
 └── step: default
```

And:

```basic
FOR I=10 TO 1 STEP -1
```

must preserve the STEP expression.

Do not create or modify the runtime FOR stack.

That is interpreter responsibility.

---

# 24. NEXT

Parse:

```basic
NEXT I
```

and any other syntax supported by the established BASIC specification.

The parser should produce a NEXT statement referencing the loop variable where applicable.

Do not attempt to resolve the matching FOR statement in this phase unless the existing architecture explicitly requires parser-level structural linking.

Runtime loop matching belongs to the interpreter.

---

# 25. GOTO / GOSUB / RETURN

Parse control-flow statements structurally.

Examples:

```basic
GOTO 100
GOSUB 500
RETURN
```

For:

```basic
GOTO 100
```

the target should be represented as a numeric line reference rather than as an ordinary runtime number expression if the language grammar treats it specifically as a line target.

Do not jump anywhere.

Do not access the program store.

Do not modify the runtime stack.

---

# 26. DATA / READ / RESTORE

Parse these as proper BASIC statements.

Examples:

```basic
DATA 10,20,30
READ A
RESTORE
```

Preserve DATA items in their syntactic form.

Do not populate runtime DATA structures yet.

That is interpreter/program-runtime work.

---

# 27. REM

`REM` is special because the remainder of the statement/line is comment text.

The parser must respect the tokenizer's handling of comments.

Example:

```basic
10 A=10:REM THIS IS A COMMENT
```

The comment must not be interpreted as additional BASIC syntax.

If the tokenizer already consumes the remainder of the line as a comment token, use that representation.

Do not duplicate comment handling unnecessarily.

---

# 28. Graphics commands

The project intentionally includes graphics extensions:

```text
DRAW
PLOT
CIRCLE
RECT
```

These must be treated as legitimate BASIC statements if they are already part of the project's command definitions.

Examples:

```basic
DRAW X,Y
PLOT X,Y
CIRCLE 50,30,10
RECT 10,10,50,30
```

The parser should produce structured statements and expressions for their arguments.

For example:

```text
CIRCLE
 ├── X expression
 ├── Y expression
 └── radius expression
```

Do not draw anything.

The interpreter/runtime graphics subsystem will consume these nodes later.

---

# 29. Unknown commands

The parser must NOT silently turn arbitrary unknown words into valid statements.

If the tokenizer produces an identifier/keyword that cannot legally occur in the current syntactic position, produce a parser error.

However, remember the project's existing design:

> Commands that are not represented by a dedicated keyboard key can still be typed as BASIC text.

Therefore:

```basic
DATA ...
DRAW ...
PLOT ...
```

etc. must be accepted when they are valid language commands.

Keyboard availability must never determine parser validity.

The parser deals with the BASIC language, not the physical keyboard.

---

# 30. Error handling

Parser errors should be explicit and useful.

A parse error should contain at least:

```text
error type
message
token/location
```

Examples:

```text
Expected expression after '='
Unexpected token ')'
Expected ')' after expression
Expected line number
Invalid assignment target
Unexpected end of statement
Expected expression after ','
Unknown statement
```

Where possible, report:

```text
line number
source position
token
```

For example:

```text
Line 20, column 8:
Expected expression after '='
```

Do not throw generic errors such as:

```text
Parse failed
```

when a useful diagnostic can be produced.

---

# 31. Error recovery

The parser should be able to parse individual lines independently.

For a program containing:

```basic
10 A=10
20 B=
30 PRINT A
```

line 20 should produce a clear syntax error.

Do not allow the malformed line to corrupt parser state for subsequent lines.

The parser should not attempt aggressive error recovery at this stage.

Correctness is more important than producing a long list of cascading errors.

---

# 32. AST / parsed nodes must preserve source information

Where practical, parsed nodes should retain enough information to identify their originating token/span.

This will become extremely useful for:

* error messages
* debugging
* LIST formatting
* future editor integration
* tracing
* interpreter diagnostics

Do not strip all source information during parsing.

---

# 33. No evaluation during parsing

This rule is extremely important.

Given:

```basic
10 A=2+3*4
```

the parser must produce:

```text
Assignment
 ├── Variable A
 └── Binary(+)
      ├── 2
      └── Binary(*)
           ├── 3
           └── 4
```

It must NOT produce:

```text
A = 14
```

The parser knows syntax.

The interpreter knows meaning.

---

# 34. No RAM interaction

The parser must not call:

```text
Fx880pMemory
```

to allocate variables or store programs.

The parser may inspect static language metadata if required.

It must not modify:

* program RAM
* variable RAM
* stack RAM
* free-memory counters
* program storage
* runtime state

Memory sizing was deliberately separated from parsing.

Keep it that way.

---

# 35. Parser API

Design a small API.

Conceptually something similar to:

```text
parseProgram(source)
parseLine(source/tokens)
parseStatement(tokens)
parseExpression(tokens)
```

The exact API should follow the existing project architecture.

Prefer token-based parsing internally:

```text
BasicToken[]
```

rather than repeatedly reparsing raw strings.

The tokenizer remains the single source of truth for lexical analysis.

---

# 36. Parser state

The parser should use a cursor over the token stream.

Conceptually:

```text
tokens
   ↓
[0][1][2][3][4]...
       ^
      cursor
```

Useful internal operations:

```text
peek()
advance()
match(...)
expect(...)
check(...)
isAtEnd()
```

Use whatever equivalent abstraction fits the existing codebase.

Avoid scattering raw array-index manipulation throughout every parse function.

---

# 37. Statement dispatch

Use a clean dispatch mechanism.

Conceptually:

```text
parseStatement()
{
    if assignment:
        return parseAssignment()

    if PRINT:
        return parsePrint()

    if IF:
        return parseIf()

    if FOR:
        return parseFor()

    ...
}
```

Do not make expression parsing responsible for statement recognition.

Likewise, statement parsers should call the expression parser rather than implementing their own expression logic.

---

# 38. Expression parser

The expression parser should have a clear hierarchy.

Conceptually:

```text
parseExpression()
    ↓
parseLogical()
    ↓
parseRelational()
    ↓
parseAdditive()
    ↓
parseMultiplicative()
    ↓
parseUnary()
    ↓
parsePrimary()
```

The exact levels should follow the actual operator precedence of the language.

`parsePrimary()` should handle things such as:

```text
number
string
variable
function call
array reference
parenthesized expression
```

This separation is strongly preferred because the expression grammar will become central to the interpreter.

---

# 39. Parser should be deterministic

Do not introduce:

* backtracking-heavy parsing
* parser generators
* PEG frameworks
* generalized compiler frameworks
* external dependencies

The FX-880P BASIC grammar is small enough that a handwritten parser is the correct engineering choice.

The implementation should be easy to inspect and debug.

---

# 40. Program ordering

The parser should preserve source order.

If the input contains:

```basic
30 PRINT 3
10 PRINT 1
20 PRINT 2
```

the parser should not automatically reorder the lines.

Program storage/runtime can later decide whether program lines are sorted, replaced, merged, etc.

The parser represents what it was given.

---

# 41. Duplicate line numbers

Do not silently decide runtime semantics for duplicate line numbers during parsing.

For example:

```basic
10 PRINT 1
10 PRINT 2
```

may eventually be handled by the program store according to calculator semantics.

The parser should produce two parsed lines unless the established architecture explicitly defines duplicate-line rejection at parse time.

---

# 42. LIST compatibility

The parser must retain enough information for the existing formatter/LIST subsystem to continue working.

The intended flow is:

```text
source
   ↓
tokenizer
   ↓
parser
   ↓
parsed representation
   ↓
formatter
   ↓
LIST output
```

However, do not rewrite the formatter unnecessarily during this phase.

If the formatter currently operates directly on tokens, leave it functional.

The parser is not a reason to break existing LIST behavior.

---

# 43. Round-trip consideration

Ultimately, the system should support the concept:

```text
source
 → tokenize
 → parse
 → format
 → readable BASIC
```

The parser should therefore preserve syntactically significant information.

Whitespace itself does not need to be preserved unless the existing formatter explicitly requires it.

Semantic structure must be preserved.

---

# 44. Testing strategy

Parser tests are extremely important.

Do not rely only on the UI to test parsing.

Create unit tests covering at least:

## Simple assignments

```basic
A=10
A=2+3
A=B
A=B+C*2
```

## Parentheses

```basic
A=(2+3)*4
A=2*(3+4)
A=((A))
```

## Unary operators

```basic
A=-10
A=-B
A=-(B+1)
```

## Strings

```basic
A$="HELLO"
PRINT "HELLO"
PRINT A$
```

## Functions

```basic
A=SIN(X)
A=COS(X)+SIN(Y)
A=ABS(B-C)
```

## Arrays

```basic
A(10)=5
B=A(I)+1
```

## Multiple statements

```basic
10 A=10:B=20:PRINT A+B
```

## Control flow

```basic
10 GOTO 100
20 GOSUB 500
30 RETURN
```

## IF

```basic
10 IF A>10 THEN PRINT A
```

using the exact syntax supported by the language specification.

## FOR/NEXT

```basic
10 FOR I=1 TO 10
20 NEXT I
```

## DATA

```basic
10 DATA 10,20,30
20 READ A
```

## Graphics

```basic
10 PLOT X,Y
20 DRAW X,Y
30 CIRCLE 50,30,10
40 RECT 10,10,50,30
```

---

# 45. Negative tests

Tests must also verify that invalid syntax is rejected.

Examples:

```basic
A=
A=*
A=(1+2
A=1+2)
A=1+
PRINT ,
FOR I=
FOR I=1
```

and invalid assignment targets:

```basic
10+20=30
```

The parser should reject these with useful errors.

---

# 46. Expression precedence tests

These tests are mandatory.

For:

```basic
A=2+3*4
```

verify that the root operator is `+`.

For:

```basic
A=(2+3)*4
```

verify that the root operator is `*`.

For:

```basic
A=-2*3
```

verify that unary `-` applies to `2` before multiplication, according to the language's precedence rules.

Do not merely test that parsing succeeds.

Inspect the resulting structure.

---

# 47. Parser tests should not require the UI

The parser must be testable entirely from code:

```text
tokens → parser → parsed structure
```

No Android activity.

No screen.

No keyboard.

No RAM.

No emulator.

This will make debugging the BASIC implementation dramatically easier.

---

# 48. Do not implement the interpreter yet

This phase ends when:

```text
source
 ↓
tokenizer
 ↓
parser
 ↓
valid parsed structure
```

works reliably.

Do NOT proceed into:

```text
parsed structure
 ↓
execute
```

during this phase.

The interpreter will be a separate phase.

This separation is intentional.

---

# 49. Do not implement runtime semantics

In particular, do not implement:

```text
LET execution
variable assignment
FOR execution
NEXT execution
IF branching
GOTO
GOSUB
RETURN
PRINT output
INPUT
DATA storage
READ state
graphics execution
```

The parser only represents these constructs.

---

# 50. Preserve the FX-880P character of the language

This is a calculator BASIC implementation, not a modern BASIC redesign.

Do not automatically add:

* modern type systems
* modern string interpolation
* modern boolean syntax
* modern compound statements
* C-style operators
* JavaScript-like semantics
* Python-like semantics
* automatic language extensions

If something is not part of the established FX-880P BASIC syntax, do not add it merely because it is convenient.

The existing tokenizer, keyboard model, command definitions, and FX-880P reference material are the authority.

---

# 51. Unknown future commands

The parser architecture should make it easy to add another BASIC statement later.

For example:

```text
DRAW
PLOT
CIRCLE
RECT
```

were deliberately added as project-specific extensions.

Adding another command should ideally require:

1. command metadata/definition
2. statement parser implementation if necessary
3. tests

It should NOT require rewriting the expression parser or parser architecture.

---

# 52. Implementation order

Implement in this order:

### Step 1 — Parser infrastructure

Implement:

```text
token cursor
peek
advance
match
expect
error reporting
```

### Step 2 — Primary expressions

Implement:

```text
numbers
strings
variables
parentheses
```

### Step 3 — Unary expressions

Implement:

```text
+
-
```

according to the established operator set.

### Step 4 — Binary expressions

Implement precedence-aware parsing.

### Step 5 — Function calls and array references

Implement:

```text
SIN(X)
A(I)
```

and all equivalent existing constructs.

### Step 6 — Assignment

Implement:

```text
A=expression
A$=expression
A(I)=expression
```

where syntactically valid.

### Step 7 — Simple statements

Implement:

```text
PRINT
INPUT
REM
DATA
READ
RESTORE
```

### Step 8 — Control statements

Implement:

```text
IF
FOR
NEXT
GOTO
GOSUB
RETURN
END
STOP
```

according to the actual supported syntax.

### Step 9 — Graphics extensions

Implement:

```text
DRAW
PLOT
CIRCLE
RECT
```

### Step 10 — Program lines

Implement:

```text
line number + statements + ':' + statements
```

### Step 11 — Error handling

Improve diagnostics and negative tests.

### Step 12 — Full parser test suite

Only after the parser is structurally complete.

---

# 53. Definition of done

This phase is complete when all of the following are true:

* `BasicTokenizer` produces the expected tokens.
* `BasicParser` consumes those tokens.
* Program lines are parsed.
* Immediate-mode statements are parsed.
* Multiple statements separated by `:` are parsed.
* Assignments are parsed.
* Expressions respect operator precedence.
* Parentheses work.
* Unary operators work.
* String literals work.
* Variable references work.
* Array references work where supported.
* Function calls work.
* BASIC statements are represented structurally.
* Control-flow statements are represented structurally.
* Graphics statements are represented structurally.
* Invalid syntax produces useful parser errors.
* Parser tests run without the UI.
* Parser does not evaluate expressions.
* Parser does not modify RAM.
* Parser does not execute anything.
* Existing tokenizer and formatter behavior remains intact.

---

# 54. Important engineering rule

Do not optimize prematurely.

The first goal is:

> **Correct, readable, deterministic parsing.**

The BASIC language is small.

A straightforward handwritten parser is preferable to a clever abstraction.

The parser will become the foundation for the interpreter, so correctness and maintainability are more important than minimizing lines of code.

---

# 55. Expected end state

After this phase, code should be able to conceptually do:

```text
source:

10 A=10
20 B=A*2+5
30 IF B>20 THEN PRINT B
40 GOTO 20
```

and produce a structure equivalent to:

```text
Program
│
├── Line 10
│   └── Assignment
│       ├── A
│       └── 10
│
├── Line 20
│   └── Assignment
│       ├── B
│       └──
│           +
│          / \
│         *   5
│        / \
│       A   2
│
├── Line 30
│   └── IF
│       ├── >
│       │   ├── B
│       │   └── 20
│       └── PRINT
│           └── B
│
└── Line 40
    └── GOTO
        └── 20
```

At that point the project has crossed an important architectural boundary:

```text
keyboard
   ↓
tokenizer
   ↓
parser
   ↓
BASIC program representation
   ↓
             ← NEXT PHASE →
          BASIC INTERPRETER
```

The interpreter can then execute this representation using the already-established:

```text
Fx880pMemory
Program Store
Variables
Runtime Stack
```

without having to understand raw source text or keyboard input.

---

# 56. Final instruction to the implementer

Before writing code:

1. Inspect the existing project.
2. Inspect all current BASIC token definitions.
3. Inspect the existing command definitions.
4. Inspect the memory architecture.
5. Inspect existing formatter/LIST behavior.
6. Inspect existing tests.
7. Reuse existing terminology and architecture wherever possible.

Then implement the parser incrementally.

Do not rewrite unrelated components.

Do not implement the interpreter.

Do not implement runtime behavior.

Do not modify the UI except where absolutely necessary to expose parser functionality for testing.

Add focused unit tests for every parser feature implemented.

When finished, report:

```text
Files changed
Files added
Parser architecture
Supported syntax
Tests added
Tests passing
Any tokenizer changes required
Any unresolved syntax questions
```

The parser is the foundation for the next phase: **BASIC interpretation and execution**.
