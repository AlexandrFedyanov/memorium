package com.cacao.memorium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Minimal AngelCode BMFont (.fnt, text format) renderer for the original game font. */
class BitmapFont(fnt: String, private val pages: List<ImageBitmap>) {
    private class Glyph(
        val x: Int, val y: Int, val width: Int, val height: Int,
        val xOffset: Int, val yOffset: Int, val xAdvance: Int, val page: Int,
    )

    private val glyphs = HashMap<Int, Glyph>()
    private val kernings = HashMap<Long, Int>()

    init {
        val whitespace = Regex("\\s+")
        for (line in fnt.lineSequence()) {
            val tokens = line.trim().split(whitespace)
            val attrs = tokens.drop(1).associate {
                val eq = it.indexOf('=')
                if (eq < 0) it to "" else it.substring(0, eq) to it.substring(eq + 1).trim('"')
            }
            fun int(key: String) = attrs[key]?.toIntOrNull() ?: 0
            when (tokens.firstOrNull()) {
                "char" -> glyphs[int("id")] = Glyph(
                    int("x"), int("y"), int("width"), int("height"),
                    int("xoffset"), int("yoffset"), int("xadvance"), int("page"),
                )
                "kerning" -> kernings[pairKey(int("first"), int("second"))] = int("amount")
            }
        }
    }

    private fun pairKey(first: Int, second: Int) = (first.toLong() shl 32) or second.toLong()

    private inline fun forEachGlyph(text: String, action: (Glyph, Float) -> Unit): Float {
        var x = 0f
        var previous = -1
        for (ch in text) {
            val glyph = glyphs[ch.code] ?: glyphs['?'.code] ?: continue
            if (previous >= 0) x += kernings[pairKey(previous, ch.code)] ?: 0
            action(glyph, x)
            x += glyph.xAdvance
            previous = ch.code
        }
        return x
    }

    /** Text width in font units (multiply by the draw scale). */
    fun width(text: String): Float = forEachGlyph(text) { _, _ -> }

    /** Draws [text] with its visual centre line at [centerY]; glyphs are white in the atlas and get tinted. */
    fun draw(scope: DrawScope, text: String, left: Float, centerY: Float, scale: Float, color: Color) = with(scope) {
        val filter = ColorFilter.tint(color, BlendMode.Modulate)
        translate(left, centerY - VISUAL_CENTER * scale) {
            scale(scale, scale, pivot = Offset.Zero) {
                forEachGlyph(text) { g, x ->
                    if (g.width > 0 && g.height > 0) {
                        drawImage(
                            image = pages[g.page],
                            srcOffset = IntOffset(g.x, g.y),
                            srcSize = IntSize(g.width, g.height),
                            dstOffset = IntOffset((x + g.xOffset).toInt(), g.yOffset),
                            dstSize = IntSize(g.width, g.height),
                            colorFilter = filter,
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** Height of a text line used for layout, in font units (the font is ~100 px). */
        const val LINE = 100f
        /** Vertical centre of the letters below the line top, in font units. */
        private const val VISUAL_CENTER = 58f
    }
}
