package com.cacao.memorium

import kotlinx.browser.window

/** UI strings from the original `languages.xml` (English and Russian), picked by the browser language. */
object Strings {
    private val en = mapOf(
        "new_game" to "New game", "records" to "Records", "easy" to "Easy", "hard" to "Hard",
        "start" to "start", "map_size" to "Map size", "move" to "Move", "time" to "Time",
        "score" to "Score", "again" to "Again", "menu" to "Menu",
    )
    private val ru = mapOf(
        "new_game" to "Новая игра", "records" to "Рекорды", "easy" to "Легко", "hard" to "Сложно",
        "start" to "Поехали", "map_size" to "Размер", "move" to "Ходы", "time" to "Время",
        "score" to "Счет", "again" to "Заново", "menu" to "Меню",
    )
    private val current = if (window.navigator.language.lowercase().startsWith("ru")) ru else en

    operator fun get(key: String): String = current[key] ?: key
}
