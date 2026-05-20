package com.brainfocus.app.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class DiagonalWipeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var progress = 0.0f

    fun setScreenshot(bmp: Bitmap?) {
        bitmap = bmp
        invalidate()
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0.0f, 1.0f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        path.reset()
        val limit = w + h
        val d = progress * limit

        if (progress >= 1.0f) return // Completely revealed

        if (progress <= 0.0f) {
            canvas.drawBitmap(bmp, 0f, 0f, paint) // Completely covered by old
            return
        }

        // Build the polygon of the unrevealed area (always contains bottom-left corner)
        path.moveTo(0f, h)

        // Go up along the left edge towards the top edge
        if (d < w) {
            path.lineTo(0f, 0f)     // Top-left corner is still covered
            path.lineTo(w - d, 0f)  // Intersection on the top edge
        } else {
            path.lineTo(0f, d - w)  // Intersection shifted to the left edge
        }

        // Go to the intersection on the right or bottom edge
        if (d < h) {
            path.lineTo(w, d)       // Intersection on the right edge
            path.lineTo(w, h)       // Bottom-right corner is still covered
        } else {
            path.lineTo(w + h - d, h) // Intersection shifted to the bottom edge
        }

        path.close()

        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        canvas.restore()
    }
}
