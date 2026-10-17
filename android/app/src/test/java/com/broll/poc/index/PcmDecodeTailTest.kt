package com.broll.poc.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmDecodeTailTest {
    @Test
    fun flushDecodeTail_withOneMonoSampleLeftover_flushesResampler() {
        val resampler = AudioResampler(48_000, 16_000)
        resampler.push(ShortArray(480) { 500 })
        // mono int16 LE: one sample 0x0010 = 16
        val leftover = byteArrayOf(0x10, 0x00)
        val out = flushDecodeTail(leftover, channels = 1, floatPcm = false, resampler)
        assertTrue(out.isNotEmpty())
        assertEquals(0, out.size % 2)
    }
}
