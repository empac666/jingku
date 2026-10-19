package com.broll.poc.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/** Full width 16:9 still, independent of the source image's aspect ratio. */
class AspectFrame @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val clip = android.graphics.Path()
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        // Clip the full-bleed still itself; CardView's compat background can leave
        // child pixels outside its rounded outline on some renderers.
        val radius = 16f * resources.displayMetrics.density
        clip.reset()
        clip.addRoundRect(android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat()),
            floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f), android.graphics.Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(clip); super.dispatchDraw(canvas); canvas.restoreToCount(save)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY))
    }
}
