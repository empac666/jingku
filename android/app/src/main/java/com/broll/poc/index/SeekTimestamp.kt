package com.broll.poc.index

/**
 * Map a user query to SenseVoice token timestamps (absolute seconds already stored).
 */
object SeekTimestamp {
    data class Result(
        val seekSec: Double,
        /** Empty when token-aligned; otherwise explains fallback. */
        val alignmentNote: String,
    )

    const val NOTE_SEGMENT_UNALIGNED = "段起点（未对齐）"

    fun seekSecForQuery(
        label: String,
        query: String?,
        segmentStartSec: Double,
        tokens: Array<String>,
        wordTimesSec: DoubleArray,
    ): Result {
        if (query.isNullOrBlank()) {
            return Result(segmentStartSec, "")
        }
        val q = query.trim()
        if (q.isEmpty()) return Result(segmentStartSec, "")

        val charIdx = label.indexOf(q, ignoreCase = true)
        if (charIdx < 0) {
            return Result(segmentStartSec, "查询未出现在台词文本，使用段起点")
        }

        if (wordTimesSec.isEmpty()) {
            return Result(segmentStartSec, NOTE_SEGMENT_UNALIGNED)
        }

        if (tokens.isNotEmpty() && tokens.size == wordTimesSec.size) {
            val tokenIdx = tokenIndexForCharOffset(label, charIdx, tokens)
            if (tokenIdx != null) {
                return Result(wordTimesSec[tokenIdx], "")
            }
            return Result(segmentStartSec, NOTE_SEGMENT_UNALIGNED)
        }

        return Result(segmentStartSec, NOTE_SEGMENT_UNALIGNED)
    }

    /**
     * Walk [tokens] left-to-right in [label]; return index of token covering [charOffset].
     */
    fun tokenIndexForCharOffset(label: String, charOffset: Int, tokens: Array<String>): Int? {
        var searchFrom = 0
        for (i in tokens.indices) {
            val tok = tokens[i]
            if (tok.isEmpty()) continue
            val found = label.indexOf(tok, searchFrom, ignoreCase = true)
            if (found < 0) continue
            val end = found + tok.length
            if (charOffset >= found && charOffset < end) return i
            searchFrom = end
        }
        return null
    }
}
