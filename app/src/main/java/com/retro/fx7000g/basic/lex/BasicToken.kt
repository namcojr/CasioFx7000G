package com.retro.fx7000g.basic.lex

/** A position in the tokenized source, carried by every token and lexical error. */
data class SourcePos(val index: Int, val line: Int, val column: Int)

/**
 * Operators recognised by BASIC. `=` doubles as both comparison and the
 * assignment operator; the parser decides which based on context.
 */
enum class BasicOperator(val symbol: String) {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIVIDE("/"),
    POWER("^"),
    EQUAL("="),
    NOT_EQUAL("<>"),
    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">=")
}

/** Punctuation recognised by BASIC (including the `?` PRINT shorthand). */
enum class BasicPunctuation(val symbol: String) {
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    COMMA(","),
    SEMICOLON(";"),
    COLON(":"),
    QUESTION("?")
}

/** Built-in constants, reusing the calculator's `PI` / `E` vocabulary. */
enum class BasicConstant(val spelling: String) {
    PI("PI"),
    E("E")
}

/**
 * A single lexical token produced by [BasicTokenizer].
 *
 * The token hierarchy is deliberately explicit so a future BASIC parser can
 * distinguish program structure, statements, expressions and the immediate
 * commands without re-inspecting raw text.
 */
sealed interface BasicToken {

    /** Source position of the first character of this token. */
    val position: SourcePos

    /** A concise, human-readable rendering used by diagnostics and tests. */
    fun describe(): String

    /** A line number at the start of a physical program line, e.g. `10`. */
    data class LineNumber(val value: Int, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "LINE($value)"
    }

    /** A reserved statement/command word such as `PRINT` or `NEXT`. */
    data class Keyword(val keyword: BasicKeyword, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "KW(${keyword.spelling})"
    }

    /** A built-in function such as `LEFT$` or `SIN`. */
    data class Function(val function: BasicFunction, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "FN(${function.spelling})"
    }

    /** A built-in constant such as `PI` or `E`. */
    data class Constant(val constant: BasicConstant, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "CONST(${constant.spelling})"
    }

    /**
     * A numeric literal. [raw] preserves the exact source spelling (useful for
     * listing and round-tripping) while [value] is the parsed value.
     */
    data class Number(val value: Double, val raw: String, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "NUM($raw)"
    }

    /** A double-quoted string literal; [value] holds the contents without quotes. */
    data class StringLiteral(val value: String, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "STR(\"$value\")"
    }

    /** A variable/identifier such as `A`, `NAME$` or `COUNTER`. */
    data class Identifier(val name: String, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "ID($name)"
    }

    /** An arithmetic or comparison operator. */
    data class Operator(val operator: BasicOperator, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "OP(${operator.symbol})"
    }

    /** A punctuation mark. */
    data class Punctuation(val punctuation: BasicPunctuation, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "PUNCT(${punctuation.symbol})"
    }

    /** The remainder of a `REM` line, preserved verbatim. */
    data class Comment(val text: String, override val position: SourcePos) : BasicToken {
        override fun describe(): String = "COMMENT($text)"
    }

    /** End of a physical source line (also emitted before [EndOfInput]). */
    data class EndOfLine(override val position: SourcePos) : BasicToken {
        override fun describe(): String = "EOL"
    }

    /** End of the tokenized source, always the final token. */
    data class EndOfInput(override val position: SourcePos) : BasicToken {
        override fun describe(): String = "EOF"
    }
}
