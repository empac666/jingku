package com.broll.poc.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoFrameThumb {
    suspend fun load(context: Context, videoUri: String, timeSec: Double, maxEdge: Int = 320): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(videoUri))
                val us = (timeSec.coerceAtLeast(0.0) * 1_000_000).toLong()
                val frame = retriever.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                scaleDown(frame, maxEdge)
            } catch (_: Throwable) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {
                }
            }
        }

    private fun scaleDown(src: Bitmap?, maxEdge: Int): Bitmap? {
        if (src == null) return null
        val w = src.width
        val h = src.height
        if (w <= maxEdge && h <= maxEdge) return src
        val scale = maxEdge.toFloat() / maxOf(w, h)
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
