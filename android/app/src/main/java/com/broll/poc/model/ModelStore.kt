package com.broll.poc.model

import android.content.Context
import com.broll.poc.AppConfig
import com.broll.poc.BuildConfig
import java.io.File

class ModelStore(private val context: Context) {
    val modelFile: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "models/${AppConfig.MODEL_FILE_NAME}")
    val internalCopy: File
        get() = File(context.filesDir, "models/${AppConfig.MODEL_FILE_NAME}")
    val tmpImport: File
        get() = File("/data/local/tmp/${AppConfig.MODEL_FILE_NAME}")

    fun ensureDirs() {
        modelFile.parentFile?.mkdirs()
        internalCopy.parentFile?.mkdirs()
    }

    fun isValidModel(file: File): Boolean = VerifiedDownload.validOrDelete(file, AppConfig.MODEL_SHA256)

    fun locate(): File? {
        val candidates = if (BuildConfig.DEBUG) listOf(modelFile, internalCopy, tmpImport)
            else listOf(modelFile, internalCopy)
        return candidates.firstOrNull { isValidModel(it) }
    }

    fun importFromLocalTmp(): File {
        check(BuildConfig.DEBUG) { "Local tmp import is debug-only" }
        check(isValidModel(tmpImport)) { "Invalid local model SHA-256" }
        ensureDirs()
        tmpImport.copyTo(modelFile, overwrite = true)
        check(isValidModel(modelFile)) { "Imported model SHA-256 mismatch" }
        return modelFile
    }

    fun download(onProgress: (Long, Long) -> Unit): File =
        VerifiedDownload.download(AppConfig.MODEL_URL, modelFile, AppConfig.MODEL_SHA256, onProgress)
}
