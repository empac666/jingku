package com.broll.poc.index

import com.broll.poc.data.BundledDb
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * 「我们去北京」：OR + bigram 索引应能召回「我们」「去北」等子查询（旧 AND+末字一元组会漏）。
 */
class FtsOrRecallTest {
    @Test
    fun orMatch_findsPartialBigrams() {
        val path = Files.createTempFile("broll_fts", ".db").toAbsolutePath().toString()
        val db = BundledDb.open(path)
        try {
            db.execSQL(
                """
                CREATE TABLE segments (
                  id INTEGER PRIMARY KEY,
                  fts_content TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE VIRTUAL TABLE segments_fts USING fts5(fts_content)")
            val indexed = FtsText.indexTokenize("我们去北京")
            db.execSQL("INSERT INTO segments(id, fts_content) VALUES (1, ?)", arrayOf(indexed))
            db.execSQL("INSERT INTO segments_fts(rowid, fts_content) VALUES (1, ?)", arrayOf(indexed))

            fun hits(query: String): Int {
                val match = FtsText.buildOrMatchQuery(query) ?: return 0
                db.query(
                    "SELECT rowid FROM segments_fts WHERE segments_fts MATCH ?",
                    arrayOf(match),
                ).use { c ->
                    var n = 0
                    while (c.moveToNext()) n++
                    return n
                }
            }

            assertTrue(hits("我们") >= 1)
            assertTrue(hits("去北") >= 1)
            assertTrue(hits("北京") >= 1)
            assertTrue(hits("我们去北京") >= 1)
            assertTrue(hits("北") >= 1)
        } finally {
            db.close()
            Files.deleteIfExists(java.nio.file.Paths.get(path))
        }
    }
}
