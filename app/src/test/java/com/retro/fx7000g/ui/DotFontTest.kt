package com.retro.fx7000g.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DotFont] – the 5x7 dot-matrix glyph lookup table.
 */
class DotFontTest {

    @Test
    fun dimensionsAreFiveBySeven() {
        assertEquals(5, DotFont.WIDTH)
        assertEquals(7, DotFont.HEIGHT)
    }

    @Test
    fun everyGlyphHasSevenRowsOfFiveColumns() {
        // Sample across the printable set the LCD renders.
        val samples = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ.+-()!^=/% ".toCharArray()
        for (c in samples) {
            val glyph = DotFont.glyph(c)
            assertEquals("row count for '$c'", DotFont.HEIGHT, glyph.size)
            for (row in glyph) {
                assertEquals("column count for '$c'", DotFont.WIDTH, row.length)
            }
        }
    }

    @Test
    fun knownGlyphIsNotBlank() {
        val glyph = DotFont.glyph('1')
        assertTrue(glyph.any { it.contains('#') })
    }

    @Test
    fun unknownGlyphFallsBackToBlank() {
        val glyph = DotFont.glyph('\uFFFF')
        assertEquals(DotFont.HEIGHT, glyph.size)
        assertTrue(glyph.all { row -> row.all { it == ' ' } })
    }

    @Test
    fun spaceIsBlank() {
        val glyph = DotFont.glyph(' ')
        assertTrue(glyph.all { row -> row.all { it == ' ' } })
    }

    @Test
    fun distinctCharactersHaveDistinctGlyphs() {
        assertNotEquals(
            DotFont.glyph('0').toList(),
            DotFont.glyph('1').toList()
        )
    }

    @Test
    fun asciiMinusAndUnicodeMinusRenderIdentically() {
        assertArrayEquals(DotFont.glyph('-'), DotFont.glyph('\u2212'))
    }

    @Test
    fun specialSymbolsAreMapped() {
        // ×, ÷, √, π, ² should not fall back to blank.
        for (c in listOf('\u00D7', '\u00F7', '\u221A', '\u03C0', '\u00B2')) {
            assertTrue("glyph for '$c' should be lit", DotFont.glyph(c).any { it.contains('#') })
        }
    }
}
