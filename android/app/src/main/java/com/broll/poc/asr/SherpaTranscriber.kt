package com.broll.poc.asr

import com.broll.poc.AppConfig
import com.broll.poc.model.AsrModelStore
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeechSegment
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 段级起点来自 VAD/固定窗；SenseVoice 词级 timestamps 加段起点得到绝对秒并落库。
 */
data class DialogueSegment(
    val startSec: Double,
    val endSec: Double,
    val text: String,
    val wordTimesSec: DoubleArray,
    val wordTokens: Array<String>,
    val timestampNote: String,
)

class SherpaTranscriber(
    private val store: AsrModelStore,
) {
    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null

    fun close() {
        vad?.release()
        vad = null
        recognizer?.release()
        recognizer = null
    }

    fun ensureLoaded() {
        if (!store.isSenseVoiceReady()) {
            error("Bundled SenseVoice assets unavailable")
        }
        if (recognizer == null) {
            val modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = store.senseVoiceOnnx,
                    language = "auto",
                    useInverseTextNormalization = true,
                ),
                tokens = store.senseVoiceTokens,
                numThreads = 2,
                debug = false,
                provider = "cpu",
            )
            val config = OfflineRecognizerConfig(
                featConfig = getFeatureConfig(AppConfig.AUDIO_SAMPLE_RATE, 80),
                modelConfig = modelConfig,
                decodingMethod = "greedy_search",
            )
            recognizer = OfflineRecognizer(assetManager = store.assets, config = config)
        }
        if (vad == null) {
            val vadPath = store.locateVad()
            if (vadPath != null) {
                vad = Vad(
                    assetManager = store.assets,
                    config = VadModelConfig(
                        sileroVadModelConfig = SileroVadModelConfig(
                            model = vadPath,
                            threshold = 0.5f,
                            minSilenceDuration = 0.25f,
                            minSpeechDuration = 0.5f,
                            windowSize = 512,
                            maxSpeechDuration = AppConfig.DIALOGUE_MAX_SEGMENT_SEC.toFloat(),
                        ),
                        sampleRate = AppConfig.AUDIO_SAMPLE_RATE,
                        numThreads = 1,
                        provider = "cpu",
                        debug = false,
                    ),
                )
            }
        }
    }

    fun transcribePcmFile(pcm16kLe: File, shouldContinue: () -> Boolean): List<DialogueSegment> {
        ensureLoaded()
        val rec = recognizer ?: error("recognizer missing")
        val vadEngine = vad
        return if (vadEngine != null) {
            transcribeWithVadStreaming(rec, vadEngine, pcm16kLe, shouldContinue)
        } else {
            transcribeFixedWindowsStreaming(rec, pcm16kLe, shouldContinue)
        }
    }

    private fun transcribeWithVadStreaming(
        rec: OfflineRecognizer,
        vadEngine: Vad,
        pcmFile: File,
        shouldContinue: () -> Boolean,
    ): List<DialogueSegment> {
        val out = ArrayList<DialogueSegment>()
        val windowSamples = 512
        val windowBytes = windowSamples * 2
        RandomAccessFile(pcmFile, "r").use { raf ->
            val buf = ByteArray(windowBytes)
            while (shouldContinue()) {
                val n = raf.read(buf)
                if (n <= 0) break
                val samples = shortsToFloats(buf, n)
                if (samples.isNotEmpty()) {
                    vadEngine.acceptWaveform(samples)
                    drainVad(rec, vadEngine, out)
                }
            }
        }
        vadEngine.flush()
        drainVad(rec, vadEngine, out)
        return out
    }

    private fun transcribeFixedWindowsStreaming(
        rec: OfflineRecognizer,
        pcmFile: File,
        shouldContinue: () -> Boolean,
    ): List<DialogueSegment> {
        val out = ArrayList<DialogueSegment>()
        val maxSamples = (AppConfig.DIALOGUE_MAX_SEGMENT_SEC * AppConfig.AUDIO_SAMPLE_RATE).toInt()
        val maxBytes = maxSamples * 2
        val chunk = ByteArray(maxBytes)
        var globalSampleOffset = 0
        RandomAccessFile(pcmFile, "r").use { raf ->
            while (shouldContinue()) {
                val n = raf.read(chunk)
                if (n <= 0) break
                val sampleCount = n / 2
                val floats = shortsToFloats(chunk, n)
                val startSec = globalSampleOffset.toDouble() / AppConfig.AUDIO_SAMPLE_RATE
                val endSec = (globalSampleOffset + sampleCount).toDouble() / AppConfig.AUDIO_SAMPLE_RATE
                val dialogue = recognizeSegment(rec, floats, startSec, endSec)
                if (dialogue.text.isNotBlank()) out.add(dialogue)
                globalSampleOffset += sampleCount
            }
        }
        return out
    }

    private fun drainVad(rec: OfflineRecognizer, vadEngine: Vad, out: MutableList<DialogueSegment>) {
        while (!vadEngine.empty()) {
            val segment: SpeechSegment = vadEngine.front()
            val startSec = segment.start.toDouble() / AppConfig.AUDIO_SAMPLE_RATE
            val durationSec = segment.samples.size.toDouble() / AppConfig.AUDIO_SAMPLE_RATE
            val endSec = startSec + durationSec
            val dialogue = recognizeSegment(rec, segment.samples, startSec, endSec)
            if (dialogue.text.isNotBlank()) out.add(dialogue)
            vadEngine.pop()
        }
    }

    private fun recognizeSegment(
        rec: OfflineRecognizer,
        samples: FloatArray,
        startSec: Double,
        endSec: Double,
    ): DialogueSegment {
        val stream: OfflineStream = rec.createStream()
        try {
            stream.acceptWaveform(samples, AppConfig.AUDIO_SAMPLE_RATE)
            rec.decode(stream)
            val result = rec.getResult(stream)
            val tokens = result.tokens
            val ts = result.timestamps
            val n = minOf(tokens.size, ts.size)
            val wordTokens = Array(n) { tokens[it] }
            val wordTimes = DoubleArray(n) { i -> startSec + ts[i].toDouble() }
            val note = if (n > 0) {
                "词级绝对时间（OfflineRecognizerResult.tokens + timestamps + 段起点）"
            } else {
                "仅段级时间戳（VAD/固定窗）；SenseVoice 未返回 tokens/timestamps"
            }
            return DialogueSegment(
                startSec = startSec,
                endSec = endSec,
                text = result.text.trim(),
                wordTimesSec = wordTimes,
                wordTokens = wordTokens,
                timestampNote = note,
            )
        } finally {
            stream.release()
        }
    }

    private fun shortsToFloats(pcm: ByteArray, byteCount: Int): FloatArray {
        val frames = byteCount / 2
        if (frames <= 0) return FloatArray(0)
        val bb = ByteBuffer.wrap(pcm, 0, frames * 2).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(frames)
        for (i in 0 until frames) {
            out[i] = bb.short.toFloat() / 32768f
        }
        return out
    }
}
