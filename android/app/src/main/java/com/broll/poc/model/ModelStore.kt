package com.broll.poc.model

import android.content.Context
import com.broll.poc.AppConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelStore(private val context: Context) {

    val modelFile: File
        get() {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            return File(base, "models/${AppConfig.MODEL_FILE_NAME}")
        }

    val internalCopy: File
        get() = File(context.filesDir, "models/${AppConfig.MODEL_FILE_NAME}")

    val tmpImport: File
        get() = File("/data/local/tmp/${AppConfig.MODEL_FILE_NAME}")

    fun ensureDirs() {
        modelFile.parentFile?.mkdirs()
        internalCopy.parentFile?.mkdirs()
    }

    fun isValidModel(file: File): Boolean =
        file.isFile && file.length() == AppConfig.MODEL_EXPECTED_BYTES

    fun locate(): File? {
        val candidates = listOf(modelFile, internalCopy, tmpImport)
        return candidates.firstOrNull { isValidModel(it) }
    }

    fun importFromLocalTmp(): File {
        val src = tmpImport
        if (!isValidModel(src)) {
            val len = if (src.isFile) src.length() else 0L
            if (src.isFile) src.delete()
            error("tmp 模型大小须为 ${AppConfig.MODEL_EXPECTED_BYTES} 字节，当前 $len")
        }
        if (isValidModel(modelFile)) return modelFile
        ensureDirs()
        src.inputStream().use { input ->
            FileOutputStream(modelFile).use { output ->
                input.copyTo(output, bufferSize = 1024 * 256)
                output.fd.sync()
            }
        }
        if (!isValidModel(modelFile)) {
            modelFile.delete()
            error("导入后校验失败，已删除不完整文件")
        }
        return modelFile
    }

    fun download(onProgress: (read: Long, total: Long) -> Unit): File {
        ensureDirs()
        val dest = modelFile
        val tmp = File(dest.parentFile, dest.name + ".partial")
        if (tmp.exists()) tmp.delete()
        val conn = (URL(AppConfig.MODEL_URL).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 120_000
            setRequestProperty("User-Agent", "broll-poc/0.1")
            setRequestProperty("Accept", "*/*")
        }
        try {
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) {
                error("下载失败 HTTP $code")
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buf = ByteArray(256 * 1024)
                    var read = 0L
                    var lastReport = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        if (read - lastReport >= 2L * 1024 * 1024 || (total > 0 && read == total)) {
                            lastReport = read
                            onProgress(read, total)
                        }
                    }
                    output.fd.sync()
                    onProgress(read, total)
                }
            }
        } finally {
            conn.disconnect()
        }
        val downloaded = tmp.length()
        if (downloaded != AppConfig.MODEL_EXPECTED_BYTES) {
            tmp.delete()
            error("下载大小 $downloaded 字节，期望 ${AppConfig.MODEL_EXPECTED_BYTES}，已删除 partial")
        }
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        if (!isValidModel(dest)) {
            dest.delete()
            error("保存后校验失败")
        }
        return dest
    }
}
