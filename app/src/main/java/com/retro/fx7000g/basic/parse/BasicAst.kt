package com.retro.fx7000g.basic.parse

import com.retro.fx7000g.basic.lex.BasicConstant
import com.retro.fx7000g.basic.lex.BasicFunction
import com.retro.fx7000g.basic.lex.BasicKeyword
import com.retro.fx7000g.basic.lex.BasicOperator
import com.retro.fx7000g.basic.lex.SourcePos

/**
 * The parsed representation of a whole BASIC program.
 *
 * Produced by [BasicParser.parseProgram] from the `BasicToken` stream emitted by
 * `com.retro.fx7000g.basic.lex.BasicTokenizer`. It describes *syntax* only:
 * nothing here is evaluated and no memory is allocated. [lines] follows source
 * order exactly; the parser never sorts, merges or rejects duplicate line
 * numbers (that belongs to `ProgramStore`/the runtime).
 */
data class BasicProgram(
    val lines: List<BasicProgramLine>,
    val position: SourcePos
)

/**
 * One parsed physical program line: an optional line number followed by one or
 * more `:`-separated statements.
 *
 * [lineNumber] is `null` for immediate-mode input parsed with
 * [BasicParser.parseLine]. A line number with no statement (for example `10`,
 * which deletes the line in `ProgramStore`) yields an empty [statements] list.
 */
data class BasicProgramLine(
    val lineNumber: Int?,
    val statements: List<BasicStatement>,
    val position: SourcePos
) {
    /** True when this line came from immediate-mode input rather than a program. */
    val isImmediate: Boolean get() = lineNumber == null
}

/** A parsed BASIC expression. Expressions are never evaluated during parsing. */
sealed interface BasicExpression {
    val position: SourcePos

    /** An expression that may stand on the left of `=` (and in INPUT/READ). */
    sealed interface LValue : BasicExpression

    /** A numeric literal, keeping its exact source spelling in [raw]. */
    data class NumberLiteral(
        val value: Double,
        val raw: String,
        override val position: SourcePos
    ) : BasicExpression

    /** A double-quoted string literal; [value] excludes the quotes. */
    data class StringLiteral(
        val value: String,
        override val position: SourcePos
    ) : BasicExpression

    /** A reserved constant such as `PI`. */
    data class ConstantReference(
        val constant: BasicConstant,
        override val position: SourcePos
    ) : BasicExpression

    /** A scalar variable such as `A` or `NAME$`. */
    data class VariableReference(
        val name: String,
        override val position: SourcePos
    ) : LValue

    /** An array element such as `A(10)` or `M(I,J)`. */
    data class ArrayReference(
        val name: String,
        val indices: List<BasicExpression>,
        override val position: SourcePos
    ) : LValue

    /** A prefix `+`/`-` such as `-B`. */
    data class UnaryExpression(
        val operator: BasicOperator,
        val operand: BasicExpression,
        override val position: SourcePos
    ) : BasicExpression

    /** An infix operation such as `2+3*4`. */
    data class BinaryExpression(
        val left: BasicExpression,
        val operator: BasicOperator,
        val right: BasicExpression,
        override val position: SourcePos
    ) : BasicExpression

    /** A built-in function call such as `SIN(X)` or the nullary `INKEY$`. */
    data class FunctionCall(
        val function: BasicFunction,
        val arguments: List<BasicExpression>,
        override val position: SourcePos
    ) : BasicExpression

    /**
     * A function-like call whose head word is tokenized as a keyword rather
     * than a [BasicFunction]. Today this is `PEEK`, which the tokenizer emits as
     * `KW(PEEK)` but which is used as `PEEK(addr)` inside expressions. Modelling
     * it here avoids changing the tokenizer (and its existing tests).
     */
    data class KeywordCall(
        val keyword: BasicKeyword,
        val arguments: List<BasicExpression>,
        override val position: SourcePos
    ) : BasicExpression

    /** A parenthesised expression such as `(2+3)`. */
    data class Grouping(
        val inner: BasicExpression,
        override val position: SourcePos
    ) : BasicExpression
}

/** A `,`/`;` separator between two PRINT items. */
enum class BasicPrintSeparator(val symbol: String) {
    COMMA(","),
    SEMICOLON(";")
}

/**
 * One printable item of a PRINT statement. [expression] is always present;
 * [separator] is the punctuation *following* it, or `null` when it is the last
 * item. A trailing separator (`PRINT A;`) is modelled as a final item whose
 * [separator] is set.
 */
data class PrintItem(
    val expression: BasicExpression,
    val separator: BasicPrintSeparator? = null
)

/** One item of a DATA statement, preserved in its syntactic form. */
sealed interface DataItem {
    val position: SourcePos

    /** A numeric item such as `10` or `-2.5` (the sign is folded into [raw]). */
    data class Number(
        val value: Double,
        val raw: String,
        override val position: SourcePos
    ) : DataItem

    /** A quoted string item such as `"HELLO"`. */
    data class Text(
        val value: String,
        override val position: SourcePos
    ) : DataItem

    /** An unquoted item such as `HELLO` or `PI`. */
    data class Word(
        val text: String,
        override val position: SourcePos
    ) : DataItem
}

/** One `A(...)` declaration within a DIM statement. */
data class DimDeclaration(
    val name: String,
    val dimensions: List<BasicExpression>,
    val position: SourcePos
)

/** A parsed BASIC statement. Statements never execute anything. */
sealed interface BasicStatement {
    val position: SourcePos

    /** `target = value`, `LET target = value`, including array `A(i)=v`. */
    data class Assignment(
        val target: BasicExpression.LValue,
        val value: BasicExpression,
        val explicitLet: Boolean,
        override val position: SourcePos
    ) : BasicStatement

    /** `PRINT items` (or the `?` shorthand). */
    data class Print(
        val items: List<PrintItem>,
        override val position: SourcePos
    ) : BasicStatement

    /** `INPUT [prompt;] target[, target...]`. */
    data class Input(
        val prompt: BasicExpression.StringLiteral?,
        val targets: List<BasicExpression.LValue>,
        override val position: SourcePos
    ) : BasicStatement

    /** `IF condition THEN statement`. */
    data class If(
        val condition: BasicExpression,
        val thenBranch: BasicStatement,
        override val position: SourcePos
    ) : BasicStatement

    /** `FOR variable = start TO limit [STEP step]`. */
    data class For(
        val variable: BasicExpression.VariableReference,
        val start: BasicExpression,
        val limit: BasicExpression,
        val step: BasicExpression?,
        override val position: SourcePos
    ) : BasicStatement

    /** `NEXT [variable]`. */
    data class Next(
        val variable: BasicExpression.VariableReference?,
        override val position: SourcePos
    ) : BasicStatement

    /** `GOTO line`; [implicit] marks the compact `IF ... THEN 100` form. */
    data class Goto(
        val line: Int,
        val implicit: Boolean,
        override val position: SourcePos
    ) : BasicStatement

    /** `GOSUB line`. */
    data class Gosub(
        val line: Int,
        override val position: SourcePos
    ) : BasicStatement

    /** `RETURN`. */
    data class Return(override val position: SourcePos) : BasicStatement

    /** `END`. */
    data class End(override val position: SourcePos) : BasicStatement

    /** `STOP`. */
    data class Stop(override val position: SourcePos) : BasicStatement

    /** `REM text`; [text] is the verbatim comment tail. */
    data class Rem(
        val text: String,
        override val position: SourcePos
    ) : BasicStatement

    /** `DATA item[, item...]`. */
    data class Data(
        val items: List<DataItem>,
        override val position: SourcePos
    ) : BasicStatement

    /** `READ target[, target...]`. */
    data class Read(
        val targets: List<BasicExpression.LValue>,
        override val position: SourcePos
    ) : BasicStatement

    /** `RESTORE [line]`. */
    data class Restore(
        val line: Int?,
        override val position: SourcePos
    ) : BasicStatement

    /** `DIM A(...)[, B(...)...]`. */
    data class Dim(
        val declarations: List<DimDeclaration>,
        override val position: SourcePos
    ) : BasicStatement

    /** A graphics statement: DRAW, PLOT, LINE, BOX, CIRCLE or RECT. */
    data class Graphics(
        val keyword: BasicKeyword,
        val arguments: List<BasicExpression>,
        override val position: SourcePos
    ) : BasicStatement

    /** `LOCATE x,y`. */
    data class Locate(
        val x: BasicExpression,
        val y: BasicExpression,
        override val position: SourcePos
    ) : BasicStatement

    /** `POKE address,value`. */
    data class Poke(
        val address: BasicExpression,
        val value: BasicExpression,
        override val position: SourcePos
    ) : BasicStatement

    /**
     * A simple/immediate command with no bespoke grammar (CLS, BEEP, ERASE,
     * SET, TAB, LIST, EDIT, RUN, CONT, FREE, NEW, CLEAR). Any arguments that
     * follow are parsed as ordinary expressions.
     */
    data class Command(
        val keyword: BasicKeyword,
        val arguments: List<BasicExpression>,
        override val position: SourcePos
    ) : BasicStatement
}
