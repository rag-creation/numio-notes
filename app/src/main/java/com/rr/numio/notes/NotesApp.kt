package com.rr.numio.notes

import android.app.Application
import android.os.Build
import java.io.File

// If the app ever crashes, the error is saved (only the error, never any notes),
// so next time it can be sent to the developer from inside the app.
class NotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { crashFile(this).writeText(report(error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun report(error: Throwable): String {
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
        return buildString {
            append("Notes by Numio ").append(version).append('\n')
            append("Android ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\n\n")
            append(error.stackTraceToString().take(12_000))
        }
    }

    companion object {
        fun crashFile(app: android.content.Context) = File(app.filesDir, "last_crash.txt")
    }
}
