package com.coldcat.browser

import android.app.Application
import android.content.Context
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class ColdCatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        CrashLog.beginSession(this)
        CrashLog.checkpoint(this, "app:onCreate")

        // If the previous run died (native kill or crash), start conservatively:
        // no child-process helpers and no separate extension process. This is
        // the most common source of device-specific startup failures.
        decideSafeMode()

        val safe = safeMode(this)
        CrashLog.checkpoint(this, "engine:create start (safeMode=$safe)")
        // GeckoView initialisation can fail on unusual devices. Never let that
        // kill the process silently: keep the error and show it in the UI.
        startupError = runCatching { createRuntime(safe) }.exceptionOrNull()
        startupError?.let {
            CrashLog.checkpoint(this, "engine:create failed: ${it.javaClass.simpleName}: ${it.message}")
            CrashLog.write(this, it, "startup")
        } ?: CrashLog.checkpoint(this, "engine:create done")
    }

    private fun decideSafeMode() {
        val report = runCatching { ExitInfo.latest(this) }.getOrNull()
        val p = prefs(this)
        val lastSeen = p.getLong(KEY_EXIT_SEEN, 0L)
        if (report != null && report.timestamp > lastSeen) {
            p.edit().putLong(KEY_EXIT_SEEN, report.timestamp).apply()
            if (report.isCrash) p.edit().putBoolean(KEY_SAFE_MODE, true).apply()
        }
        if (!p.contains(KEY_SAFE_MODE)) {
            p.edit().putBoolean(KEY_SAFE_MODE, false).apply()
        }
    }

    private fun createRuntime(safe: Boolean): GeckoRuntime {
        val contentBlocking = ContentBlocking.Settings.Builder()
            .enhancedTrackingProtectionLevel(
                if (safe) ContentBlocking.EtpLevel.DEFAULT else ContentBlocking.EtpLevel.STRICT,
            )
            .build()

        val builder = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlocking)
            .extensionsProcessEnabled(!safe)

        if (safe) {
            builder.appZygoteProcessEnabled(false)
                .isolatedProcessEnabled(false)
        }

        return GeckoRuntime.create(this, builder.build()).also { runtime = it }
    }

    companion object {
        private const val PREFS = "coldcat_prefs"
        private const val KEY_SAFE_MODE = "safe_mode"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_EXIT_SEEN = "exit_seen_at"

        lateinit var runtime: GeckoRuntime
            private set

        /** Non-null when the GeckoView runtime could not be created. */
        var startupError: Throwable? = null
            private set

        /** Set by [AddonsActivity] so open tabs pick up a new extension on return. */
        var extensionsChanged = false

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun safeMode(context: Context): Boolean =
            prefs(context).getBoolean(KEY_SAFE_MODE, false)

        fun setSafeMode(context: Context, value: Boolean) {
            prefs(context).edit().putBoolean(KEY_SAFE_MODE, value).apply()
        }

        fun searchEngine(context: Context): SearchEngine =
            SearchEngine.byName(prefs(context).getString(KEY_SEARCH_ENGINE, null))

        fun setSearchEngine(context: Context, engine: SearchEngine) {
            prefs(context).edit().putString(KEY_SEARCH_ENGINE, engine.name).apply()
        }
    }
}
