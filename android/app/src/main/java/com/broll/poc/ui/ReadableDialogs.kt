package com.broll.poc.ui

import android.content.Context
import android.graphics.Typeface
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import com.broll.poc.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ReadableDialogs {
    fun reportView(context: Context, trace: String): ScrollView = ScrollView(context).apply {
        addView(HorizontalScrollView(context).apply {
            addView(TextView(context).apply {
                text = trace; typeface = Typeface.MONOSPACE; textSize = 12f
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.on_surface))
                val pad = (24 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                setTextIsSelectable(true)
            })
        })
    }
    fun licenses(context: Context, body: String) = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.about_licenses)
        .setMessage(context.getString(R.string.model_attribution) + "\n\n" + body)
        .setPositiveButton(android.R.string.ok, null).show()
}
