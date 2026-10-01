package com.cacao.memorium

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.TimeSource

/**
 * The menu panels sit in one vertical strip: settings above the title panel, records below it.
 * [offset] is how far the strip is scrolled up, in screen heights (libGDX `stageGroup.getY()`).
 */
private enum class Panel(val offset: Float) { Title(0f), Settings(-1.1f), Records(1.1f) }

/** A drag (rather than a tap) cancels button clicks, as in the original. */
private class DragState {
    var active = false
    var dragged = false
    var downY = 0f
    var lastY = 0f
}

private const val WHEEL_COOLDOWN_MS = 600

@Composable
fun MenuScreen(onStart: () -> Unit) {
    val assets = LocalAssets.current
    val vp = LocalViewport.current
    val w = vp.width
    val h = vp.height
    val scope = rememberCoroutineScope()

    val strip = remember { Animatable(0f) }
    var panel by remember { mutableStateOf(Panel.Title) }
    val drag = remember { DragState() }
    var lastWheel by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    val fade = remember { Animatable(1f) }
    var leaving by remember { mutableStateOf(false) }

    var complexity by remember { mutableIntStateOf(Settings.complexity) }
    var mapSize by remember { mutableIntStateOf(Settings.mapSize) }
    var musicOn by remember { mutableStateOf(Audio.enabled) }

    LaunchedEffect(Unit) {
        Audio.startMusic()
        fade.animateTo(0f, tween(350, easing = LinearEasing))
    }

    fun show(target: Panel, durationMs: Int = 500) {
        panel = target
        scope.launch { strip.animateTo(target.offset, tween(durationMs, easing = Sine)) }
    }

    /** Snaps the strip to a panel after a drag, like `FirstScreen.touchUp`. */
    fun settle() {
        val y = strip.value
        val threshold = 1f / 15f
        val target = when (panel) {
            Panel.Title -> when {
                y >= threshold -> Panel.Records
                y <= -threshold -> Panel.Settings
                else -> Panel.Title
            }
            Panel.Records -> if (y <= 1f - threshold) Panel.Title else Panel.Records
            Panel.Settings -> if (y >= threshold - 1f) Panel.Title else Panel.Settings
        }
        show(target, (500 * abs(target.offset - y) / 1.1f).toInt().coerceIn(150, 500))
    }

    fun wheel(delta: Float) {
        if (delta == 0f || lastWheel.elapsedNow().inWholeMilliseconds < WHEEL_COOLDOWN_MS) return
        lastWheel = TimeSource.Monotonic.markNow()
        val order = listOf(Panel.Settings, Panel.Title, Panel.Records)
        val index = (order.indexOf(panel) + if (delta > 0) 1 else -1).coerceIn(0, order.lastIndex)
        if (order[index] != panel) show(order[index])
    }

    BackHandler { if (!leaving && panel != Panel.Title) show(Panel.Title) }

    val dragInput = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                val y = change.position.y
                when (event.type) {
                    PointerEventType.Press -> if (!leaving) {
                        drag.active = true
                        drag.dragged = false
                        drag.downY = y
                        drag.lastY = y
                        scope.launch { strip.stop() }
                    }
                    PointerEventType.Move -> if (drag.active && change.pressed) {
                        if (drag.dragged) {
                            val current = strip.value
                            val delta = (drag.lastY - y) / h
                            if (current in -1f..1f || (current > 1f && delta < 0) || (current < -1f && delta > 0)) {
                                scope.launch { strip.snapTo(current + delta) }
                            }
                        } else if (abs(drag.downY - y) > h / 30f) {
                            drag.dragged = true
                        }
                        drag.lastY = y
                    }
                    PointerEventType.Release -> if (drag.active) {
                        drag.active = false
                        if (drag.dragged || strip.value != panel.offset) settle()
                    }
                    PointerEventType.Scroll -> if (!leaving && !drag.active) wheel(change.scrollDelta.y)
                }
            }
        }
    }

    /** Buttons react only to taps, not to the end of a drag, and not while leaving the screen. */
    val canClick = { !drag.dragged && !leaving }

    Stage(Modifier.fillMaxSize().then(dragInput)) {
        Stage(Modifier.frame(0f, -strip.value * h, w, h)) {
            TitlePanel(canClick, onNewGame = { show(Panel.Settings) }, onRecords = { show(Panel.Records) })
            SettingsPanel(
                complexity = complexity,
                mapSize = mapSize,
                canClick = canClick,
                onComplexity = { complexity = it; Settings.complexity = it },
                onMapSize = { mapSize = it; Settings.mapSize = it },
                onStart = {
                    leaving = true
                    scope.launch {
                        fade.animateTo(1f, tween(350, easing = LinearEasing))
                        onStart()
                    }
                },
            )
            RecordsPanel(canClick)
        }

        ImageButton(
            region = if (musicOn) assets.musicOn else assets.musicOff,
            x = w - w / 4f, bottom = w / 12f, width = w / 6f, height = w / 6f,
            canClick = canClick,
            onClick = {
                musicOn = !musicOn
                Audio.enabled = musicOn
                Settings.isMusicOn = musicOn
            },
        )
        Fade(assets.screenFade, fade.value)
    }
}

@Composable
private fun TitlePanel(canClick: () -> Boolean, onNewGame: () -> Unit, onRecords: () -> Unit) {
    val assets = LocalAssets.current
    val vp = LocalViewport.current
    val w = vp.width
    val h = vp.height
    val buttonHeight = vp.line + h / 40f
    val newGame = Strings["new_game"]
    val records = Strings["records"]

    Sprite(assets.titleBackground, 0f, 0f, w, h)
    TextButton(newGame, w / 2 - textWidth(newGame) / 2, h / 2 - buttonHeight, Color.Black, canClick, buttonHeight, onNewGame)
    TextButton(records, w / 2 - textWidth(records) / 2, h / 2, Color.Black, canClick, buttonHeight, onRecords)
    Sprite(assets.arrowUp, w / 2 - w / 16, h / 2 - buttonHeight - w / 16, w / 8, w / 16)
    Sprite(assets.arrowDown, w / 2 - w / 16, h / 2 + buttonHeight, w / 8, w / 16)
}

@Composable
private fun SettingsPanel(
    complexity: Int,
    mapSize: Int,
    canClick: () -> Boolean,
    onComplexity: (Int) -> Unit,
    onMapSize: (Int) -> Unit,
    onStart: () -> Unit,
) {
    val assets = LocalAssets.current
    val vp = LocalViewport.current
    val w = vp.width
    val h = vp.height
    val line = vp.line

    // The panel lies above the title panel: y-up from h to 2.1h. Rows are positioned as in the original.
    Sprite(assets.settingsBackground, 0f, vp.top(h, 1.1f * h), w, 1.1f * h)

    val sizeRowUp = 1.6f * h - line / 2
    val sizeWidths = Settings.MAP_SIZES.map { textWidth(it.toString()) }
    val middleX = w / 2 - sizeWidths[1] / 2
    val sizeX = listOf(
        middleX - sizeWidths[0] - h / 20,
        middleX,
        middleX + sizeWidths[1] + h / 20,
    )
    val easy = Strings["easy"]
    val hard = Strings["hard"]
    val start = Strings["start"]
    val complexityRowUp = sizeRowUp + 1.2f * line
    val complexityX = listOf(w / 3 - textWidth(easy) / 2, 2 * w / 3 - textWidth(hard) / 2)

    TextButton(easy, complexityX[0], vp.top(complexityRowUp, line), Color.White, canClick) { onComplexity(Settings.EASY) }
    TextButton(hard, complexityX[1], vp.top(complexityRowUp, line), Color.White, canClick) { onComplexity(Settings.HARD) }
    Settings.MAP_SIZES.forEachIndexed { i, size ->
        TextButton(size.toString(), sizeX[i], vp.top(sizeRowUp, line), Color.White, canClick) { onMapSize(size) }
    }
    TextButton(start, w / 2 - textWidth(start) / 2, vp.top(sizeRowUp - 1.2f * line, line), Color.White, canClick, onClick = onStart)

    // Hand-drawn circles around the current choices (drawn over the labels, as in the original).
    val complexityWidth = textWidth(if (complexity == Settings.HARD) hard else easy)
    val c = if (complexity == Settings.HARD) 1 else 0
    Sprite(
        assets.complexityMark,
        complexityX[c] - 0.2f * complexityWidth, vp.top(complexityRowUp, line), 1.3f * complexityWidth, line,
    )
    val s = Settings.MAP_SIZES.indexOf(mapSize).coerceAtLeast(0)
    Sprite(assets.sizeMark, sizeX[s] - 0.2f * sizeWidths[s], vp.top(sizeRowUp, line), 1.5f * sizeWidths[s], line)
}

@Composable
private fun RecordsPanel(canClick: () -> Boolean) {
    val assets = LocalAssets.current
    val vp = LocalViewport.current
    val w = vp.width
    val h = vp.height
    val line = vp.line
    val scope = rememberCoroutineScope()

    // The panel lies below the title panel: y-up from -1.1h to 0. Rows hang off y-up = -0.6h.
    Sprite(assets.recordsBackground, 0f, vp.top(-1.1f * h, 1.1f * h), w, 1.1f * h)
    val base = -0.6f * h
    val columns = listOf(w / 5, w / 2, 4 * w / 5)
    val rows = listOf(0.1f, -1.1f, -2.3f).map { base + it * line }
    val headerUp = base + 1.3f * line

    @Composable
    fun Cell(text: String, column: Int, yUp: Float) =
        Label(text, columns[column] - textWidth(text) / 2, vp.top(yUp, line), Color.White)

    Cell(Strings["map_size"], 0, headerUp)
    Cell(Strings["easy"], 1, headerUp)
    Cell(Strings["hard"], 2, headerUp)

    var tab by remember { mutableIntStateOf(Settings.recordTab) }
    Settings.MAP_SIZES.forEachIndexed { row, size ->
        Cell(size.toString(), 0, rows[row])
        Cell(recordText(Settings.EASY, size, tab), 1, rows[row])
        Cell(recordText(Settings.HARD, size, tab), 2, rows[row])
    }

    // Tabs: time | score | moves. The selected one is enlarged and underlined.
    val tabHeight = w / 8
    val tabUp = base - 3.6f * tabHeight
    val grow = w / 30
    fun tabWidth(t: Int) = if (t == Settings.TAB_SCORE) w / 4 else w / 8
    fun tabCenter(t: Int) = when (t) {
        Settings.TAB_TIME -> w / 5
        Settings.TAB_SCORE -> w / 2
        else -> 4 * w / 5
    }
    fun tabLeft(t: Int, enlarged: Boolean) = tabCenter(t) - tabWidth(t) / 2 - if (enlarged) grow / 2 else 0f

    // The underline position is kept in screen widths so that it survives window resizes.
    val lineX = remember { Animatable(tabLeft(tab, true) / w) }
    val lineWidth = remember { Animatable((tabWidth(tab) + grow) / w) }

    for (t in listOf(Settings.TAB_TIME, Settings.TAB_SCORE, Settings.TAB_MOVES)) {
        val region = when (t) {
            Settings.TAB_TIME -> assets.timeTab
            Settings.TAB_SCORE -> assets.scoreTab
            else -> assets.movesTab
        }
        ImageButton(
            region = region,
            x = tabCenter(t) - tabWidth(t) / 2,
            bottom = tabUp,
            width = tabWidth(t),
            height = tabHeight,
            canClick = canClick,
            selected = t == tab,
            onClick = {
                tab = t
                Settings.recordTab = t
                scope.launch { lineX.animateTo(tabLeft(t, true) / w, tween(150, easing = LinearEasing)) }
                scope.launch { lineWidth.animateTo((tabWidth(t) + grow) / w, tween(150, easing = LinearEasing)) }
            },
        )
    }
    val lineHeight = h / 120
    Sprite(assets.whiteLine, lineX.value * w, vp.top(tabUp - 1.5f * lineHeight, lineHeight), lineWidth.value * w, lineHeight)
}

private fun recordText(complexity: Int, mapSize: Int, tab: Int): String {
    val key = recordKey(complexity, mapSize) + when (tab) {
        Settings.TAB_TIME -> "T"
        Settings.TAB_MOVES -> "M"
        else -> ""
    }
    if (key !in Settings) return "---"
    val value = Settings.getInt(key)
    return if (tab == Settings.TAB_TIME) formatTime(value) else value.toString()
}

@Composable
private fun TextButton(
    text: String,
    x: Float, y: Float,
    color: Color,
    canClick: () -> Boolean,
    height: Float = LocalViewport.current.line,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    Label(
        text, x, y,
        color = if (pressed) PressedText else color,
        height = height,
        pressed = pressed,
        modifier = Modifier.pressable(
            onPress = {
                pressed = it
                if (it) Audio.click()
            },
            onClick = { if (canClick()) onClick() },
        ),
    )
}

/**
 * An image button that grows while pressed. A [selected] button stays enlarged and ignores taps
 * (the current records tab). [bottom] is the libGDX y-up coordinate of its lower edge.
 */
@Composable
private fun ImageButton(
    region: Region,
    x: Float, bottom: Float, width: Float, height: Float,
    canClick: () -> Boolean,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val vp = LocalViewport.current
    var pressed by remember { mutableStateOf(false) }
    val grow = if (pressed || selected) vp.width / 30 else 0f
    Sprite(
        region,
        x - grow / 2, vp.top(bottom, height + grow), width + grow, height + grow,
        modifier = Modifier.pressable(
            enabled = !selected,
            onPress = {
                pressed = it
                if (it) Audio.click()
            },
            onClick = { if (canClick()) onClick() },
        ),
    )
}
