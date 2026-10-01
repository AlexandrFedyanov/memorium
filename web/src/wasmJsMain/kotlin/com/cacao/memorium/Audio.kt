package com.cacao.memorium

import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement

// play() rejects until the page got a user gesture; that is expected, so the rejection is swallowed.
private fun playElement(audio: HTMLAudioElement): Unit = js("{ audio.play().catch(function () {}); }")

/** Sound effects and background music; everything is silent while the "music" toggle is off. */
object Audio {
    private fun element(path: String) = (document.createElement("audio") as HTMLAudioElement).apply {
        src = path
        preload = "auto"
    }

    private val click = element("data/click4.wav")
    private val cardFlip = element("data/cards2.mp3")
    private val doorsOpen = element("data/dors_open2_1.mp3")
    private val doorsClosed = element("data/dor_closed.mp3")
    private val music = element("data/music.mp3").apply { loop = true }

    var enabled = Settings.isMusicOn
        set(value) {
            field = value
            if (value) playElement(music) else music.pause()
        }

    init {
        // Browsers block autoplay until the first user gesture: start the music then.
        document.addEventListener("pointerdown", { if (enabled && music.paused) playElement(music) })
        document.addEventListener("keydown", { if (enabled && music.paused) playElement(music) })
    }

    fun startMusic() {
        if (enabled) playElement(music)
    }

    private fun play(sound: HTMLAudioElement, volume: Double) {
        if (!enabled) return
        // A fresh copy per play so that quick repeats overlap instead of restarting each other.
        val copy = sound.cloneNode() as HTMLAudioElement
        copy.volume = volume
        playElement(copy)
    }

    fun click() = play(click, 1.0)
    fun cardFlip(volume: Double = 0.5) = play(cardFlip, volume)
    fun doorsOpen() = play(doorsOpen, 1.0)
    fun doorsClosed() = play(doorsClosed, 1.0)
}
