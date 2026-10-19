package com.broll.poc.model

import android.content.Context
import com.broll.poc.AppConfig

/** Install-time pack assets are available through the ordinary AssetManager. No duplicate weights. */
class AsrModelStore(context: Context) {
    val assets = context.applicationContext.assets
    val senseVoiceOnnx = "offline/${AppConfig.ASR_MODEL_ONNX}"
    val senseVoiceTokens = "offline/${AppConfig.ASR_TOKENS}"
    val vadPath = "offline/${AppConfig.VAD_MODEL_FILE}"
    fun ensureDirs() = Unit
    private fun exists(path: String): Boolean = runCatching { assets.open(path).use { it.read() >= 0 } }.getOrDefault(false)
    fun isSenseVoiceReady() = exists(senseVoiceOnnx) && exists(senseVoiceTokens)
    fun isVadReady() = exists(vadPath)
    fun locateVad(): String? = vadPath.takeIf { isVadReady() }
}
