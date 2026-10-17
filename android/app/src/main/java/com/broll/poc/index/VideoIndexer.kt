package com.broll.poc.index

import android.content.Context
import android.net.Uri
import com.broll.poc.AppConfig
import com.broll.poc.IndexProgress
import com.broll.poc.IndexStage
import com.broll.poc.IndexTaskCallbacks
import com.broll.poc.asr.SherpaTranscriber
import com.broll.poc.data.IndexDatabase
import com.broll.poc.embed.Embedder
import com.broll.poc.metrics.DeviceProbe
import com.broll.poc.metrics.MetricsLogger
import com.broll.poc.metrics.MinuteMetricsAccumulator
import com.broll.poc.model.AsrModelStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil

class VideoIndexer(
    private val context: Context,
    private val db: IndexDatabase,
    private val logger: MetricsLogger,
) {
    data class Summary(
        val dialogueSegments: Int,
        val durationSec: Double,
        val embedderMode: String,
        val asrUsedVad: Boolean,
        val timestampLevel: String,
    )

    suspend fun index(
        uri: Uri,
        videoName: String,
        embedder: Embedder,
        batchIndex: Int = 1,
        batchTotal: Int = 1,
    ): Summary = withContext(Dispatchers.Default) {
        indexBlocking(uri, videoName, embedder, batchIndex, batchTotal)
    }

    private suspend fun indexBlocking(
        uri: Uri,
        videoName: String,
        embedder: Embedder,
        batchIndex: Int,
        batchTotal: Int,
    ): Summary {
        val jobContext = coroutineContext
        val mode = embedder.mode
        val duration = readDurationSec(context, uri)
        if (duration <= 0.0) error("读不到视频时长")
        val minutes = ceil(duration / 60.0).toInt().coerceAtLeast(1)
        val minuteMs = LongArray(minutes)
        val minuteProbe = DeviceProbe(context)
        val minuteAcc = Array(minutes) { MinuteMetricsAccumulator(minuteProbe) }

        logger.log(
            event = "index",
            stage = "start",
            videoDurationSec = duration,
            detail = "name=$videoName mode=$mode",
            embedderMode = mode,
        )
        logger.logSnapshot(event = "index", stage = "device_start", embedderMode = mode)

        db.requireSearchModeCompatible(mode)
        db.clearVideo(uri.toString())

        var dialogueCount = 0
        var timestampLevel = "segment"
        val asrStore = AsrModelStore(context)
        val transcriber = SherpaTranscriber(asrStore)
        val pcm = File(context.cacheDir, "broll_pcm16k.raw")
        var completed = false

        fun report(
            stage: IndexStage,
            message: String,
            embedCurrent: Int = 0,
            embedTotal: Int = 0,
        ) {
            val progress = IndexProgress(
                batchIndex = batchIndex,
                batchTotal = batchTotal,
                videoName = videoName,
                stage = stage,
                message = message,
                embedCurrent = embedCurrent,
                embedTotal = embedTotal,
            )
            IndexTaskCallbacks.progress(progress)
        }

        fun touchMinute(startSec: Double, elapsedMs: Long) {
            val idx = (startSec / 60.0).toInt().coerceIn(0, minutes - 1)
            minuteMs[idx] += elapsedMs
            minuteAcc[idx].onWorkAtVideoSec(startSec, elapsedMs)
        }

        try {
            report(IndexStage.DECODE, context.getString(com.broll.poc.R.string.index_stage_decode))
            val decodeStart = System.nanoTime()
            val hasAudio = decodeVideoToPcm16k(context, uri, pcm, { jobContext.isActive })
            val decodeMs = (System.nanoTime() - decodeStart) / 1_000_000
            logger.log(
                event = "index",
                stage = "pcm_decode",
                elapsedMs = decodeMs,
                videoDurationSec = duration,
                detail = "hasAudio=$hasAudio bytes=${if (pcm.isFile) pcm.length() else 0}",
                embedderMode = mode,
            )

            if (hasAudio) {
                report(IndexStage.TRANSCRIBE, context.getString(com.broll.poc.R.string.index_stage_transcribe))
                val asrStart = System.nanoTime()
                val segments = transcriber.transcribePcmFile(pcm, { jobContext.isActive })
                val asrMs = (System.nanoTime() - asrStart) / 1_000_000
                val usedVad = asrStore.locateVad() != null
                if (segments.any { it.wordTimesSec.isNotEmpty() }) {
                    timestampLevel = "word_model_times"
                }
                logger.log(
                    event = "index",
                    stage = "asr_done",
                    elapsedMs = asrMs,
                    videoDurationSec = duration,
                    detail = "segments=${segments.size} vad=$usedVad",
                    embedderMode = mode,
                )

                val totalSeg = segments.size.coerceAtLeast(1)
                for ((idx, seg) in segments.withIndex()) {
                    if (!jobContext.isActive) throw CancellationException()
                    val t0 = System.nanoTime()
                    val prefixed = AppConfig.INDEX_PREFIX + seg.text
                    val vector = embedder.embedText(prefixed)
                    val label = seg.text
                    db.insert(
                        videoUri = uri.toString(),
                        videoName = videoName,
                        modality = "dialogue",
                        startSec = seg.startSec,
                        endSec = seg.endSec,
                        full = vector,
                        label = label,
                        embedderMode = mode,
                        timestampNote = seg.timestampNote,
                        wordTimesSec = seg.wordTimesSec,
                        wordTokens = seg.wordTokens,
                    )
                    dialogueCount++
                    jobContext.ensureActive()
                    report(IndexStage.EMBED,
                        context.getString(com.broll.poc.R.string.index_stage_embed, (idx + 1).toString(), totalSeg.toString()),
                        embedCurrent = idx + 1, embedTotal = totalSeg)
                    val elapsed = (System.nanoTime() - t0) / 1_000_000
                    touchMinute(seg.startSec, elapsed)
                    logger.log(
                        event = "index",
                        stage = "dialogue_embed",
                        elapsedMs = elapsed,
                        videoDurationSec = duration,
                        detail = "i=$idx/${segments.size} ${fmt(seg.startSec)}-${fmt(seg.endSec)} ${seg.text.take(40)}",
                        embedderMode = mode,
                    )
                }
            } else {
                logger.log(
                    event = "index",
                    stage = "asr_skip",
                    videoDurationSec = duration,
                    detail = "无音轨，台词索引跳过",
                    embedderMode = mode,
                )
            }
            jobContext.ensureActive()
            completed = true
        } catch (e: CancellationException) {
            db.clearVideo(uri.toString())
            throw e
        } catch (e: Throwable) {
            db.clearVideo(uri.toString())
            throw e
        } finally {
            if (!completed) {
                db.clearVideo(uri.toString())
            }
            transcriber.close()
            pcm.delete()
        }

        for (m in minuteMs.indices) {
            val acc = minuteAcc[m]
            logger.logMinuteSummary(
                videoDurationSec = duration,
                minuteIndex = m,
                indexMsForMinute = minuteMs[m],
                deltaChargeUah = acc.deltaChargeUah(),
                deltaPct = acc.deltaPct(),
                thermalPeak = acc.thermalPeak(),
                pssPeak = acc.pssPeak(),
                embedderMode = mode,
            )
        }
        logger.logSnapshot(event = "index", stage = "device_end", embedderMode = mode)
        logger.log(
            event = "index",
            stage = "end",
            videoDurationSec = duration,
            detail = "dialogue=$dialogueCount rows=${db.count()}",
            embedderMode = mode,
        )
        return Summary(
            dialogueSegments = dialogueCount,
            durationSec = duration,
            embedderMode = mode,
            asrUsedVad = asrStore.locateVad() != null,
            timestampLevel = timestampLevel,
        )
    }

    private fun fmt(sec: Double): String = String.format(java.util.Locale.US, "%.1f", sec)
}
