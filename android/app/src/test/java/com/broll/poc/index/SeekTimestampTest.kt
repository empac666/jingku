package com.broll.poc.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekTimestampTest {
    @Test
    fun tokenAlign_beijingQuery_usesBeiTokenTime() {
        val label = "我们去北京"
        val tokens = arrayOf("我们", "去", "北京")
        val times = doubleArrayOf(1.0, 1.5, 2.0)
        val r = SeekTimestamp.seekSecForQuery(label, "北京", 0.0, tokens, times)
        assertEquals(2.0, r.seekSec, 1e-6)
        assertTrue(r.alignmentNote.isEmpty())
    }

    @Test
    fun tokenAlign_singleChar_usesMatchingToken() {
        val label = "我们去北京"
        val tokens = arrayOf("我", "们", "去", "北", "京")
        val times = doubleArrayOf(1.0, 1.1, 1.2, 1.3, 1.4)
        val r = SeekTimestamp.seekSecForQuery(label, "北", 0.0, tokens, times)
        assertEquals(1.3, r.seekSec, 1e-6)
    }

    @Test
    fun fallback_whenTokensMismatchLabel_usesSegmentStart() {
        val label = "我们去北京"
        val tokens = arrayOf("foo", "bar")
        val times = doubleArrayOf(5.0, 6.0)
        val r = SeekTimestamp.seekSecForQuery(label, "北京", 3.0, tokens, times)
        assertEquals(3.0, r.seekSec, 1e-6)
        assertEquals(SeekTimestamp.NOTE_SEGMENT_UNALIGNED, r.alignmentNote)
    }

    @Test
    fun fallback_whenNoTimestamps_usesSegmentStart() {
        val label = "我们去北京"
        val tokens = arrayOf("我们", "去", "北京")
        val r = SeekTimestamp.seekSecForQuery(label, "北京", 4.5, tokens, doubleArrayOf())
        assertEquals(4.5, r.seekSec, 1e-6)
        assertEquals(SeekTimestamp.NOTE_SEGMENT_UNALIGNED, r.alignmentNote)
    }

    @Test
    fun tokenAlign_skipsMetaTokensInStream() {
        val label = "今天天气不错"
        val tokens = arrayOf("<|zh|>", "<|NEUTRAL|>", "今", "天", "天", "气", "不", "错")
        val times = doubleArrayOf(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7)
        val r = SeekTimestamp.seekSecForQuery(label, "天气", 0.0, tokens, times)
        assertEquals(0.4, r.seekSec, 1e-6)
        assertTrue(r.alignmentNote.isEmpty())
    }

    @Test
    fun fallback_itnNumericTextMismatch_usesSegmentStart() {
        val label = "一共123元"
        val tokens = arrayOf("一共", "一百二十三", "元")
        val times = doubleArrayOf(1.0, 2.0, 3.0)
        val r = SeekTimestamp.seekSecForQuery(label, "123", 5.0, tokens, times)
        assertEquals(5.0, r.seekSec, 1e-6)
        assertEquals(SeekTimestamp.NOTE_SEGMENT_UNALIGNED, r.alignmentNote)
    }
}
