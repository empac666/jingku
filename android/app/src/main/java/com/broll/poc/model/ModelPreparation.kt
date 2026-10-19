package com.broll.poc.model

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** Extraction survives Activity recreation. Only the application context is retained. */
object ModelPreparation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var task: Deferred<Unit>? = null
    @Volatile var percent = 0
        private set
    @Synchronized fun running() = task?.isCompleted == false
    @Synchronized fun start(context: Context): Deferred<Unit> {
        task?.takeIf { !it.isCompleted }?.let { return it }
        val app = context.applicationContext
        percent = 0
        return scope.async {
            val asr = AsrModelStore(app)
            check(asr.isSenseVoiceReady() && asr.isVadReady()) { "Bundled speech assets unavailable" }
            ModelStore(app).prepare { copied, total -> percent = (copied * 100 / total).toInt() }
            Unit
        }.also { task = it }
    }
}
