package com.retro.fx7000g.basic.memory

import com.retro.fx7000g.basic.lex.BasicToken

/**
 * Estimates how many bytes of FX-880P-style program storage a tokenized BASIC
 * program requires.
 *
 * This is deliberately separate from the tokenizer: the tokenizer answers "what
 * is this text made of?" while the sizer answers "how much calculator RAM would
 * that cost?". Keeping the accounting rules here means they can be refined
 * against the FX-880P manual without touching `BasicTokenizer`, `ProgramStore`
 * or the allocator. It also lets the same byte count be used both to reserve
 * memory and to report `FRE()`-style figures.
 *
 * The rules implemented for this phase (see MEMORY.md section 10):
 *  - a line number costs [LINE_NUMBER_BYTES] (2) bytes;
 *  - a reserved command/function/constant token costs [RESERVED_TOKEN_BYTES]
 *    (2) bytes;
 *  - every other token costs 1 byte per source character it renders (digits,
 *    letters, spaces, operator symbols and the surrounding string quotes);
 *  - each physical line costs [LINE_TERMINATOR_BYTES] (1) byte of
 *    termination/control information.
 *
 * These figures are intentionally conservative. Where the exact FX-880P rule is
 * unknown, the assumption is documented here in one place rather than spread
 * across the codebase, and can be corrected later without ripple effects.
 */
object BasicProgramMemorySizer {

    /** Cost of a line-number field at the start of a program line. */
    const val LINE_NUMBER_BYTES: Int = 2

    /** Cost of a reserved word token (keyword, function or constant). */
    const val RESERVED_TOKEN_BYTES: Int = 2

    /** Cost of the per-line termination/control byte. */
    const val LINE_TERMINATOR_BYTES: Int = 1

    /**
     * Total storage cost of [tokens] in bytes.
     *
     * Every token is counted (see [sizeOf]); [BasicToken.EndOfInput] is free, as
     * it represents the end of the token stream rather than stored program data.
     * Pass the full stream produced by `BasicTokenizer.tokenize` for a whole
     * program, or a single line's tokens for one line.
     */
    fun size(tokens: List<BasicToken>): Int {
        var total = 0L
        for (token in tokens) {
            total += sizeOf(token)
        }
        check(total <= Int.MAX_VALUE) { "Program storage size overflow: $total bytes" }
        return total.toInt()
    }

    /** Storage cost of a single [token] in bytes. */
    fun sizeOf(token: BasicToken): Int = when (token) {
        is BasicToken.LineNumber -> LINE_NUMBER_BYTES
        is BasicToken.Keyword -> RESERVED_TOKEN_BYTES
        is BasicToken.Function -> RESERVED_TOKEN_BYTES
        is BasicToken.Constant -> RESERVED_TOKEN_BYTES
        is BasicToken.Number -> token.raw.length
        is BasicToken.StringLiteral -> token.value.length + 2 // the surrounding quotes
        is BasicToken.Identifier -> token.name.length
        is BasicToken.Operator -> token.operator.symbol.length
        is BasicToken.Punctuation -> token.punctuation.symbol.length
        is BasicToken.Comment -> token.text.length
        is BasicToken.EndOfLine -> LINE_TERMINATOR_BYTES
        is BasicToken.EndOfInput -> 0
    }
}
