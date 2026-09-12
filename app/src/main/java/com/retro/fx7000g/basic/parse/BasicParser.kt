package com.retro.fx7000g.basic.parse

import com.retro.fx7000g.basic.lex.BasicFunction
import com.retro.fx7000g.basic.lex.BasicKeyword
import com.retro.fx7000g.basic.lex.BasicOperator
import com.retro.fx7000g.basic.lex.BasicPunctuation
import com.retro.fx7000g.basic.lex.BasicToken
import com.retro.fx7000g.basic.lex.BasicTokenizer
import com.retro.fx7000g.basic.lex.SourcePos

/**
 * Hand-written recursive-descent parser for the FX-7000G BASIC subset.
 *
 * It consumes the [BasicToken] stream produced by [BasicTokenizer] and produces
 * an explicit parsed structure ([BasicProgram], [BasicStatement],
 * [BasicExpression]). It is deliberately a pure syntax component:
 *
 *  - it never evaluates an expression or a condition;
 *  - it never looks up or creates a variable;
 *  - it never touches `Fx7000gMemory`, `ProgramStore`, `ProgState` or the UI;
 *  - it never executes a statement.
 *
 * The grammar is small enough that a deterministic handwritten parser is the
 * right tool, so there is no backtracking, no parser generator and no external
 * dependency. Statement recognition lives in `parseStatement()`; every
 * statement delegates expression work to the shared expression parser rather
 * than re-implementing it.
 *
 * Two flavours of input are supported:
 *
 * ```
 * BasicParser.parseProgram("10 PRINT \"HI\"\n20 END")   // whole line-numbered program
 * BasicParser.parseLine("PRINT A")                      // one line, immediate-mode
 * BasicParser.parseStatement("A=10")                    // one statement
 * BasicParser.parseExpression("2+3*4")                  // one expression
 * ```
 */
object BasicParser {

    /** Parses a whole line-numbered program from [source]. */
    fun parseProgram(source: String): BasicProgram = parseProgram(BasicTokenizer.tokenize(source))

    /** Parses a whole line-numbered program from an existing token stream. */
    fun parseProgram(tokens: List<BasicToken>): BasicProgram = Impl(tokens).parseProgram()

    /** Parses a single physical line, with or without a leading line number. */
    fun parseLine(source: String): BasicProgramLine = parseLine(BasicTokenizer.tokenize(source))

    /** Parses a single physical line from an existing token stream. */
    fun parseLine(tokens: List<BasicToken>): BasicProgramLine = Impl(tokens).parseLine()

    /** Parses exactly one immediate-mode statement (no line number). */
    fun parseStatement(source: String): BasicStatement = parseStatement(BasicTokenizer.tokenize(source))

    /** Parses exactly one statement from an existing token stream. */
    fun parseStatement(tokens: List<BasicToken>): BasicStatement = Impl(tokens).parseStatementOnly()

    /** Parses exactly one expression from [source]. */
    fun parseExpression(source: String): BasicExpression = parseExpression(BasicTokenizer.tokenize(source))

    /** Parses exactly one expression from an existing token stream. */
    fun parseExpression(tokens: List<BasicToken>): BasicExpression = Impl(tokens).parseExpressionOnly()

    /**
     * Functions that may be written without an argument list. `INKEY$`, `RAN#`
     * and `RND` are nullary on the FX-880P; every other function must be
     * followed by `(`.
     */
    private val nullaryFunctions: Set<BasicFunction> =
        setOf(BasicFunction.INKEY, BasicFunction.RAN, BasicFunction.RND)

    /** Graphics keywords and the number of comma-separated arguments each takes. */
    private val graphicsArity: Map<BasicKeyword, Int> = mapOf(
        BasicKeyword.DRAW to 2,
        BasicKeyword.PLOT to 2,
        BasicKeyword.LINE to 4,
        BasicKeyword.BOX to 4,
        BasicKeyword.RECT to 4,
        BasicKeyword.CIRCLE to 3
    )

    /** The comparison operators that may appear in an expression. */
    private val relationalOperators: Set<BasicOperator> = setOf(
        BasicOperator.EQUAL,
        BasicOperator.NOT_EQUAL,
        BasicOperator.LESS,
        BasicOperator.LESS_EQUAL,
        BasicOperator.GREATER,
        BasicOperator.GREATER_EQUAL
    )

    /**
     * The token-cursor based implementation. One instance parses one token
     * stream; the stream is expected to be terminated by
     * [BasicToken.EndOfInput], exactly as [BasicTokenizer] produces it.
     */
    private class Impl(private val tokens: List<BasicToken>) {
        private var index = 0

        // --- entry points ---------------------------------------------------

        fun parseProgram(): BasicProgram {
            val start = peek().position
            val lines = ArrayList<BasicProgramLine>()
            while (!atEnd()) {
                if (peek() is BasicToken.EndOfLine) {
                    advance()
                    continue
                }
                lines += parsePhysicalLine(requireLineNumber = true)
            }
            return BasicProgram(lines, start)
        }

        fun parseLine(): BasicProgramLine {
            while (peek() is BasicToken.EndOfLine) advance()
            return parsePhysicalLine(requireLineNumber = false)
        }

        fun parseStatementOnly(): BasicStatement {
            while (peek() is BasicToken.EndOfLine) advance()
            val statement = parseStatement()
            if (!atLineEnd()) unexpected(peek())
            return statement
        }

        fun parseExpressionOnly(): BasicExpression {
            val expression = parseExpression()
            if (!atLineEnd()) unexpected(peek())
            return expression
        }

        // --- program/line structure ----------------------------------------

        private fun parsePhysicalLine(requireLineNumber: Boolean): BasicProgramLine {
            val start = peek().position
            val lineNumber: Int?
            if (peek() is BasicToken.LineNumber) {
                lineNumber = (advance() as BasicToken.LineNumber).value
            } else {
                if (requireLineNumber) {
                    fail(BasicParseErrorKind.EXPECTED_LINE_NUMBER, peek().position, "Expected line number")
                }
                lineNumber = null
            }

            val statements = ArrayList<BasicStatement>()
            if (!atLineEnd()) {
                statements += parseStatement()
                while (matchPunctuation(BasicPunctuation.COLON)) {
                    if (atLineEnd() || isPunctuation(BasicPunctuation.COLON)) {
                        fail(BasicParseErrorKind.EMPTY_STATEMENT, peek().position, "Empty statement")
                    }
                    statements += parseStatement()
                }
            }
            if (!atLineEnd()) unexpected(peek())
            if (peek() is BasicToken.EndOfLine) advance()
            return BasicProgramLine(lineNumber, statements, start)
        }

        // --- cursor and matching helpers -----------------------------------

        private fun peek(): BasicToken = tokens[index]

        private fun peekAt(offset: Int): BasicToken {
            val target = index + offset
            return if (target < tokens.size) tokens[target] else tokens.last()
        }

        private fun advance(): BasicToken {
            val token = tokens[index]
            if (index < tokens.size - 1) index++
            return token
        }

        private fun atEnd(): Boolean = peek() is BasicToken.EndOfInput

        private fun atLineEnd(): Boolean = atEnd() || peek() is BasicToken.EndOfLine

        private fun isPunctuation(punctuation: BasicPunctuation): Boolean {
            val token = peek()
            return token is BasicToken.Punctuation && token.punctuation == punctuation
        }

        private fun atStatementEnd(): Boolean =
            atLineEnd() || isPunctuation(BasicPunctuation.COLON)

        private fun matchPunctuation(punctuation: BasicPunctuation): Boolean {
            if (isPunctuation(punctuation)) {
                advance()
                return true
            }
            return false
        }

        private fun matchOperator(operator: BasicOperator): Boolean {
            val token = peek()
            if (token is BasicToken.Operator && token.operator == operator) {
                advance()
                return true
            }
            return false
        }

        private fun matchKeyword(keyword: BasicKeyword): Boolean {
            val token = peek()
            if (token is BasicToken.Keyword && token.keyword == keyword) {
                advance()
                return true
            }
            return false
        }

        // --- diagnostics ----------------------------------------------------

        private fun fail(
            kind: BasicParseErrorKind,
            position: SourcePos,
            detail: String
        ): Nothing = throw BasicParseException(kind, position, detail)

        private fun unexpected(token: BasicToken): Nothing =
            fail(BasicParseErrorKind.UNEXPECTED_TOKEN, token.position, "Unexpected token ${quote(token)}")

        private fun quote(token: BasicToken): String = when (token) {
            is BasicToken.LineNumber -> "'${token.value}'"
            is BasicToken.Keyword -> "'${token.keyword.spelling}'"
            is BasicToken.Function -> "'${token.function.spelling}'"
            is BasicToken.Constant -> "'${token.constant.spelling}'"
            is BasicToken.Number -> "'${token.raw}'"
            is BasicToken.StringLiteral -> "'\"${token.value}\"'"
            is BasicToken.Identifier -> "'${token.name}'"
            is BasicToken.Operator -> "'${token.operator.symbol}'"
            is BasicToken.Punctuation -> "'${token.punctuation.symbol}'"
            is BasicToken.Comment -> "'${token.text}'"
            is BasicToken.EndOfLine -> "end of line"
            is BasicToken.EndOfInput -> "end of input"
        }

        // --- statement dispatch --------------------------------------------

        private fun parseStatement(): BasicStatement {
            val token = peek()
            return when (token) {
                is BasicToken.Identifier -> parseAssignment(token.position, explicitLet = false)
                is BasicToken.Keyword -> parseKeywordStatement(token)
                is BasicToken.Punctuation -> when (token.punctuation) {
                    BasicPunctuation.QUESTION -> parsePrintShorthand(token)
                    BasicPunctuation.COLON ->
                        fail(BasicParseErrorKind.EMPTY_STATEMENT, token.position, "Empty statement")
                    else ->
                        fail(BasicParseErrorKind.UNKNOWN_STATEMENT, token.position, "Unknown statement '${token.punctuation.symbol}'")
                }
                is BasicToken.Number, is BasicToken.StringLiteral, is BasicToken.Function,
                is BasicToken.Constant, is BasicToken.Operator ->
                    fail(BasicParseErrorKind.INVALID_ASSIGNMENT_TARGET, token.position, "Invalid assignment target")
                is BasicToken.EndOfLine, is BasicToken.EndOfInput ->
                    fail(BasicParseErrorKind.EXPECTED_STATEMENT, token.position, "Unexpected end of statement")
                is BasicToken.Comment ->
                    fail(BasicParseErrorKind.UNKNOWN_STATEMENT, token.position, "Unexpected comment")
                is BasicToken.LineNumber ->
                    fail(BasicParseErrorKind.UNKNOWN_STATEMENT, token.position, "Unexpected line number")
            }
        }

        private fun parseKeywordStatement(token: BasicToken.Keyword): BasicStatement =
            when (token.keyword) {
                BasicKeyword.LET -> {
                    advance()
                    parseAssignment(token.position, explicitLet = true)
                }
                BasicKeyword.PRINT -> parsePrintStatement(token)
                BasicKeyword.INPUT -> parseInput()
                BasicKeyword.IF -> parseIf()
                BasicKeyword.FOR -> parseFor()
                BasicKeyword.NEXT -> parseNext()
                BasicKeyword.GOTO -> parseGoto()
                BasicKeyword.GOSUB -> parseGosub()
                BasicKeyword.RETURN -> parseReturn()
                BasicKeyword.END -> {
                    advance()
                    BasicStatement.End(token.position)
                }
                BasicKeyword.STOP -> {
                    advance()
                    BasicStatement.Stop(token.position)
                }
                BasicKeyword.REM -> parseRem()
                BasicKeyword.DATA -> parseData()
                BasicKeyword.READ -> parseRead()
                BasicKeyword.RESTORE -> parseRestore()
                BasicKeyword.DIM -> parseDim()
                BasicKeyword.DRAW, BasicKeyword.PLOT, BasicKeyword.LINE,
                BasicKeyword.BOX, BasicKeyword.CIRCLE, BasicKeyword.RECT -> parseGraphics(token)
                BasicKeyword.LOCATE -> parseLocate()
                BasicKeyword.POKE -> parsePoke()
                BasicKeyword.CLS, BasicKeyword.BEEP, BasicKeyword.ERASE, BasicKeyword.SET,
                BasicKeyword.TAB, BasicKeyword.LIST, BasicKeyword.EDIT, BasicKeyword.RUN,
                BasicKeyword.CONT, BasicKeyword.FREE, BasicKeyword.NEW, BasicKeyword.CLEAR ->
                    parseCommand(token)
                BasicKeyword.PEEK, BasicKeyword.THEN, BasicKeyword.TO, BasicKeyword.STEP ->
                    fail(BasicParseErrorKind.UNKNOWN_STATEMENT, token.position, "Unknown statement '${token.keyword.spelling}'")
            }

        // --- simple statements ---------------------------------------------

        private fun parseAssignment(statementStart: SourcePos, explicitLet: Boolean): BasicStatement.Assignment {
            val target = parseLValue("assignment")
            if (!matchOperator(BasicOperator.EQUAL)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected '=' after assignment target")
            }
            val value = expectExpression(after = "=")
            return BasicStatement.Assignment(target, value, explicitLet, statementStart)
        }

        private fun parseLValue(context: String): BasicExpression.LValue {
            val token = peek()
            if (token !is BasicToken.Identifier) {
                fail(BasicParseErrorKind.INVALID_ASSIGNMENT_TARGET, token.position, "Invalid $context target")
            }
            advance()
            if (matchPunctuation(BasicPunctuation.LEFT_PAREN)) {
                val indices = parseExpressionList(BasicPunctuation.RIGHT_PAREN, allowEmpty = false)
                expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after subscript")
                return BasicExpression.ArrayReference(token.name, indices, token.position)
            }
            return BasicExpression.VariableReference(token.name, token.position)
        }

        private fun parsePrintStatement(token: BasicToken.Keyword): BasicStatement.Print {
            advance()
            return parsePrintBody(token.position)
        }

        private fun parsePrintShorthand(token: BasicToken.Punctuation): BasicStatement.Print {
            advance()
            return parsePrintBody(token.position)
        }

        private fun parsePrintBody(start: SourcePos): BasicStatement.Print {
            val items = ArrayList<PrintItem>()
            if (!atStatementEnd()) {
                var expression = expectExpression()
                while (true) {
                    val separator = when {
                        isPunctuation(BasicPunctuation.SEMICOLON) -> BasicPrintSeparator.SEMICOLON
                        isPunctuation(BasicPunctuation.COMMA) -> BasicPrintSeparator.COMMA
                        else -> null
                    }
                    if (separator == null) {
                        items += PrintItem(expression, null)
                        break
                    }
                    advance()
                    if (atStatementEnd()) {
                        items += PrintItem(expression, separator)
                        break
                    }
                    items += PrintItem(expression, separator)
                    expression = expectExpression(after = separator.symbol)
                }
            }
            return BasicStatement.Print(items, start)
        }

        private fun parseInput(): BasicStatement.Input {
            val start = advance().position
            var prompt: BasicExpression.StringLiteral? = null
            if (peek() is BasicToken.StringLiteral) {
                val token = advance() as BasicToken.StringLiteral
                prompt = BasicExpression.StringLiteral(token.value, token.position)
                if (isPunctuation(BasicPunctuation.SEMICOLON) || isPunctuation(BasicPunctuation.COMMA)) advance()
            }
            val targets = ArrayList<BasicExpression.LValue>()
            targets += parseLValue("INPUT")
            while (matchPunctuation(BasicPunctuation.COMMA)) targets += parseLValue("INPUT")
            return BasicStatement.Input(prompt, targets, start)
        }

        private fun parseIf(): BasicStatement.If {
            val start = advance().position
            val condition = expectExpression(after = "IF")
            if (!matchKeyword(BasicKeyword.THEN)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected THEN")
            }
            val branch = parseThenBranch()
            return BasicStatement.If(condition, branch, start)
        }

        private fun parseThenBranch(): BasicStatement {
            if (atStatementEnd()) {
                fail(BasicParseErrorKind.EXPECTED_STATEMENT, peek().position, "Expected statement after THEN")
            }
            val token = peek()
            if (token is BasicToken.Number && lineNumberOf(token) != null && endsStatementAt(1)) {
                advance()
                return BasicStatement.Goto(lineNumberOf(token)!!, implicit = true, position = token.position)
            }
            return parseStatement()
        }

        private fun parseFor(): BasicStatement.For {
            val start = advance().position
            val token = peek()
            if (token !is BasicToken.Identifier) {
                fail(BasicParseErrorKind.INVALID_ASSIGNMENT_TARGET, token.position, "Expected loop variable after FOR")
            }
            if (peekAt(1) is BasicToken.Punctuation &&
                (peekAt(1) as BasicToken.Punctuation).punctuation == BasicPunctuation.LEFT_PAREN
            ) {
                fail(BasicParseErrorKind.INVALID_ASSIGNMENT_TARGET, token.position, "FOR loop variable cannot be an array")
            }
            advance()
            val variable = BasicExpression.VariableReference(token.name, token.position)
            if (!matchOperator(BasicOperator.EQUAL)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected '=' after FOR variable")
            }
            val begin = expectExpression(after = "=")
            if (!matchKeyword(BasicKeyword.TO)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected TO")
            }
            val limit = expectExpression(after = "TO")
            val step = if (matchKeyword(BasicKeyword.STEP)) expectExpression(after = "STEP") else null
            return BasicStatement.For(variable, begin, limit, step, start)
        }

        private fun parseNext(): BasicStatement.Next {
            val start = advance().position
            val variable = if (peek() is BasicToken.Identifier) {
                val token = advance() as BasicToken.Identifier
                BasicExpression.VariableReference(token.name, token.position)
            } else {
                null
            }
            return BasicStatement.Next(variable, start)
        }

        private fun parseGoto(): BasicStatement.Goto {
            val token = advance() as BasicToken.Keyword
            return BasicStatement.Goto(parseLineReference("GOTO"), implicit = false, position = token.position)
        }

        private fun parseGosub(): BasicStatement.Gosub {
            val token = advance() as BasicToken.Keyword
            return BasicStatement.Gosub(parseLineReference("GOSUB"), token.position)
        }

        private fun parseReturn(): BasicStatement.Return {
            val token = advance() as BasicToken.Keyword
            return BasicStatement.Return(token.position)
        }

        private fun parseRem(): BasicStatement.Rem {
            val token = advance() as BasicToken.Keyword
            val text = if (peek() is BasicToken.Comment) {
                (advance() as BasicToken.Comment).text
            } else {
                ""
            }
            return BasicStatement.Rem(text, token.position)
        }

        private fun parseData(): BasicStatement.Data {
            val token = advance() as BasicToken.Keyword
            val items = ArrayList<DataItem>()
            if (!atStatementEnd()) {
                items += parseDataItem()
                while (matchPunctuation(BasicPunctuation.COMMA)) items += parseDataItem()
            }
            return BasicStatement.Data(items, token.position)
        }

        private fun parseDataItem(): DataItem {
            val token = peek()
            when (token) {
                is BasicToken.Number -> {
                    advance()
                    return DataItem.Number(token.value, token.raw, token.position)
                }
                is BasicToken.StringLiteral -> {
                    advance()
                    return DataItem.Text(token.value, token.position)
                }
                is BasicToken.Identifier -> {
                    advance()
                    return DataItem.Word(token.name, token.position)
                }
                is BasicToken.Constant -> {
                    advance()
                    return DataItem.Word(token.constant.spelling, token.position)
                }
                is BasicToken.Operator -> {
                    val sign = token.operator
                    if ((sign == BasicOperator.PLUS || sign == BasicOperator.MINUS) &&
                        peekAt(1) is BasicToken.Number
                    ) {
                        advance()
                        val number = advance() as BasicToken.Number
                        val negative = sign == BasicOperator.MINUS
                        val raw = if (negative) "-${number.raw}" else number.raw
                        val value = if (negative) -number.value else number.value
                        return DataItem.Number(value, raw, token.position)
                    }
                }
                else -> Unit
            }
            fail(BasicParseErrorKind.EXPECTED_EXPRESSION, token.position, "Expected DATA item")
        }

        private fun parseRead(): BasicStatement.Read {
            val token = advance() as BasicToken.Keyword
            val targets = ArrayList<BasicExpression.LValue>()
            targets += parseLValue("READ")
            while (matchPunctuation(BasicPunctuation.COMMA)) targets += parseLValue("READ")
            return BasicStatement.Read(targets, token.position)
        }

        private fun parseRestore(): BasicStatement.Restore {
            val token = advance() as BasicToken.Keyword
            val line = if (peek() is BasicToken.Number) parseLineReference("RESTORE") else null
            return BasicStatement.Restore(line, token.position)
        }

        private fun parseDim(): BasicStatement.Dim {
            val token = advance() as BasicToken.Keyword
            val declarations = ArrayList<DimDeclaration>()
            declarations += parseDimDeclaration()
            while (matchPunctuation(BasicPunctuation.COMMA)) declarations += parseDimDeclaration()
            return BasicStatement.Dim(declarations, token.position)
        }

        private fun parseDimDeclaration(): DimDeclaration {
            val token = peek()
            if (token !is BasicToken.Identifier) {
                fail(BasicParseErrorKind.INVALID_ASSIGNMENT_TARGET, token.position, "Expected array name after DIM")
            }
            advance()
            if (!matchPunctuation(BasicPunctuation.LEFT_PAREN)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected '(' after array name")
            }
            val dimensions = parseExpressionList(BasicPunctuation.RIGHT_PAREN, allowEmpty = false)
            expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after DIM dimensions")
            return DimDeclaration(token.name, dimensions, token.position)
        }

        private fun parseGraphics(token: BasicToken.Keyword): BasicStatement.Graphics {
            val expected = graphicsArity.getValue(token.keyword)
            advance()
            val parenthesised = matchPunctuation(BasicPunctuation.LEFT_PAREN)
            val arguments = ArrayList<BasicExpression>()
            if (!parenthesised || !isPunctuation(BasicPunctuation.RIGHT_PAREN)) {
                arguments += expectExpression(after = token.keyword.spelling)
                while (matchPunctuation(BasicPunctuation.COMMA)) arguments += expectExpression(after = ",")
            }
            if (parenthesised) {
                expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after ${token.keyword.spelling} arguments")
            }
            if (arguments.size != expected) {
                fail(
                    BasicParseErrorKind.MALFORMED_STATEMENT,
                    token.position,
                    "${token.keyword.spelling} expects $expected arguments but found ${arguments.size}"
                )
            }
            return BasicStatement.Graphics(token.keyword, arguments, token.position)
        }

        private fun parseLocate(): BasicStatement.Locate {
            val token = advance() as BasicToken.Keyword
            val x = expectExpression(after = "LOCATE")
            if (!matchPunctuation(BasicPunctuation.COMMA)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected ',' after LOCATE X")
            }
            val y = expectExpression(after = ",")
            return BasicStatement.Locate(x, y, token.position)
        }

        private fun parsePoke(): BasicStatement.Poke {
            val token = advance() as BasicToken.Keyword
            val address = expectExpression(after = "POKE")
            if (!matchPunctuation(BasicPunctuation.COMMA)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected ',' after POKE address")
            }
            val value = expectExpression(after = ",")
            return BasicStatement.Poke(address, value, token.position)
        }

        private fun parseCommand(token: BasicToken.Keyword): BasicStatement.Command {
            advance()
            val arguments = ArrayList<BasicExpression>()
            if (matchPunctuation(BasicPunctuation.LEFT_PAREN)) {
                if (!isPunctuation(BasicPunctuation.RIGHT_PAREN)) {
                    arguments += expectExpression(after = token.keyword.spelling)
                    while (matchPunctuation(BasicPunctuation.COMMA)) arguments += expectExpression(after = ",")
                }
                expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after ${token.keyword.spelling}")
            } else if (!atStatementEnd()) {
                arguments += expectExpression(after = token.keyword.spelling)
                while (matchPunctuation(BasicPunctuation.COMMA)) arguments += expectExpression(after = ",")
            }
            return BasicStatement.Command(token.keyword, arguments, token.position)
        }

        // --- expressions ----------------------------------------------------

        private fun parseExpression(): BasicExpression = parseRelational()

        private fun parseRelational(): BasicExpression {
            var left = parseAdditive()
            while (true) {
                val token = peek()
                if (token !is BasicToken.Operator || token.operator !in relationalOperators) return left
                advance()
                val right = expectOperand(token.operator.symbol) { parseAdditive() }
                left = BasicExpression.BinaryExpression(left, token.operator, right, left.position)
            }
        }

        private fun parseAdditive(): BasicExpression {
            var left = parseMultiplicative()
            while (true) {
                val token = peek()
                if (token !is BasicToken.Operator ||
                    (token.operator != BasicOperator.PLUS && token.operator != BasicOperator.MINUS)
                ) {
                    return left
                }
                advance()
                val right = expectOperand(token.operator.symbol) { parseMultiplicative() }
                left = BasicExpression.BinaryExpression(left, token.operator, right, left.position)
            }
        }

        private fun parseMultiplicative(): BasicExpression {
            var left = parseUnary()
            while (true) {
                val token = peek()
                if (token !is BasicToken.Operator ||
                    (token.operator != BasicOperator.TIMES && token.operator != BasicOperator.DIVIDE)
                ) {
                    return left
                }
                advance()
                val right = expectOperand(token.operator.symbol) { parseUnary() }
                left = BasicExpression.BinaryExpression(left, token.operator, right, left.position)
            }
        }

        private fun parseUnary(): BasicExpression {
            val token = peek()
            if (token is BasicToken.Operator &&
                (token.operator == BasicOperator.PLUS || token.operator == BasicOperator.MINUS)
            ) {
                advance()
                val operand = parseUnary()
                return BasicExpression.UnaryExpression(token.operator, operand, token.position)
            }
            return parsePower()
        }

        private fun parsePower(): BasicExpression {
            val left = parsePrimary()
            val token = peek()
            if (token is BasicToken.Operator && token.operator == BasicOperator.POWER) {
                advance()
                val right = parseUnary()
                return BasicExpression.BinaryExpression(left, token.operator, right, left.position)
            }
            return left
        }

        private fun parsePrimary(): BasicExpression {
            val token = peek()
            return when (token) {
                is BasicToken.Number -> {
                    advance()
                    BasicExpression.NumberLiteral(token.value, token.raw, token.position)
                }
                is BasicToken.StringLiteral -> {
                    advance()
                    BasicExpression.StringLiteral(token.value, token.position)
                }
                is BasicToken.Constant -> {
                    advance()
                    BasicExpression.ConstantReference(token.constant, token.position)
                }
                is BasicToken.LineNumber -> {
                    // A standalone expression parsed from source has its leading
                    // number tokenized as a line number. Reinterpret it as a
                    // literal: line numbers never occur *inside* program
                    // expressions, so this only affects the expression-only API.
                    advance()
                    BasicExpression.NumberLiteral(token.value.toDouble(), token.value.toString(), token.position)
                }
                is BasicToken.Identifier -> parseIdentifierOrArray()
                is BasicToken.Function -> parseFunctionCall()
                is BasicToken.Keyword ->
                    if (token.keyword == BasicKeyword.PEEK) {
                        parsePseudoFunction(token)
                    } else {
                        fail(BasicParseErrorKind.EXPECTED_EXPRESSION, token.position, "Unexpected keyword '${token.keyword.spelling}' in expression")
                    }
                is BasicToken.Punctuation ->
                    if (token.punctuation == BasicPunctuation.LEFT_PAREN) {
                        parseGrouping()
                    } else {
                        fail(BasicParseErrorKind.EXPECTED_EXPRESSION, token.position, "Unexpected token '${token.punctuation.symbol}'")
                    }
                else ->
                    fail(BasicParseErrorKind.EXPECTED_EXPRESSION, token.position, "Unexpected token ${quote(token)}")
            }
        }

        private fun parseGrouping(): BasicExpression {
            val open = advance() as BasicToken.Punctuation
            val inner = expectExpression()
            expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after expression")
            return BasicExpression.Grouping(inner, open.position)
        }

        private fun parseIdentifierOrArray(): BasicExpression {
            val token = advance() as BasicToken.Identifier
            if (!isPunctuation(BasicPunctuation.LEFT_PAREN)) {
                return BasicExpression.VariableReference(token.name, token.position)
            }
            advance()
            val indices = parseExpressionList(BasicPunctuation.RIGHT_PAREN, allowEmpty = false)
            expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after subscript")
            return BasicExpression.ArrayReference(token.name, indices, token.position)
        }

        private fun parseFunctionCall(): BasicExpression {
            val token = advance() as BasicToken.Function
            if (!isPunctuation(BasicPunctuation.LEFT_PAREN)) {
                if (token.function in nullaryFunctions) {
                    return BasicExpression.FunctionCall(token.function, emptyList(), token.position)
                }
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected '(' after function ${token.function.spelling}")
            }
            advance()
            val arguments = parseExpressionList(BasicPunctuation.RIGHT_PAREN, allowEmpty = true)
            expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after function arguments")
            return BasicExpression.FunctionCall(token.function, arguments, token.position)
        }

        private fun parsePseudoFunction(token: BasicToken.Keyword): BasicExpression {
            advance()
            if (!matchPunctuation(BasicPunctuation.LEFT_PAREN)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, "Expected '(' after ${token.keyword.spelling}")
            }
            val arguments = parseExpressionList(BasicPunctuation.RIGHT_PAREN, allowEmpty = false)
            expectPunctuation(BasicPunctuation.RIGHT_PAREN, "Expected ')' after ${token.keyword.spelling}")
            return BasicExpression.KeywordCall(token.keyword, arguments, token.position)
        }

        // --- expression helpers --------------------------------------------

        private fun parseExpressionList(
            close: BasicPunctuation,
            allowEmpty: Boolean
        ): List<BasicExpression> {
            val list = ArrayList<BasicExpression>()
            if (isPunctuation(close)) {
                if (!allowEmpty) fail(BasicParseErrorKind.EXPECTED_EXPRESSION, peek().position, "Expected expression")
                return list
            }
            list += expectExpression()
            while (matchPunctuation(BasicPunctuation.COMMA)) list += expectExpression(after = ",")
            return list
        }

        private fun expectExpression(after: String? = null): BasicExpression {
            if (!startsExpression(peek())) {
                val detail = if (after != null) "Expected expression after '$after'" else "Expected expression"
                fail(BasicParseErrorKind.EXPECTED_EXPRESSION, peek().position, detail)
            }
            return parseExpression()
        }

        /**
         * Requires an expression at [after] and parses it with [parse], which
         * must be the next precedence level down. Using the correct level is
         * what makes `A*2+5` parse as `(A*2)+5` rather than `A*(2+5)`.
         */
        private inline fun expectOperand(after: String, parse: () -> BasicExpression): BasicExpression {
            if (!startsExpression(peek())) {
                fail(BasicParseErrorKind.EXPECTED_EXPRESSION, peek().position, "Expected expression after '$after'")
            }
            return parse()
        }

        private fun startsExpression(token: BasicToken): Boolean = when (token) {
            is BasicToken.Number,
            is BasicToken.StringLiteral,
            is BasicToken.Constant,
            is BasicToken.Identifier,
            is BasicToken.Function -> true
            is BasicToken.Keyword -> token.keyword == BasicKeyword.PEEK
            is BasicToken.Punctuation -> token.punctuation == BasicPunctuation.LEFT_PAREN
            is BasicToken.Operator ->
                token.operator == BasicOperator.PLUS || token.operator == BasicOperator.MINUS
            else -> false
        }

        private fun expectPunctuation(punctuation: BasicPunctuation, message: String) {
            if (!matchPunctuation(punctuation)) {
                fail(BasicParseErrorKind.EXPECTED_TOKEN, peek().position, message)
            }
        }

        // --- line references ------------------------------------------------

        private fun parseLineReference(context: String): Int {
            val token = peek()
            val line = lineNumberOf(token)
            if (line == null || line <= 0) {
                fail(BasicParseErrorKind.EXPECTED_LINE_NUMBER, token.position, "Expected line number after '$context'")
            }
            advance()
            return line
        }

        /** The integer line number [token] denotes, or `null` if it is not one. */
        private fun lineNumberOf(token: BasicToken): Int? {
            if (token !is BasicToken.Number) return null
            val value = token.value
            if (value % 1.0 != 0.0 || value < 0.0 || value > Int.MAX_VALUE.toDouble()) return null
            return value.toInt()
        }

        /** True when the token [offset] past the cursor finishes the statement. */
        private fun endsStatementAt(offset: Int): Boolean {
            val token = peekAt(offset)
            return token is BasicToken.EndOfLine ||
                token is BasicToken.EndOfInput ||
                (token is BasicToken.Punctuation && token.punctuation == BasicPunctuation.COLON)
        }






    }
}
