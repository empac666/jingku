package com.broll.poc.model

import android.content.Context
import com.broll.poc.AppConfig
import com.broll.poc.BuildConfig
import java.io.File

class ModelStore(private val context: Context) {
    val modelFile: File get() = File(context.filesDir, "models/${AppConfig.MODEL_FILE_NAME}")
    fun ensureDirs() { modelFile.parentFile?.mkdirs() }
    fun locate(): File? = modelFile.takeIf {
        BundledModelExtractor.ready(it, AppConfig.MODEL_EXPECTED_BYTES, AppConfig.MODEL_SHA256, BuildConfig.VERSION_CODE)
    }
    fun prepare(progress: (Long, Long) -> Unit): File = BundledModelExtractor.prepare(
        modelFile, AppConfig.MODEL_EXPECTED_BYTES, AppConfig.MODEL_SHA256, BuildConfig.VERSION_CODE,
        open = { context.assets.open("offline/${AppConfig.MODEL_FILE_NAME}") }, progress = progress,
    )
}
