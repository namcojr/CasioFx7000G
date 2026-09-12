package com.retro.fx7000g.basic.lex

/**
 * Renders a [BasicToken] stream back into canonical BASIC source for LIST
 * output.
 *
 * This is deliberately a lexical/presentation layer: it never parses, validates
 * or executes a program, and it does not touch `ProgState` or `ProgramStore`.
 * Its only job is to make the whitespace of tokenized input uniform so the same
 * program always lists the same way:
 *
 * ```
 * 10PRINT B:GOTO10   ->   10 PRINT B : GO TO 10
 * 10 A = 10          ->   10 A=10
 * ```
 *
 * Canonical rules:
 *  - Each physical line is `line number`, one space, then the statement body.
 *  - Keywords, identifiers, numbers, strings, constants and functions are
 *    separated from their neighbours by a single space.
 *  - Operators bind tightly to their operands (`A=10`, `1+2*3`). A leading
 *    `+`/`-` sign keeps a space before it when it follows a word (`STEP -1`),
 *    but never a space after it (`A=-B`).
 *  - `(`, `)`, `,` and `;` bind tightly (`LEFT$("HELLO",2)`).
 *  - `:` (the statement separator) is surrounded by single spaces
 *    (`A=10 : PRINT A`).
 *  - Numeric literals keep their original source spelling
 *    ([BasicToken.Number.raw]), so `1.5E3` and `&HFF` survive a LIST.
 *
 * Keywords are emitted using their canonical display spelling
 * ([displaySpelling]). That is where FX-880P presentation differences live:
 * the internal [BasicKeyword.GOTO] token is displayed as `GO TO`.
 *
 * The tokenizer stays responsible only for lexing; every formatting decision
 * lives here. Round-tripping is textual rather than token-level: `GO TO`
 * re-tokenizes as the identifier `GO` plus the keyword `TO`, because treating
 * that pair as `GOTO` would be a parsing concern rather than a lexical one.
 */
object BasicFormatter {

    /**
     * Keywords whose FX-880P display spelling differs from the internal token
     * spelling. Keywords not listed here are displayed using their own
     * [BasicKeyword.spelling].
     */
    private val keywordDisplayOverrides: Map<BasicKeyword, String> = mapOf(
        BasicKeyword.GOTO to "GO TO"
    )

    /**
     * The canonical display spelling of [keyword], e.g. internal `GOTO` is
     * displayed as `GO TO`.
     */
    fun displaySpelling(keyword: BasicKeyword): String =
        keywordDisplayOverrides[keyword] ?: keyword.spelling

    /**
     * Formats a whole token stream, emitting one output line per physical
     * [BasicToken.EndOfLine]. Empty physical lines are skipped and
     * [BasicToken.EndOfInput] is ignored. The result never has a trailing
     * newline.
     */
    fun format(tokens: List<BasicToken>): String {
        val out = StringBuilder()
        var line = ArrayList<BasicToken>()
        for (token in tokens) {
            when (token) {
                is BasicToken.EndOfLine, is BasicToken.EndOfInput -> {
                    if (line.isNotEmpty()) {
                        if (out.isNotEmpty()) out.append('\n')
                        out.append(formatLine(line))
                        line = ArrayList()
                    }
                }
                else -> line.add(token)
            }
        }
        // Defensive: render a trailing line that has no explicit EndOfLine.
        if (line.isNotEmpty()) {
            if (out.isNotEmpty()) out.append('\n')
            out.append(formatLine(line))
        }
        return out.toString()
    }

    /**
     * Formats the tokens of a single physical line. [BasicToken.EndOfLine] and
     * [BasicToken.EndOfInput] entries are ignored, so a full token stream may be
     * passed directly.
     */
    fun formatLine(tokens: List<BasicToken>): String {
        val out = StringBuilder()
        var previous: BasicToken? = null
        for (token in tokens) {
            if (token is BasicToken.EndOfLine || token is BasicToken.EndOfInput) continue
            previous?.let { out.append(separator(it, token)) }
            out.append(render(token))
            previous = token
        }
        return out.toString().trim()
    }

    /** The canonical source text of a single [token]. */
    private fun render(token: BasicToken): String = when (token) {
        is BasicToken.LineNumber -> token.value.toString()
        is BasicToken.Keyword -> displaySpelling(token.keyword)
        is BasicToken.Function -> token.function.spelling
        is BasicToken.Constant -> token.constant.spelling
        is BasicToken.Number -> token.raw
        is BasicToken.StringLiteral -> "\"${token.value}\""
        is BasicToken.Identifier -> token.name
        is BasicToken.Operator -> token.operator.symbol
        is BasicToken.Punctuation -> token.punctuation.symbol
        is BasicToken.Comment -> token.text.trim()
        is BasicToken.EndOfLine, is BasicToken.EndOfInput -> ""
    }

    /**
     * The canonical whitespace between two adjacent tokens of the same line.
     * The result is `""`, `" "` or, in principle, any whitespace string; callers
     * only ever rely on it being canonical.
     */
    private fun separator(previous: BasicToken, next: BasicToken): String {
        // A line number is always separated from the statement that follows.
        if (previous is BasicToken.LineNumber) return " "

        if (next is BasicToken.Operator) {
            val isSign = next.operator == BasicOperator.PLUS ||
                next.operator == BasicOperator.MINUS
            // A sign is "unary" when the previous token cannot end a value; a
            // word (e.g. `STEP`) keeps a space before it, an operator or an
            // open bracket does not.
            return if (isSign && !isValueEnding(previous)) unarySignPrefix(previous) else ""
        }
        // Operators (and a unary sign's operand) bind tightly on the right.
        if (previous is BasicToken.Operator) return ""

        if (next is BasicToken.Punctuation) {
            return when (next.punctuation) {
                BasicPunctuation.LEFT_PAREN,
                BasicPunctuation.RIGHT_PAREN,
                BasicPunctuation.COMMA,
                BasicPunctuation.SEMICOLON -> ""
                BasicPunctuation.COLON,
                BasicPunctuation.QUESTION -> " "
            }
        }
        if (previous is BasicToken.Punctuation) {
            return when (previous.punctuation) {
                BasicPunctuation.LEFT_PAREN,
                BasicPunctuation.COMMA,
                BasicPunctuation.SEMICOLON -> ""
                BasicPunctuation.COLON,
                BasicPunctuation.QUESTION,
                BasicPunctuation.RIGHT_PAREN -> " "
            }
        }
        return " "
    }

    /** Whitespace before a unary `+`/`-` that follows [previous]. */
    private fun unarySignPrefix(previous: BasicToken): String = when (previous) {
        is BasicToken.Operator -> ""
        is BasicToken.Punctuation -> when (previous.punctuation) {
            BasicPunctuation.LEFT_PAREN,
            BasicPunctuation.COMMA -> ""
            else -> " "
        }
        else -> " "
    }

    /** True when [token] can be the end of a value (operand) on its left. */
    private fun isValueEnding(token: BasicToken): Boolean = when (token) {
        is BasicToken.Number,
        is BasicToken.StringLiteral,
        is BasicToken.Identifier,
        is BasicToken.Constant -> true
        is BasicToken.Punctuation -> token.punctuation == BasicPunctuation.RIGHT_PAREN
        else -> false
    }
}

