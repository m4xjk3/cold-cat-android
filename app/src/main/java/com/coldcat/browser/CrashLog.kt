package com.coldcat.browser

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stores the last uncaught exception next to the app data so the next launch can
 * show it to the user. Native GeckoView crashes cannot be caught here, but Java
 * and Kotlin failures (the common startup problems) are recorded.
 */
object CrashLog {
    private const val FILE_NAME = "last-crash.txt"
    private const val CHECKPOINT_FILE = "checkpoints.txt"
    private const val MAX_CHECKPOINTS = 40
    private const val PREFS = "coldcat_diag"
    private const val KEY_ACTIVE = "session_active"
    private const val KEY_STARTED = "session_started"
    private val lock = Any()

    /** Start time of the previous run if it ended without [endSession]. */
    var previousUncleanExit: Long? = null
        private set

    fun beginSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ACTIVE, false)) {
            previousUncleanExit = prefs.getLong(KEY_STARTED, 0L)
        }
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_STARTED, System.currentTimeMillis())
            .apply()
    }

    fun endSession(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ACTIVE, false).apply()
    }

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            write(app, error, thread.name)
            previous?.uncaughtException(thread, error)
        }
    }

    fun write(context: Context, error: Throwable, threadName: String? = null) {
        runCatching {
            synchronized(lock) {
                File(context.filesDir, FILE_NAME)
                    .writeText(describe(context, error, threadName))
            }
        }
    }

    fun read(context: Context): String? = runCatching {
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /**
     * Breadcrumbs written to disk before risky steps. If the process is killed
     * without a Java exception and Android gives us no tombstone, these lines
     * still show how far startup got.
     */
    fun checkpoint(context: Context, message: String) {
        runCatching {
            synchronized(lock) {
                val file = File(context.filesDir, CHECKPOINT_FILE)
                val line = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date()) +
                    "  " + message + "\n"
                val kept = file.takeIf { it.exists() }
                    ?.readLines()
                    ?.takeLast(MAX_CHECKPOINTS - 1)
                    .orEmpty()
                file.writeText(kept.joinToString("", postfix = if (kept.isEmpty()) "" else "\n") + line)
            }
        }
    }

    fun checkpoints(context: Context): String = runCatching {
        File(context.filesDir, CHECKPOINT_FILE).takeIf { it.exists() }?.readText()
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "(none)"

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }

    fun deviceInfo(context: Context): String = buildString {
        appendLine("App: ${context.getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("Engine: GeckoView ${org.mozilla.geckoview.BuildConfig.MOZ_APP_VERSION} (Gecko ${org.mozilla.geckoview.BuildConfig.GRE_MILESTONE})")
    }

    fun describe(context: Context, error: Throwable, threadName: String? = null): String {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
        return buildString {
            appendLine("Cold Cat crash report")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            append(deviceInfo(context))
            appendLine("Thread: ${threadName ?: Thread.currentThread().name}")
            appendLine()
            append(stack)
        }
    }

    /** Report for a run that vanished without a Java exception (native crash / kill). */
    fun describeUncleanExit(context: Context): String = buildString {
        appendLine("Cold Cat report")
        appendLine("Previous run ended without a normal shutdown, so the crash was")
        appendLine("likely native (GeckoView) or the system killed the process.")
        previousUncleanExit?.takeIf { it > 0 }?.let {
            appendLine("Previous run started: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))}")
        }
        appendLine()
        append(deviceInfo(context))
    }
}
