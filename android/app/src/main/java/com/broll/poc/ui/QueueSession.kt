package com.broll.poc.ui

/** Only events observed by this UI session are represented; there is no persistent inventory. */
data class QueueVideo(val uri: String, val name: String)
class QueueSession {
    val pending = mutableListOf<QueueVideo>()
    var batch: List<QueueVideo> = emptyList()
        private set
    var lastObserved = 0
        private set
    var active = 0
        private set
    var replay: QueueVideo? = null
    var successful = false
        private set
    var stage = ""
    var percent: Int? = null
    var terminal: String? = null
    val counts = mutableMapOf<String, Int>()
    fun start() {
        batch = pending.toList(); lastObserved = 0; active = 0; stage = ""; terminal = null; counts.clear(); replay = null; successful = false
    }
    fun observe(index: Int) { lastObserved = maxOf(lastObserved, index); active = index }
    fun finish(success: Boolean, label: String) {
        terminal = label; successful = success
        if (!success) pending.removeAll(batch.take((lastObserved - 1).coerceAtLeast(0)).toSet())
    }
    fun completeSuccess() { pending.removeAll(batch.toSet()); clearBatch() }
    fun clearBatch() { batch = emptyList(); lastObserved = 0; active = 0; terminal = null; counts.clear(); replay = null; successful = false }
    fun rows() = (listOfNotNull(replay) + batch + pending).distinctBy { it.uri }
}
