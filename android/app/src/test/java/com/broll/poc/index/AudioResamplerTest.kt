package com.broll.poc.index

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioResamplerTest {
    @Test
    fun lowpass_frequencyResponse_48k_meetsPassbandAndStopband() {
        assertFrequencyResponse(48_000)
    }

    @Test
    fun lowpass_frequencyResponse_44_1k_meetsPassbandAndStopband() {
        assertFrequencyResponse(44_100)
    }

    private fun assertFrequencyResponse(sampleRate: Int) {
        val taps = AudioResampler.designLowpassTaps(sampleRate, 7_250.0, AudioResampler.NUM_TAPS)
        val db1k = AudioResampler.frequencyResponseDb(taps, sampleRate, 1_000.0)
        val db8k = AudioResampler.frequencyResponseDb(taps, sampleRate, 8_000.0)
        val db12k = AudioResampler.frequencyResponseDb(taps, sampleRate, 12_000.0)
        assertTrue("1 kHz gain ${db1k} dB @ ${sampleRate} Hz", db1k >= -1.0)
        assertTrue("8 kHz gain ${db8k} dB @ ${sampleRate} Hz", db8k <= -15.0)
        assertTrue("12 kHz gain ${db12k} dB @ ${sampleRate} Hz", db12k <= -40.0)
    }

    @Test
    fun downsample_constantInput_producesPcmBytes() {
        val resampler = AudioResampler(48_000, 16_000)
        val block = ShortArray(4800) { 10_000 }
        var totalBytes = 0
        repeat(5) { totalBytes += resampler.push(block).size }
        totalBytes += resampler.flush().size
        assertTrue(totalBytes > 1000)
    }

    @Test
    fun downsample_stepSettles_nearInputLevel() {
        val resampler = AudioResampler(48_000, 16_000)
        val block = ShortArray(4800) { 10_000 }
        repeat(10) { resampler.push(block) }
        val pcm = resampler.flush() + resampler.push(block)
        assertTrue(pcm.size >= 2)
        val last = (pcm[pcm.size - 2].toInt() and 0xff) or (pcm[pcm.size - 1].toInt() shl 8)
        assertTrue(abs(last - 10_000) < 5000)
    }

    @Test
    fun srcRateChange_rebuildsFirTaps() {
        val resampler = AudioResampler(48_000, 16_000)
        val taps48 = AudioResampler.designLowpassTaps(48_000, 7_250.0, AudioResampler.NUM_TAPS)
        val db8kBefore = AudioResampler.frequencyResponseDb(taps48, 48_000, 8_000.0)
        resampler.srcRate = 44_100
        val taps44 = AudioResampler.designLowpassTaps(44_100, 7_250.0, AudioResampler.NUM_TAPS)
        val db8kAfter = AudioResampler.frequencyResponseDb(taps44, 44_100, 8_000.0)
        assertTrue(db8kBefore <= -15.0)
        assertTrue(db8kAfter <= -15.0)
    }
}
