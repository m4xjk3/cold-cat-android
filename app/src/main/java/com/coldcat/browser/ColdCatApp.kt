package com.coldcat.browser

import android.app.Application
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class ColdCatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        CrashLog.beginSession(this)

        // GeckoView initialisation can fail on unusual devices. Never let that
        // kill the process silently: keep the error and show it in the UI.
        startupError = runCatching { createRuntime() }.exceptionOrNull()
        startupError?.let { CrashLog.write(this, it, "startup") }
    }

    private fun createRuntime(): GeckoRuntime {
        val contentBlocking = ContentBlocking.Settings.Builder()
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .build()

        val settings = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlocking)
            .extensionsProcessEnabled(true)
            .build()

        return GeckoRuntime.create(this, settings).also { runtime = it }
    }

    companion object {
        lateinit var runtime: GeckoRuntime
            private set

        /** Non-null when the GeckoView runtime could not be created. */
        var startupError: Throwable? = null
            private set

        /** Set by [AddonsActivity] so open tabs pick up a new extension on return. */
        var extensionsChanged = false
    }
}
