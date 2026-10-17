package com.broll.poc.data

import android.content.ContentValues
import android.content.Context
import com.broll.poc.AppConfig
import com.broll.poc.embed.bytesToFloats
import com.broll.poc.embed.cosine
import com.broll.poc.embed.floatsToBytes
import com.broll.poc.index.FtsText
import com.broll.poc.index.SeekTimestamp
import org.json.JSONArray
import java.io.File

data class IndexDbOpenFailure(val message: String)

data class Segment(
    val id: Long,
    val videoUri: String,
    val videoName: String,
    val modality: String,
    val startSec: Double,
    val endSec: Double,
    val label: String,
    val score: Float,
    val embedderMode: String = "",
    val timestampNote: String = "",
    val wordTimesSec: DoubleArray = doubleArrayOf(),
    val wordTokens: Array<String> = emptyArray(),
) {
    /** 跳转秒数：优先 SenseVoice token 时间，否则段起点。 */
    fun seekSecForQuery(query: String?): Double =
        SeekTimestamp.seekSecForQuery(label, query, startSec, wordTokens, wordTimesSec).seekSec

    fun seekAlignmentNote(query: String?): String =
        SeekTimestamp.seekSecForQuery(label, query, startSec, wordTokens, wordTimesSec).alignmentNote
}

class IndexDatabase private constructor(
    private val db: BundledDb,
) {

    @Volatile
    private var ftsExternalContent: Boolean = true

    @Volatile
    private var ftsUsesBm25: Boolean = true

    companion object {
        const val DB_NAME = "broll_dialogue.db"

        fun tryOpen(context: Context): Pair<IndexDatabase?, IndexDbOpenFailure?> {
            val app = context.applicationContext
            val bundled = try {
                BundledDb.open(app, DB_NAME)
            } catch (e: Throwable) {
                return Pair(null, IndexDbOpenFailure("打开索引库失败：${e.message ?: e.javaClass.simpleName}"))
            }
            try {
                val version = bundled.userVersion()
                if (version > 0 && version != AppConfig.INDEX_META_GENERATION) {
                    bundled.close()
                    return Pair(null, IndexDbOpenFailure(
                        "索引库 schema 版本为 $version（需要 ${AppConfig.INDEX_META_GENERATION}）。请清空并重建索引。",
                    ))
                }
                if (version == AppConfig.INDEX_META_GENERATION) {
                    val generation = readMetaOn(bundled, "generation")
                    val prefixId = readMetaOn(bundled, "embed_prefix_id")
                    if (generation != AppConfig.INDEX_META_GENERATION.toString() ||
                        prefixId != AppConfig.INDEX_EMBED_PREFIX_ID
                    ) {
                        bundled.close()
                        return Pair(
                            null,
                            IndexDbOpenFailure(
                                "索引元数据不匹配（generation=$generation prefix=$prefixId）。请清空并重建索引。",
                            ),
                        )
                    }
                }
                val db = IndexDatabase(bundled)
                if (version == 0) {
                    db.onCreate()
                    bundled.setUserVersion(AppConfig.INDEX_META_GENERATION)
                } else {
                    db.onOpen()
                    db.ensureSchemaColumns()
                }
                return Pair(db, null)
            } catch (e: Throwable) {
                bundled.close()
                return Pair(null, IndexDbOpenFailure("打开索引库失败：${e.message ?: e.javaClass.simpleName}"))
            }
        }

        fun wipeAndOpen(context: Context): Pair<IndexDatabase?, IndexDbOpenFailure?> {
            val app = context.applicationContext
            try {
                deleteDbFiles(app)
                val bundled = BundledDb.open(app, DB_NAME)
                val db = IndexDatabase(bundled)
                db.onCreate()
                bundled.setUserVersion(AppConfig.INDEX_META_GENERATION)
                return Pair(db, null)
            } catch (e: Throwable) {
                return Pair(null, IndexDbOpenFailure("清空并重建索引库失败：${e.message ?: e.javaClass.simpleName}"))
            }
        }

        private fun deleteDbFiles(context: Context) {
            val base = context.getDatabasePath(DB_NAME)
            val dir = base.parentFile
            val prefix = DB_NAME
            listOf(
                base,
                File(base.absolutePath + "-journal"),
                File(base.absolutePath + "-wal"),
                File(base.absolutePath + "-shm"),
            ).forEach { it.delete() }
            dir?.listFiles()?.orEmpty()
                ?.filter { it.name.startsWith(prefix) }
                ?.forEach { it.delete() }
        }

        private fun readMetaOn(bundled: BundledDb, key: String): String? {
            bundled.query("SELECT value FROM index_meta WHERE key = ?", arrayOf(key)).use { c ->
                return if (c.moveToNext()) c.getString(0) else null
            }
        }
    }

    fun ftsModule(): String = if (ftsExternalContent) "fts5" else "fts4"

    private fun onOpen() {
        val sql = masterSql("segments_fts")
        ftsExternalContent = sql.contains("fts5", ignoreCase = true)
        ftsUsesBm25 = ftsExternalContent
    }

    private fun onCreate() {
        db.execSQL(
            """
            CREATE TABLE segments (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              video_uri TEXT NOT NULL,
              video_name TEXT NOT NULL,
              modality TEXT NOT NULL,
              start_sec REAL NOT NULL,
              end_sec REAL NOT NULL,
              dim INTEGER NOT NULL,
              vector BLOB NOT NULL,
              vector256 BLOB NOT NULL DEFAULT X'',
              label TEXT NOT NULL,
              fts_content TEXT NOT NULL DEFAULT '',
              timestamp_note TEXT NOT NULL DEFAULT '',
              word_times_json TEXT NOT NULL DEFAULT '[]',
              word_tokens_json TEXT NOT NULL DEFAULT '[]',
              embedder_mode TEXT NOT NULL DEFAULT 'unknown'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE index_meta (
              key TEXT PRIMARY KEY NOT NULL,
              value TEXT NOT NULL
            )
            """.trimIndent(),
        )
        writeMeta()
        createFts()
    }

    private fun ensureSchemaColumns() {
        if (!columnExists("segments", "word_tokens_json")) {
            db.execSQL("ALTER TABLE segments ADD COLUMN word_tokens_json TEXT NOT NULL DEFAULT '[]'")
        }
    }

    private fun columnExists(table: String, column: String): Boolean {
        db.query("PRAGMA table_info($table)", null).use { c ->
            while (c.moveToNext()) {
                if (column.equals(c.getString(1), ignoreCase = true)) return true
            }
        }
        return false
    }

    private fun writeMeta() {
        db.execSQL("DELETE FROM index_meta")
        db.execSQL(
            "INSERT INTO index_meta(key, value) VALUES (?, ?)",
            arrayOf("generation", AppConfig.INDEX_META_GENERATION.toString()),
        )
        db.execSQL(
            "INSERT INTO index_meta(key, value) VALUES (?, ?)",
            arrayOf("embed_prefix_id", AppConfig.INDEX_EMBED_PREFIX_ID),
        )
    }

    private fun readMeta(key: String): String? {
        db.query("SELECT value FROM index_meta WHERE key = ?", arrayOf(key)).use { c ->
            return if (c.moveToNext()) c.getString(0) else null
        }
    }

    private fun createFts() {
        db.execSQL("CREATE INDEX idx_segments_video ON segments(video_uri)")
        db.execSQL("CREATE INDEX idx_segments_modality ON segments(modality)")
        if (probeFts5()) {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE segments_fts USING fts5(
                  fts_content,
                  content='segments',
                  content_rowid='id',
                  tokenize='unicode61 remove_diacritics 0'
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TRIGGER segments_ai AFTER INSERT ON segments BEGIN
                  INSERT INTO segments_fts(rowid, fts_content) VALUES (new.id, new.fts_content);
                END
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TRIGGER segments_ad AFTER DELETE ON segments BEGIN
                  INSERT INTO segments_fts(segments_fts, rowid, fts_content) VALUES ('delete', old.id, old.fts_content);
                END
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TRIGGER segments_au AFTER UPDATE ON segments BEGIN
                  INSERT INTO segments_fts(segments_fts, rowid, fts_content) VALUES ('delete', old.id, old.fts_content);
                  INSERT INTO segments_fts(rowid, fts_content) VALUES (new.id, new.fts_content);
                END
                """.trimIndent(),
            )
            ftsExternalContent = true
            ftsUsesBm25 = true
        } else {
            db.execSQL(
                "CREATE VIRTUAL TABLE segments_fts USING fts4(fts_content, tokenize=unicode61)",
            )
            ftsExternalContent = false
            ftsUsesBm25 = false
        }
    }

    fun clearVideo(videoUri: String) {
        db.beginTransaction()
        try {
            if (!ftsExternalContent) {
                val ids = ArrayList<Long>()
                db.query("SELECT id FROM segments WHERE video_uri = ?", arrayOf(videoUri)).use { c ->
                    while (c.moveToNext()) ids.add(c.getLong(0))
                }
                for (id in ids) {
                    db.execSQL("DELETE FROM segments_fts WHERE docid = ?", arrayOf(id))
                }
            }
            db.delete("segments", "video_uri = ?", arrayOf(videoUri))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insert(
        videoUri: String,
        videoName: String,
        modality: String,
        startSec: Double,
        endSec: Double,
        full: FloatArray,
        label: String,
        embedderMode: String,
        timestampNote: String = "",
        wordTimesSec: DoubleArray = doubleArrayOf(),
        wordTokens: Array<String> = emptyArray(),
    ): Long {
        val ftsContent = FtsText.indexTokenize(label)
        val wordJson = JSONArray(wordTimesSec.toList()).toString()
        val tokensJson = JSONArray(wordTokens.toList()).toString()
        val values = ContentValues().apply {
            put("video_uri", videoUri)
            put("video_name", videoName)
            put("modality", modality)
            put("start_sec", startSec)
            put("end_sec", endSec)
            put("dim", full.size)
            put("vector", floatsToBytes(full))
            // Retain an empty legacy column for generation-6 compatibility.
            put("vector256", byteArrayOf())
            put("label", label)
            put("fts_content", ftsContent)
            put("timestamp_note", timestampNote)
            put("word_times_json", wordJson)
            put("word_tokens_json", tokensJson)
            put("embedder_mode", embedderMode)
        }
        val id = db.insert("segments", null, values)
        if (id > 0 && !ftsExternalContent) {
            db.execSQL(
                "INSERT INTO segments_fts(docid, fts_content) VALUES (?, ?)",
                arrayOf<Any?>(id, ftsContent),
            )
        }
        return id
    }

    fun distinctEmbedderModes(): List<String> {
        val modes = ArrayList<String>()
        db.query(
            "SELECT DISTINCT embedder_mode FROM segments WHERE embedder_mode IS NOT NULL AND embedder_mode != ''",
            null,
        ).use { c ->
            while (c.moveToNext()) modes.add(c.getString(0) ?: "")
        }
        return modes
    }

    fun requireSearchModeCompatible(currentMode: String) {
        val modes = distinctEmbedderModes()
        if (modes.isEmpty()) return
        if (modes.size > 1) {
            error("索引库混合多种 embedder_mode：${modes.joinToString()}。请清空后重建索引。")
        }
        val indexed = modes.single()
        if (indexed != currentMode) {
            error("索引为 $indexed，当前引擎为 $currentMode。请清空后重建索引。")
        }
    }

    private fun probeFts5(): Boolean {
        return try {
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS probe_fts USING fts5(label)")
            db.execSQL("DROP TABLE IF EXISTS probe_fts")
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun masterSql(name: String): String {
        db.query("SELECT sql FROM sqlite_master WHERE name = ?", arrayOf(name)).use { c ->
            return if (c.moveToNext()) c.getString(0) ?: "" else ""
        }
    }

    fun count(): Int {
        db.query("SELECT COUNT(*) FROM segments", null).use { c ->
            return if (c.moveToNext()) c.getInt(0) else 0
        }
    }

    fun countDialogue(videoUri: String? = null): Int {
        val sql = if (videoUri == null) {
            "SELECT COUNT(*) FROM segments WHERE modality = 'dialogue' ORDER BY id ASC"
        } else {
            "SELECT COUNT(*) FROM segments WHERE modality = 'dialogue' AND video_uri = ? ORDER BY id ASC"
        }
        val args = if (videoUri == null) null else arrayOf<String?>(videoUri)
        db.query(sql, args).use { c ->
            return if (c.moveToNext()) c.getInt(0) else 0
        }
    }

    /** 台词语义检索：完整 768 维归一化余弦，不去重。 */
    fun searchSemantic768(query768: FloatArray, limit: Int = AppConfig.SEARCH_TOP_K): List<Segment> {
        require(query768.size == AppConfig.FULL_DIM && query768.all { it.isFinite() })
        val hits = ArrayList<Segment>()
        db.query(
            """
            SELECT id, video_uri, video_name, modality, start_sec, end_sec, label, vector, embedder_mode, timestamp_note, word_times_json, word_tokens_json
            FROM segments WHERE modality = 'dialogue' ORDER BY id ASC
            """.trimIndent(),
            null,
        ).use { c ->
            while (c.moveToNext()) {
                val vec = bytesToFloats(c.getBlob(7))
                require(vec.size == AppConfig.FULL_DIM && vec.all { it.isFinite() }) { "索引向量不兼容，请清空重建" }
                val score = cosine(query768, vec)
                hits.add(rowToSegment(c, score, 7))
            }
        }
        hits.sortByDescending { it.score }
        return hits.take(limit)
    }

    private fun rowToSegment(c: BundledDb.SqliteRows, score: Float, vectorCol: Int): Segment {
        return Segment(
            id = c.getLong(0),
            videoUri = c.getString(1) ?: "",
            videoName = c.getString(2) ?: "",
            modality = c.getString(3) ?: "",
            startSec = c.getDouble(4),
            endSec = c.getDouble(5),
            label = c.getString(6) ?: "",
            score = score,
            embedderMode = c.getString(8) ?: "",
            timestampNote = c.getString(9) ?: "",
            wordTimesSec = parseWordTimes(c.getString(10)),
            wordTokens = parseWordTokens(c.getString(11)),
        )
    }

    fun close() {
        db.close()
    }

    private fun parseWordTimes(json: String?): DoubleArray {
        if (json.isNullOrBlank() || json == "[]") return doubleArrayOf()
        return try {
            val arr = JSONArray(json)
            DoubleArray(arr.length()) { i -> arr.getDouble(i) }
        } catch (_: Throwable) {
            doubleArrayOf()
        }
    }

    private fun parseWordTokens(json: String?): Array<String> {
        if (json.isNullOrBlank() || json == "[]") return emptyArray()
        return try {
            val arr = JSONArray(json)
            Array(arr.length()) { i -> arr.getString(i) ?: "" }
        } catch (_: Throwable) {
            emptyArray()
        }
    }
}
