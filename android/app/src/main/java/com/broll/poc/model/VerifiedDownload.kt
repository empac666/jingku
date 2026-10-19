package com.broll.poc.model

import java.io.File
import java.security.MessageDigest

/** SHA helper shared by bundled asset preparation and its host tests. */
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
}
