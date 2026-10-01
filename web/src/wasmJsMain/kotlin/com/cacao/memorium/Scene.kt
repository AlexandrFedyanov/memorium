package com.cacao.memorium

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The virtual phone screen the game is laid out on, in pixels. Like the original, every size is
 * relative to the screen width/height; the font was designed for a 1080 px wide screen.
 */
class GameViewport(val width: Float, val height: Float) {
    val fontScale = width / 1080f
    /** Height of a text label. */
    val line = BitmapFont.LINE * fontScale

    /** Converts a libGDX (y-up, bottom-left origin) coordinate of a box of height [h] to a top coordinate. */
    fun top(yUp: Float, h: Float) = height - yUp - h
}

val LocalViewport = staticCompositionLocalOf<GameViewport> { error("No viewport") }
val LocalAssets = staticCompositionLocalOf<Assets> { error("No assets") }

/** Absolute-positioning container (like a scene2d Group): children place themselves with [frame]. */
@Composable
fun Stage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
        layout(width, height) { placeables.forEach { it.place(0, 0) } }
    }
}

/** Puts an element of a [Stage] at ([x], [y]) (top-left, y down) with the given size. */
fun Modifier.frame(x: Float, y: Float, width: Float, height: Float): Modifier = this
    .layout { measurable, _ ->
        val placeable = measurable.measure(
            Constraints.fixed(width.roundToInt().coerceAtLeast(0), height.roundToInt().coerceAtLeast(0)),
        )
        layout(placeable.width, placeable.height) {
            placeable.place(IntOffset(x.roundToInt(), y.roundToInt()))
        }
    }

/** Draws [region] stretched over the whole draw area. [tint] multiplies the colours, like a libGDX batch colour. */
fun DrawScope.drawRegion(region: Region, alpha: Float = 1f, tint: Color? = null) {
    if (size.width <= 0f || size.height <= 0f || alpha <= 0f) return
    scale(size.width / region.width, size.height / region.height, pivot = Offset.Zero) {
        drawImage(
            image = region.image,
            srcOffset = IntOffset(region.x, region.y),
            srcSize = IntSize(region.width, region.height),
            alpha = alpha.coerceAtMost(1f),
            colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.Modulate) },
        )
    }
}

@Composable
fun Sprite(
    region: Region,
    x: Float, y: Float, width: Float, height: Float,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    tint: Color? = null,
) {
    Spacer(Modifier.frame(x, y, width, height).then(modifier).drawBehind { drawRegion(region, alpha, tint) })
}

/** Full-screen fade to/from the "screen switch" texture. */
@Composable
fun Fade(region: Region, alpha: Float) {
    if (alpha <= 0f) return
    val vp = LocalViewport.current
    Sprite(region, 0f, 0f, vp.width, vp.height, alpha = alpha)
}

/** Width of [text] drawn with the game font at the normal size. */
@Composable
fun textWidth(text: String): Float = LocalAssets.current.font.width(text) * LocalViewport.current.fontScale

/** A line of text in the original bitmap font; while [pressed] it is drawn larger, like the original buttons. */
@Composable
fun Label(
    text: String,
    x: Float, y: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Float = LocalViewport.current.line,
    pressed: Boolean = false,
) {
    val font = LocalAssets.current.font
    val scale = LocalViewport.current.fontScale
    val width = font.width(text) * scale
    Spacer(
        Modifier.frame(x, y, width, height).then(modifier).drawBehind {
            val k = if (pressed) PRESSED_SCALE else 1f
            font.draw(this, text, (size.width - width * k) / 2f, size.height / 2f, scale * k, color)
        },
    )
}

private const val PRESSED_SCALE = 1.15f
val PressedText = Color(0.25f, 0.25f, 0.25f)

/**
 * Button behaviour of the original scene2d listeners: [onPress] reports touch down/up,
 * [onClick] fires when the touch is released over the element.
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    onPress: (Boolean) -> Unit = {},
    onClick: () -> Unit,
): Modifier {
    val isEnabled by rememberUpdatedState(enabled)
    val press by rememberUpdatedState(onPress)
    val click by rememberUpdatedState(onClick)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown()
            if (!isEnabled) return@awaitEachGesture
            press(true)
            val up = waitForUpOrCancellation()
            press(false)
            if (up != null && isEnabled) click()
        }
    }
}

/** Escape (the web counterpart of Android's Back key) calls the innermost registered handler. */
private object BackKey {
    var handler: (() -> Unit)? = null

    init {
        window.addEventListener("keydown", { event ->
            if ((event as KeyboardEvent).key == "Escape") handler?.invoke()
        })
    }
}

@Composable
fun BackHandler(onBack: () -> Unit) {
    val current by rememberUpdatedState(onBack)
    DisposableEffect(Unit) {
        val handler = { current() }
        BackKey.handler = handler
        onDispose { if (BackKey.handler === handler) BackKey.handler = null }
    }
}

// libGDX interpolations used by the original animations.
private val EXP_MIN = 2f.pow(-5)

val Exp5 = Easing { a ->
    if (a <= 0.5f) (2f.pow(5 * (a * 2 - 1)) - EXP_MIN) / (1 - EXP_MIN) / 2
    else (2 - (2f.pow(-5 * (a * 2 - 1)) - EXP_MIN) / (1 - EXP_MIN)) / 2
}
val Exp5In = Easing { a -> (2f.pow(5 * (a - 1)) - EXP_MIN) / (1 - EXP_MIN) }
val Sine = Easing { a -> (1 - cos(a * PI.toFloat())) / 2 }
