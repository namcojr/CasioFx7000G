package com.retro.fx7000g.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Parallel "skin" system for the calculator.
 *
 * The original [DarkTheme] reproduces the existing 1985-style dark-grey look.
 * [ClassicTheme] is an alternative, opt-in skin that approximates the real
 * fx-7000G colour scheme (crème function keys, black number keys) on a brushed
 * aluminium body. Both are provided through [LocalFx7000gTheme] so the dark
 * theme stays the default and the classic skin is fully revertible.
 */

/** Functional grouping of a key, used to pick its colours per theme. */
enum class KeyRole { Number, Function, Operator, Base, Ac, Exe, Shift, Alpha }

/** Fully-resolved paint for one key, including 3D bevel shades. */
data class KeyVisual(
    val faceTop: Color,
    val faceBottom: Color,
    val bevel: Color,
    val text: Color,
    val shiftLegend: Color,
    val alphaLegend: Color,
    val border: Color
)

private fun Color.lighten(f: Float) = lerp(this, Color.White, f)
private fun Color.darken(f: Float) = lerp(this, Color.Black, f)

/**
 * Builds a [KeyVisual] from a single base fill colour, deriving a lighter top
 * face, a slightly darker bottom face and a dark bevel to fake a 3D keycap.
 */
private fun visual(
    fill: Color,
    text: Color,
    border: Color,
    shiftLegend: Color = Fx7000gColors.KeyShiftLegend,
    alphaLegend: Color = Fx7000gColors.KeyAlphaLegend,
    topLift: Float = 0.16f,
    bottomDrop: Float = 0.07f,
    bevelDrop: Float = 0.38f
) = KeyVisual(
    faceTop = fill.lighten(topLift),
    faceBottom = fill.darken(bottomDrop),
    bevel = fill.darken(bevelDrop),
    text = text,
    shiftLegend = shiftLegend,
    alphaLegend = alphaLegend,
    border = border
)

interface Fx7000gTheme {
    /** Colour behind the whole device (outside the body). */
    val bodyEdge: Color
    /** Solid fallback colour for the body when [useBodyTexture] is false. */
    val body: Color
    /** When true the body is painted with res/drawable/aluminum.jpg. */
    val useBodyTexture: Boolean
    /** CASIO / fx-7000G wordmark colour. */
    val branding: Color
    /** "GRAPHICS" model-plate background. */
    val modelPlate: Color

    fun keyVisual(role: KeyRole): KeyVisual
}

/** The original dark-grey skin (default). */
object DarkTheme : Fx7000gTheme {
    override val bodyEdge = Fx7000gColors.BodyEdge
    override val body = Fx7000gColors.Body
    override val useBodyTexture = false
    override val branding = Fx7000gColors.Branding
    override val modelPlate = Fx7000gColors.ModelPlate

    override fun keyVisual(role: KeyRole): KeyVisual {
        val text = Fx7000gColors.KeyText
        val border = Fx7000gColors.KeyBorder
        val fill = when (role) {
            KeyRole.Number -> Fx7000gColors.KeyNumber
            KeyRole.Function -> Fx7000gColors.KeyFunction
            KeyRole.Operator -> Fx7000gColors.KeyOperator
            KeyRole.Base -> Fx7000gColors.KeyBase
            KeyRole.Ac -> Fx7000gColors.KeyAc
            KeyRole.Exe -> Fx7000gColors.KeyExe
            KeyRole.Shift -> Fx7000gColors.KeyShift
            KeyRole.Alpha -> Fx7000gColors.KeyAlpha
        }
        return visual(fill = fill, text = text, border = border)
    }
}

/**
 * Approximation of the real fx-7000G: brushed-aluminium body, crème function
 * keys with black text, near-black number keys with white text, grey
 * arithmetic operators. Special keys (AC / EXE / SHIFT / ALPHA) keep their
 * signature colours.
 */
object ClassicTheme : Fx7000gTheme {
    override val bodyEdge = Color(0xFF2A2622)
    override val body = Color(0xFF6E6A64) // fallback if the texture is missing
    override val useBodyTexture = true
    override val branding = Color(0xFF181410)
    override val modelPlate = Color(0xFF7A1414)

    // fx-7000G-inspired keycap colours.
    private val cremeFill = Color(0xFFDED7C4)
    private val cremeText = Color(0xFF1F1D18)
    private val cremeBorder = Color(0xFF9A927E)
    private val numberFill = Color(0xFF37373D)
    private val numberText = Color(0xFFF3F1EC)
    private val numberBorder = Color(0xFF121214)
    private val operatorFill = Color(0xFF8C8A90)
    private val operatorText = Color(0xFFFAFAF8)
    private val baseFill = Color(0xFFCFC9BB)

    // Legends sit above/below the label; keep them readable on crème caps.
    private val darkShiftLegend = Color(0xFFB25E12)
    private val darkAlphaLegend = Color(0xFF7A3B8A)

    override fun keyVisual(role: KeyRole): KeyVisual = when (role) {
        KeyRole.Number -> visual(
            fill = numberFill, text = numberText, border = numberBorder
        )
        KeyRole.Function -> visual(
            fill = cremeFill, text = cremeText, border = cremeBorder,
            shiftLegend = darkShiftLegend, alphaLegend = darkAlphaLegend
        )
        KeyRole.Base -> visual(
            fill = baseFill, text = cremeText, border = cremeBorder,
            shiftLegend = darkShiftLegend, alphaLegend = darkAlphaLegend
        )
        KeyRole.Operator -> visual(
            fill = operatorFill, text = operatorText, border = Color(0xFF4E4C52)
        )
        KeyRole.Ac -> visual(
            fill = Fx7000gColors.KeyAc, text = Color.White, border = Color(0xFF3A0F0F)
        )
        KeyRole.Exe -> visual(
            fill = Fx7000gColors.KeyExe, text = Color.White, border = Color(0xFF14304A)
        )
        KeyRole.Shift -> visual(
            fill = Fx7000gColors.KeyShift, text = Color.White, border = Color(0xFF5A3C10)
        )
        KeyRole.Alpha -> visual(
            fill = Fx7000gColors.KeyAlpha, text = Color.White, border = Color(0xFF3A1D44)
        )
    }
}

/** Maps a key's dark-theme fill colour back to its functional [KeyRole]. */
fun roleFor(color: Color): KeyRole = when (color) {
    Fx7000gColors.KeyNumber -> KeyRole.Number
    Fx7000gColors.KeyOperator -> KeyRole.Operator
    Fx7000gColors.KeyBase -> KeyRole.Base
    Fx7000gColors.KeyAc -> KeyRole.Ac
    Fx7000gColors.KeyExe -> KeyRole.Exe
    Fx7000gColors.KeyShift -> KeyRole.Shift
    Fx7000gColors.KeyAlpha -> KeyRole.Alpha
    else -> KeyRole.Function
}

/** Current skin. Defaults to [DarkTheme] so nothing changes until opted in. */
val LocalFx7000gTheme = staticCompositionLocalOf<Fx7000gTheme> { DarkTheme }
