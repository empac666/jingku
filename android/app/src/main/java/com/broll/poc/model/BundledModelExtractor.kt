package com.broll.poc.model

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class InsufficientModelSpace : java.io.IOException("Insufficient model storage")

/** Pure IO core. A stale partial is restarted; no unverified file is ever published. */
object BundledModelExtractor {
    const val RESERVE_BYTES = 64L * 1024 * 1024
    fun hasSpace(available: Long, required: Long): Boolean =
        required >= 0 && available >= RESERVE_BYTES && required <= available - RESERVE_BYTES

    private fun marker(file: File) = File(file.path + ".verified")
    fun ready(file: File, size: Long, sha: String, version: Int): Boolean =
        file.isFile && file.length() == size &&
            runCatching { marker(file).readText() == "$version:$sha" }.getOrDefault(false)

    @Synchronized
    fun prepare(file: File, size: Long, sha: String, version: Int,
                available: () -> Long = { file.parentFile!!.usableSpace },
                open: () -> InputStream, progress: (Long, Long) -> Unit = { _, _ -> }): File {
        file.parentFile!!.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.delete()
        if (ready(file, size, sha, version)) return file
        marker(file).delete()
        file.delete()
        if (!hasSpace(available(), size)) throw InsufficientModelSpace()
        try {
            open().use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var copied = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        copied += n
                        check(copied <= size) { "Bundled model size mismatch" }
                        progress(copied, size)
                    }
                    output.flush()
                    output.fd.sync()
                }
            }
            check(tmp.length() == size && VerifiedDownload.matches(tmp, sha)) { "Bundled model SHA-256 mismatch" }
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            writeMarker(file, version, sha)
            return file
        } finally { tmp.delete() }
    }

    private fun writeMarker(file: File, version: Int, sha: String) {
        val tmp = File(file.path + ".verified.tmp")
        try {
            FileOutputStream(tmp).use { it.write("$version:$sha".toByteArray()); it.fd.sync() }
            Files.move(tmp.toPath(), marker(file).toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { tmp.delete() }
    }
}
