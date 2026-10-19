package com.broll.poc.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VerifiedDownloadTest {
    private val sha = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    private fun fixture(block: (File) -> Unit) {
        val root = Files.createTempDirectory("bundled-model").toFile()
        try { block(File(root, "model")) } finally { root.deleteRecursively() }
    }
    private fun prepare(file: File, hash: String = sha, version: Int = 4, text: String = "abc") =
        BundledModelExtractor.prepare(file, 3, hash, version, available = { Long.MAX_VALUE },
            open = { text.byteInputStream() })

    @Test fun successPublishesVerifiedModelAndMarker() = fixture { file ->
        prepare(file)
        assertEquals("abc", file.readText())
        assertTrue(BundledModelExtractor.ready(file, 3, sha, 4))
        assertFalse(File(file.path + ".tmp").exists())
    }
    @Test fun shaMismatchDeletesPartialAndDoesNotPublish() = fixture { file ->
        assertTrue(runCatching { prepare(file, text = "bad") }.isFailure)
        assertFalse(file.exists())
        assertFalse(File(file.path + ".tmp").exists())
        assertFalse(File(file.path + ".verified").exists())
    }
    @Test fun matchingMarkerSkipsHashAndAssetRead() = fixture { file ->
        prepare(file)
        BundledModelExtractor.prepare(file, 3, sha, 4, available = { error("must skip space check") },
            open = { error("must skip asset read") })
        assertTrue(BundledModelExtractor.ready(file, 3, sha, 4))
    }
    @Test fun partialTmpIsRestartedAfterInterruptedPreparation() = fixture { file ->
        File(file.path + ".tmp").writeText("partial")
        prepare(file)
        assertEquals("abc",file.readText())
        assertFalse(File(file.path + ".tmp").exists())
    }
    @Test fun changedSizeOrMarkerTriggersValidationAndRepair() = fixture { file ->
        prepare(file)
        file.writeText("wrong size")
        assertFalse(BundledModelExtractor.ready(file, 3, sha, 4))
        prepare(file)
        File(file.path + ".verified").writeText("stale")
        assertFalse(BundledModelExtractor.ready(file, 3, sha, 4))
        prepare(file, version = 5)
        assertTrue(BundledModelExtractor.ready(file, 3, sha, 5))
        assertFalse(BundledModelExtractor.ready(file, 3, sha, 4))
    }
    @Test fun lowSpaceFailsBeforeOpeningAsset() = fixture { file ->
        val failure = runCatching {
            BundledModelExtractor.prepare(file, 3, sha, 4, available = { 0 }, open = { error("must not open") })
        }.exceptionOrNull()
        assertTrue(failure is InsufficientModelSpace)
        assertFalse(file.exists())
    }
    @Test fun interruptedStreamLeavesNoPublishedOrPartialFile() = fixture { file ->
        val failure = runCatching {
            BundledModelExtractor.prepare(file, 3, sha, 4, available = { Long.MAX_VALUE },
                open = { object : java.io.InputStream() {
                    var reads = 0
                    override fun read(): Int { if (reads++ == 0) return 97; throw java.io.IOException("interrupted") }
                } })
        }.exceptionOrNull()
        assertTrue(failure is java.io.IOException)
        assertFalse(file.exists())
        assertFalse(File(file.path + ".tmp").exists())
        prepare(file)
        assertTrue(BundledModelExtractor.ready(file, 3, sha, 4))
    }
    @Test fun markerMismatchReextractsEvenWhenSizeMatches() = fixture { file ->
        prepare(file)
        File(file.path + ".verified").writeText("stale")
        file.writeText("bad")
        prepare(file)
        assertEquals("abc", file.readText())
    }
    @Test fun freeSpaceBoundariesAndOverflow() {
        val reserve = BundledModelExtractor.RESERVE_BYTES
        assertFalse(BundledModelExtractor.hasSpace(reserve + 2, 3))
        assertTrue(BundledModelExtractor.hasSpace(reserve + 3, 3))
        assertFalse(BundledModelExtractor.hasSpace(-1, 3))
        assertFalse(BundledModelExtractor.hasSpace(Long.MAX_VALUE, Long.MAX_VALUE))
        assertFalse(BundledModelExtractor.hasSpace(Long.MAX_VALUE, -1))
    }
}
