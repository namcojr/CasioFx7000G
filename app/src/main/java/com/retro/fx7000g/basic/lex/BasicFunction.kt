package com.retro.fx7000g.basic.lex

/**
 * Built-in functions recognised by the BASIC tokenizer.
 *
 * These are the string/numeric functions exposed by the existing keyboard
 * mapping together with the mathematical functions already provided by the
 * calculator engine, so the future parser can reuse the calculator's maths
 * rather than reimplementing it.
 */
enum class BasicFunction(val spelling: String) {
    // String functions - the '$' is part of the spelling.
    LEFT("LEFT$"),
    RIGHT("RIGHT$"),
    MID("MID$"),
    CHR("CHR$"),
    STR("STR$"),
    HEX("HEX$"),
    INKEY("INKEY$"),

    // Numeric / shape functions.
    SGN("SGN"),
    ABS("ABS"),
    INT("INT"),
    FRAC("FRAC"),
    MIN("MIN"),
    MAX("MAX"),
    FIX("FIX"),
    ROUND("ROUND"),
    RAN("RAN#"),
    RND("RND"),
    POL("POL"),
    REC("REC"),

    // Mathematical functions mirrored from the calculator engine.
    SIN("SIN"),
    COS("COS"),
    TAN("TAN"),
    ASIN("ASIN"),
    ACOS("ACOS"),
    ATAN("ATAN"),
    SINH("SINH"),
    COSH("COSH"),
    TANH("TANH"),
    ASINH("ASINH"),
    ACOSH("ACOSH"),
    ATANH("ATANH"),
    LOG("LOG"),
    LN("LN"),
    EXP("EXP"),
    SQRT("SQRT"),
    CBRT("CBRT");

    companion object {
        private val bySpelling: Map<String, BasicFunction> =
            entries.associateBy { it.spelling }

        /**
         * Case-insensitive function lookup.
         *
         * `RAN#` is not assembled here because the `#` terminates identifier
         * scanning; [BasicTokenizer] recognises that specific spelling itself.
         */
        fun from(text: String): BasicFunction? = bySpelling[text.uppercase()]
    }
}
