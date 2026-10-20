package com.broll.poc.embed

import android.content.Context
import com.broll.poc.AppConfig
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.EmbeddingOptions
import com.google.ai.edge.litertlm.InputData

interface Embedder : AutoCloseable {
    val mode: String
    fun initialize()
    fun embedText(text: String): FloatArray
}

class LiteRtEmbedder(
    context: Context,
    private val modelPath: String,
) : Embedder {
    override val mode: String = "litertlm"

    private val cacheDir = context.cacheDir.absolutePath
    private var engine: EmbeddingEngine? = null
    private val options = EmbeddingOptions(
        normalize = true,
        insertSpecialTokens = null,
        outputSize = AppConfig.FULL_DIM,
    )

    override fun initialize() {
        val cpu = Backend.CPU(threadCount = 4)
        val config = EmbeddingEngineConfig(
            modelPath = modelPath,
            backend = cpu,
            cacheDir = cacheDir,
            )
        val created = EmbeddingEngine(config)
        created.initialize()
        engine = created
    }

    override fun embedText(text: String): FloatArray = compute(listOf(InputData.Text(text)))



    private fun compute(inputs: List<InputData>): FloatArray {
        val eng = engine ?: error("EmbeddingEngine 尚未 initialize")
        val result = eng.computeEmbedding(inputs, options).embedding
        if (result.size != AppConfig.FULL_DIM) {
            error("嵌入维度 ${result.size}，期望 ${AppConfig.FULL_DIM}")
        }
        return result
    }

    override fun close() {
        engine?.close()
        engine = null
    }
}
