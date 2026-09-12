package com.retro.fx7000g.basic.lex

/** Categories of lexical problems the BASIC tokenizer can report. */
enum class BasicLexErrorKind {
    /** A character with no meaning in BASIC, e.g. `@` or a stray `$`. */
    UNEXPECTED_CHARACTER,

    /** A run of characters that looks like a number but cannot be parsed. */
    MALFORMED_NUMBER,

    /** A double-quoted string that is never closed before the end of the line. */
    UNTERMINATED_STRING,

    /** A recognised prefix used in an invalid way, e.g. `&` not followed by `H`. */
    INVALID_TOKEN
}

/**
 * Thrown when [BasicTokenizer] cannot turn the source into tokens.
 *
 * [position] points at the first offending character so callers can surface a
 * concise, calculator-style diagnostic. Invalid input is never silently
 * discarded.
 */
class BasicLexException(
    val kind: BasicLexErrorKind,
    val position: SourcePos,
    detail: String
) : Exception("$detail at line ${position.line}, column ${position.column}")
