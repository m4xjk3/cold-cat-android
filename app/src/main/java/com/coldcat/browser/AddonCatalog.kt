package com.coldcat.browser

/** A small curated list of AMO extensions that are useful on Android. */
object AddonCatalog {
    data class Addon(
        val id: String,
        val name: String,
        val tagline: String,
        val url: String,
        val color: Int,
    )

    val recommended = listOf(
        Addon(
            id = "uBlock0@raymondhill.net",
            name = "uBlock Origin",
            tagline = "Efficient ad and tracker blocker.",
            url = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi",
            color = 0xFFB71C1C.toInt(),
        ),
        Addon(
            id = "addon@darkreader.org",
            name = "Dark Reader",
            tagline = "Dark mode for every website.",
            url = "https://addons.mozilla.org/firefox/downloads/latest/darkreader/latest.xpi",
            color = 0xFF37474F.toInt(),
        ),
        Addon(
            id = "{446900e4-71c2-419f-a6a7-df9c091e268b}",
            name = "Bitwarden",
            tagline = "Open-source password manager.",
            url = "https://addons.mozilla.org/firefox/downloads/latest/bitwarden-password-manager/latest.xpi",
            color = 0xFF1565C0.toInt(),
        ),
        Addon(
            id = "jid1-MnnxcxisBPnSXQ@jetpack",
            name = "Privacy Badger",
            tagline = "Automatically learns to block trackers.",
            url = "https://addons.mozilla.org/firefox/downloads/latest/privacy-badger17/latest.xpi",
            color = 0xFF2E7D32.toInt(),
        ),
        Addon(
            id = "{74145f27-f039-47ce-a470-a662b129930a}",
            name = "ClearURLs",
            tagline = "Removes tracking parameters from links.",
            url = "https://addons.mozilla.org/firefox/downloads/latest/clearurls/latest.xpi",
            color = 0xFF6A1B9A.toInt(),
        ),
        Addon(
            id = "sponsorBlocker@ajay.app",
            name = "SponsorBlock",
            tagline = "Skips sponsor segments in videos.",
            url = "https://addons.mozilla.org/firefox/downloads/latest/sponsorblock/latest.xpi",
            color = 0xFFEF6C00.toInt(),
        ),
    )
}
