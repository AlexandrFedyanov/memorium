package com.cacao.memorium

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val FLIP_MS = 200
private const val FLIP_BACK_DELAY_MS = 700L

private class Card(val index: Int) {
    /** 1..20: index of the face picture. */
    var face by mutableIntStateOf(1)
    var faceUp by mutableStateOf(false)
    var touchable by mutableStateOf(true)
    /** 0 = waiting off-screen on the right, 1 = in its grid cell. */
    val arrival = Animatable(0f)
    /** Horizontal scale while turning over. */
    val turn = Animatable(1f)
}

/** A result line that slides in from the side; [shift] is in screen widths (0 = in place). */
private class ResultText(val text: String, val color: Color, val fromLeft: Boolean) {
    val shift = Animatable(if (fromLeft) -1f else 1f)
}

private class Results(
    val captions: List<ResultText>,
    val values: List<ResultText>,
    val menu: ResultText,
    val again: ResultText,
) {
    var buttonsReady by mutableStateOf(false)
    val all get() = captions + values + listOf(menu, again)
}

/** Game rules and animations of the original `GameScreen`. */
private class MemoryGame(private val scope: CoroutineScope, val complexity: Int, val mapSize: Int) {
    val columns = when (mapSize) { 24 -> 4; 40 -> 5; else -> 3 }
    val rows = mapSize / columns
    val cards = List(mapSize) { Card(it) }

    /** Cards opened since the last mismatch, in order (`presedCards`). */
    private val opened = mutableListOf<Int>()
    /** Cards currently open (`gameEndCounter`). */
    private var openCount = 0
    private var moves = 0
    private var startedAt: TimeMark? = null
    private var seconds = 0
    /** Brief lock after each tap so that two cards can't be opened by one gesture. */
    private var locked = false

    /** 1 = doors closed, 0 = open. */
    val doors = Animatable(1f)
    /** Brightness of the doors: they darken while opening. */
    val doorShade = Animatable(1f)
    val fade = Animatable(1f)
    var results by mutableStateOf<Results?>(null)
    var leaving by mutableStateOf(false)

    fun start() {
        scope.launch { fade.animateTo(0f, tween(350, easing = LinearEasing)) }
        deal(startDelayMs = 2000)
        scope.launch {
            delay(800)
            openDoors()
        }
    }

    /** Shuffles the pairs and flies the cards in from the right, one after another. */
    private fun deal(startDelayMs: Long) {
        val faces = List(mapSize) { it / 2 + 1 }.shuffled()
        cards.forEachIndexed { i, card ->
            card.face = faces[i]
            card.faceUp = false
            card.touchable = true
            scope.launch {
                card.turn.snapTo(1f)
                card.arrival.snapTo(0f)
                delay(startDelayMs + i * 50L)
                card.arrival.animateTo(1f, tween(300, easing = LinearEasing))
            }
        }
    }

    private fun openDoors() {
        Audio.doorsOpen()
        scope.launch { doorShade.animateTo(0.4f, tween(1000, easing = Exp5)) }
        scope.launch { doors.animateTo(0f, tween(1000, easing = LinearEasing)) }
    }

    private fun closeDoors() {
        Audio.doorsClosed()
        scope.launch { doorShade.animateTo(1f, tween(1000, easing = Exp5In)) }
        scope.launch { doors.animateTo(1f, tween(1000, easing = LinearEasing)) }
    }

    fun onCardClick(card: Card) {
        if (locked || leaving || !card.touchable || openCount == mapSize) return
        locked = true
        scope.launch {
            delay(50)
            locked = false
        }
        opened += card.index
        turnUp(card)
        Audio.cardFlip(0.5)
        if (moves == 0) startedAt = TimeSource.Monotonic.markNow()
        moves++
        openCount++
        if (opened.size % 2 == 0) checkPair()
        if (openCount == mapSize) {
            seconds = startedAt?.elapsedNow()?.inWholeSeconds?.toInt() ?: 0
            scope.launch {
                delay(1000)
                if (openCount == mapSize) finish()
            }
        }
    }

    /** After every second card: a mismatch turns the pair (easy) or every open card (hard) back. */
    private fun checkPair() {
        val first = cards[opened[opened.size - 2]]
        val second = cards[opened.last()]
        if (first.face == second.face) return
        if (complexity == Settings.EASY) {
            turnDown(first)
            turnDown(second)
            openCount -= 2
        } else {
            opened.forEach { turnDown(cards[it]) }
            openCount = 0
        }
        opened.clear()
    }

    private fun turnUp(card: Card) {
        card.touchable = false
        scope.launch {
            card.turn.animateTo(0f, tween(FLIP_MS, easing = LinearEasing))
            card.faceUp = true
            card.turn.animateTo(1f, tween(FLIP_MS, easing = LinearEasing))
        }
    }

    private fun turnDown(card: Card) {
        scope.launch {
            delay(FLIP_BACK_DELAY_MS)
            Audio.cardFlip(0.25)
            card.turn.animateTo(0f, tween(FLIP_MS, easing = LinearEasing))
            card.faceUp = false
            card.turn.animateTo(1f, tween(FLIP_MS, easing = LinearEasing))
            card.touchable = true
        }
    }

    /** Scores the round, saves records, closes the doors and slides the results in. */
    private fun finish() {
        val (moveLimit, timeLimit) = when (complexity) {
            Settings.EASY -> when (mapSize) { 12 -> 24 to 40; 24 -> 60 to 80; else -> 120 to 160 }
            else -> when (mapSize) { 12 -> 36 to 120; 24 -> 140 to 300; else -> 400 to 800 }
        }
        val score = (if (moveLimit > moves) (moveLimit - moves) * 5 else 0) +
            (if (timeLimit > seconds) timeLimit - seconds else 0)

        val key = recordKey(complexity, mapSize)
        val newScore = score != 0 && Settings.getInt(key) < score
        if (newScore) Settings.putInt(key, score)
        val newTime = (if (key + "T" in Settings) Settings.getInt(key + "T") else 60000) > seconds
        if (newTime) Settings.putInt(key + "T", seconds)
        val newMoves = (if (key + "M" in Settings) Settings.getInt(key + "M") else 1000) > moves
        if (newMoves) Settings.putInt(key + "M", moves)

        fun value(text: String, record: Boolean) = ResultText(text, if (record) Color.Yellow else Color.White, false)
        val result = Results(
            captions = listOf("time", "move", "score").map { ResultText(Strings[it], Color.White, true) },
            values = listOf(
                value(formatTime(seconds), newTime),
                value(moves.toString(), newMoves),
                value(score.toString(), newScore),
            ),
            menu = ResultText(Strings["menu"], Color.White, true),
            again = ResultText(Strings["again"], Color.White, false),
        )
        results = result
        closeDoors()

        val schedule = listOf(
            result.captions[0] to 1500L, result.values[0] to 1750L,
            result.captions[1] to 2000L, result.values[1] to 2250L,
            result.captions[2] to 2500L, result.values[2] to 2750L,
            result.menu to 3150L, result.again to 3150L,
        )
        for ((text, at) in schedule) {
            scope.launch {
                delay(at)
                text.shift.animateTo(0f, tween(250, easing = LinearEasing))
                if (text !== result.again) Audio.click()
                if (text === result.menu) result.buttonsReady = true
            }
        }
    }

    /** "Again": slide the results out, then deal a new round behind opening doors. */
    fun again() {
        val result = results ?: return
        if (!result.buttonsReady) return
        result.buttonsReady = false
        for (text in result.all) {
            scope.launch { text.shift.animateTo(if (text.fromLeft) -1f else 1f, tween(300, easing = LinearEasing)) }
        }
        scope.launch {
            delay(300)
            results = null
            opened.clear()
            openCount = 0
            moves = 0
            seconds = 0
            startedAt = null
            deal(startDelayMs = 1500)
            openDoors()
        }
    }

    fun leave(onLeft: () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            fade.animateTo(1f, tween(350, easing = LinearEasing))
            onLeft()
        }
    }
}

@Composable
fun GameScreen(onMenu: () -> Unit) {
    val assets = LocalAssets.current
    val vp = LocalViewport.current
    val w = vp.width
    val h = vp.height
    val scope = rememberCoroutineScope()
    val toMenu by rememberUpdatedState(onMenu)
    val game = remember { MemoryGame(scope, Settings.complexity, Settings.mapSize) }

    LaunchedEffect(Unit) { game.start() }
    BackHandler { game.leave(toMenu) }

    Stage(Modifier.fillMaxSize()) {
        Sprite(assets.gameBackground, 0f, 0f, w, h)

        // Grid of square cards with 1/10 gaps, centred on the screen.
        val size = w / (game.columns + 1)
        val gap = size / 10
        val gridWidth = size * game.columns + gap * (game.columns - 1)
        val gridHeight = size * game.rows + gap * (game.rows - 1)
        for (card in game.cards) {
            key(card.index) {
                val x = (w - gridWidth) / 2 + (card.index % game.columns) * (size + gap) +
                    (1f - card.arrival.value) * 2 * gridWidth
                val y = (h - gridHeight) / 2 + (card.index / game.columns) * (size + gap)
                val width = size * card.turn.value
                Sprite(
                    if (card.faceUp) assets.cards[card.face] else assets.cards[0],
                    x + (size - width) / 2, y, width, size,
                    modifier = Modifier.pressable { game.onCardClick(card) },
                )
            }
        }

        val doorWidth = w / 2 * game.doors.value
        val shade = game.doorShade.value
        val doorTint = Color(shade, shade, shade)
        Sprite(assets.doorLeft, 0f, 0f, doorWidth, h, tint = doorTint)
        Sprite(assets.doorRight, w - doorWidth, 0f, doorWidth, h, tint = doorTint)

        game.results?.let { ResultsOverlay(it, onMenu = { game.leave(toMenu) }, onAgain = game::again) }
        Fade(assets.screenFade, game.fade.value)
    }
}

@Composable
private fun ResultsOverlay(results: Results, onMenu: () -> Unit, onAgain: () -> Unit) {
    val vp = LocalViewport.current
    val w = vp.width
    val h = vp.height
    val line = vp.line
    val margin = w / 20
    val rowTops = listOf(0.25f * h - 1.6f * line, 0.25f * h - 0.5f * line, 0.25f * h + 0.6f * line)

    // Captions end just left of the centre, values start just right of it.
    results.captions.forEachIndexed { i, text ->
        Label(text.text, w / 2 - margin - textWidth(text.text) + text.shift.value * w, rowTops[i], text.color)
    }
    results.values.forEachIndexed { i, text ->
        Label(text.text, w / 2 + margin + text.shift.value * w, rowTops[i], text.color)
    }
    ResultButton(results.menu, centerX = w / 4, enabled = results.buttonsReady, onClick = onMenu)
    ResultButton(results.again, centerX = 3 * w / 4, enabled = results.buttonsReady, onClick = onAgain)
}

@Composable
private fun ResultButton(text: ResultText, centerX: Float, enabled: Boolean, onClick: () -> Unit) {
    val vp = LocalViewport.current
    var pressed by remember { mutableStateOf(false) }
    Label(
        text.text,
        centerX - textWidth(text.text) / 2 + text.shift.value * vp.width,
        0.9f * vp.height - vp.line,
        color = if (pressed) PressedText else text.color,
        pressed = pressed,
        modifier = Modifier.pressable(
            enabled = enabled,
            onPress = {
                pressed = it
                if (it) Audio.click()
            },
            onClick = onClick,
        ),
    )
}
