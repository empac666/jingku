package com.broll.poc

import android.content.Context
import com.broll.poc.embed.Embedder
import com.broll.poc.embed.LiteRtEmbedder
import com.broll.poc.metrics.MetricsLogger
import com.broll.poc.model.ModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide embedder; survives Activity rotation. Closed asynchronously on explicit reset only.
 */
object EngineHolder {
    private lateinit var appContext: Context
    private val mutex = Mutex()
    private var embedder: Embedder? = null
    private var metrics: MetricsLogger? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun bindMetrics(logger: MetricsLogger) {
        metrics = logger
    }

    suspend fun open(): Embedder = mutex.withLock {
        openLocked()
    }

    private fun openLocked(): Embedder {
        val current = embedder
        val store = ModelStore(appContext)
        val model = store.locate()
        if (current != null && current.mode == "litertlm" && model != null) return current
        if (current != null) {
            current.close()
            embedder = null
        }
        if (model == null) {
            error("未找到有效嵌入模型（须为 ${AppConfig.MODEL_EXPECTED_BYTES} 字节）。请先下载。")
        }
        val t0 = System.nanoTime()
        val engine = LiteRtEmbedder(appContext, model.absolutePath)
        engine.initialize()
        metrics?.log(
            event = "model",
            stage = "init",
            elapsedMs = (System.nanoTime() - t0) / 1_000_000,
            detail = "litertlm path=${model.absolutePath} bytes=${model.length()}",
            embedderMode = "litertlm",
        )
        embedder = engine
        return engine
    }

    fun closeAsync(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            mutex.withLock {
                embedder?.close()
                embedder = null
            }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            embedder?.close()
            embedder = null
        }
    }
}
