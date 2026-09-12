package com.retro.fx7000g.basic.lex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [BasicTokenizer]. These verify the produced token sequence only -
 * no program is executed and no semantic validation is performed.
 */
class BasicTokenizerTest {

    /** Convenience: tokenize [src] and render each token as a short string. */
    private fun sig(src: String): List<String> = BasicTokenizer.tokenize(src).map { it.describe() }

    private fun firstNumber(src: String): BasicToken.Number =
        BasicTokenizer.tokenize(src).first { it is BasicToken.Number } as BasicToken.Number

    // --- fundamental line shapes -------------------------------------------

    @Test
    fun printString() {
        assertEquals(
            listOf("LINE(10)", "KW(PRINT)", "STR(\"HELLO\")", "EOL", "EOF"),
            sig("10 PRINT \"HELLO\"")
        )
    }

    @Test
    fun assignment() {
        assertEquals(
            listOf("LINE(10)", "ID(A)", "OP(=)", "NUM(10)", "EOL", "EOF"),
            sig("10 A=10")
        )
    }

    @Test
    fun forNextProgramWithStep() {
        val src = "10 FOR I=1 TO 10 STEP 1\n20 PRINT I\n30 NEXT I"
        assertEquals(
            listOf(
                "LINE(10)", "KW(FOR)", "ID(I)", "OP(=)", "NUM(1)", "KW(TO)",
                "NUM(10)", "KW(STEP)", "NUM(1)", "EOL",
                "LINE(20)", "KW(PRINT)", "ID(I)", "EOL",
                "LINE(30)", "KW(NEXT)", "ID(I)", "EOL",
                "EOF"
            ),
            sig(src)
        )
    }

    @Test
    fun ifThenGoto() {
        assertEquals(
            listOf(
                "LINE(10)", "KW(IF)", "ID(A)", "OP(>)", "NUM(10)",
                "KW(THEN)", "NUM(50)", "EOL", "EOF"
            ),
            sig("10 IF A>10 THEN 50")
        )
    }

    @Test
    fun ifThenGotoKeyword() {
        assertEquals(
            listOf(
                "LINE(10)", "KW(IF)", "ID(A)", "OP(=)", "NUM(10)",
                "KW(THEN)", "KW(GOTO)", "NUM(100)", "EOL", "EOF"
            ),
            sig("10 IF A=10 THEN GOTO 100")
        )
    }

    @Test
    fun goto() {
        assertEquals(
            listOf("LINE(10)", "KW(GOTO)", "NUM(100)", "EOL", "EOF"),
            sig("10 GOTO 100")
        )
    }

    @Test
    fun multipleStatements() {
        assertEquals(
            listOf(
                "LINE(10)", "ID(A)", "OP(=)", "NUM(10)", "PUNCT(:)",
                "KW(PRINT)", "ID(A)", "EOL", "EOF"
            ),
            sig("10 A=10:PRINT A")
        )
    }

    @Test
    fun inputStatement() {
        assertEquals(
            listOf("LINE(10)", "KW(INPUT)", "ID(A)", "EOL", "EOF"),
            sig("10 INPUT A")
        )
    }

    @Test
    fun dataStatement() {
        assertEquals(
            listOf(
                "LINE(10)", "KW(DATA)", "NUM(1)", "PUNCT(,)", "NUM(2)", "PUNCT(,)",
                "NUM(3)", "PUNCT(,)", "NUM(4)", "EOL", "EOF"
            ),
            sig("10 DATA 1,2,3,4")
        )
    }

    @Test
    fun readStatement() {
        assertEquals(
            listOf("LINE(10)", "KW(READ)", "ID(A)", "EOL", "EOF"),
            sig("10 READ A")
        )
    }

    @Test
    fun remCommentConsumesWholeLine() {
        assertEquals(
            listOf(
                "LINE(10)", "KW(REM)", "COMMENT( THIS IS A COMMENT)", "EOL", "EOF"
            ),
            sig("10 REM THIS IS A COMMENT")
        )
    }

    @Test
    fun remDoesNotTokenizeKeywordsInsideComment() {
        val tokens = BasicTokenizer.tokenize("10 REM PRINT FOR NEXT IS NOT CODE")
        val keywordsAfterRem = tokens
            .dropWhile { !(it is BasicToken.Keyword && it.keyword == BasicKeyword.REM) }
            .drop(1)
            .filterIsInstance<BasicToken.Keyword>()
        assertTrue(keywordsAfterRem.isEmpty())
        assertEquals("COMMENT( PRINT FOR NEXT IS NOT CODE)", tokens[2].describe())
    }

    @Test
    fun drawPlotCircle() {
        assertEquals(
            listOf("LINE(10)", "KW(DRAW)", "NUM(10)", "PUNCT(,)", "NUM(20)", "EOL", "EOF"),
            sig("10 DRAW 10,20")
        )
        assertEquals(
            listOf("LINE(10)", "KW(PLOT)", "NUM(30)", "PUNCT(,)", "NUM(40)", "EOL", "EOF"),
            sig("10 PLOT 30,40")
        )
        assertEquals(
            listOf(
                "LINE(10)", "KW(CIRCLE)", "PUNCT(()", "NUM(50)", "PUNCT(,)",
                "NUM(50)", "PUNCT(,)", "NUM(20)", "PUNCT())", "EOL", "EOF"
            ),
            sig("10 CIRCLE(50,50,20)")
        )
    }

    // --- functions, case handling and whitespace ---------------------------

    @Test
    fun leftStringFunction() {
        assertEquals(
            listOf(
                "LINE(10)", "KW(PRINT)", "FN(LEFT$)", "PUNCT(()", "STR(\"HELLO\")",
                "PUNCT(,)", "NUM(2)", "PUNCT())", "EOL", "EOF"
            ),
            sig("10 PRINT LEFT$(\"HELLO\",2)")
        )
    }

    @Test
    fun stringFunctionsAreNotPlainIdentifiers() {
        assertEquals(
            listOf(
                "LINE(10)", "KW(PRINT)", "FN(MID$)", "PUNCT(()", "ID(A$)",
                "PUNCT(,)", "NUM(2)", "PUNCT(,)", "NUM(3)", "PUNCT())", "EOL", "EOF"
            ),
            sig("10 PRINT MID$(A$,2,3)")
        )
        assertEquals(
            listOf("LINE(10)", "ID(A)", "OP(=)", "FN(INKEY$)", "EOL", "EOF"),
            sig("10 A=INKEY$")
        )
        assertEquals(
            listOf("LINE(10)", "ID(A)", "OP(=)", "FN(STR$)", "PUNCT(()", "NUM(3)", "PUNCT())", "EOL", "EOF"),
            sig("10 A=STR$(3)")
        )
    }

    @Test
    fun keywordsAreCaseInsensitive() {
        val expected = listOf("LINE(10)", "KW(PRINT)", "STR(\"HI\")", "EOL", "EOF")
        assertEquals(expected, sig("10 PRINT \"HI\""))
        assertEquals(expected, sig("10 print \"HI\""))
        assertEquals(expected, sig("10 Print \"HI\""))
        assertEquals(expected, sig("10 pRiNt \"HI\""))
    }

    @Test
    fun identifierCaseIsPreserved() {
        assertEquals(
            listOf("LINE(10)", "ID(Counter)", "OP(=)", "NUM(1)", "EOL", "EOF"),
            sig("10 Counter=1")
        )
    }

    @Test
    fun whitespaceIsIgnored() {
        assertEquals(
            listOf("LINE(10)", "KW(PRINT)", "STR(\"HI\")", "EOL", "EOF"),
            sig("  10   PRINT   \"HI\"  ")
        )
    }

    @Test
    fun emptyAndWhitespaceOnlyInput() {
        assertEquals(listOf("EOF"), sig(""))
        assertEquals(listOf("EOF"), sig("   "))
        assertEquals(listOf("EOF"), sig("\t"))
    }

    @Test
    fun lineNumberOnlyIsAValidDeletionLine() {
        assertEquals(listOf("LINE(10)", "EOL", "EOF"), sig("10"))
    }

    @Test
    fun numbersInsideStatementsAreNotLineNumbers() {
        assertEquals(
            listOf("LINE(10)", "KW(PRINT)", "NUM(10)", "EOL", "EOF"),
            sig("10 PRINT 10")
        )
        assertEquals(
            listOf("LINE(100)", "KW(FOR)", "ID(I)", "OP(=)", "NUM(1)", "KW(TO)", "NUM(10)", "EOL", "EOF"),
            sig("100 FOR I=1 TO 10")
        )
    }

    @Test
    fun leadingDecimalIsANumberNotALineNumber() {
        assertEquals(listOf("NUM(10.5)", "EOL", "EOF"), sig("10.5"))
    }

    // --- operators and punctuation -----------------------------------------

    @Test
    fun arithmeticOperators() {
        assertEquals(
            listOf(
                "LINE(10)", "ID(A)", "OP(=)", "NUM(1)", "OP(+)", "NUM(2)", "OP(-)",
                "NUM(3)", "OP(*)", "NUM(4)", "OP(/)", "NUM(5)", "OP(^)", "NUM(6)",
                "EOL", "EOF"
            ),
            sig("10 A=1+2-3*4/5^6")
        )
    }

    @Test
    fun comparisonOperators() {
        assertEquals(
            listOf(
                "LINE(10)", "ID(A)", "OP(<=)", "ID(B)", "OP(>=)", "ID(C)",
                "OP(<>)", "ID(D)", "OP(<)", "ID(E)", "OP(>)", "ID(F)", "OP(=)",
                "ID(G)", "EOL", "EOF"
            ),
            sig("10 A<=B>=C<>D<E>F=G")
        )
    }

    @Test
    fun calculatorGlyphOperatorAliases() {
        // × ÷ − are the glyphs the calculator keypad inserts.
        assertEquals(
            listOf(
                "LINE(10)", "ID(A)", "OP(=)", "NUM(2)", "OP(*)", "NUM(3)",
                "OP(/)", "NUM(4)", "OP(-)", "NUM(5)", "EOL", "EOF"
            ),
            sig("10 A=2\u00D73\u00F74\u22125")
        )
    }

    @Test
    fun punctuation() {
        assertEquals(
            listOf(
                "LINE(10)", "KW(PRINT)", "STR(\"VALUE=\")", "PUNCT(;)", "ID(A)",
                "EOL", "EOF"
            ),
            sig("10 PRINT \"VALUE=\";A")
        )
        assertEquals(
            listOf("LINE(10)", "PUNCT(?)", "STR(\"HI\")", "EOL", "EOF"),
            sig("10 ?\"HI\"")
        )
    }

    // --- constants and literals --------------------------------------------

    @Test
    fun builtInConstants() {
        // PI is reserved (the manual: "entered into a formula using PI").
        assertEquals(
            listOf(
                "LINE(10)", "ID(A)", "OP(=)", "CONST(PI)", "OP(*)", "ID(E)",
                "EOL", "EOF"
            ),
            sig("10 A=PI*E")
        )
        assertEquals(
            listOf("LINE(10)", "ID(A)", "OP(=)", "CONST(PI)", "EOL", "EOF"),
            sig("10 A=\u03C0")
        )
    }

    @Test
    fun bareEIsAVariableNotAConstant() {
        // The FX-880P reserves PI but not E; its manual shows `20 E=15`.
        assertEquals(
            listOf("LINE(10)", "ID(E)", "OP(=)", "NUM(0)", "EOL", "EOF"),
            sig("10 E=0")
        )
        assertEquals(
            listOf("LINE(10)", "ID(e)", "OP(=)", "NUM(1)", "EOL", "EOF"),
            sig("10 e=1")
        )
    }

    @Test
    fun scientificNotationIsStillANumber() {
        // `E` remains part of a numeric literal when it directly follows digits.
        val num = firstNumber("10 A=10E3")
        assertEquals(10000.0, num.value, 0.0)
        assertEquals("10E3", num.raw)
        assertEquals(
            listOf("LINE(10)", "ID(E)", "OP(=)", "NUM(10E3)", "EOL", "EOF"),
            sig("10 E=10E3")
        )
    }

    @Test
    fun ranHashFunction() {
        assertEquals(
            listOf("LINE(10)", "ID(A)", "OP(=)", "FN(RAN#)", "EOL", "EOF"),
            sig("10 A=RAN#")
        )
    }

    @Test
    fun continueIsAnAliasForCont() {
        assertEquals(listOf("LINE(10)", "KW(CONT)", "EOL", "EOF"), sig("10 CONTINUE"))
        assertEquals(listOf("LINE(10)", "KW(CONT)", "EOL", "EOF"), sig("10 continue"))
    }

    @Test
    fun hexadecimalLiteral() {
        val num = firstNumber("10 A=&HFF")
        assertEquals(255.0, num.value, 0.0)
        assertEquals("&HFF", num.raw)
    }

    @Test
    fun exponentNumberLiteral() {
        val num = firstNumber("10 A=1.5E3")
        assertEquals(1500.0, num.value, 0.0)
        assertEquals("1.5E3", num.raw)
        assertEquals("NUM(1.5E3)", num.describe())
    }

    // --- positions ----------------------------------------------------------

    @Test
    fun tokensCarrySourcePositions() {
        val tokens = BasicTokenizer.tokenize("10 PRINT 10")
        assertEquals(SourcePos(0, 1, 1), tokens[0].position)
        assertEquals(SourcePos(3, 1, 4), tokens[1].position)
        assertEquals(SourcePos(9, 1, 10), tokens[2].position)
    }

    @Test
    fun positionsAdvanceAcrossLines() {
        val tokens = BasicTokenizer.tokenize("10 A=1\n20 B=2")
        val secondLine = tokens.first { it is BasicToken.LineNumber && it.value == 20 }
        assertEquals(2, secondLine.position.line)
        assertEquals(1, secondLine.position.column)
        assertEquals(7, secondLine.position.index)
    }

    @Test
    fun crlfLineEndings() {
        val tokens = BasicTokenizer.tokenize("10 A=1\r\n20 B=2")
        val secondLine = tokens.first { it is BasicToken.LineNumber && it.value == 20 }
        assertEquals(2, secondLine.position.line)
    }

    // --- identifiers vs functions ------------------------------------------

    @Test
    fun stringVariableAssignment() {
        assertEquals(
            listOf("LINE(10)", "ID(A$)", "OP(=)", "STR(\"HI\")", "EOL", "EOF"),
            sig("10 A$=\"HI\"")
        )
    }

    @Test
    fun unknownFunctionIsAnIdentifier() {
        assertEquals(
            listOf(
                "LINE(10)", "ID(A)", "OP(=)", "ID(FOO)", "PUNCT(()", "NUM(1)",
                "PUNCT())", "EOL", "EOF"
            ),
            sig("10 A=FOO(1)")
        )
    }

    @Test
    fun pokeAndPeekAreKeywords() {
        assertEquals(
            listOf("LINE(10)", "KW(POKE)", "NUM(1)", "PUNCT(,)", "NUM(2)", "EOL", "EOF"),
            sig("10 POKE 1,2")
        )
        assertEquals(
            listOf(
                "LINE(10)", "ID(A)", "OP(=)", "KW(PEEK)", "PUNCT(()", "NUM(1)",
                "PUNCT())", "EOL", "EOF"
            ),
            sig("10 A=PEEK(1)")
        )
    }

    // --- lexical errors -----------------------------------------------------

    @Test
    fun malformedNumberThrows() {
        val ex = assertThrows(BasicLexException::class.java) {
            BasicTokenizer.tokenize("10 A=1.2.3")
        }
        assertEquals(BasicLexErrorKind.MALFORMED_NUMBER, ex.kind)
        assertEquals(5, ex.position.index)
    }

    @Test
    fun malformedExponentThrows() {
        val ex = assertThrows(BasicLexException::class.java) {
            BasicTokenizer.tokenize("10 A=1E")
        }
        assertEquals(BasicLexErrorKind.MALFORMED_NUMBER, ex.kind)
    }

    @Test
    fun unterminatedStringThrows() {
        val ex = assertThrows(BasicLexException::class.java) {
            BasicTokenizer.tokenize("10 PRINT \"HELLO")
        }
        assertEquals(BasicLexErrorKind.UNTERMINATED_STRING, ex.kind)
        assertEquals(9, ex.position.index)
        assertEquals(10, ex.position.column)
    }

    @Test
    fun unterminatedStringStopsAtEndOfLine() {
        val ex = assertThrows(BasicLexException::class.java) {
            BasicTokenizer.tokenize("10 PRINT \"ABC\n20 END")
        }
        assertEquals(BasicLexErrorKind.UNTERMINATED_STRING, ex.kind)
    }

    @Test
    fun unexpectedCharacterThrows() {
        val ex = assertThrows(BasicLexException::class.java) {
            BasicTokenizer.tokenize("10 A=@")
        }
        assertEquals(BasicLexErrorKind.UNEXPECTED_CHARACTER, ex.kind)
        assertEquals(5, ex.position.index)
        assertEquals(6, ex.position.column)
    }

    @Test
    fun invalidAmpersandThrows() {
        val ex = assertThrows(BasicLexException::class.java) {
            BasicTokenizer.tokenize("10 A=&x")
        }
        assertEquals(BasicLexErrorKind.INVALID_TOKEN, ex.kind)
    }

    @Test
    fun hexPrefixWithoutDigitsThrows() {
        val ex = assertThrows(BasicLexException::class.java) {
            BasicTokenizer.tokenize("10 A=&H")
        }
        assertEquals(BasicLexErrorKind.MALFORMED_NUMBER, ex.kind)
    }
}
