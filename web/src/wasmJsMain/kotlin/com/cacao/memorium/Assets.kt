package com.cacao.memorium

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.window
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.fetch.Response

/** A rectangle of a texture atlas, in atlas pixels (y grows downwards, as in the PNG). */
class Region(val image: ImageBitmap, val x: Int, val y: Int, val width: Int, val height: Int)

/** Everything the game draws, loaded once at startup from the shared Android assets. */
class Assets(val atlas: ImageBitmap, val logoAtlas: ImageBitmap, val font: BitmapFont) {
    private fun main(x: Int, y: Int, w: Int, h: Int) = Region(atlas, x, y, w, h)

    // Logo screen
    val logoBackground = Region(logoAtlas, 700, 0, 100, 100)
    val logoFade = Region(logoAtlas, 600, 0, 100, 100)
    val logo = Region(logoAtlas, 0, 0, 600, 150)
    /** 20 animation frames of 200x200, 5 per row, starting at y = 150. */
    val logoFrames = List(20) { Region(logoAtlas, (it % 5) * 200, 150 + (it / 5) * 200, 200, 200) }

    // Menu
    val titleBackground = main(0, 0, 540, 900)
    val recordsBackground = main(0, 900, 540, 990)
    val settingsBackground = main(540, 0, 540, 990)
    val complexityMark = main(1686, 0, 212, 110)
    val sizeMark = main(1940, 0, 108, 112)
    val musicOn = main(1748, 450, 150, 150)
    val musicOff = main(1898, 450, 150, 150)
    val timeTab = main(1748, 150, 150, 150)
    val movesTab = main(1898, 150, 150, 150)
    val scoreTab = main(1748, 300, 300, 150)
    val arrowUp = main(1648, 600, 200, 100)
    val arrowDown = main(1848, 600, 200, 100)
    val whiteLine = main(1748, 948, 100, 100)
    val screenFade = main(1648, 948, 100, 100)

    // Game
    val gameBackground = main(540, 990, 540, 900)
    val doorLeft = main(1080, 0, 270, 900)
    val doorRight = main(1350, 0, 270, 900)
    /** cards[0] is the card back, cards[1..20] are the faces. */
    val cards: List<Region> = List(20) { main(1248 + (it % 4) * 200, 1048 + (it / 4) * 200, 200, 200) } +
        main(1848, 848, 200, 200)

    companion object {
        suspend fun load(): Assets = coroutineScope {
            val atlas = async { loadImage("texture/atlas.png") }
            val logo = async { loadImage("texture/logoAtlas2.png") }
            val pages = (1..3).map { async { loadImage("font/b$it.png") } }
            val fnt = async { loadText("font/b.fnt") }
            Assets(atlas.await(), logo.await(), BitmapFont(fnt.await(), pages.map { it.await() }))
        }
    }
}

private suspend fun fetch(path: String): Response {
    val response = window.fetch(path).await<Response>()
    check(response.ok) { "Failed to load $path: ${response.status}" }
    return response
}

private suspend fun loadText(path: String): String = fetch(path).text().await<JsString>().toString()

private suspend fun loadImage(path: String): ImageBitmap {
    val buffer = fetch(path).arrayBuffer().await<ArrayBuffer>()
    val array = Int8Array(buffer)
    val bytes = ByteArray(array.length) { array[it] }
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
