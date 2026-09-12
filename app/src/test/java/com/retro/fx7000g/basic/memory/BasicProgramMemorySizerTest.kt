package com.retro.fx7000g.basic.memory

import com.retro.fx7000g.basic.lex.BasicConstant
import com.retro.fx7000g.basic.lex.BasicFunction
import com.retro.fx7000g.basic.lex.BasicKeyword
import com.retro.fx7000g.basic.lex.BasicOperator
import com.retro.fx7000g.basic.lex.BasicPunctuation
import com.retro.fx7000g.basic.lex.BasicToken
import com.retro.fx7000g.basic.lex.BasicTokenizer
import com.retro.fx7000g.basic.lex.SourcePos
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [BasicProgramMemorySizer]. They deliberately check byte rules
 * rather than Kotlin string length, and are independent of the future parser:
 * tokens are produced by the existing tokenizer or built directly.
 */
class BasicProgramMemorySizerTest {

    private val pos = SourcePos(index = 0, line = 1, column = 1)

    /** Size of the full token stream produced for [source]. */
    private fun sizeOfSource(source: String): Int =
        BasicProgramMemorySizer.size(BasicTokenizer.tokenize(source))

    // --- individual token rules ---------------------------------------------

    @Test
    fun lineNumberCostsTwoBytes() {
        assertEquals(2, BasicProgramMemorySizer.sizeOf(BasicToken.LineNumber(10, pos)))
    }

    @Test
    fun reservedCommandCostsTwoBytes() {
        assertEquals(2, BasicProgramMemorySizer.sizeOf(BasicToken.Keyword(BasicKeyword.PRINT, pos)))
    }

    @Test
    fun reservedFunctionCostsTwoBytes() {
        assertEquals(2, BasicProgramMemorySizer.sizeOf(BasicToken.Function(BasicFunction.SIN, pos)))
    }

    @Test
    fun reservedConstantCostsTwoBytes() {
        assertEquals(2, BasicProgramMemorySizer.sizeOf(BasicToken.Constant(BasicConstant.PI, pos)))
    }

    @Test
    fun ordinaryCharactersCostOneByteEach() {
        assertEquals(3, BasicProgramMemorySizer.sizeOf(BasicToken.Identifier("ABC", pos)))
        assertEquals(3, BasicProgramMemorySizer.sizeOf(BasicToken.Number(123.0, "123", pos)))
        assertEquals(2, BasicProgramMemorySizer.sizeOf(BasicToken.Operator(BasicOperator.LESS_EQUAL, pos)))
        assertEquals(1, BasicProgramMemorySizer.sizeOf(BasicToken.Punctuation(BasicPunctuation.COLON, pos)))
    }

    @Test
    fun stringLiteralAlsoPaysForItsQuotes() {
        assertEquals(4, BasicProgramMemorySizer.sizeOf(BasicToken.StringLiteral("AB", pos)))
    }

    @Test
    fun commentCostsItsRawText() {
        assertEquals(6, BasicProgramMemorySizer.sizeOf(BasicToken.Comment(" HELLO", pos)))
    }

    @Test
    fun endOfLineCostsATerminatorAndEndOfInputIsFree() {
        assertEquals(
            BasicProgramMemorySizer.LINE_TERMINATOR_BYTES,
            BasicProgramMemorySizer.sizeOf(BasicToken.EndOfLine(pos))
        )
        assertEquals(0, BasicProgramMemorySizer.sizeOf(BasicToken.EndOfInput(pos)))
    }

    // --- tokenized programs --------------------------------------------------

    @Test
    fun singlePrintLine() {
        // 2 (line) + 2 (PRINT) + 1 (B) + 1 (terminator)
        assertEquals(6, sizeOfSource("10 PRINT B"))
    }

    @Test
    fun printStringLine() {
        // 2 + 2 + 7 ("HELLO" + quotes) + 1
        assertEquals(12, sizeOfSource("10 PRINT \"HELLO\""))
    }

    @Test
    fun assignmentLine() {
        // 2 + 1 (A) + 1 (=) + 2 ("10") + 1
        assertEquals(7, sizeOfSource("10 A=10"))
    }

    @Test
    fun multipleStatementsOnOneLine() {
        // 2 + A(1) + =(1) + 10(2) + : (1) + PRINT(2) + A(1) + terminator(1)
        assertEquals(11, sizeOfSource("10 A=10:PRINT A"))
    }

    @Test
    fun forLine() {
        // 2 + FOR(2) + I(1) + =(1) + 1(1) + TO(2) + 10(2) + STEP(2) + 1(1) + term(1)
        assertEquals(15, sizeOfSource("10 FOR I=1 TO 10 STEP 1"))
    }

    @Test
    fun dataLine() {
        // 2 + DATA(2) + 1 , 1 , 1 , 1 + terminator(1)
        assertEquals(12, sizeOfSource("10 DATA 1,2,3,4"))
    }

    @Test
    fun functionCostsLessThanAnEquivalentIdentifier() {
        // SIN is one reserved token (2 bytes); the 3-char name ABC is text (3).
        assertEquals(7, sizeOfSource("10 A=SIN"))
        assertEquals(8, sizeOfSource("10 A=ABC"))
    }

    @Test
    fun multiLineProgramSumsEachLine() {
        // line 1 = 12, line 2 (20 END) = 2 + 2 + 1 = 5
        assertEquals(17, sizeOfSource("10 PRINT \"HELLO\"\n20 END"))
    }

    @Test
    fun emptyInputCostsNothing() {
        assertEquals(0, sizeOfSource(""))
    }
}
