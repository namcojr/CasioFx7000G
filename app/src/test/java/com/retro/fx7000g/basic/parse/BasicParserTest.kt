package com.retro.fx7000g.basic.parse

import com.retro.fx7000g.basic.lex.BasicFunction
import com.retro.fx7000g.basic.lex.BasicKeyword
import com.retro.fx7000g.basic.lex.BasicOperator
import com.retro.fx7000g.basic.lex.BasicTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [BasicParser]. They verify the produced structure only - no program
 * is executed, no expression is evaluated and no memory is touched.
 */
class BasicParserTest {

    private fun line(src: String): BasicProgramLine = BasicParser.parseLine(src)

    private fun statement(src: String): BasicStatement = BasicParser.parseStatement(src)

    private fun expression(src: String): BasicExpression = BasicParser.parseExpression(src)

    private fun assignment(src: String): BasicStatement.Assignment =
        statement(src) as BasicStatement.Assignment

    /** Parses `A=<value>` and returns the right-hand side expression. */
    private fun rhs(value: String): BasicExpression = assignment("A=$value").value

    private fun binary(e: BasicExpression): BasicExpression.BinaryExpression =
        e as BasicExpression.BinaryExpression

    private fun unary(e: BasicExpression): BasicExpression.UnaryExpression =
        e as BasicExpression.UnaryExpression

    // --- assignment ---------------------------------------------------------

    @Test
    fun assignmentParsesTargetAndValue() {
        val a = assignment("A=10")
        assertEquals("A", (a.target as BasicExpression.VariableReference).name)
        val n = a.value as BasicExpression.NumberLiteral
        assertEquals(10.0, n.value, 0.0)
        assertEquals("10", n.raw)
        assertFalse(a.explicitLet)
    }

    @Test
    fun letIsTheSameAssignmentFlaggedExplicit() {
        val a = assignment("LET A=10")
        assertTrue(a.explicitLet)
        assertEquals("A", (a.target as BasicExpression.VariableReference).name)
    }

    @Test
    fun rhsMayBeAnotherVariable() {
        val v = rhs("B") as BasicExpression.VariableReference
        assertEquals("B", v.name)
    }

    // --- operator precedence (mandatory checks) -----------------------------

    @Test
    fun multiplicationBindsTighterThanAddition() {
        // A=2+3*4  ->  Binary(+) with Binary(*) on the right
        val root = binary(rhs("2+3*4"))
        assertEquals(BasicOperator.PLUS, root.operator)
        assertEquals(2.0, (root.left as BasicExpression.NumberLiteral).value, 0.0)
        assertEquals(BasicOperator.TIMES, binary(root.right).operator)
    }

    @Test
    fun parenthesesOverridePrecedence() {
        // A=(2+3)*4  ->  Binary(*) with a grouped Binary(+) on the left
        val root = binary(rhs("(2+3)*4"))
        assertEquals(BasicOperator.TIMES, root.operator)
        val left = root.left as BasicExpression.Grouping
        assertEquals(BasicOperator.PLUS, binary(left.inner).operator)
    }

    @Test
    fun unaryMinusAppliesBeforeMultiplication() {
        // A=-2*3  ->  Binary(*) whose left operand is Unary(-, 2)
        val root = binary(rhs("-2*3"))
        assertEquals(BasicOperator.TIMES, root.operator)
        val left = unary(root.left)
        assertEquals(BasicOperator.MINUS, left.operator)
        assertEquals(2.0, (left.operand as BasicExpression.NumberLiteral).value, 0.0)
    }

    @Test
    fun arithmeticIsLeftAssociativeAndMixedPrecedenceIsCorrect() {
        // 2+3+4 -> (2+3)+4
        val sum = binary(rhs("2+3+4"))
        assertEquals(BasicOperator.PLUS, sum.operator)
        assertEquals(BasicOperator.PLUS, binary(sum.left).operator)

        // 2*3+4 -> (2*3)+4, NOT 2*(3+4)
        val mixed = binary(rhs("2*3+4"))
        assertEquals(BasicOperator.PLUS, mixed.operator)
        assertEquals(BasicOperator.TIMES, binary(mixed.left).operator)

        // 10-3-2 -> (10-3)-2
        val minus = binary(rhs("10-3-2"))
        assertEquals(BasicOperator.MINUS, minus.operator)
        assertEquals(BasicOperator.MINUS, binary(minus.left).operator)
    }

    @Test
    fun powerIsRightAssociative() {
        // 2^3^2 -> 2^(3^2)
        val root = binary(rhs("2^3^2"))
        assertEquals(BasicOperator.POWER, root.operator)
        assertEquals(BasicOperator.POWER, binary(root.right).operator)
    }

    // --- parentheses --------------------------------------------------------

    @Test
    fun rightHandGrouping() {
        val root = binary(rhs("2*(3+4)"))
        assertEquals(BasicOperator.TIMES, root.operator)
        val right = root.right as BasicExpression.Grouping
        assertEquals(BasicOperator.PLUS, binary(right.inner).operator)
    }

    @Test
    fun nestedParenthesesArePreserved() {
        val outer = rhs("((A))") as BasicExpression.Grouping
        val inner = outer.inner as BasicExpression.Grouping
        assertEquals("A", (inner.inner as BasicExpression.VariableReference).name)
    }

    // --- unary operators ----------------------------------------------------

    @Test
    fun unaryMinusOnANumber() {
        val u = unary(rhs("-10"))
        assertEquals(BasicOperator.MINUS, u.operator)
        assertEquals(10.0, (u.operand as BasicExpression.NumberLiteral).value, 0.0)
    }

    @Test
    fun unaryMinusOnAVariable() {
        val u = unary(rhs("-B"))
        assertEquals("B", (u.operand as BasicExpression.VariableReference).name)
    }

    @Test
    fun unaryMinusOnAGroup() {
        val u = unary(rhs("-(B+1)"))
        val group = u.operand as BasicExpression.Grouping
        assertEquals(BasicOperator.PLUS, binary(group.inner).operator)
    }

    // --- strings ------------------------------------------------------------

    @Test
    fun stringVariableAssignment() {
        val a = assignment("A$=\"HELLO\"")
        assertEquals("A$", (a.target as BasicExpression.VariableReference).name)
        assertEquals("HELLO", (a.value as BasicExpression.StringLiteral).value)
    }

    @Test
    fun printStringLiteral() {
        val p = statement("PRINT \"HELLO\"") as BasicStatement.Print
        assertEquals(1, p.items.size)
        assertEquals("HELLO", (p.items[0].expression as BasicExpression.StringLiteral).value)
        assertNull(p.items[0].separator)
    }

    @Test
    fun printStringVariable() {
        val p = statement("PRINT A$") as BasicStatement.Print
        assertEquals("A$", (p.items[0].expression as BasicExpression.VariableReference).name)
    }

    // --- functions ----------------------------------------------------------

    @Test
    fun functionCallWithAnArgument() {
        val call = rhs("SIN(X)") as BasicExpression.FunctionCall
        assertEquals(BasicFunction.SIN, call.function)
        assertEquals(1, call.arguments.size)
        assertEquals("X", (call.arguments[0] as BasicExpression.VariableReference).name)
    }

    @Test
    fun functionsComposeWithOperators() {
        val root = binary(rhs("COS(X)+SIN(Y)"))
        assertEquals(BasicOperator.PLUS, root.operator)
        assertEquals(BasicFunction.COS, (root.left as BasicExpression.FunctionCall).function)
        assertEquals(BasicFunction.SIN, (root.right as BasicExpression.FunctionCall).function)
    }

    @Test
    fun functionArgumentIsAFullExpression() {
        val call = rhs("ABS(B-C)") as BasicExpression.FunctionCall
        assertEquals(BasicFunction.ABS, call.function)
        assertEquals(BasicOperator.MINUS, binary(call.arguments[0]).operator)
    }

    @Test
    fun nullaryFunctionsNeedNoParentheses() {
        val inkey = rhs("INKEY$") as BasicExpression.FunctionCall
        assertEquals(BasicFunction.INKEY, inkey.function)
        assertTrue(inkey.arguments.isEmpty())
        val ran = rhs("RAN#") as BasicExpression.FunctionCall
        assertEquals(BasicFunction.RAN, ran.function)
        assertTrue(ran.arguments.isEmpty())
    }

    @Test
    fun peekIsParsedAsAFunctionLikeKeyword() {
        // PEEK is tokenized as a keyword but used as a function in an expression.
        val call = rhs("PEEK(1)") as BasicExpression.KeywordCall
        assertEquals(BasicKeyword.PEEK, call.keyword)
        assertEquals(1, call.arguments.size)
    }

    // --- array references ---------------------------------------------------

    @Test
    fun arrayElementAssignment() {
        val a = assignment("A(10)=5")
        val target = a.target as BasicExpression.ArrayReference
        assertEquals("A", target.name)
        assertEquals(1, target.indices.size)
        assertEquals(10.0, (target.indices[0] as BasicExpression.NumberLiteral).value, 0.0)
    }

    @Test
    fun arrayReferenceInsideAnExpression() {
        val root = binary(rhs("A(I)+1"))
        val array = root.left as BasicExpression.ArrayReference
        assertEquals("A", array.name)
        assertEquals("I", (array.indices[0] as BasicExpression.VariableReference).name)
    }

    @Test
    fun multiDimensionalArrayReference() {
        val call = rhs("M(I,J)") as BasicExpression.ArrayReference
        assertEquals("M", call.name)
        assertEquals(2, call.indices.size)
    }

    // --- expression-only API ------------------------------------------------

    @Test
    fun parseExpressionFromSource() {
        val root = binary(expression("2+3*4"))
        assertEquals(BasicOperator.PLUS, root.operator)
    }

    @Test
    fun parseExpressionFromTokens() {
        val root = binary(BasicParser.parseExpression(BasicTokenizer.tokenize("2+3")))
        assertEquals(BasicOperator.PLUS, root.operator)
    }

    // --- program and line structure -----------------------------------------

    @Test
    fun programLinesKeepTheirNumbersAndOrder() {
        val program = BasicParser.parseProgram("30 PRINT 3\n10 PRINT 1\n20 PRINT 2")
        assertEquals(listOf(30, 10, 20), program.lines.map { it.lineNumber })
    }

    @Test
    fun duplicateLineNumbersAreBothPreserved() {
        val program = BasicParser.parseProgram("10 PRINT 1\n10 PRINT 2")
        assertEquals(2, program.lines.size)
        assertTrue(program.lines.all { it.lineNumber == 10 })
    }

    @Test
    fun eachStatementOnItsOwnLine() {
        val program = BasicParser.parseProgram("10 A=10\n20 PRINT A")
        assertEquals(2, program.lines.size)
        assertTrue(program.lines[0].statements.single() is BasicStatement.Assignment)
        assertTrue(program.lines[1].statements.single() is BasicStatement.Print)
    }

    @Test
    fun lineNumberAloneYieldsNoStatements() {
        val l = line("10")
        assertEquals(10, l.lineNumber)
        assertTrue(l.statements.isEmpty())
    }

    @Test
    fun immediateLineHasNoLineNumber() {
        val l = line("PRINT A")
        assertTrue(l.isImmediate)
        assertNull(l.lineNumber)
        assertTrue(l.statements.single() is BasicStatement.Print)
    }

    @Test
    fun lineNumberedImmediateFormIsAlsoAccepted() {
        val l = line("10 PRINT A")
        assertEquals(10, l.lineNumber)
        assertEquals(1, l.statements.size)
    }

    // --- statement separators ----------------------------------------------

    @Test
    fun colonSeparatesStatements() {
        val l = line("10 A=10:B=20:PRINT A+B")
        assertEquals(10, l.lineNumber)
        assertEquals(3, l.statements.size)
        assertTrue(l.statements[0] is BasicStatement.Assignment)
        assertTrue(l.statements[1] is BasicStatement.Assignment)
        assertTrue(l.statements[2] is BasicStatement.Print)
    }

    @Test
    fun questionMarkIsPrintShorthand() {
        val l = line("10 ? A")
        val p = l.statements.single() as BasicStatement.Print
        assertEquals("A", (p.items[0].expression as BasicExpression.VariableReference).name)
    }

    // --- control flow: GOTO / GOSUB / RETURN --------------------------------

    @Test
    fun gotoReferencesALineNumber() {
        val g = line("10 GOTO 100").statements.single() as BasicStatement.Goto
        assertEquals(100, g.line)
        assertFalse(g.implicit)
    }

    @Test
    fun gosubReferencesALineNumber() {
        val g = line("20 GOSUB 500").statements.single() as BasicStatement.Gosub
        assertEquals(500, g.line)
    }

    @Test
    fun returnAndEndAndStop() {
        assertTrue(line("30 RETURN").statements.single() is BasicStatement.Return)
        assertTrue(line("40 END").statements.single() is BasicStatement.End)
        assertTrue(line("50 STOP").statements.single() is BasicStatement.Stop)
    }

    // --- IF -----------------------------------------------------------------

    @Test
    fun ifWithPrintBody() {
        val i = line("10 IF A>10 THEN PRINT A").statements.single() as BasicStatement.If
        val condition = binary(i.condition)
        assertEquals(BasicOperator.GREATER, condition.operator)
        assertEquals("A", (condition.left as BasicExpression.VariableReference).name)
        assertTrue(i.thenBranch is BasicStatement.Print)
    }

    @Test
    fun ifWithGotoBody() {
        val i = line("10 IF A=10 THEN GOTO 100").statements.single() as BasicStatement.If
        val branch = i.thenBranch as BasicStatement.Goto
        assertEquals(100, branch.line)
        assertFalse(branch.implicit)
    }

    @Test
    fun ifWithCompactLineTarget() {
        val i = line("10 IF A>10 THEN 50").statements.single() as BasicStatement.If
        val branch = i.thenBranch as BasicStatement.Goto
        assertEquals(50, branch.line)
        assertTrue(branch.implicit)
    }

    // --- FOR / NEXT ---------------------------------------------------------

    @Test
    fun forWithoutStep() {
        val f = line("10 FOR I=1 TO 10").statements.single() as BasicStatement.For
        assertEquals("I", f.variable.name)
        assertEquals(1.0, (f.start as BasicExpression.NumberLiteral).value, 0.0)
        assertEquals(10.0, (f.limit as BasicExpression.NumberLiteral).value, 0.0)
        assertNull(f.step)
    }

    @Test
    fun forWithStep() {
        val f = line("10 FOR I=10 TO 1 STEP -1").statements.single() as BasicStatement.For
        val step = unary(f.step!!)
        assertEquals(BasicOperator.MINUS, step.operator)
    }

    @Test
    fun nextWithAndWithoutVariable() {
        val withVar = line("20 NEXT I").statements.single() as BasicStatement.Next
        assertEquals("I", withVar.variable!!.name)
        val without = line("30 NEXT").statements.single() as BasicStatement.Next
        assertNull(without.variable)
    }

    // --- DATA / READ / RESTORE ---------------------------------------------

    @Test
    fun dataWithNumbers() {
        val d = line("10 DATA 10,20,30").statements.single() as BasicStatement.Data
        assertEquals(3, d.items.size)
        assertEquals(20.0, (d.items[1] as DataItem.Number).value, 0.0)
    }

    @Test
    fun dataWithStringAndNegativeNumber() {
        val d = line("10 DATA \"AB\",-2").statements.single() as BasicStatement.Data
        assertEquals("AB", (d.items[0] as DataItem.Text).value)
        val number = d.items[1] as DataItem.Number
        assertEquals(-2.0, number.value, 0.0)
        assertEquals("-2", number.raw)
    }

    @Test
    fun dataWithUnquotedWord() {
        val d = line("10 DATA HELLO").statements.single() as BasicStatement.Data
        assertEquals("HELLO", (d.items.single() as DataItem.Word).text)
    }

    @Test
    fun readTargets() {
        val r = line("20 READ A,B").statements.single() as BasicStatement.Read
        assertEquals(2, r.targets.size)
        assertEquals("A", (r.targets[0] as BasicExpression.VariableReference).name)
    }

    @Test
    fun restoreWithAndWithoutLine() {
        assertNull((line("10 RESTORE").statements.single() as BasicStatement.Restore).line)
        assertEquals(100, (line("10 RESTORE 100").statements.single() as BasicStatement.Restore).line)
    }

    // --- DIM ----------------------------------------------------------------

    @Test
    fun dimWithoutAndWithDimensions() {
        val one = line("10 DIM A(10)").statements.single() as BasicStatement.Dim
        assertEquals("A", one.declarations.single().name)
        assertEquals(1, one.declarations.single().dimensions.size)

        val two = line("10 DIM A(10),B(5,5)").statements.single() as BasicStatement.Dim
        assertEquals(2, two.declarations.size)
        assertEquals(2, two.declarations[1].dimensions.size)
    }

    // --- graphics -----------------------------------------------------------

    @Test
    fun plotAndDrawTakeTwoArguments() {
        val plot = line("10 PLOT X,Y").statements.single() as BasicStatement.Graphics
        assertEquals(BasicKeyword.PLOT, plot.keyword)
        assertEquals(2, plot.arguments.size)
        val draw = line("20 DRAW X,Y").statements.single() as BasicStatement.Graphics
        assertEquals(BasicKeyword.DRAW, draw.keyword)
    }

    @Test
    fun circleTakesThreeArguments() {
        val circle = line("30 CIRCLE 50,30,10").statements.single() as BasicStatement.Graphics
        assertEquals(BasicKeyword.CIRCLE, circle.keyword)
        assertEquals(3, circle.arguments.size)
    }

    @Test
    fun parenthesisedGraphicsForm() {
        val circle = line("10 CIRCLE(50,50,20)").statements.single() as BasicStatement.Graphics
        assertEquals(3, circle.arguments.size)
    }

    @Test
    fun rectLineAndBoxTakeFourArguments() {
        assertEquals(4, (line("40 RECT 10,10,50,30").statements.single() as BasicStatement.Graphics).arguments.size)
        assertEquals(4, (line("10 LINE 1,2,3,4").statements.single() as BasicStatement.Graphics).arguments.size)
        assertEquals(4, (line("10 BOX 1,2,3,4").statements.single() as BasicStatement.Graphics).arguments.size)
    }

    // --- INPUT --------------------------------------------------------------

    @Test
    fun inputWithoutPrompt() {
        val i = line("10 INPUT A").statements.single() as BasicStatement.Input
        assertNull(i.prompt)
        assertEquals("A", (i.targets.single() as BasicExpression.VariableReference).name)
    }

    @Test
    fun inputWithStringVariable() {
        val i = line("10 INPUT A$").statements.single() as BasicStatement.Input
        assertEquals("A$", (i.targets.single() as BasicExpression.VariableReference).name)
    }

    @Test
    fun inputWithPromptAndMultipleTargets() {
        val i = line("10 INPUT \"VALUE\";A,B").statements.single() as BasicStatement.Input
        assertEquals("VALUE", i.prompt!!.value)
        assertEquals(2, i.targets.size)
    }

    // --- REM ----------------------------------------------------------------

    @Test
    fun remCapturesTheCommentTail() {
        val r = line("10 REM THIS IS A COMMENT").statements.single() as BasicStatement.Rem
        assertEquals(" THIS IS A COMMENT", r.text)
    }

    @Test
    fun remCanFollowAnotherStatement() {
        val l = line("10 A=10:REM X")
        assertEquals(2, l.statements.size)
        assertTrue(l.statements[1] is BasicStatement.Rem)
    }

    // --- simple / immediate commands ---------------------------------------

    @Test
    fun nullaryCommandsParseWithoutArguments() {
        for (src in listOf("CLS", "BEEP", "LIST", "RUN", "CONT", "FREE", "NEW", "CLEAR")) {
            val c = statement(src) as BasicStatement.Command
            assertTrue("$src should have no arguments", c.arguments.isEmpty())
        }
    }

    @Test
    fun locateAndPokeHaveBespokeStatements() {
        val locate = line("10 LOCATE 5,3").statements.single() as BasicStatement.Locate
        assertEquals(5.0, (locate.x as BasicExpression.NumberLiteral).value, 0.0)
        assertEquals(3.0, (locate.y as BasicExpression.NumberLiteral).value, 0.0)

        val poke = line("10 POKE 1,2").statements.single() as BasicStatement.Poke
        assertEquals(1.0, (poke.address as BasicExpression.NumberLiteral).value, 0.0)
    }

    // --- PRINT separators ---------------------------------------------------

    @Test
    fun printSeparatorsArePreserved() {
        val p = statement("PRINT A;B;C") as BasicStatement.Print
        assertEquals(3, p.items.size)
        assertEquals(BasicPrintSeparator.SEMICOLON, p.items[0].separator)
        assertEquals(BasicPrintSeparator.SEMICOLON, p.items[1].separator)
        assertNull(p.items[2].separator)
    }

    @Test
    fun trailingPrintSeparatorIsPreserved() {
        val p = statement("PRINT A;") as BasicStatement.Print
        assertEquals(BasicPrintSeparator.SEMICOLON, p.items.single().separator)
    }

    @Test
    fun barePrintHasNoItems() {
        val p = statement("PRINT") as BasicStatement.Print
        assertTrue(p.items.isEmpty())
    }

    // --- negative tests -----------------------------------------------------

    private fun parseError(src: String): BasicParseException =
        assertThrows(BasicParseException::class.java) { statement(src) }

    private fun lineError(src: String): BasicParseException =
        assertThrows(BasicParseException::class.java) { line(src) }

    @Test
    fun missingExpressionAfterEquals() {
        val ex = parseError("A=")
        assertEquals(BasicParseErrorKind.EXPECTED_EXPRESSION, ex.kind)
        assertEquals("Expected expression after '='", ex.detail)
        assertTrue(ex.message!!.startsWith("Line 1, column 3:"))
    }

    @Test
    fun invalidStartOfExpression() {
        assertEquals(BasicParseErrorKind.EXPECTED_EXPRESSION, parseError("A=*").kind)
    }

    @Test
    fun danglingOperator() {
        assertEquals(BasicParseErrorKind.EXPECTED_EXPRESSION, parseError("A=1+").kind)
    }

    @Test
    fun missingClosingParenthesis() {
        assertEquals(BasicParseErrorKind.EXPECTED_TOKEN, parseError("A=(1+2").kind)
    }

    @Test
    fun strayClosingParenthesis() {
        assertEquals(BasicParseErrorKind.UNEXPECTED_TOKEN, parseError("A=1+2)").kind)
    }

    @Test
    fun printWithALeadingCommaIsRejected() {
        assertEquals(BasicParseErrorKind.EXPECTED_EXPRESSION, parseError("PRINT ,").kind)
    }

    @Test
    fun forWithoutAStartValue() {
        assertEquals(BasicParseErrorKind.EXPECTED_EXPRESSION, parseError("FOR I=").kind)
    }

    @Test
    fun forWithoutTo() {
        val ex = parseError("FOR I=1")
        assertEquals(BasicParseErrorKind.EXPECTED_TOKEN, ex.kind)
        assertEquals("Expected TO", ex.detail)
    }

    @Test
    fun expressionCannotBeAnAssignmentTarget() {
        // The tokenizer reads the leading 10 as a line number, leaving the
        // statement body "+20=30", whose turn this is on an expression token.
        val ex = lineError("10+20=30")
        assertEquals(BasicParseErrorKind.INVALID_ASSIGNMENT_TARGET, ex.kind)
        assertEquals("Invalid assignment target", ex.detail)
    }

    @Test
    fun unknownKeywordIsNotAStatement() {
        assertEquals(BasicParseErrorKind.UNKNOWN_STATEMENT, lineError("10 THEN 50").kind)
    }

    @Test
    fun trailingTokenAfterStatementIsRejected() {
        assertEquals(BasicParseErrorKind.UNEXPECTED_TOKEN, parseError("A=1 B").kind)
    }

    // --- statement separators: no empty statements --------------------------

    @Test
    fun doubleColonCreatesAnEmptyStatement() {
        assertEquals(BasicParseErrorKind.EMPTY_STATEMENT, lineError("10 A=10::PRINT A").kind)
    }

    @Test
    fun trailingColonCreatesAnEmptyStatement() {
        assertEquals(BasicParseErrorKind.EMPTY_STATEMENT, lineError("10 A=10:").kind)
    }

    // --- line references ----------------------------------------------------

    @Test
    fun gotoRequiresALineNumber() {
        assertEquals(BasicParseErrorKind.EXPECTED_LINE_NUMBER, lineError("10 GOTO").kind)
        assertEquals(BasicParseErrorKind.EXPECTED_LINE_NUMBER, lineError("10 GOTO A").kind)
    }

    @Test
    fun gosubRequiresALineNumber() {
        assertEquals(BasicParseErrorKind.EXPECTED_LINE_NUMBER, lineError("10 GOSUB A").kind)
    }

    // --- statement shape ----------------------------------------------------

    @Test
    fun graphicsArityIsChecked() {
        val ex = lineError("10 CIRCLE 1,2")
        assertEquals(BasicParseErrorKind.MALFORMED_STATEMENT, ex.kind)
    }

    @Test
    fun programSourceRequiresLineNumbers() {
        val ex = assertThrows(BasicParseException::class.java) {
            BasicParser.parseProgram("PRINT A")
        }
        assertEquals(BasicParseErrorKind.EXPECTED_LINE_NUMBER, ex.kind)
    }

    @Test
    fun parseErrorsIdentifyTheOffendingLine() {
        val ex = assertThrows(BasicParseException::class.java) {
            BasicParser.parseProgram("10 A=10\n20 B=")
        }
        assertEquals(2, ex.position.line)
        assertEquals(BasicParseErrorKind.EXPECTED_EXPRESSION, ex.kind)
    }

    // --- whole-program shape (PARSER.md end state) --------------------------

    @Test
    fun docExampleProgramProducesTheExpectedStructure() {
        val program = BasicParser.parseProgram(
            "10 A=10\n20 B=A*2+5\n30 IF B>20 THEN PRINT B\n40 GOTO 20"
        )
        assertEquals(listOf(10, 20, 30, 40), program.lines.map { it.lineNumber })

        val first = program.lines[0].statements.single() as BasicStatement.Assignment
        assertEquals(10.0, (first.value as BasicExpression.NumberLiteral).value, 0.0)

        // 20 B=A*2+5  ->  B = (A*2)+5
        val second = program.lines[1].statements.single() as BasicStatement.Assignment
        val sum = binary(second.value)
        assertEquals(BasicOperator.PLUS, sum.operator)
        assertEquals(BasicOperator.TIMES, binary(sum.left).operator)

        // 30 IF B>20 THEN PRINT B
        val third = program.lines[2].statements.single() as BasicStatement.If
        assertEquals(BasicOperator.GREATER, binary(third.condition).operator)
        assertTrue(third.thenBranch is BasicStatement.Print)

        // 40 GOTO 20
        val fourth = program.lines[3].statements.single() as BasicStatement.Goto
        assertEquals(20, fourth.line)
    }

    @Test
    fun blankLinesAndCrlfAreHandled() {
        val program = BasicParser.parseProgram("10 A=1\r\n\r\n20 END")
        assertEquals(listOf(10, 20), program.lines.map { it.lineNumber })
    }

    @Test
    fun emptyProgramHasNoLines() {
        assertTrue(BasicParser.parseProgram("").lines.isEmpty())
        assertTrue(BasicParser.parseProgram("   \n\n").lines.isEmpty())
    }




}


