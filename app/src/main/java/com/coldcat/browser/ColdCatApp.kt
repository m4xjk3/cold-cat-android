package com.coldcat.browser

import android.app.Application
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class ColdCatApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val contentBlocking = ContentBlocking.Settings.Builder()
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .build()

        val settings = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlocking)
            .extensionsProcessEnabled(true)
            .build()

        runtime = GeckoRuntime.create(this, settings)
    }

    companion object {
        lateinit var runtime: GeckoRuntime
            private set

        /** Set by [AddonsActivity] so open tabs pick up a new extension on return. */
        var extensionsChanged = false
    }
}
