package com.broll.poc.index

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MonoChunk(val samples: ShortArray, val leftover: ByteArray)

/** Decode interleaved PCM bytes to mono int16; [leftover] holds trailing bytes that do not form a full frame. */
fun toMonoShorts(pcm: ByteArray, channels: Int, floatPcm: Boolean): MonoChunk {
    if (floatPcm) {
        val frameBytes = 4 * channels
        val frames = pcm.size / frameBytes
        val leftover = pcm.copyOfRange(frames * frameBytes, pcm.size)
        val out = ShortArray(frames)
        val buf = ByteBuffer.wrap(pcm, 0, frames * frameBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (f in 0 until frames) {
            var sum = 0.0
            for (c in 0 until channels) {
                sum += buf.float.toDouble()
            }
            val mono = (sum / channels).coerceIn(-1.0, 1.0)
            out[f] = (mono * 32767.0).toInt().toShort()
        }
        return MonoChunk(out, leftover)
    }
    val frameBytes = 2 * channels
    val frames = pcm.size / frameBytes
    val leftover = pcm.copyOfRange(frames * frameBytes, pcm.size)
    val out = ShortArray(frames)
    var i = 0
    for (f in 0 until frames) {
        var sum = 0
        for (c in 0 until channels) {
            val lo = pcm[i].toInt() and 0xff
            val hi = pcm[i + 1].toInt()
            sum += ((hi shl 8) or lo).toShort().toInt()
            i += 2
        }
        out[f] = (sum / channels).toShort()
    }
    return MonoChunk(out, leftover)
}

/** After decode EOS: push trailing bytes through mono + resampler (unit-tested). */
fun flushDecodeTail(
    pcmLeftover: ByteArray,
    channels: Int,
    floatPcm: Boolean,
    resampler: AudioResampler,
): ByteArray {
    if (pcmLeftover.isEmpty()) return resampler.flush()
    val mono = toMonoShorts(pcmLeftover, channels.coerceAtLeast(1), floatPcm)
    val out = ByteArrayOutputStreamCompat()
    val pcm16k = resampler.push(mono.samples)
    if (pcm16k.isNotEmpty()) out.write(pcm16k)
    val tail = resampler.flush()
    if (tail.isNotEmpty()) out.write(tail)
    return out.toByteArray()
}

private class ByteArrayOutputStreamCompat {
    private val buf = java.io.ByteArrayOutputStream()
    fun write(bytes: ByteArray) = buf.write(bytes)
    fun toByteArray(): ByteArray = buf.toByteArray()
}
