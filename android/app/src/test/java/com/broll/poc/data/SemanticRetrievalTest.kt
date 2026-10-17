package com.broll.poc.data

import com.broll.poc.AppConfig
import com.broll.poc.embed.floatsToBytes
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

/** End-to-end SQL/blob/ranking contract without model inference or Android UI stubs. */
class SemanticRetrievalTest {
    @Test fun fullDimensionsControlRanking_andDuplicateWindowsRemainInTopThree() {
        val path = Files.createTempFile("jingku-semantic", ".db")
        val raw = BundledDb.open(path.toString())
        try {
            raw.execSQL("""CREATE TABLE segments(id INTEGER PRIMARY KEY, video_uri TEXT, video_name TEXT,
                modality TEXT, start_sec REAL, end_sec REAL, label TEXT, vector BLOB,
                embedder_mode TEXT, timestamp_note TEXT, word_times_json TEXT, word_tokens_json TEXT)""")
            fun add(id: Int, v: FloatArray, modality: String = "dialogue") {
                raw.execSQL("INSERT INTO segments VALUES(?, 'video://same', 'same.mp4', ?, 1, 4, 'no literal query overlap', ?, 'litertlm', '', '[]', '[]')",
                    arrayOf(id, modality, floatsToBytes(v)))
            }
            val q = FloatArray(768).also { it[767] = 1f }
            add(1, FloatArray(768).also { it[0] = 1f })
            add(2, q)
            add(3, q) // same URI/time; do not deduplicate
            add(4, FloatArray(768).also { it[767] = 0.8f; it[0] = 0.6f })
            add(5, q, "shot") // legacy shot must not enter dialogue search
            val ctor = IndexDatabase::class.java.getDeclaredConstructor(BundledDb::class.java)
            ctor.isAccessible = true
            val db = ctor.newInstance(raw)
            assertEquals(4, db.countDialogue())
            assertEquals(4, db.countDialogue("video://same"))
            assertEquals(0, db.countDialogue("video://missing"))
            assertEquals(0, db.countDialogue("video://same' OR 1=1 --"))
            val hits = db.searchSemantic768(q)
            assertEquals(listOf(2L, 3L, 4L), hits.map { it.id })
            assertTrue(runCatching { db.searchSemantic768(FloatArray(256)) }.isFailure)
            assertEquals(1f, hits[0].score, 1e-6f)
            assertEquals(0.8f, hits[2].score, 1e-6f)
            assertEquals(1.0, hits[0].seekSecForQuery("semantic paraphrase"), 0.0)
            assertTrue(0.78 < AppConfig.SEMANTIC_LOW_CONFIDENCE)
        } finally {
            raw.close()
            Files.deleteIfExists(path)
        }
    }
}
