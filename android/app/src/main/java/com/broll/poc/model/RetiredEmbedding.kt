package com.broll.poc.model

import java.io.File

/** Called only after the replacement is verified and atomically published. */
object RetiredEmbedding {
    fun removeAfterReplacement(directory: File) {
        val retired = "embeddinggemma-2-740m.litertlm"
        listOf("", ".verified", ".tmp", ".verified.tmp").forEach { suffix ->
            // Cleanup failure is harmless; never block a verified replacement.
            runCatching { File(directory, retired + suffix).delete() }
        }
    }
}
