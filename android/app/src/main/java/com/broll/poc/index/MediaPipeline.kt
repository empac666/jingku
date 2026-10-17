package com.broll.poc.index

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.broll.poc.AppConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

fun readDurationSec(context: Context, uri: Uri): Double {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        return (ms ?: 0L) / 1000.0
    } finally {
        retriever.release()
    }
}

/** 将视频音轨解码为 mono 16 kHz PCM16 LE 原始文件（供 ASR 使用）。 */
fun decodeVideoToPcm16k(
    context: Context,
    uri: Uri,
    outFile: File,
    shouldContinue: () -> Boolean,
): Boolean = decodeToPcm16k(context, uri, outFile, shouldContinue)

private fun decodeToPcm16k(
    context: Context,
    uri: Uri,
    outFile: File,
    shouldContinue: () -> Boolean,
): Boolean {
    val extractor = MediaExtractor()
    extractor.setDataSource(context, uri, null)
    var track = -1
    var inputRate = AppConfig.AUDIO_SAMPLE_RATE
    var channels = 1
    for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("audio/")) {
            track = i
            inputRate = if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                AppConfig.AUDIO_SAMPLE_RATE
            }
            channels = if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }
            break
        }
    }
    if (track < 0) {
        extractor.release()
        return false
    }
    extractor.selectTrack(track)
    val format = extractor.getTrackFormat(track)
    val mime = format.getString(MediaFormat.KEY_MIME) ?: error("音频轨没有 MIME")
    val codec = MediaCodec.createDecoderByType(mime)
    codec.configure(format, null, null, 0)
    codec.start()
    val info = MediaCodec.BufferInfo()
    var inputEos = false
    var outputEos = false
    var stalls = 0
    var floatPcm = false
    var pcmLeftover = ByteArray(0)
    val resampler = AudioResampler(inputRate, AppConfig.AUDIO_SAMPLE_RATE)
    try {
        outFile.parentFile?.mkdirs()
        outFile.outputStream().use { output ->
            while (!outputEos && shouldContinue()) {
                if (!inputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)
                        if (buf == null) {
                            stalls++
                            if (stalls > 200) break
                            continue
                        }
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            val outRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            resampler.srcRate = outRate
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            floatPcm = outFmt.getInteger(MediaFormat.KEY_PCM_ENCODING) ==
                                android.media.AudioFormat.ENCODING_PCM_FLOAT
                        }
                    }
                    outIndex >= 0 -> {
                        stalls = 0
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null && info.size > 0) {
                            val bytes = ByteArray(info.size)
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            outBuf.get(bytes)
                            val merged = if (pcmLeftover.isEmpty()) bytes else pcmLeftover + bytes
                            val mono = toMonoShorts(merged, channels.coerceAtLeast(1), floatPcm)
                            pcmLeftover = mono.leftover
                            val pcm16k = resampler.push(mono.samples)
                            if (pcm16k.isNotEmpty()) output.write(pcm16k)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEos = true
                        }
                    }
                    else -> {
                        stalls++
                        if (inputEos && stalls > 200) outputEos = true
                    }
                }
            }
            val tailBytes = flushDecodeTail(pcmLeftover, channels, floatPcm, resampler)
            if (tailBytes.isNotEmpty()) output.write(tailBytes)
        }
    } finally {
        try {
            codec.stop()
        } catch (_: Throwable) {
        }
        codec.release()
        extractor.release()
    }
    return outFile.isFile && outFile.length() > 0L
}

fun wrapWavPcm16(pcmLe: ByteArray, sampleRate: Int): ByteArray {
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(36 + pcmLe.size)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1)
    header.putShort(1)
    header.putInt(sampleRate)
    header.putInt(sampleRate * 2)
    header.putShort(2)
    header.putShort(16)
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcmLe.size)
    return header.array() + pcmLe
}
