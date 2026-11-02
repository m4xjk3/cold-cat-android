package com.coldcat.browser

import java.net.URLEncoder

/** Turns whatever the user typed into a URL or a DuckDuckGo search. */
object UrlUtils {
    const val HOME = "resource://android/assets/home.html"
    private const val SEARCH = "https://duckduckgo.com/?q="

    fun toUri(input: String): String {
        val text = input.trim()
        if (text.isEmpty()) return HOME

        val lower = text.lowercase()
        if (lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("about:") ||
            lower.startsWith("resource://") ||
            lower.startsWith("file:")
        ) {
            return text
        }

        if (text.startsWith("localhost") || text.matches(Regex("""\d{1,3}(\.\d{1,3}){3}(:\d+)?"""))) {
            return "http://$text"
        }

        val looksLikeHost = !text.contains(' ') && text.contains('.')
        if (!looksLikeHost) return SEARCH + URLEncoder.encode(text, "UTF-8")

        return "https://$text"
    }

    fun isHome(url: String?): Boolean = url.isNullOrBlank() || url == HOME

    /** Extracts the first http(s) link from text shared by another Android app. */
    fun firstHttpUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = LINK.find(text)?.value ?: return null
        return match.trimEnd('.', ',', ';', ')', ']', '"', '\'')
    }

    fun pretty(url: String?): String {
        if (isHome(url)) return ""
        return url!!.removePrefix("https://").removePrefix("http://").removeSuffix("/")
    }

    private val LINK = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
}
