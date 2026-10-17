package com.broll.poc.index

/**
 * 为 SQLite FTS unicode61 准备中文 bigram 索引串（CJK 连续区按字切分并生成相邻二元组）。
 * 多字 CJK 串：重叠 bigram + 每个汉字一元组（支持单字查询「北」）；不单独追加「仅末字」一元组（避免旧 AND 漏召回）。
 * 查询侧：多字串只 OR bigram，不 OR 全部单字；仅单字查询使用一元组。
 */
object FtsText {
    private val cjk = Regex("\\p{Script=Han}")

    /** 入库用：bigram + 汉字一元组。 */
    fun indexTokenize(text: String): String = indexTokenizeRuns(text).joinToString(" ")

    /** 查询用：拆成 token 列表，供 OR 召回。 */
    fun queryTokens(text: String): List<String> = queryTokenizeRuns(text)

    /** FTS MATCH 子句：token 之间 OR，短语加引号。 */
    fun buildOrMatchQuery(text: String): String? {
        val tokens = queryTokens(text)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" OR ") { quoteToken(it) }
    }

    private fun quoteToken(token: String): String = "\"${token.replace("\"", "\"\"")}\""

    private fun indexTokenizeRuns(text: String): List<String> =
        tokenizeCjkRuns(text, includeUnigramsForMultiChar = true)

    private fun queryTokenizeRuns(text: String): List<String> =
        tokenizeCjkRuns(text, includeUnigramsForMultiChar = false)

    private fun tokenizeCjkRuns(text: String, includeUnigramsForMultiChar: Boolean): List<String> {
        val normalized = text.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        val tokens = ArrayList<String>()
        var i = 0
        while (i < normalized.length) {
            val ch = normalized[i]
            if (ch.isWhitespace()) {
                i++
                continue
            }
            if (isCjkChar(ch)) {
                val run = StringBuilder()
                while (i < normalized.length && isCjkChar(normalized[i])) {
                    run.append(normalized[i])
                    i++
                }
                val s = run.toString()
                if (s.length == 1) {
                    tokens.add(s)
                } else {
                    for (j in 0 until s.length - 1) {
                        tokens.add(s.substring(j, j + 2))
                    }
                    if (includeUnigramsForMultiChar) {
                        for (c in s) {
                            tokens.add(c.toString())
                        }
                    }
                }
            } else {
                val word = StringBuilder()
                while (i < normalized.length && !normalized[i].isWhitespace() && !isCjkChar(normalized[i])) {
                    word.append(normalized[i])
                    i++
                }
                if (word.isNotEmpty()) tokens.add(word.toString())
            }
        }
        return tokens
    }

    private fun isCjkChar(ch: Char): Boolean = cjk.matches(ch.toString())
}
