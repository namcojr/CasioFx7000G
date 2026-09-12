package com.retro.fx7000g.basic.lex

import com.retro.fx7000g.lex.CommonLexerUtils

/**
 * Dedicated tokenizer for the FX-7000G BASIC subset.
 *
 * It is intentionally independent from the calculator expression tokenizer
 * embedded in `com.retro.fx7000g.calc.Evaluator`: BASIC has its own keywords,
 * line numbers, statements, strings and operators. Only the low-level scanning
 * primitives are shared (see [CommonLexerUtils]).
 *
 * The tokenizer is purely lexical. It never executes, stores or semantically
 * validates a program; those belong to later phases. It does, however, produce
 * tokens rich enough that a future parser can distinguish program statements
 * from immediate commands without re-inspecting raw text.
 *
 * A leading integer on a physical line is reported as a [BasicToken.LineNumber],
 * matching the line-number handling in `ProgramStore`. Everything after it is
 * tokenized normally, so the second `10` in `10 A=10` stays a numeric literal.
 */
object BasicTokenizer {

    /**
     * Tokenizes [source] into a flat list of [BasicToken]s.
     *
     * Every non-empty physical line is terminated by [BasicToken.EndOfLine] and
     * the whole source by [BasicToken.EndOfInput], which is always the final
     * token. Empty (or whitespace-only) input yields just an
     * [BasicToken.EndOfInput].
     *
     * @throws BasicLexException on unexpected characters, malformed numbers or
     *   unterminated string literals.
     */
    fun tokenize(source: String): List<BasicToken> = Impl(source).run()

    private class Impl(private val source: String) {
        private var index = 0
        private var line = 1
        private var column = 1

        /** True until the first token of the current physical line is emitted. */
        private var atLineStart = true

        /** Tokens produced on the current physical line (excluding its EOL). */
        private var tokensOnLine = 0

        private val tokens = ArrayList<BasicToken>()

        fun run(): List<BasicToken> {
            while (index < source.length) {
                val c = source[index]
                when {
                    CommonLexerUtils.isLineBreak(c) -> {
                        tokens += consumeNewline()
                        tokensOnLine = 0
                        atLineStart = true
                    }

                    CommonLexerUtils.isWhitespace(c) -> advance()

                    atLineStart && CommonLexerUtils.isAsciiDigit(c) && lineNumberAhead() ->
                        emit(readLineNumber())

                    c == '"' -> emit(readString())

                    CommonLexerUtils.isIdentifierStart(c) -> {
                        val token = readWord()
                        emit(token)
                        if (token is BasicToken.Keyword && token.keyword == BasicKeyword.REM) {
                            readComment()?.let { emit(it) }
                        }
                    }

                    CommonLexerUtils.isAsciiDigit(c) || c == '.' -> emit(readNumber())

                    c == '\u03C0' -> { // π - built-in constant
                        val p = pos()
                        advance()
                        emit(BasicToken.Constant(BasicConstant.PI, p))
                    }

                    c == '&' -> emit(readHexLiteral())

                    isOperatorOrPunctuation(c) -> emit(readOperatorOrPunctuation())

                    else -> throw BasicLexException(
                        BasicLexErrorKind.UNEXPECTED_CHARACTER,
                        pos(),
                        "Unexpected character '${printable(c)}'"
                    )
                }
            }

            if (tokensOnLine > 0) tokens += BasicToken.EndOfLine(pos())
            tokens += BasicToken.EndOfInput(pos())
            return tokens
        }

        // --- token producers --------------------------------------------------

        private fun readLineNumber(): BasicToken.LineNumber {
            val p = pos()
            val start = index
            while (index < source.length && CommonLexerUtils.isAsciiDigit(source[index])) advance()
            val raw = source.substring(start, index)
            val value = raw.toIntOrNull() ?: throw BasicLexException(
                BasicLexErrorKind.MALFORMED_NUMBER, p, "Line number out of range: $raw"
            )
            return BasicToken.LineNumber(value, p)
        }

        private fun readString(): BasicToken.StringLiteral {
            val p = pos()
            val end = CommonLexerUtils.scanString(source, index)
            if (end < 0) {
                throw BasicLexException(
                    BasicLexErrorKind.UNTERMINATED_STRING, p, "Unterminated string literal"
                )
            }
            val content = source.substring(index + 1, end - 1)
            while (index < end) advance()
            return BasicToken.StringLiteral(content, p)
        }

        private fun readNumber(): BasicToken.Number {
            val p = pos()
            val start = index
            val end = CommonLexerUtils.scanNumber(source, index)
            while (index < end) advance()
            val raw = source.substring(start, index)
            val value = raw.replace('E', 'e').toDoubleOrNull() ?: throw BasicLexException(
                BasicLexErrorKind.MALFORMED_NUMBER, p, "Malformed number literal: $raw"
            )
            return BasicToken.Number(value, raw, p)
        }

        private fun readWord(): BasicToken {
            val p = pos()
            val start = index
            val end = CommonLexerUtils.scanIdentifier(source, index)
            while (index < end) advance()
            val text = source.substring(start, index)

            // `RAN#` - '#' is not an identifier character, so assemble it here.
            if (text.equals("RAN", ignoreCase = true) &&
                index < source.length && source[index] == '#'
            ) {
                advance()
                return BasicToken.Function(BasicFunction.RAN, p)
            }

            BasicKeyword.from(text)?.let { return BasicToken.Keyword(it, p) }
            BasicFunction.from(text)?.let { return BasicToken.Function(it, p) }
            when (text.uppercase()) {
                "PI" -> return BasicToken.Constant(BasicConstant.PI, p)
                "E" -> return BasicToken.Constant(BasicConstant.E, p)
            }
            return BasicToken.Identifier(text, p)
        }

        /** Consumes the rest of a `REM` line as a single comment token. */
        private fun readComment(): BasicToken.Comment? {
            if (index >= source.length || CommonLexerUtils.isLineBreak(source[index])) return null
            val p = pos()
            val start = index
            while (index < source.length && !CommonLexerUtils.isLineBreak(source[index])) advance()
            return BasicToken.Comment(source.substring(start, index), p)
        }

        /** Reads the calculator's `&H` hexadecimal literal prefix. */
        private fun readHexLiteral(): BasicToken.Number {
            val p = pos()
            if (index + 1 >= source.length ||
                (source[index + 1] != 'H' && source[index + 1] != 'h')
            ) {
                throw BasicLexException(
                    BasicLexErrorKind.INVALID_TOKEN, p, "Expected 'H' after '&'"
                )
            }
            advance() // &
            advance() // H
            val start = index
            while (index < source.length && hexDigitValue(source[index]) >= 0) advance()
            if (index == start) {
                throw BasicLexException(
                    BasicLexErrorKind.MALFORMED_NUMBER, p, "Expected hexadecimal digits after '&H'"
                )
            }
            val digits = source.substring(start, index)
            val value = digits.toLongOrNull(16)?.toDouble() ?: throw BasicLexException(
                BasicLexErrorKind.MALFORMED_NUMBER, p, "Invalid hexadecimal literal: &H$digits"
            )
            return BasicToken.Number(value, "&H$digits", p)
        }

        private fun readOperatorOrPunctuation(): BasicToken {
            val p = pos()
            return when (val c = source[index]) {
                '+' -> { advance(); BasicToken.Operator(BasicOperator.PLUS, p) }
                '-', '\u2212' -> { advance(); BasicToken.Operator(BasicOperator.MINUS, p) }
                '*', '\u00D7' -> { advance(); BasicToken.Operator(BasicOperator.TIMES, p) }
                '/', '\u00F7' -> { advance(); BasicToken.Operator(BasicOperator.DIVIDE, p) }
                '^' -> { advance(); BasicToken.Operator(BasicOperator.POWER, p) }
                '=' -> { advance(); BasicToken.Operator(BasicOperator.EQUAL, p) }
                '<' -> {
                    advance()
                    when {
                        index < source.length && source[index] == '=' -> {
                            advance(); BasicToken.Operator(BasicOperator.LESS_EQUAL, p)
                        }
                        index < source.length && source[index] == '>' -> {
                            advance(); BasicToken.Operator(BasicOperator.NOT_EQUAL, p)
                        }
                        else -> BasicToken.Operator(BasicOperator.LESS, p)
                    }
                }
                '>' -> {
                    advance()
                    if (index < source.length && source[index] == '=') {
                        advance(); BasicToken.Operator(BasicOperator.GREATER_EQUAL, p)
                    } else {
                        BasicToken.Operator(BasicOperator.GREATER, p)
                    }
                }
                '(' -> { advance(); BasicToken.Punctuation(BasicPunctuation.LEFT_PAREN, p) }
                ')' -> { advance(); BasicToken.Punctuation(BasicPunctuation.RIGHT_PAREN, p) }
                ',' -> { advance(); BasicToken.Punctuation(BasicPunctuation.COMMA, p) }
                ';' -> { advance(); BasicToken.Punctuation(BasicPunctuation.SEMICOLON, p) }
                ':' -> { advance(); BasicToken.Punctuation(BasicPunctuation.COLON, p) }
                '?' -> { advance(); BasicToken.Punctuation(BasicPunctuation.QUESTION, p) }
                else -> throw BasicLexException(
                    BasicLexErrorKind.UNEXPECTED_CHARACTER,
                    p,
                    "Unexpected character '${printable(c)}'"
                )
            }
        }

        // --- helpers ----------------------------------------------------------

        private fun emit(token: BasicToken) {
            tokens += token
            tokensOnLine++
            atLineStart = false
        }

        private fun pos(): SourcePos = SourcePos(index, line, column)

        private fun advance() {
            if (index >= source.length) return
            val c = source[index]
            index++
            if (c == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }

        private fun consumeNewline(): BasicToken.EndOfLine {
            val p = pos()
            if (source[index] == '\r' &&
                index + 1 < source.length && source[index + 1] == '\n'
            ) {
                index += 2
            } else {
                index++
            }
            line++
            column = 1
            return BasicToken.EndOfLine(p)
        }

        /**
         * True when the digits at [index] should be read as a line number.
         * `10.5` and `10E3` are numeric literals, not line numbers.
         */
        private fun lineNumberAhead(): Boolean {
            var j = index
            while (j < source.length && CommonLexerUtils.isAsciiDigit(source[j])) j++
            if (j >= source.length) return true
            val next = source[j]
            return next != '.' && next != 'E'
        }

        private fun isOperatorOrPunctuation(c: Char): Boolean =
            c in "+-*/^=<>(),;:?" || c == '\u00D7' || c == '\u00F7' || c == '\u2212'

        private fun hexDigitValue(c: Char): Int = when (c) {
            in '0'..'9' -> c - '0'
            in 'A'..'F' -> c - 'A' + 10
            in 'a'..'f' -> c - 'a' + 10
            else -> -1
        }

        private fun printable(c: Char): String = when (c) {
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            else -> c.toString()
        }
    }
}
