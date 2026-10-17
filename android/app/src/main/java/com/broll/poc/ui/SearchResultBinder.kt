package com.broll.poc.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.broll.poc.AppConfig
import com.broll.poc.R
import com.broll.poc.data.Segment
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

object SearchResultBinder {
    fun inflateCard(parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)

    fun bind(
        root: View,
        segment: Segment,
        query: String,
        scope: CoroutineScope,
        onPlay: (Segment) -> Unit,
        thumbJobs: MutableMap<View, Job>,
    ) {
        val card = root as MaterialCardView
        val thumb = root.findViewById<ImageView>(R.id.imageThumb)
        val title = root.findViewById<TextView>(R.id.textVideoName)
        val dialogue = root.findViewById<TextView>(R.id.textDialogue)
        val meta = root.findViewById<TextView>(R.id.textMeta)
        val lowConf = root.findViewById<TextView>(R.id.textLowConfidence)

        title.text = segment.videoName.ifBlank { segment.videoUri }
        val highlightColor = ContextCompat.getColor(root.context, R.color.highlight_match)
        dialogue.text = DialogueHighlighter.highlight(segment.label, query, highlightColor)
        val scoreValue = segment.score.toDouble()
        meta.text = root.context.getString(
            R.string.result_meta_format,
            timestamp(segment.startSec),
            timestamp(segment.endSec),
            scoreValue,
        )
        val low = segment.score < AppConfig.SEMANTIC_LOW_CONFIDENCE
        lowConf.visibility = if (low) View.VISIBLE else View.GONE
        lowConf.text = root.context.getString(R.string.low_confidence_hint)

        thumbJobs[root]?.cancel()
        thumb.setImageDrawable(null)
        thumbJobs[root] = scope.launch {
            val bmp = VideoFrameThumb.load(root.context, segment.videoUri, segment.startSec)
            if (bmp != null) {
                thumb.post { thumb.setImageBitmap(bmp) }
            }
        }

        card.setOnClickListener { onPlay(segment) }
    }

    private fun timestamp(sec: Double): String {
        val seconds = sec.toLong().coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
    }
    fun formatScore(score: Float): String = String.format(Locale.US, "%.3f", score)
}
