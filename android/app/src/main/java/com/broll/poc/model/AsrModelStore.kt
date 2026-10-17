package com.broll.poc.model

import android.content.Context
import com.broll.poc.AppConfig
import com.broll.poc.BuildConfig
import java.io.File
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/** Downloaded weights are accepted only after pinned SHA-256 verification. */
class AsrModelStore(private val context: Context) {
    private val modelsRoot: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "models")
    val senseVoiceDir: File get() = File(modelsRoot, AppConfig.ASR_MODEL_DIR)
    val senseVoiceOnnx: File get() = File(senseVoiceDir, AppConfig.ASR_MODEL_ONNX)
    val senseVoiceTokens: File get() = File(senseVoiceDir, AppConfig.ASR_TOKENS)
    val vadFile: File get() = File(modelsRoot, AppConfig.VAD_MODEL_FILE)
    val tmpSenseVoiceDir: File get() = File("/data/local/tmp/${AppConfig.ASR_MODEL_DIR}")
    val tmpVad: File get() = File("/data/local/tmp/${AppConfig.VAD_MODEL_FILE}")

    fun ensureDirs() { senseVoiceDir.mkdirs() }
    fun isSenseVoiceReady(): Boolean {
        val onnx = VerifiedDownload.validOrDelete(senseVoiceOnnx, AppConfig.ASR_ONNX_SHA256)
        val tokens = VerifiedDownload.validOrDelete(senseVoiceTokens, AppConfig.ASR_VOCAB_SHA256)
        return onnx && tokens
    }
    fun isVadReady(): Boolean = VerifiedDownload.validOrDelete(vadFile, AppConfig.VAD_SHA256)
    fun locateSenseVoice(): File? = if (isSenseVoiceReady()) senseVoiceOnnx else null
    fun locateVad(): File? = when {
        isVadReady() -> vadFile
        BuildConfig.DEBUG && VerifiedDownload.validOrDelete(tmpVad, AppConfig.VAD_SHA256) -> tmpVad
        else -> null
    }
    fun importSenseVoiceFromTmp(): File {
        check(BuildConfig.DEBUG) { "Local tmp import is debug-only" }
        val onnx = File(tmpSenseVoiceDir, AppConfig.ASR_MODEL_ONNX)
        val tokens = File(tmpSenseVoiceDir, AppConfig.ASR_TOKENS)
        check(VerifiedDownload.matches(onnx, AppConfig.ASR_ONNX_SHA256) &&
            VerifiedDownload.matches(tokens, AppConfig.ASR_VOCAB_SHA256)) { "Invalid local ASR SHA-256" }
        ensureDirs()
        onnx.copyTo(senseVoiceOnnx, overwrite = true)
        tokens.copyTo(senseVoiceTokens, overwrite = true)
        check(isSenseVoiceReady()) { "Imported ASR SHA-256 mismatch" }
        return senseVoiceOnnx
    }
    fun importVadFromTmp(): File {
        check(BuildConfig.DEBUG) { "Local tmp import is debug-only" }
        check(VerifiedDownload.matches(tmpVad, AppConfig.VAD_SHA256)) { "Invalid local VAD SHA-256" }
        ensureDirs()
        tmpVad.copyTo(vadFile, overwrite = true)
        check(isVadReady()) { "Imported VAD SHA-256 mismatch" }
        return vadFile
    }
    fun downloadVad(onProgress: (Long, Long) -> Unit): File =
        VerifiedDownload.download(AppConfig.VAD_MODEL_URL, vadFile, AppConfig.VAD_SHA256, onProgress)

    fun downloadSenseVoiceArchive(onProgress: (Long, Long) -> Unit): File {
        ensureDirs()
        val archive = File(modelsRoot, "sensevoice.tar.bz2")
        try {
            VerifiedDownload.download(AppConfig.ASR_ARCHIVE_URL, archive, AppConfig.ASR_ARCHIVE_SHA256, onProgress)
            BZip2CompressorInputStream(archive.inputStream()).use { bz ->
                TarArchiveInputStream(bz).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val name = entry.name.substringAfterLast('/')
                        if (entry.isFile && name in listOf(AppConfig.ASR_MODEL_ONNX, AppConfig.ASR_TOKENS)) {
                            File(senseVoiceDir, name).outputStream().use { tar.copyTo(it) }
                        }
                        entry = tar.nextEntry
                    }
                }
            }
            check(isSenseVoiceReady()) { "Extracted ASR SHA-256 mismatch" }
            return senseVoiceOnnx
        } catch (e: Exception) {
            senseVoiceOnnx.delete()
            senseVoiceTokens.delete()
            throw e
        } finally {
            archive.delete()
        }
    }
}
