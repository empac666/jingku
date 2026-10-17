package com.broll.poc.index

import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mono PCM16 resampler with Kaiser-windowed sinc low-pass (~7.25 kHz @ 16 kHz output)
 * before fractional linear interpolation. Kaiser 窗为 I0 修正贝塞尔（非正弦比近似）。
 */
class AudioResampler(
    srcRate: Int,
    private val dstRate: Int,
    private val cutoffHz: Double = 7_250.0,
) {
    var srcRate: Int = srcRate
        set(value) {
            if (value == field) return
            field = value
            rebuildFirTaps()
        }

    private var fir: DoubleArray = designLowpassTaps(srcRate, effectiveCutoff(srcRate), NUM_TAPS)
    private val firState = DoubleArray(fir.size)
    private var sampleBuf = ShortArray(dstRate * 2)
    private var size = 0
    private var pos = 0.0

    init {
        rebuildFirTaps()
    }

    private fun effectiveCutoff(rate: Int): Double = cutoffHz.coerceAtMost(rate * 0.48)

    private fun rebuildFirTaps() {
        fir = designLowpassTaps(srcRate, effectiveCutoff(srcRate), NUM_TAPS)
        firState.fill(0.0)
    }

    fun push(mono: ShortArray): ByteArray {
        if (mono.isEmpty()) return ByteArray(0)
        ensure(size + mono.size)
        for (s in mono) {
            sampleBuf[size++] = filterSample(s)
        }
        return drain(keepOne = true)
    }

    fun flush(): ByteArray = drain(keepOne = false)

    private fun filterSample(sample: Short): Short {
        if (srcRate <= dstRate) return sample
        shiftFirState()
        firState[0] = sample.toDouble()
        var acc = 0.0
        for (i in fir.indices) {
            acc += fir[i] * firState[i]
        }
        return acc.toInt().coerceIn(-32768, 32767).toShort()
    }

    private fun shiftFirState() {
        if (fir.size <= 1) return
        for (i in fir.size - 1 downTo 1) {
            firState[i] = firState[i - 1]
        }
    }

    private fun drain(keepOne: Boolean): ByteArray {
        if (srcRate <= 0 || size == 0) return ByteArray(0)
        val step = srcRate.toDouble() / dstRate.toDouble()
        val out = ByteArrayOutputStream()
        val limit = if (keepOne) size - 1 else size
        while (pos < limit) {
            val i = pos.toInt()
            val frac = (pos - i).toFloat()
            val a = sampleBuf[i].toInt()
            val b = if (i + 1 < size) sampleBuf[i + 1].toInt() else a
            val v = (a + (b - a) * frac).toInt().coerceIn(-32768, 32767)
            out.write(v and 0xff)
            out.write((v shr 8) and 0xff)
            pos += step
        }
        val drop = pos.toInt().coerceAtMost(size)
        if (drop > 0) {
            val remain = size - drop
            if (remain > 0) {
                System.arraycopy(sampleBuf, drop, sampleBuf, 0, remain)
            }
            size = remain
            pos -= drop
            if (size == 0) pos = 0.0
        }
        return out.toByteArray()
    }

    private fun ensure(needed: Int) {
        if (sampleBuf.size >= needed) return
        var cap = sampleBuf.size
        while (cap < needed) cap *= 2
        val copy = sampleBuf.copyOf(cap)
        System.arraycopy(sampleBuf, 0, copy, 0, size)
        sampleBuf = copy
    }

    companion object {
        const val NUM_TAPS = 63

        /** Modified Bessel function I0(x), series until terms are negligible. */
        fun besselI0(x: Double): Double {
            if (x == 0.0) return 1.0
            var sum = 1.0
            var term = 1.0
            val half = x / 2.0
            var k = 1.0
            while (true) {
                term *= (half / k) * (half / k)
                sum += term
                if (term < 1e-15 * sum) break
                k += 1.0
            }
            return sum
        }

        fun designLowpassTaps(sampleRate: Int, cutoffHz: Double, numTaps: Int): DoubleArray {
            if (sampleRate <= 0 || numTaps < 3) return doubleArrayOf(1.0)
            val fc = cutoffHz / sampleRate.toDouble()
            val m = numTaps - 1
            val mid = m / 2.0
            val beta = 8.6
            val i0Beta = besselI0(beta)
            val taps = DoubleArray(numTaps)
            var sum = 0.0
            for (i in 0 until numTaps) {
                val alpha = if (mid == 0.0) 0.0 else (i - mid) / mid
                val inside = 1.0 - alpha * alpha
                val kaiser = if (inside <= 0.0) 0.0 else besselI0(beta * sqrt(inside)) / i0Beta
                val x = i - mid
                val sinc = if (x == 0.0) 2.0 * fc else sin(2.0 * PI * fc * x) / (PI * x)
                taps[i] = sinc * kaiser
                sum += taps[i]
            }
            if (sum != 0.0) {
                for (i in taps.indices) taps[i] /= sum
            }
            return taps
        }

        /** Normalized magnitude at [freqHz] for a low-pass designed at [sampleRate] (unit tests). */
        fun frequencyResponseMagnitude(taps: DoubleArray, sampleRate: Int, freqHz: Double): Double {
            var re = 0.0
            var im = 0.0
            val omega = 2.0 * PI * freqHz / sampleRate
            for (n in taps.indices) {
                re += taps[n] * cos(omega * n)
                im -= taps[n] * sin(omega * n)
            }
            return sqrt(re * re + im * im)
        }

        fun frequencyResponseDb(taps: DoubleArray, sampleRate: Int, freqHz: Double): Double {
            val mag = frequencyResponseMagnitude(taps, sampleRate, freqHz).coerceAtLeast(1e-12)
            return 20.0 * kotlin.math.log10(mag)
        }
    }
}
