package com.cacao.memorium

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

private const val FRAME_MS = 50L

/** Studio logo with a 20-frame animation, then a fade into the menu. */
@Composable
fun LogoScreen(onFinished: () -> Unit) {
    val assets = LocalAssets.current
    val vp = LocalViewport.current
    val finished by rememberUpdatedState(onFinished)
    var frame by remember { mutableIntStateOf(0) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(700)
        val start = withFrameMillis { it }
        val duration = FRAME_MS * assets.logoFrames.size
        while (true) {
            val elapsed = withFrameMillis { it } - start
            if (elapsed >= duration) break
            frame = (elapsed / FRAME_MS).toInt()
        }
        // The animation is played as a loop, so it comes to rest on its first frame.
        frame = 0
        delay(250)
        fade.animateTo(1f, tween(350, easing = LinearEasing))
        finished()
    }

    val w = vp.width
    val h = vp.height
    Stage(Modifier.fillMaxSize()) {
        Sprite(assets.logoBackground, 0f, 0f, w, h)
        Sprite(assets.logo, 0.1f * w, h / 2 - 0.075f * w, 0.6f * w, 0.15f * w)
        Sprite(assets.logoFrames[frame], 0.68f * w, h / 2 - 0.175f * w, 0.2f * w, 0.2f * w)
        Fade(assets.logoFade, fade.value)
    }
}
