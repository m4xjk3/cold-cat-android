package com.coldcat.browser

import java.net.URLEncoder

/** Search providers. Some are blocked in some countries, so the user can switch. */
enum class SearchEngine(val title: String, private val template: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    YANDEX("Яндекс", "https://yandex.ru/search/?text="),
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q=");

    fun searchUrl(query: String): String =
        template + URLEncoder.encode(query, "UTF-8")

    companion object {
        val DEFAULT: SearchEngine =
            if (java.util.Locale.getDefault().language == "ru") YANDEX else DUCKDUCKGO

        fun byName(name: String?): SearchEngine =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
