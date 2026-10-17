package com.broll.poc

enum class IndexStage {
    DECODE,
    TRANSCRIBE,
    EMBED,
}

data class IndexProgress(
    val batchIndex: Int,
    val batchTotal: Int,
    val videoName: String,
    val stage: IndexStage,
    val message: String,
    val embedCurrent: Int = 0,
    val embedTotal: Int = 0,
) {
    val embedPercent: Int?
        get() = if (stage == IndexStage.EMBED && embedTotal > 0) {
            ((embedCurrent.coerceAtMost(embedTotal)) * 100) / embedTotal
        } else {
            null
        }
}
