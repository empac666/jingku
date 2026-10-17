package com.broll.poc.ui

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan

object DialogueHighlighter {
    /**
     * 字面匹配高亮：查询与台词均 trim；无字面命中则不伪造高亮。
     */
    fun highlight(label: String, query: String, highlightColor: Int): CharSequence {
        val q = query.trim()
        if (q.isEmpty()) return label
        val lowerLabel = label.lowercase()
        val lowerQ = q.lowercase()
        val idx = lowerLabel.indexOf(lowerQ)
        if (idx < 0) return label
        val span = SpannableString(label)
        span.setSpan(
            BackgroundColorSpan(highlightColor),
            idx,
            idx + q.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        span.setSpan(StyleSpan(Typeface.BOLD), idx, idx + q.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return span
    }

    fun hasLiteralMatch(label: String, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false
        return label.lowercase().contains(q.lowercase())
    }
}
