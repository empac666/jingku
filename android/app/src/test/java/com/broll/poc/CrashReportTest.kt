package com.broll.poc

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File

class CrashReportTest {
    @Test fun reportContainsDeviceAndNestedTraceAndOverwritesPreviousReport() {
        val dir = Files.createTempDirectory("crash-report").toFile()
        try {
            val text = CrashReport.format("设备 Model", "16 API 36", "arm64-v8a", "1.0.2 (3)",
                "2026-10-17", "main", IllegalStateException("startup", IllegalArgumentException("cause")))
            listOf("设备 Model", "16 API 36", "arm64-v8a", "1.0.2 (3)", "2026-10-17", "main",
                "IllegalStateException: startup", "Caused by: java.lang.IllegalArgumentException: cause")
                .forEach { assertTrue(text.contains(it)) }
            CrashReport.write(dir, "old crash")
            CrashReport.write(dir, text)
            assertEquals(text, File(dir, "crash/last_crash.txt").readText())
        } finally { dir.deleteRecursively() }
    }

    @Test fun handlerChainsOriginalExceptionEvenIfDiskWriteFails() {
        val original = IllegalStateException("original")
        var chained = false
        val thread = Thread.currentThread()
        val handler = CrashReport.handler(Thread.UncaughtExceptionHandler { t, e ->
            assertSame(thread, t); assertSame(original, e); chained = true
        }) { _, _ -> throw java.io.IOException("disk full") }
        handler.uncaughtException(thread, original)
        assertTrue(chained)
    }

    @Test fun handlerWritesBeforeChaining() {
        var saved: Throwable? = null
        val error = LinkageError("native load")
        CrashReport.handler(Thread.UncaughtExceptionHandler { _, e -> assertSame(e, saved) }) {
            _, e -> saved = e
        }.uncaughtException(Thread.currentThread(), error)
        assertSame(error, saved)
    }
}
