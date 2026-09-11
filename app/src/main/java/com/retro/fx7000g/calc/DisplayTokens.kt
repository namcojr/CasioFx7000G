package com.retro.fx7000g.calc

object DisplayTokens {
    val deletable = listOf(
        "sinh\u207B\u00B9(", "cosh\u207B\u00B9(", "tanh\u207B\u00B9(",
        "sin\u207B\u00B9(", "cos\u207B\u00B9(", "tan\u207B\u00B9(",
        "INKEY$(", "RIGHT$(", "LEFT$(", "MID$(", "CHR$(", "STR$(", "HEX$(",
        "sinh(", "cosh(", "tanh(", "sin(", "cos(", "tan(",
        "CONTINUE", "LOCATE ", "RESTORE ",
        "PRINT ", "INPUT ", "GOSUB ", "RETURN", "GOTO ", "THEN ",
        "DATA ", "READ ", "DRAW ", "PLOT ", "ERASE ", "POKE ", "PEEK ",
        "CLEAR ", "CIRCLE ", "RECT ", "LINE ", "BEEP ", "REM ", "SET ", "TAB ",
        "DIM ", "IF ", " TO ", "FOR ", "LET ", "END", "STOP", "LIST ", "EDIT ",
        "log(", "ln(", "FIX(", "SGN(", "Abs(", "Int(", "Frac(", "Pol(", "Rec(",
        "Ran#", "Ans", "nPr", "nCr", "and", "xor", "Not", "or", "&H",
        "10^(", "e^(", "\u02E3\u221A", "\u00B3\u221A", "\u221A(", "\u207B\u00B9"
    ).sortedByDescending { it.length }

    fun trailingTokenLength(textBeforeCursor: String): Int =
        deletable.firstOrNull { textBeforeCursor.endsWith(it) }?.length ?: 1
}