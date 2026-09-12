package com.retro.fx7000g.basic.lex

/**
 * The reserved statement/command words recognised by the FX-7000G BASIC
 * tokenizer.
 *
 * The list mirrors the commands exposed by the project's existing PROG keyboard
 * mapping (the authoritative vocabulary for this simulator) plus the graphics
 * keywords planned for the language. It is intentionally a useful
 * FX-880P-inspired subset rather than a complete reproduction of the original
 * FX-880P manual.
 */
enum class BasicKeyword(val spelling: String) {
    PRINT("PRINT"),
    INPUT("INPUT"),
    GOTO("GOTO"),
    IF("IF"),
    THEN("THEN"),
    FOR("FOR"),
    TO("TO"),
    NEXT("NEXT"),
    STEP("STEP"),
    DRAW("DRAW"),
    PLOT("PLOT"),
    LET("LET"),
    ERASE("ERASE"),
    GOSUB("GOSUB"),
    RETURN("RETURN"),
    END("END"),
    STOP("STOP"),
    READ("READ"),
    DATA("DATA"),
    RESTORE("RESTORE"),
    POKE("POKE"),
    PEEK("PEEK"),
    LOCATE("LOCATE"),
    DIM("DIM"),
    SET("SET"),
    TAB("TAB"),
    BEEP("BEEP"),
    REM("REM"),
    CLEAR("CLEAR"),
    CLS("CLS"),
    LIST("LIST"),
    EDIT("EDIT"),
    RUN("RUN"),
    CONT("CONT"),
    FREE("FREE"),
    NEW("NEW"),
    LINE("LINE"),
    BOX("BOX"),
    CIRCLE("CIRCLE"),
    RECT("RECT");

    companion object {
        private val bySpelling: Map<String, BasicKeyword> =
            entries.associateBy { it.spelling }

        /** Extra accepted spellings that map onto a canonical keyword. */
        private val aliases: Map<String, BasicKeyword> = mapOf(
            "CONTINUE" to CONT
        )

        /**
         * Case-insensitive keyword lookup. Returns `null` for ordinary
         * identifiers so the tokenizer can fall through to function/constant/
         * variable handling.
         */
        fun from(text: String): BasicKeyword? {
            val key = text.uppercase()
            return bySpelling[key] ?: aliases[key]
        }
    }
}
