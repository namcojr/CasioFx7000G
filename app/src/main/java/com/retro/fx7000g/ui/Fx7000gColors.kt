package com.retro.fx7000g.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Palette approximating the 1985 FX-7000G's dark grey body and green LCD. */
object Fx7000gColors {
    val Body = Color(0xFF2B2E33)
    val BodyEdge = Color(0xFF17181B)
    val Bezel = Color(0xFF3A3D42)

    val Branding = Color(0xFFE8E8E8)
    val ModelPlate = Color(0xFF8A1F1F)

    // LCD substrate: fixed color for the bezel/padding surround outside the dot grid.
    val LcdBackground = Color(0xFFA9B598)

    // LCD pixel base tones – internal use only; call lcdDotColors(contrast) for rendering.
    private val lcdOffBase  = Color(0xFF8FA882)  // OFF pixel at normal contrast (clearly visible)
    private val lcdOnBase   = Color(0xFF1E2614)  // ON  pixel at normal contrast (deep dark green)
    private val lcdOffDark  = Color(0xFF414D2E)  // OFF pixel at maximum contrast (both states dark)
    private val lcdOnDark   = Color(0xFF252D18)  // ON  pixel at maximum contrast

    /**
     * Returns `(dotOff, dotOn)` adjusted for [contrast] in `[0f, 1f]`.
     *
     * **Physical model (reflective 1985-era dot-matrix LCD):**
     *
     * - At `contrast = 0.5` (default): dotOff is a clearly visible medium green-grey;
     *   dotOn is deep dark green — strong separation, easy readability.
     * - As contrast rises toward `1.0`: both pixel states shift darker together and their
     *   luminance gap narrows, so the OFF matrix becomes increasingly prominent and eventually
     *   competes visually with the ON characters — exactly as on the physical hardware.
     * - As contrast falls toward `0.0`: both states lighten toward the substrate tone;
     *   the display "washes out" but neither state vanishes entirely.
     *
     * The OFF pixel is always perceptibly distinct from the ON pixel at every contrast level
     * (they converge only asymptotically at the theoretical maximum).
     */
    fun lcdDotColors(contrast: Float): Pair<Color, Color> {
        val t = contrast.coerceIn(0f, 1f)
        val dotOff = if (t <= 0.5f) lerp(LcdBackground, lcdOffBase, t * 2f)
                     else           lerp(lcdOffBase,     lcdOffDark, (t - 0.5f) * 2f)
        val dotOn  = if (t <= 0.5f) lerp(lcdOffBase,    lcdOnBase,  t * 2f)
                     else           lerp(lcdOnBase,      lcdOnDark,  (t - 0.5f) * 2f)
        return dotOff to dotOn
    }

    // Keys
    val KeyNumber = Color(0xFF404349)
    val KeyFunction = Color(0xFF34373C)
    val KeyOperator = Color(0xFF4A4E55)
    val KeyBase = Color(0xFF35505F)
    val KeyAc = Color(0xFF8A2B2B)
    val KeyExe = Color(0xFF2F5B8A)
    val KeyShift = Color(0xFFB07A2A)
    val KeyAlpha = Color(0xFF7A3B8A)
    val KeyText = Color(0xFFF2F2F2)
    val KeyShiftLegend = Color(0xFFE0A63A)
    val KeyAlphaLegend = Color(0xFFCF8FE0)
    val KeyBorder = Color(0xFF17181B)
}
