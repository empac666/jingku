package com.broll.poc

import android.content.Context
import android.os.Build
import java.util.Date

object CrashGuard {
    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(CrashReport.handler(previous) { thread, error ->
            CrashReport.write(context.filesDir, describe(error, thread))
        })
    }

    fun describe(error: Throwable, thread: Thread = Thread.currentThread()): String =
        CrashReport.format("${Build.MANUFACTURER} ${Build.MODEL}",
            "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            Build.SUPPORTED_ABIS.joinToString(), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            Date().toString(), thread.name, error)
}
