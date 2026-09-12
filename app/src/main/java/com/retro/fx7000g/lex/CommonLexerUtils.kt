package com.retro.fx7000g.lex

/**
 * Low-level lexical primitives shared by the calculator and BASIC tokenizers.
 *
 * These helpers deliberately know nothing about token types or language
 * keywords; they only answer "how far does this run of source extend?".
 * The calculator's `com.retro.fx7000g.calc.Evaluator` and the dedicated
 * [com.retro.fx7000g.basic.lex.BasicTokenizer] can each build their own token
 * vocabularies on top of these primitives instead of duplicating the scanning
 * rules.
 */
object CommonLexerUtils {

    /** True for the ASCII digits `0`-`9`. */
    fun isAsciiDigit(c: Char): Boolean = c in '0'..'9'

    /** True for the ASCII letters `A`-`Z` and `a`-`z`. */
    fun isLetter(c: Char): Boolean = c in 'A'..'Z' || c in 'a'..'z'

    /** True when [c] may begin an identifier. */
    fun isIdentifierStart(c: Char): Boolean = isLetter(c)

    /** True when [c] may continue an identifier (after the first character). */
    fun isIdentifierPart(c: Char): Boolean = isLetter(c) || isAsciiDigit(c)

    /** True for the horizontal whitespace characters the tokenizers skip. */
    fun isWhitespace(c: Char): Boolean = c == ' ' || c == '\t'

    /** True for the line-break characters (`\n` and `\r`). */
    fun isLineBreak(c: Char): Boolean = c == '\n' || c == '\r'

    /**
     * Scans a decimal numeric literal starting at [start] and returns the index
     * one past its end.
     *
     * The scan is intentionally permissive: it consumes digits and `.`
     * characters plus an optional `E` exponent (with an optional sign), leaving
     * strict validation to the caller (for example `String.toDoubleOrNull`).
     */
    fun scanNumber(src: String, start: Int): Int {
        var i = start
        while (i < src.length && (isAsciiDigit(src[i]) || src[i] == '.')) i++
        if (i < src.length && src[i] == 'E') {
            i++
            if (i < src.length && (src[i] == '+' || src[i] == '-')) i++
            while (i < src.length && isAsciiDigit(src[i])) i++
        }
        return i
    }

    /**
     * Scans an identifier starting at [start] and returns the index one past its
     * end. An identifier is a letter followed by any number of letters/digits,
     * with an optional trailing `$` (BASIC's string-variable marker).
     */
    fun scanIdentifier(src: String, start: Int): Int {
        var i = start
        while (i < src.length && isIdentifierPart(src[i])) i++
        if (i < src.length && src[i] == '$') i++
        return i
    }

    /**
     * Scans a double-quoted string literal. [start] must point at the opening
     * quote. Returns the index one past the closing quote, or `-1` when the
     * string is unterminated (a line break or the end of the source is reached
     * first).
     */
    fun scanString(src: String, start: Int): Int {
        var i = start + 1
        while (i < src.length) {
            when (src[i]) {
                '"' -> return i + 1
                '\n', '\r' -> return -1
                else -> i++
            }
        }
        return -1
    }
}
