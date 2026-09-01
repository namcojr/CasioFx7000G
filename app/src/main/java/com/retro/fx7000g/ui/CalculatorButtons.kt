package com.retro.fx7000g.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.retro.fx7000g.calc.CalcAction
import com.retro.fx7000g.calc.CalculatorState

private const val INV = "\u207B\u00B9" // superscript "-1"
private const val ROOT = "\u02E3\u221A" // x-th root  (ˣ√)
private const val DEG = "\u00B0"        // degrees
private const val MIN = "\u2032"        // arc-minutes
private const val SEC = "\u2033"        // arc-seconds

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

/** Height weight for the dense, half-height function/mode rows. */
private const val FUNC_ROW = 0.5f
/** Height weight for the taller, more rectangular number/operator rows. */
private const val NUM_ROW = 0.85f

private fun keypad(): List<KeyRow> = listOf(
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            KeySpec("SHIFT", CalcAction.ToggleShift, Fx7000gColors.KeyShift),
            KeySpec("ALPHA", CalcAction.ToggleAlpha, Fx7000gColors.KeyAlpha),
            KeySpec("hyp", CalcAction.ToggleHyp, Fx7000gColors.KeyFunction),
            KeySpec("MODE", CalcAction.OpenModeMenu, Fx7000gColors.KeyFunction),
            KeySpec("DEL", CalcAction.Delete, Fx7000gColors.KeyFunction),
            KeySpec("AC", CalcAction.Clear, Fx7000gColors.KeyAc)
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            KeySpec("sin", ins("sin("), Fx7000gColors.KeyFunction, "sin$INV", ins("sin$INV("), hyp = true).withAlpha("L"),
            KeySpec("cos", ins("cos("), Fx7000gColors.KeyFunction, "cos$INV", ins("cos$INV("), hyp = true).withAlpha("N"),
            KeySpec("tan", ins("tan("), Fx7000gColors.KeyFunction, "tan$INV", ins("tan$INV("), hyp = true).withAlpha("O"),
            KeySpec("(", ins("("), Fx7000gColors.KeyFunction).withAlpha("P"),
            KeySpec(")", ins(")"), Fx7000gColors.KeyFunction, ",", ins(",")).withAlpha("Q")
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            KeySpec("log", ins("log("), Fx7000gColors.KeyFunction, "10\u02E3", ins("10^(")).withAlpha("R"),
            KeySpec("ln", ins("ln("), Fx7000gColors.KeyFunction, "e\u02E3", ins("e^(")).withAlpha("S"),
            KeySpec("x\u00B2", ins("\u00B2"), Fx7000gColors.KeyFunction, ROOT, ins(ROOT)).withAlpha("T"),
            KeySpec("x\u02B8", ins("^"), Fx7000gColors.KeyFunction, "x$INV", ins(INV)).withAlpha("U"),
            KeySpec("\u221A", ins("\u221A("), Fx7000gColors.KeyFunction, "Rng", CalcAction.Range).withAlpha("V")
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            KeySpec("x!", ins("!"), Fx7000gColors.KeyFunction).withAlpha("W"),
            KeySpec("Abs", ins("Abs("), Fx7000gColors.KeyFunction, SEC, ins(SEC)).withAlpha("Y"),
            KeySpec("Int", ins("Int("), Fx7000gColors.KeyFunction, DEG, ins(DEG)),
            KeySpec("Frac", ins("Frac("), Fx7000gColors.KeyFunction, MIN, ins(MIN)),
            KeySpec("X", ins("X"), Fx7000gColors.KeyFunction, "\u2192", ins("\u2192"))
        )
    ),
    KeyRow(
        compact = true, heightWeight = FUNC_ROW,
        keys = listOf(
            KeySpec("DEC", CalcAction.ConvertBase(10), Fx7000gColors.KeyBase, "and", ins("and")),
            KeySpec("HEX", CalcAction.ConvertBase(16), Fx7000gColors.KeyBase, "or", ins("or")),
            KeySpec("BIN", CalcAction.ConvertBase(2), Fx7000gColors.KeyBase, "xor", ins("xor")),
            KeySpec("OCT", CalcAction.ConvertBase(8), Fx7000gColors.KeyBase, "Not", ins("Not")),
            KeySpec("Graph", CalcAction.Graph, Fx7000gColors.KeyFunction, "Bltin", CalcAction.OpenPresets)
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
            KeySpec("7", ins("7"), Fx7000gColors.KeyNumber, "nPr", ins("nPr")).withAlpha("A"),
            KeySpec("8", ins("8"), Fx7000gColors.KeyNumber, "nCr", ins("nCr")).withAlpha("B"),
            KeySpec("9", ins("9"), Fx7000gColors.KeyNumber, "Ran#", ins("Ran#")).withAlpha("C"),
            KeySpec("\u00F7", ins("\u00F7"), Fx7000gColors.KeyOperator),
            KeySpec("\u00D7", ins("\u00D7"), Fx7000gColors.KeyOperator)
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
            KeySpec("4", ins("4"), Fx7000gColors.KeyNumber, "Pol", ins("Pol(")).withAlpha("D"),
            KeySpec("5", ins("5"), Fx7000gColors.KeyNumber, "Rec", ins("Rec(")).withAlpha("E"),
            KeySpec("6", ins("6"), Fx7000gColors.KeyNumber, "%", ins("%")).withAlpha("F"),
            KeySpec("\u2212", ins("\u2212"), Fx7000gColors.KeyOperator),
            KeySpec("+", ins("+"), Fx7000gColors.KeyOperator)
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
            KeySpec("1", ins("1"), Fx7000gColors.KeyNumber).withAlpha("G"),
            KeySpec("2", ins("2"), Fx7000gColors.KeyNumber).withAlpha("H"),
            KeySpec("3", ins("3"), Fx7000gColors.KeyNumber).withAlpha("I"),
            KeySpec("\u03C0", ins("\u03C0"), Fx7000gColors.KeyFunction, "e", ins("e")).withAlpha("J"),
            KeySpec("EXP", ins("E"), Fx7000gColors.KeyFunction).withAlpha("K")
        )
    ),
    KeyRow(
        heightWeight = NUM_ROW,
        keys = listOf(
            KeySpec("0", ins("0"), Fx7000gColors.KeyNumber).withAlpha("Z"),
            KeySpec(".", ins("."), Fx7000gColors.KeyNumber),
            KeySpec("(-)", ins("\u2212"), Fx7000gColors.KeyNumber),
            KeySpec("Ans", ins("Ans"), Fx7000gColors.KeyFunction),
            KeySpec("M+", CalcAction.MemoryAdd, Fx7000gColors.KeyFunction, "M\u2212", CalcAction.MemorySubtract).withAlpha("M")
        )
    ),
    KeyRow(
        heightWeight = 0.72f,
        keys = listOf(
            KeySpec("\u25C4", CalcAction.MoveLeft, Fx7000gColors.KeyFunction),
            KeySpec("\u25BA", CalcAction.MoveRight, Fx7000gColors.KeyFunction),
            KeySpec("EXE", CalcAction.Evaluate, Fx7000gColors.KeyExe, weight = 3f)
        )
    )
)

@Composable
fun Keypad(state: CalculatorState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        for (row in keypad()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(row.heightWeight),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (key in row.keys) {
                    KeyButton(
                        key = key,
                        shiftActive = state.shift,
                        alphaActive = state.alpha,
                        hypActive = state.hyp,
                        compact = row.compact,
                        onClick = { state.onAction(resolveAction(key, state)) },
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
    shiftActive: Boolean,
    alphaActive: Boolean,
    hypActive: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shiftHi = shiftActive && key.shifted != null
    val alphaHi = alphaActive && key.alphaAction != null
    val hypHi = hypActive && key.hyp
    val view = LocalView.current
    val theme = LocalFx7000gTheme.current

    // Resolve the base look from the theme. ALPHA keeps a full-color face for
    // contrast; SHIFT and HYP no longer recolor the whole key, instead the
    // relevant text blinks (see below).
    val base = theme.keyVisual(roleFor(key.color))
    val visual = when {
        alphaHi -> base.copy(
            faceTop = Fx7000gColors.KeyAlpha.copy(alpha = 0.75f),
            faceBottom = Fx7000gColors.KeyAlpha.copy(alpha = 0.55f)
        )
        else -> base
    }

    // White/yellow blink used to signal that this key's SHIFT legend or HYP
    // function is the currently-armed action.
    val blink = rememberInfiniteTransition(label = "prefixBlink")
    val blinkPhase by blink.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "prefixBlinkPhase"
    )
    val blinkColor = if (blinkPhase < 0.5f) Color.White else Color(0xFFFFEB3B)

    val shape = RoundedCornerShape(if (compact) 5.dp else 7.dp)
    val lift = if (compact) 2.dp else 3.dp

    Box(
        modifier = modifier.clickable {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (key.shiftLabel != null) {
                    Text(
                        text = key.shiftLabel,
                        color = if (shiftHi) blinkColor else visual.shiftLegend,
                        fontSize = if (compact) 7.sp else 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = key.label,
                    color = if (hypHi) blinkColor else visual.text,
                    fontSize = if (compact) 11.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
                if (key.alphaLabel != null) {
                    Text(
                        text = key.alphaLabel,
                        color = visual.alphaLegend,
                        fontSize = if (compact) 7.sp else 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Chooses which action a key press produces given the active prefixes. */
private fun resolveAction(key: KeySpec, state: CalculatorState): CalcAction = when {
    state.alpha && key.alphaAction != null -> key.alphaAction!!
    state.hyp && key.hyp -> {
        val base = key.label // "sin" / "cos" / "tan"
        if (state.shift) ins("$base$H_INV(") else ins("${base}h(")
    }
    state.shift && key.shifted != null -> key.shifted!!
    else -> key.primary
}

private const val H_INV = "h\u207B\u00B9" // e.g. sinh⁻¹

