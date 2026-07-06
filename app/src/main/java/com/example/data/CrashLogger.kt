package com.example.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Permanent crash forensics — so a crash can never be a mystery again.
 *
 * Every uncaught exception is written to files/crash_log.txt (last 5 crashes,
 * with build version, Android version, device model and the full stack trace)
 * BEFORE being passed on to Crashlytics + the system handler. The log survives
 * the crash and can be read on the next launch or pulled via adb.
 */
object CrashLogger {

    private const val FILE_NAME = "crash_log.txt"
    private const val MAX_CRASHES = 5

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                append(appContext, thread, throwable)
            } catch (_: Exception) { /* never let logging break crash handling */ }
            // Chain to Crashlytics + system so reporting still works
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun append(context: Context, thread: Thread, e: Throwable) {
        val file = File(context.filesDir, FILE_NAME)
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val entry = buildString {
            append("═══ CRASH $stamp ═══\n")
            append("App: v${com.example.BuildConfig.VERSION_NAME} (${com.example.BuildConfig.VERSION_CODE})\n")
            append("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
            append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
            append("Thread: ${thread.name}\n")
            append(android.util.Log.getStackTraceString(e))
            append("\n")
        }
        // Keep only the last MAX_CRASHES entries
        val existing = if (file.exists()) file.readText() else ""
        val blocks = (existing.split("═══ CRASH").filter { it.isNotBlank() })
            .takeLast(MAX_CRASHES - 1)
            .joinToString("") { "═══ CRASH$it" }
        file.writeText(blocks + entry)
    }

    /** Last recorded crash text, or null. Read on next launch for diagnostics. */
    fun lastCrash(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        val text = file.readText()
        val idx = text.lastIndexOf("═══ CRASH")
        return if (idx >= 0) text.substring(idx) else null
    }
}
