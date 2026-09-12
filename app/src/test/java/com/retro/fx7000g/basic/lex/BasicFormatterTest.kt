package com.retro.fx7000g.basic.lex

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [BasicFormatter]. These verify lexical/presentation output only -
 * no program is executed and no semantic validation is performed.
 */
class BasicFormatterTest {

    /** Convenience: tokenize [src] and render it back as canonical source. */
    private fun format(src: String): String = BasicFormatter.format(BasicTokenizer.tokenize(src))

    // --- the canonical example ---------------------------------------------

    @Test
    fun compactInputIsNormalized() {
        assertEquals("10 PRINT B : GO TO 10", format("10PRINT B:GOTO10"))
    }

    @Test
    fun alreadySpacedInputUsesTheSameCanonicalForm() {
        // Same token stream as the compact input, but with internal GOTO spelling.
        assertEquals("10 PRINT B : GO TO 10", format("10 PRINT B : GOTO 10"))
    }

    @Test
    fun looseWhitespaceIsCollapsed() {
        assertEquals("10 PRINT B : GO TO 10", format("  10   PRINT   B   :   GOTO   10  "))
        assertEquals("10 PRINT B : GO TO 10", format("10\tPRINT\tB\t:\tGOTO\t10"))
    }

    @Test
    fun formattingIsTextuallyIdempotent() {
        val once = format("10PRINT B:GOTO10")
        assertEquals("10 PRINT B : GO TO 10", once)
        assertEquals(once, format(once))
    }

    // --- keyword display spellings -----------------------------------------

    @Test
    fun gotoHasAnExplicitDisplaySpelling() {
        assertEquals("GO TO", BasicFormatter.displaySpelling(BasicKeyword.GOTO))
        assertEquals("10 GO TO 100", format("10GOTO100"))
        assertEquals("10 GO TO 100", format("10 goto 100"))
    }

    @Test
    fun otherKeywordsKeepTheirInternalSpelling() {
        assertEquals("PRINT", BasicFormatter.displaySpelling(BasicKeyword.PRINT))
        assertEquals("FOR", BasicFormatter.displaySpelling(BasicKeyword.FOR))
        assertEquals("STEP", BasicFormatter.displaySpelling(BasicKeyword.STEP))
        assertEquals("10 PRINT \"HI\"", format("10PRINT\"HI\""))
    }

    // --- operators and signs -----------------------------------------------

    @Test
    fun operatorsBindTightly() {
        assertEquals("10 A=10", format("10 A = 10"))
        assertEquals("10 A=1+2*3", format("10 A=1+2*3"))
        assertEquals("10 A=(2+3)*4", format("10 A=(2+3)*4"))
    }

    @Test
    fun leadingSignKeepsSpaceBeforeItButNotAfter() {
        assertEquals("10 FOR I=10 TO 0 STEP -1", format("10 FOR I=10 TO 0 STEP - 1"))
        assertEquals("10 A=-B", format("10 A = - B"))
        assertEquals("10 A=2*(-3)", format("10 A=2*(-3)"))
    }

    @Test
    fun comparisonOperatorsBindTightly() {
        assertEquals("10 IF A>10 THEN 50", format("10 IF A > 10 THEN 50"))
        assertEquals("10 IF A>=10 THEN GO TO 50", format("10 IF A>=10 THEN GOTO 50"))
    }

    // --- punctuation --------------------------------------------------------

    @Test
    fun parenthesisCommaAndSemicolonBindTightly() {
        assertEquals("10 CIRCLE(50,50,20)", format("10 CIRCLE(50,50,20)"))
        assertEquals("10 PRINT LEFT$(\"HELLO\",2)", format("10 PRINT LEFT$ ( \"HELLO\" , 2 )"))
        assertEquals("10 PRINT \"VALUE=\";A", format("10 PRINT \"VALUE=\" ; A"))
        assertEquals("10 DATA 1,2,3,4", format("10 DATA 1 , 2 , 3 , 4"))
    }

    @Test
    fun statementSeparatorIsSurroundedBySpaces() {
        assertEquals("10 A=10 : PRINT A", format("10 A=10:PRINT A"))
    }

    // --- line shape and literals -------------------------------------------

    @Test
    fun lineNumbersAreSeparatedFromTheBody() {
        assertEquals("10 PRINT A", format("10PRINT A"))
        assertEquals("10", format("10"))
        assertEquals("100 FOR I=1 TO 10", format("100FOR I=1 TO 10"))
    }

    @Test
    fun numberLiteralsKeepTheirSourceSpelling() {
        assertEquals("10 A=1.5E3", format("10 A=1.5E3"))
        assertEquals("10 A=&HFF", format("10 A = &HFF"))
    }

    @Test
    fun stringsAndCommentTextArePreserved() {
        assertEquals("10 PRINT \"HELLO WORLD\"", format("10 PRINT \"HELLO WORLD\""))
        assertEquals("10 REM THIS IS A COMMENT", format("10REM THIS IS A COMMENT"))
        assertEquals("10 REM THIS IS A COMMENT", format("10 REM   THIS IS A COMMENT"))
    }

    // --- multiple lines and empty input ------------------------------------

    @Test
    fun multipleLinesAreFormattedIndependently() {
        val src = "10PRINT \"HELLO\"\n20A=10\n30 IF A>10 THEN GOTO 100"
        assertEquals(
            "10 PRINT \"HELLO\"\n20 A=10\n30 IF A>10 THEN GO TO 100",
            format(src)
        )
    }

    @Test
    fun emptyAndBlankInputProduceNoOutput() {
        assertEquals("", format(""))
        assertEquals("", format("   "))
        assertEquals("", format("\n\n"))
    }

    @Test
    fun blankLinesAreSkippedBetweenStatements() {
        assertEquals("10 A=1\n20 END", format("10 A=1\n\n20 END"))
    }

    // --- line-level API -----------------------------------------------------

    @Test
    fun formatLineIgnoresLineTerminators() {
        assertEquals(
            "10 PRINT A",
            BasicFormatter.formatLine(BasicTokenizer.tokenize("10 PRINT A"))
        )
    }
}
