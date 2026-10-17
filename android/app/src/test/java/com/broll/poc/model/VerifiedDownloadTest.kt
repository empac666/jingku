package com.broll.poc.model

import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class VerifiedDownloadTest {
    private val abcSha = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test fun corruptExistingFileIsDeleted() {
        val file = Files.createTempFile("model-corrupt", ".bin").toFile()
        file.writeText("bad")
        try {
            assertFalse(VerifiedDownload.validOrDelete(file, abcSha))
            assertFalse(file.exists())
        } finally { file.delete() }
    }

    @Test fun corruptResponseIsRetriedAndOnlyVerifiedBytesAreSaved() = withServer(false) { url, attempts, dest ->
        VerifiedDownload.download(url, dest, abcSha) { _, _ -> }
        assertEquals(2, attempts.get())
        assertEquals("abc", dest.readText())
        assertFalse(java.io.File(dest.path + ".partial").exists())
    }

    @Test fun repeatedCorruptionFailsAndLeavesNoModelOrPartial() = withServer(true) { url, attempts, dest ->
        assertTrue(runCatching { VerifiedDownload.download(url, dest, abcSha) { _, _ -> } }.isFailure)
        assertEquals(2, attempts.get())
        assertFalse(dest.exists())
        assertFalse(java.io.File(dest.path + ".partial").exists())
    }

    private fun withServer(alwaysCorrupt: Boolean, block: (String, AtomicInteger, java.io.File) -> Unit) {
        val root = Files.createTempDirectory("model-download").toFile()
        val server = ServerSocket(0, 2, java.net.InetAddress.getByName("127.0.0.1"))
        val attempts = AtomicInteger()
        val worker = thread(isDaemon = true) {
            try {
                repeat(2) {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val input = socket.getInputStream().bufferedReader()
                        while (!input.readLine().isNullOrEmpty()) { /* consume request headers */ }
                        val body = if (attempts.incrementAndGet() == 1 || alwaysCorrupt) "bad" else "abc"
                        socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: 3\r\nConnection: close\r\n\r\n" + body).toByteArray())
                    }
                }
            } catch (_: java.net.SocketException) { /* server closed by test cleanup */ }
        }
        try { block("http://127.0.0.1:${server.localPort}/", attempts, java.io.File(root, "model")) }
        finally { server.close(); worker.join(6000); root.deleteRecursively() }
    }
}
