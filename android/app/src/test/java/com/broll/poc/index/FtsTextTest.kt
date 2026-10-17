package com.broll.poc.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FtsTextTest {
    @Test
    fun indexTokenize_beijingTrip_bigramsAndUnigrams() {
        val tokens = FtsText.indexTokenize("我们去北京").split(" ").toSet()
        assertTrue(tokens.containsAll(listOf("我们", "们去", "去北", "北京", "北", "京")))
    }

    @Test
    fun queryTokens_singleHan_isOneChar() {
        assertEquals(listOf("北"), FtsText.queryTokens("北"))
    }

    @Test
    fun queryTokens_multiHan_bigramsOnlyNoUnigrams() {
        assertEquals(
            listOf("我们", "们去", "去北", "北京"),
            FtsText.queryTokens("我们去北京"),
        )
        val query = FtsText.buildOrMatchQuery("我们去北京")!!
        assertFalse(query.contains("\"北\""))
        assertFalse(query.contains("\"京\""))
    }

    @Test
    fun buildOrMatchQuery_usesOrNotAnd() {
        val q = FtsText.buildOrMatchQuery("我们")!!
        assertTrue(q.contains(" OR ") || q == "\"我们\"")
        assertFalse(q.contains("\"我们\" \"们\""))
    }

    @Test
    fun buildOrMatchQuery_fullPhrase_matchesIndexBigrams() {
        val index = FtsText.indexTokenize("我们去北京")
        val query = FtsText.buildOrMatchQuery("我们去北京")!!
        FtsText.queryTokens("我们去北京").forEach { tok ->
            assertTrue("query should mention token $tok", query.contains("\"$tok\""))
        }
        assertTrue(index.split(" ").size >= 4)
    }
}
