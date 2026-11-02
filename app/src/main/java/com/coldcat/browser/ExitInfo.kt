package com.coldcat.browser

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reads Android's own history of why this process died (API 30+). This is the
 * only way to see a native GeckoView crash from inside the app: for
 * REASON_CRASH_NATIVE the system stores the tombstone, which we can show and
 * copy. Java exceptions are handled separately by [CrashLog].
 */
object ExitInfo {
    private const val PREFS = "coldcat_diag"
    private const val KEY_SHOWN = "exit_shown_at"

    data class Report(
        val reason: Int,
        val timestamp: Long,
        val description: String?,
        val processName: String?,
        val status: Int,
        val trace: String?,
    ) {
        val reasonText: String get() = reasonName(reason)

        /** Crashes and kills that explain "the app just closed". */
        val isCrash: Boolean
            get() = reason in setOf(
                ApplicationExitInfo.REASON_SIGNALED,
                ApplicationExitInfo.REASON_LOW_MEMORY,
                ApplicationExitInfo.REASON_CRASH,
                ApplicationExitInfo.REASON_CRASH_NATIVE,
                ApplicationExitInfo.REASON_ANR,
                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
                ApplicationExitInfo.REASON_DEPENDENCY_DIED,
                ApplicationExitInfo.REASON_OTHER,
            )

        fun render(context: Context): String = buildString {
            appendLine("Cold Cat exit report")
            appendLine("Reason: $reasonText (${reason})")
            appendLine("Status: $status" + if (reason == ApplicationExitInfo.REASON_SIGNALED) {
                " (signal $status)"
            } else {
                ""
            })
            appendLine("When: ${format(timestamp)}")
            if (!description.isNullOrBlank()) appendLine("Details: $description")
            if (!processName.isNullOrBlank()) appendLine("Process: $processName")
            appendLine("Safe mode: ${ColdCatApp.safeMode(context)}")
            appendLine()
            append(CrashLog.deviceInfo(context))
            appendLine("Checkpoints:")
            appendLine(CrashLog.checkpoints(context))
            appendLine("Trace:")
            append(trace?.takeIf { it.isNotBlank() } ?: "(the system did not provide a trace)")
        }
    }

    fun latest(context: Context): Report? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val entries = runCatching {
            am.getHistoricalProcessExitReasons(context.packageName, 0, 10)
        }.getOrNull().orEmpty()

        val appProcess = entries.firstOrNull {
            it.processName == context.packageName && !it.isSkippable()
        } ?: return null

        return appProcess.toReport()
    }

    /** Every recent non-normal exit of any Cold Cat process (main + extensions). */
    fun recent(context: Context, limit: Int = 5): List<Report> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return emptyList()
        return runCatching {
            am.getHistoricalProcessExitReasons(context.packageName, 0, 10)
        }.getOrNull().orEmpty()
            .filterNot { it.isSkippable() }
            .take(limit)
            .map { it.toReport() }
    }

    private fun ApplicationExitInfo.toReport(): Report {
        val trace = runCatching {
            traceInputStream?.bufferedReader()?.use { reader ->
                val buffer = CharArray(256 * 1024)
                val count = reader.read(buffer)
                if (count > 0) String(buffer, 0, count) else null
            }
        }.getOrNull()

        return Report(
            reason = reason,
            timestamp = timestamp,
            description = description,
            processName = processName,
            status = status,
            trace = trace,
        )
    }

    /** Latest crash the user has not seen yet, if any. */
    fun pending(context: Context): Report? {
        val report = latest(context)
        if (report == null || !report.isCrash) return null
        val shown = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_SHOWN, 0L)
        return report.takeIf { it.timestamp > shown }
    }

    fun markShown(context: Context, report: Report) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_SHOWN, report.timestamp).apply()
    }

    private fun ApplicationExitInfo.isSkippable(): Boolean =
        reason == ApplicationExitInfo.REASON_EXIT_SELF ||
            reason == ApplicationExitInfo.REASON_USER_REQUESTED ||
            reason == ApplicationExitInfo.REASON_USER_STOPPED

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_EXIT_SELF -> "normal exit"
        ApplicationExitInfo.REASON_SIGNALED -> "killed by signal"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "low memory"
        ApplicationExitInfo.REASON_CRASH -> "Java/Kotlin crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
        ApplicationExitInfo.REASON_ANR -> "not responding (ANR)"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization failure"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission change"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource usage"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "closed by user"
        ApplicationExitInfo.REASON_USER_STOPPED -> "stopped by user"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency died"
        ApplicationExitInfo.REASON_OTHER -> "other"
        else -> "reason $reason"
    }

    fun format(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
}
