package com.broll.poc

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** No Android dependencies: usable and testable even when application initialization fails. */
object CrashReport {
    fun format(device: String, android: String, abi: String, version: String,
               time: String, thread: String, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
        return "Time: $time\nDevice: $device\nAndroid: $android\nABI: $abi\nApp: $version\nThread: $thread\n\n$trace"
    }

    fun write(filesDir: File, text: String) {
        val directory = File(filesDir, "crash")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create crash directory" }
        File(directory, "last_crash.txt").outputStream().use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
    }

    fun handler(previous: Thread.UncaughtExceptionHandler?, save: (Thread, Throwable) -> Unit) =
        Thread.UncaughtExceptionHandler { thread, error ->
            try { save(thread, error) } catch (_: Throwable) {
                // Disk-full or report failure must never replace the original exception.
            } finally {
                previous?.uncaughtException(thread, error)
            }
        }
}
