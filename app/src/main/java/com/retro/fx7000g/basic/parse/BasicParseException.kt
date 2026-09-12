package com.retro.fx7000g.basic.parse

import com.retro.fx7000g.basic.lex.SourcePos

/** Categories of syntactic problems [BasicParser] can report. */
enum class BasicParseErrorKind {
    /** A value/expression was required but the next token cannot start one. */
    EXPECTED_EXPRESSION,

    /** A statement was required (for example after `THEN`) but none followed. */
    EXPECTED_STATEMENT,

    /** A specific symbol was required, e.g. `)` or `THEN`. */
    EXPECTED_TOKEN,

    /** GOTO/GOSUB/RESTORE required a numeric line reference. */
    EXPECTED_LINE_NUMBER,

    /** The left of `=` is not an assignable variable or array element. */
    INVALID_ASSIGNMENT_TARGET,

    /** A token appeared where the statement/expression had already finished. */
    UNEXPECTED_TOKEN,

    /** The statement begins with something that is not a known statement. */
    UNKNOWN_STATEMENT,

    /** A `:` introduced an empty statement. */
    EMPTY_STATEMENT,

    /** A recognised statement had the wrong shape, e.g. a graphics arity mismatch. */
    MALFORMED_STATEMENT
}

/**
 * Thrown when [BasicParser] cannot turn a token stream into a valid parsed
 * structure.
 *
 * [position] points at the offending token so callers can surface a concise,
 * calculator-style diagnostic, for example:
 *
 * ```
 * Line 2, column 5: Expected expression after '='
 * ```
 *
 * Invalid input is never silently discarded.
 */
class BasicParseException(
    val kind: BasicParseErrorKind,
    val position: SourcePos,
    val detail: String
) : Exception("Line ${position.line}, column ${position.column}: $detail")
