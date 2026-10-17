package com.broll.poc.model

import android.content.Context
import com.broll.poc.AppConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
 * SenseVoice + Silero VAD 模型放在应用私有目录，不打进 APK。
 */
class AsrModelStore(private val context: Context) {

    private val modelsRoot: File
        get() {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            return File(base, "models")
        }

    val senseVoiceDir: File
        get() = File(modelsRoot, AppConfig.ASR_MODEL_DIR)

    val senseVoiceOnnx: File
        get() = File(senseVoiceDir, AppConfig.ASR_MODEL_ONNX)

    val senseVoiceTokens: File
        get() = File(senseVoiceDir, AppConfig.ASR_TOKENS)

    val vadFile: File
        get() = File(modelsRoot, AppConfig.VAD_MODEL_FILE)

    val tmpSenseVoiceDir: File
        get() = File("/data/local/tmp/${AppConfig.ASR_MODEL_DIR}")

    val tmpVad: File
        get() = File("/data/local/tmp/${AppConfig.VAD_MODEL_FILE}")

    fun ensureDirs() {
        modelsRoot.mkdirs()
        senseVoiceDir.mkdirs()
    }

    fun isSenseVoiceReady(): Boolean =
        senseVoiceOnnx.isFile && senseVoiceOnnx.length() > 1_000_000L &&
            senseVoiceTokens.isFile && senseVoiceTokens.length() > 100L

    fun isVadReady(): Boolean = vadFile.isFile && vadFile.length() > 100_000L

    fun locateSenseVoice(): File? = if (isSenseVoiceReady()) senseVoiceOnnx else null

    fun locateVad(): File? = when {
        isVadReady() -> vadFile
        tmpVad.isFile && tmpVad.length() > 100_000L -> tmpVad
        else -> null
    }

    fun importSenseVoiceFromTmp(): File {
        val onnx = File(tmpSenseVoiceDir, AppConfig.ASR_MODEL_ONNX)
        val tokens = File(tmpSenseVoiceDir, AppConfig.ASR_TOKENS)
        if (!onnx.isFile || !tokens.isFile) {
            error(
                "请在 /data/local/tmp/${AppConfig.ASR_MODEL_DIR}/ 下放置 " +
                    "${AppConfig.ASR_MODEL_ONNX} 与 ${AppConfig.ASR_TOKENS}",
            )
        }
        ensureDirs()
        copyFile(onnx, senseVoiceOnnx)
        copyFile(tokens, senseVoiceTokens)
        if (!isSenseVoiceReady()) error("SenseVoice 导入后校验失败")
        return senseVoiceOnnx
    }

    fun importVadFromTmp(): File {
        if (!tmpVad.isFile) error("缺少 ${tmpVad.absolutePath}")
        ensureDirs()
        copyFile(tmpVad, vadFile)
        if (!isVadReady()) error("VAD 导入后校验失败")
        return vadFile
    }

    fun downloadVad(onProgress: (read: Long, total: Long) -> Unit): File {
        ensureDirs()
        return downloadToFile(AppConfig.VAD_MODEL_URL, vadFile, onProgress)
    }

    /**
     * 下载 tar.bz2 并解压出 SenseVoice 目录（仅提取 model.int8.onnx 与 tokens.txt）。
     */
    fun downloadSenseVoiceArchive(onProgress: (read: Long, total: Long) -> Unit): File {
        ensureDirs()
        val partial = File(modelsRoot, "sensevoice.tar.bz2.partial")
        downloadToFile(AppConfig.ASR_ARCHIVE_URL, partial, onProgress, allowUnknownSize = true)
        extractSenseVoiceFromTarBz2(partial)
        partial.delete()
        if (!isSenseVoiceReady()) error("解压后未找到 SenseVoice 模型文件")
        return senseVoiceOnnx
    }

    private fun extractSenseVoiceFromTarBz2(archive: File) {
        BZip2CompressorInputStream(archive.inputStream()).use { bz ->
            TarArchiveInputStream(bz).use { tar ->
                var entry = tar.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.substringAfterLast('/')
                        if (name == AppConfig.ASR_MODEL_ONNX || name == AppConfig.ASR_TOKENS) {
                            val dest = File(senseVoiceDir, name)
                            FileOutputStream(dest).use { out -> tar.copyTo(out) }
                            outSync(dest)
                        }
                    }
                    entry = tar.nextEntry
                }
            }
        }
    }

    private fun copyFile(src: File, dest: File) {
        src.inputStream().use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output, bufferSize = 256 * 1024)
                output.fd.sync()
            }
        }
    }

    private fun outSync(file: File) {
        FileOutputStream(file, true).use { it.fd.sync() }
    }

    private fun downloadToFile(
        url: String,
        dest: File,
        onProgress: (read: Long, total: Long) -> Unit,
        allowUnknownSize: Boolean = false,
    ): File {
        val tmp = File(dest.parentFile, dest.name + ".partial")
        if (tmp.exists()) tmp.delete()
        var total = -1L
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 300_000
            setRequestProperty("User-Agent", "broll-dialogue/0.2")
        }
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) error("下载失败 HTTP ${conn.responseCode}")
            total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buf = ByteArray(256 * 1024)
                    var read = 0L
                    var last = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        if (read - last >= 2L * 1024 * 1024 || (total > 0 && read == total)) {
                            last = read
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
        if (!allowUnknownSize && total > 0 && tmp.length() != total) {
            tmp.delete()
            error("下载大小 ${tmp.length()} 与 Content-Length $total 不符")
        }
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        return dest
    }
}
