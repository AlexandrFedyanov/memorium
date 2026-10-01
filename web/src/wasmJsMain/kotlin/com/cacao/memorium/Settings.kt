package com.cacao.memorium

import kotlinx.browser.localStorage

/** Persistent settings and records; same keys as the Android build's `mySettings` preferences. */
object Settings {
    private const val PREFIX = "memorium.mySettings."

    operator fun contains(key: String) = localStorage.getItem(PREFIX + key) != null

    fun getInt(key: String, default: Int = 0) = localStorage.getItem(PREFIX + key)?.toIntOrNull() ?: default
    fun putInt(key: String, value: Int) = localStorage.setItem(PREFIX + key, value.toString())

    private fun getBoolean(key: String, default: Boolean) =
        localStorage.getItem(PREFIX + key)?.toBooleanStrictOrNull() ?: default
    private fun putBoolean(key: String, value: Boolean) = localStorage.setItem(PREFIX + key, value.toString())

    const val EASY = 0
    const val HARD = 1
    val MAP_SIZES = listOf(12, 24, 40)

    const val TAB_SCORE = 1
    const val TAB_TIME = 2
    const val TAB_MOVES = 3

    var complexity: Int
        get() = getInt("complexity", EASY).takeIf { it == HARD } ?: EASY
        set(value) = putInt("complexity", value)

    var mapSize: Int
        get() = getInt("mapsize", 12).takeIf { it in MAP_SIZES } ?: 12
        set(value) = putInt("mapsize", value)

    var isMusicOn: Boolean
        get() = getBoolean("isMusicOn", true)
        set(value) = putBoolean("isMusicOn", value)

    var recordTab: Int
        get() = getInt("recordTabel", TAB_SCORE).takeIf { it in TAB_SCORE..TAB_MOVES } ?: TAB_SCORE
        set(value) = putInt("recordTabel", value)
}

/** Record keys look like `easy12` (score), `easy12T` (time) and `easy12M` (moves). */
fun recordKey(complexity: Int, mapSize: Int) = (if (complexity == Settings.HARD) "hard" else "easy") + mapSize

/** Formats seconds as mm:ss. */
fun formatTime(seconds: Int): String =
    "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
