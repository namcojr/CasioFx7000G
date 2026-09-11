package com.retro.fx7000g.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.fx7000g.basic.ProgSubmode
import com.retro.fx7000g.calc.CalcAction
import com.retro.fx7000g.calc.CalculatorState

private const val INV = "\u207B\u00B9" // superscript "-1"
private const val ROOT = "\u02E3\u221A" // x-th root  (ˣ√)
private const val CBRT = "\u00B3\u221A" // cube root  (³√)
private const val DEG = "\u00B0"        // degrees
private const val MIN = "\u2032"        // arc-minutes
private const val SEC = "\u2033"        // arc-seconds

private const val H_INV = "h\u207B\u00B9" // e.g. sinh⁻¹

/** Specification for a single physical key. */
private data class KeySpec(
    val label: String,
    val primary: CalcAction,
    val color: Color,
    val shiftLabel: String? = null,
    val shifted: CalcAction? = null,
    val alphaLabel: String? = null,
    val alphaAction: CalcAction? = null,
    val hyp: Boolean = false,
    val weight: Float = 1f
)

private fun ins(text: String) = CalcAction.Insert(text)

/** A key that also carries an ALPHA-layer letter. */
private fun KeySpec.withAlpha(letter: String): KeySpec =
    copy(alphaLabel = letter, alphaAction = ins(letter))

/** A keypad row: its keys plus how tall it is relative to the number rows. */
private data class KeyRow(
    val keys: List<KeySpec>,
    val heightWeight: Float = 1f,
    val compact: Boolean = false
)

/** Height weight for the keypad rows. */
private const val FUNC_ROW = 0.55f
private const val NUM_ROW = 0.79f
private const val ACTION_ROW = 0.68f

private fun keypad(isProg: Boolean = false): List<KeyRow> = listOf(
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            KeySpec("SHIFT", CalcAction.ToggleShift, Fx7000gColors.KeyShift),
            KeySpec("ALPHA", CalcAction.ToggleAlpha, Fx7000gColors.KeyAlpha),
            if (isProg) KeySpec("CLS", ins("CLS "), Fx7000gColors.KeyFunction, "CLEAR", ins("CLEAR "))
            else KeySpec("hyp", CalcAction.ToggleHyp, Fx7000gColors.KeyFunction),
            KeySpec("MODE", CalcAction.OpenModeMenu, Fx7000gColors.KeyFunction, "Rng", CalcAction.Range),
            KeySpec("DEL", CalcAction.Delete, Fx7000gColors.KeyFunction, "Mcl", CalcAction.ClearMemory),
            KeySpec("AC", CalcAction.Clear, Fx7000gColors.KeyAc)
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            if (isProg) KeySpec("PRINT", ins("PRINT "), Fx7000gColors.KeyFunction, "LOCATE", ins("LOCATE ")).withAlpha("A")
            else KeySpec("sin", ins("sin("), Fx7000gColors.KeyFunction, "sin$INV", ins("sin$INV("), hyp = true).withAlpha("A"),
            if (isProg) KeySpec("INPUT", ins("INPUT "), Fx7000gColors.KeyFunction, "RESTORE", ins("RESTORE ")).withAlpha("B")
            else KeySpec("cos", ins("cos("), Fx7000gColors.KeyFunction, "cos$INV", ins("cos$INV("), hyp = true).withAlpha("B"),
            if (isProg) KeySpec("GOTO", ins("GOTO "), Fx7000gColors.KeyFunction, "DIM", ins("DIM ")).withAlpha("C")
            else KeySpec("tan", ins("tan("), Fx7000gColors.KeyFunction, "tan$INV", ins("tan$INV("), hyp = true).withAlpha("C"),
            if (isProg) KeySpec("(", ins("("), Fx7000gColors.KeyFunction, "READ", ins("READ ")).withAlpha("D")
            // TODO: Implement FIX and DMSS as per FX-880P manual
            else KeySpec("(", ins("("), Fx7000gColors.KeyFunction, "FIX", ins("FIX")).withAlpha("D"),
            if (isProg) KeySpec(")", ins(")"), Fx7000gColors.KeyFunction, "DATA", ins("DATA ")).withAlpha("E")
            else KeySpec(")", ins(")"), Fx7000gColors.KeyFunction, "DMSS", ins("DMSS")).withAlpha("E")
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            if (isProg) KeySpec("IF", ins("IF "), Fx7000gColors.KeyFunction, "THEN", ins("THEN ")).withAlpha("F")
            else KeySpec("log", ins("log("), Fx7000gColors.KeyFunction, "10\u02E3", ins("10^(")).withAlpha("F"),
            if (isProg) KeySpec("FOR", ins("FOR "), Fx7000gColors.KeyFunction, "TO", ins("TO ")).withAlpha("G")
            else KeySpec("ln", ins("ln("), Fx7000gColors.KeyFunction, "e\u02E3", ins("e^(")).withAlpha("G"),
            if (isProg) KeySpec("NEXT", ins("NEXT "), Fx7000gColors.KeyFunction, "STEP", ins("STEP ")).withAlpha("H")
            else KeySpec("x\u00B2", ins("\u00B2"), Fx7000gColors.KeyFunction, ROOT, ins(ROOT)).withAlpha("H"),
            if (isProg) KeySpec("DRAW", ins("DRAW "), Fx7000gColors.KeyFunction, "PLOT", ins("PLOT ")).withAlpha("I")
            else KeySpec("x\u02B8", ins("^"), Fx7000gColors.KeyFunction, "x$INV", ins(INV)).withAlpha("I"),
            if (isProg) KeySpec("LET", ins("LET "), Fx7000gColors.KeyFunction, "ERASE", ins("ERASE ")).withAlpha("J")
            else KeySpec("\u221A", ins("\u221A("), Fx7000gColors.KeyFunction, CBRT, ins("$CBRT(")).withAlpha("J")
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            if (isProg) KeySpec("GOSUB", ins("GOSUB "), Fx7000gColors.KeyFunction, "LIST", ins("LIST ")).withAlpha("K")
            // TODO: Implement SGN as per FX-880P manual
            else KeySpec("x!", ins("!"), Fx7000gColors.KeyFunction, "SGN", ins("SGN(")).withAlpha("K"),
            if (isProg) KeySpec("RETURN", ins("RETURN"), Fx7000gColors.KeyFunction, "EDIT", ins("EDIT ")).withAlpha("L")
            else KeySpec("Abs", ins("Abs("), Fx7000gColors.KeyFunction, SEC, ins(SEC)).withAlpha("L"),
            if (isProg) KeySpec("END", ins("END"), Fx7000gColors.KeyFunction, "POKE", ins("POKE ")).withAlpha("M")
            else KeySpec("Int", ins("Int("), Fx7000gColors.KeyFunction, DEG, ins(DEG)).withAlpha("M"),
            if (isProg) KeySpec("STOP", ins("STOP"), Fx7000gColors.KeyFunction, "PEEK", ins("PEEK ")).withAlpha("N")
            else KeySpec("Frac", ins("Frac("), Fx7000gColors.KeyFunction, MIN, ins(MIN)).withAlpha("N"),
            if (isProg) KeySpec(":", ins(":"), Fx7000gColors.KeyFunction, "INKEY$", ins("INKEY$(")).withAlpha("O")
            else KeySpec("ENG", CalcAction.Eng, Fx7000gColors.KeyFunction, "\u2192", ins("\u2192")).withAlpha("O")
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            if (isProg) KeySpec("=", ins("="), Fx7000gColors.KeyBase, "FREE", ins("FREE")).withAlpha("P")
            else KeySpec("DEC", CalcAction.ConvertBase(10), Fx7000gColors.KeyBase, "and", ins("and")).withAlpha("P"),
            if (isProg) KeySpec("<", ins("<"), Fx7000gColors.KeyBase, "LEFT$", ins("LEFT$(")).withAlpha("Q")
            else KeySpec("HEX", CalcAction.ConvertBase(16), Fx7000gColors.KeyBase, "or", ins("or")).withAlpha("Q"),
            if (isProg) KeySpec(">", ins(">"), Fx7000gColors.KeyBase, "RIGHT$", ins("RIGHT$(")).withAlpha("R")
            else KeySpec("BIN", CalcAction.ConvertBase(2), Fx7000gColors.KeyBase, "xor", ins("xor")).withAlpha("R"),
            if (isProg) KeySpec(":", ins(":"), Fx7000gColors.KeyBase, "MID$", ins("MID$(")).withAlpha("S")
            else KeySpec("OCT", CalcAction.ConvertBase(8), Fx7000gColors.KeyBase, "Not", ins("Not")).withAlpha("S"),
            // TODO: Implement CalcAction => RUN program; CONT => CONTINUE program
            if (isProg) KeySpec("RUN", CalcAction.Graph, Fx7000gColors.KeyFunction, "CONT", ins("CONTINUE")).withAlpha("T")
            else KeySpec("Graph", CalcAction.Graph, Fx7000gColors.KeyFunction, "Bltin", CalcAction.OpenPresets).withAlpha("T")
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
            // TODO: Implement CalcAction.Program => Switch to program (X) for programs P7-P9
            if (isProg) KeySpec("7", ins("7"), Fx7000gColors.KeyNumber, "P7", CalcAction.Graph).withAlpha("U")
            else KeySpec("7", ins("7"), Fx7000gColors.KeyNumber, "nPr", ins("nPr")).withAlpha("U"),
            if (isProg) KeySpec("8", ins("8"), Fx7000gColors.KeyNumber, "P8", CalcAction.Graph).withAlpha("V")
            else KeySpec("8", ins("8"), Fx7000gColors.KeyNumber, "nCr", ins("nCr")).withAlpha("V"),
            if (isProg) KeySpec("9", ins("9"), Fx7000gColors.KeyNumber, "P9", CalcAction.Graph).withAlpha("W")
            // TODO: Implement ANGLE (CalcAction?)
            else KeySpec("9", ins("9"), Fx7000gColors.KeyNumber, "ANGLE", ins("ANGLE")).withAlpha("W"),
            if (isProg) KeySpec("/", ins("/"), Fx7000gColors.KeyOperator)
            else KeySpec("\u00F7", ins("\u00F7"), Fx7000gColors.KeyOperator),
            if (isProg) KeySpec("*", ins("*"), Fx7000gColors.KeyOperator)
            else KeySpec("\u00D7", ins("\u00D7"), Fx7000gColors.KeyOperator)
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
            // TODO: Implement CalcAction.Program => Switch to program (X) for programs P4-P6
            if (isProg) KeySpec("4", ins("4"), Fx7000gColors.KeyNumber, "P4", CalcAction.Graph).withAlpha("X")
            else KeySpec("4", ins("4"), Fx7000gColors.KeyNumber, "Pol", ins("Pol(")).withAlpha("X"),
            if (isProg) KeySpec("5", ins("5"), Fx7000gColors.KeyNumber, "P5", CalcAction.Graph).withAlpha("Y")
            else KeySpec("5", ins("5"), Fx7000gColors.KeyNumber, "Rec", ins("Rec(")).withAlpha("Y"),
            if (isProg) KeySpec("6", ins("6"), Fx7000gColors.KeyNumber, "P6", CalcAction.Graph).withAlpha("Z")
            else KeySpec("6", ins("6"), Fx7000gColors.KeyNumber, "%", ins("%")).withAlpha("Z"),
            KeySpec("-", ins("-"), Fx7000gColors.KeyOperator),
            KeySpec("+", ins("+"), Fx7000gColors.KeyOperator)
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
            // TODO: Implement CalcAction.Program => Switch to program (X) for programs P1-P3
            // TODO: Implement CHR$ and STR$ => These should insert a "(" as well for convenience "STR$("
            if (isProg) KeySpec("1", ins("1"), Fx7000gColors.KeyNumber, "P1", CalcAction.Graph).withAlpha("X")
            // TODO: Implement HEX$ and &H as per FX-880P manual
            else KeySpec("1", ins("1"), Fx7000gColors.KeyNumber, "HEX$", ins("HEX$(")).withAlpha("X"),
            if (isProg) KeySpec("2", ins("2"), Fx7000gColors.KeyNumber, "P2", CalcAction.Graph).withAlpha("Y")
            else KeySpec("2", ins("2"), Fx7000gColors.KeyNumber, "&H", ins("&H")).withAlpha("Y"),
            if (isProg) KeySpec("3", ins("3"), Fx7000gColors.KeyNumber, "P3", CalcAction.Graph).withAlpha("Z")
            else KeySpec("3", ins("3"), Fx7000gColors.KeyNumber).withAlpha("Z"),
            if (isProg) KeySpec("\"", ins("\""), Fx7000gColors.KeyFunction, "&H", ins("&H")).withAlpha("CHR$")
            else KeySpec("\u03C0", ins("\u03C0"), Fx7000gColors.KeyFunction, "e", ins("e")),
            // TODO: Implement NEW and NEW # (NEW # clears all programs when in PRG EDIT mode - Check FX-880P manual)
            if (isProg) KeySpec("EXP", ins("E"), Fx7000gColors.KeyFunction, "NEW", ins("NEW")).withAlpha("STR$")
            else KeySpec("EXP", ins("E"), Fx7000gColors.KeyFunction),
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
             // TODO: Implement CalcAction.Program => Switch to program (X) for program P0
             // TODO: Implement CIRCLE, RECT and LINE => These should insert a "(" as well for convenience "CIRCLE("
            if (isProg) KeySpec("0", ins("0"), Fx7000gColors.KeyNumber, "P0", CalcAction.Graph).withAlpha("CIRCLE")
            else KeySpec("0", ins("0"), Fx7000gColors.KeyNumber, "Rnd", CalcAction.Round),
            if (isProg) KeySpec(",", ins(","), Fx7000gColors.KeyNumber, "SET", ins("SET ")).withAlpha("RECT")
            else KeySpec(",", ins(","), Fx7000gColors.KeyNumber, "Ran#", ins("Ran#")),
            if (isProg) KeySpec(".", ins("."), Fx7000gColors.KeyNumber, "TAB", ins("TAB ")).withAlpha("LINE")
            // TODO: Check and fix implementation of "+/-"", right now only inserts a minus sign.
            else KeySpec("+/-", ins("-"), Fx7000gColors.KeyNumber),
            if (isProg) KeySpec("?", ins("?"), Fx7000gColors.KeyFunction, "REM", ins("REM "))
            else KeySpec("Ans", ins("Ans"), Fx7000gColors.KeyFunction),
            if (isProg) KeySpec(";", ins(";"), Fx7000gColors.KeyFunction, "BEEP", ins("BEEP "))
            else KeySpec("M+", CalcAction.MemoryAdd, Fx7000gColors.KeyFunction, "M-", CalcAction.MemorySubtract)
        )
    ),
    KeyRow(
        heightWeight = ACTION_ROW,
        keys = listOf(
            KeySpec("\u25C4", CalcAction.MoveLeft, Fx7000gColors.KeyFunction, if (isProg) "\u25B2" else null),
            KeySpec("\u25BA", CalcAction.MoveRight, Fx7000gColors.KeyFunction, if (isProg) "\u25BC" else null),
            KeySpec("EXE", CalcAction.Evaluate, Fx7000gColors.KeyExe, weight = 3f)
        )
    )
)

@Composable
fun Keypad(
        modifier: Modifier = Modifier,
        state: CalculatorState,
        keyVibration: Boolean = true, 
    ) {
    val isProg = state.progState != null
    val rows = remember(isProg) { keypad(isProg) }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(row.heightWeight),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (key in row.keys) {
                    KeyButton(
                        key = key,
                        compact = row.compact,
                        onClick = { state.onAction(resolveAction(key, state)) },
                        keyVibration = keyVibration,
                        modifier = Modifier
                            .weight(key.weight)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: KeySpec,
    compact: Boolean,
    keyVibration: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val theme = LocalFx7000gTheme.current
    val largeMainKey = !compact && key.label in setOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "+/-",
        "Ans", "EXP", "M+", "\u00D7", "\u00F7", "+", "-",
        "\u03C0", "\u25C4", "\u25BA", "?", ";", "\"", "*", ","
    )

    // Resolve the base look from the theme. SHIFT, ALPHA and HYP no longer
    // recolor the whole key; instead the relevant text blinks (see below).
    val base = theme.keyVisual(roleFor(key.color))
    val visual = base
    val shape = RoundedCornerShape(if (compact) 5.dp else 7.dp)
    val lift = if (compact) 2.dp else 3.dp

    Box(
        modifier = modifier.clickable {
            if (keyVibration) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            onClick()
        }
    ) {
        // Bevel/base layer: sits behind and peeks out at the bottom to fake depth.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(visual.bevel)
        )
        // Raised top face with a vertical highlight-to-shadow gradient.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = lift)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(visual.faceTop, visual.faceBottom)))
                .border(BorderStroke(1.dp, visual.border), shape)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (key.label == "hyp" || key.label == "MODE" || key.label == "DEL" || key.label == "CLS") {
                    // Keep the special function keys exactly as they are.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (key.shiftLabel != null) {
                            Text(
                                text = key.shiftLabel,
                                color = visual.shiftLegend,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = key.label,
                            color = visual.text,
                            fontSize = if (compact) 12.sp else 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )

                        if (key.alphaLabel != null) {
                            Text(
                                text = key.alphaLabel,
                                color = visual.alphaLegend,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Fixed three-position layout:
                    // SHIFT = top, MAIN = center, ALPHA = bottom.
                    if (key.shiftLabel != null) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .then(
                                    if (largeMainKey) {
                                        Modifier.offset(y = 4.dp) // Larger offset for the shift label when the main key is large
                                    } else {
                                        Modifier
                                    }
                                ),
                            text = key.shiftLabel,
                            color = visual.shiftLegend,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .then(
                                Modifier
                            ),
                        text = key.label,
                        color = visual.text,
                        fontSize = when {
                            compact -> 12.sp
                            largeMainKey -> 20.sp
                            else -> 18.sp
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )

                    if (key.alphaLabel != null) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .then(
                                    if (largeMainKey) {
                                        Modifier.offset(y = (-4).dp) // Smaller offset for the alpha label when the main key is large
                                    } else {
                                        Modifier
                                    }
                                ),
                            text = key.alphaLabel,
                            color = visual.alphaLegend,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun resolveAction(key: KeySpec, state: CalculatorState): CalcAction {
    return when {
        // ALPHA
        state.alpha && key.alphaAction != null ->
            key.alphaAction!!

        // HYP / SHIFT+HYP
        state.hyp && key.hyp -> {
            val base = key.label
            if (state.shift) {
                ins("$base$H_INV(")
            } else {
                ins("${base}h(")
            }
        }

        // SHIFT
        state.shift && key.shifted != null ->
            key.shifted!!

        // Normal
        else ->
            key.primary
    }
}

/** Chooses which action a key press produces given the active prefixes. */
// @Deprecated("Use the main resolveAction function instead")
// private fun resolveAction(key: KeySpec, state: CalculatorState): CalcAction {
//     val prog = state.progState
//     if (prog != null && prog.submode == ProgSubmode.EDIT) {
//         val isAlpha = state.alpha || prog.alphaLock
//         if (isAlpha && key.alphaAction != null) {
//             return key.alphaAction!!
//         }
//         if (state.shift) {
//             when (key.label) {
//                 "X" -> return ins(":")
//                 "\u03C0" -> return ins("\"")
//                 ")" -> return ins(",")
//                 else -> if (key.shifted != null) return key.shifted!!
//             }
//         } else {
//             when (key.label) {
//                 "sin" -> return ins("PRINT ")
//                 "cos" -> return ins("INPUT ")
//                 "tan" -> return ins("GOTO ")
//                 "log" -> return ins("IF ")
//                 "ln" -> return ins("THEN ")
//                 "x\u00B2" -> return ins("FOR ")
//                 "x\u02B8" -> return ins("TO ")
//                 "\u221A" -> return ins("NEXT ")
//                 "x!" -> return ins("GOSUB ")
//                 "Abs" -> return ins("RETURN ")
//                 "Int" -> return ins("END ")
//                 "Frac" -> return ins("STOP ")
//                 "DEC" -> return ins("=")
//                 "HEX" -> return ins("<")
//                 "BIN" -> return ins(">")
//                 "OCT" -> return ins(":")
//                 "\u03C0" -> return ins("\"")
//                 "\u00D7" -> return ins("*")
//                 "\u00F7" -> return ins("/")
//                 "+/-" -> return ins("-")
//                 "Ans" -> return ins("?")
//                 "M+" -> return ins(";")
//                 else -> {}
//             }
//         }
//     }
//     return when {
//         state.alpha && key.alphaAction != null -> key.alphaAction!!
//         state.hyp && key.hyp -> {
//             val base = key.label // "sin" / "cos" / "tan"
//             if (state.shift) ins("$base$H_INV(") else ins("${base}h(")
//         }
//         state.shift && key.shifted != null -> key.shifted!!
//         else -> key.primary
//     }
// }
