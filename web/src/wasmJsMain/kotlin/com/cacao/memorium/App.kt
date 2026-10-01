package com.cacao.memorium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

private enum class Screen { Logo, Menu, Game }

/** Portrait aspect ratios the game screen may take; outside this range it is letterboxed. */
private const val MAX_ASPECT = 0.6f
private const val MIN_ASPECT = 0.45f

@Composable
fun App() {
    var assets by remember { mutableStateOf<Assets?>(null) }
    LaunchedEffect(Unit) { assets = Assets.load() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val loaded = assets ?: return@BoxWithConstraints
        // Fill a phone-shaped window completely (as the original did); on desktop keep a portrait column.
        val availableWidth = constraints.maxWidth.toFloat()
        val availableHeight = constraints.maxHeight.toFloat()
        val aspect = availableWidth / availableHeight
        val width = if (aspect > MAX_ASPECT) availableHeight * MAX_ASPECT else availableWidth
        val height = if (aspect < MIN_ASPECT) availableWidth / MIN_ASPECT else availableHeight
        val viewport = remember(width, height) { GameViewport(width, height) }
        val density = LocalDensity.current

        var screen by remember { mutableStateOf(Screen.Logo) }
        CompositionLocalProvider(LocalAssets provides loaded, LocalViewport provides viewport) {
            Box(Modifier.size(with(density) { width.toDp() }, with(density) { height.toDp() }).clipToBounds()) {
                when (screen) {
                    Screen.Logo -> LogoScreen(onFinished = { screen = Screen.Menu })
                    Screen.Menu -> MenuScreen(onStart = { screen = Screen.Game })
                    Screen.Game -> GameScreen(onMenu = { screen = Screen.Menu })
                }
            }
        }
    }
}
