package com.broll.poc.model

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Content checks also cover previously installed files; a failed digest is never accepted. */
object VerifiedDownload {
    fun matches(file: File, expected: String): Boolean {
        if (!file.isFile) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == expected
    }

    fun validOrDelete(file: File, expected: String): Boolean {
        if (matches(file, expected)) return true
        if (file.exists()) check(file.delete()) { "Cannot delete invalid model: ${file.name}" }
        return false
    }

    fun download(url: String, dest: File, sha256: String, onProgress: (Long, Long) -> Unit): File {
        dest.parentFile?.mkdirs()
        if (validOrDelete(dest, sha256)) return dest
        val partial = File(dest.parentFile, dest.name + ".partial")
        // Retry a corrupt response once, then leave no unverified files behind.
        repeat(2) { attempt ->
            partial.delete()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 300_000
                instanceFollowRedirects = true
            }
            try {
                check(conn.responseCode in 200..299) { "Download HTTP ${conn.responseCode}" }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var read = 0L
                        var last = 0L
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            if (read - last >= 2 * 1024 * 1024) {
                                onProgress(read, total)
                                last = read
                            }
                        }
                        output.fd.sync()
                        onProgress(read, total)
                    }
                }
                if (!matches(partial, sha256)) {
                    check(attempt == 0) { "SHA-256 mismatch after retry" }
                } else {
                    check(partial.renameTo(dest)) { "Cannot save verified model" }
                    return dest
                }
            } finally {
                conn.disconnect()
                partial.delete()
            }
        }
        error("SHA-256 mismatch")
    }
}
