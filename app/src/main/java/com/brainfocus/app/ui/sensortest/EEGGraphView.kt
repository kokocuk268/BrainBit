package com.brainfocus.app.ui.sensortest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.brainfocus.app.R
import com.brainfocus.app.brainbit.EEGSample
import java.util.LinkedList

class EEGGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maxSamples = 500

    private val o1Samples = LinkedList<Float>()
    private val o2Samples = LinkedList<Float>()
    private val t3Samples = LinkedList<Float>()
    private val t4Samples = LinkedList<Float>()

    private val paintO1 = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val paintO2 = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val paintT3 = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val paintT4 = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val paintGrid = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val paintText = Paint().apply {
        textSize = 28f
        isAntiAlias = true
    }

    private val paintBackground = Paint().apply {
        style = Paint.Style.FILL
    }

    private var scale = 1e-4f

    init {
        updateColors()
    }

    private fun updateColors() {
        paintO1.color = ContextCompat.getColor(context, R.color.graph_o1)
        paintO2.color = ContextCompat.getColor(context, R.color.graph_o2)
        paintT3.color = ContextCompat.getColor(context, R.color.graph_t3)
        paintT4.color = ContextCompat.getColor(context, R.color.graph_t4)
        paintGrid.color = ContextCompat.getColor(context, R.color.graph_grid)
        paintBackground.color = ContextCompat.getColor(context, R.color.surface)
        paintText.color = ContextCompat.getColor(context, R.color.on_surface)
    }

    fun addSample(sample: EEGSample) {
        o1Samples.add(sample.o1)
        o2Samples.add(sample.o2)
        t3Samples.add(sample.t3)
        t4Samples.add(sample.t4)

        if (o1Samples.size > maxSamples) {
            o1Samples.removeFirst()
            o2Samples.removeFirst()
            t3Samples.removeFirst()
            t4Samples.removeFirst()
        }

        updateScale()
        postInvalidateOnAnimation()
    }

    private fun updateScale() {
        val allValues = o1Samples + o2Samples + t3Samples + t4Samples
        if (allValues.isEmpty()) return

        val maxAbs = allValues.maxOf { kotlin.math.abs(it) }
        if (maxAbs > 0) {
            scale = maxAbs * 1.2f
        }
    }

    fun clear() {
        o1Samples.clear()
        o2Samples.clear()
        t3Samples.clear()
        t4Samples.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBackground)

        drawGrid(canvas)

        if (o1Samples.isEmpty()) return

        drawChannel(canvas, o1Samples, paintO1)
        drawChannel(canvas, o2Samples, paintO2)
        drawChannel(canvas, t3Samples, paintT3)
        drawChannel(canvas, t4Samples, paintT4)

        drawLegend(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val centerY = height / 2f
        val gridSpacing = height / 8f

        for (i in 0..8) {
            val y = i * gridSpacing
            canvas.drawLine(0f, y, width.toFloat(), y, paintGrid)
        }

        canvas.drawLine(0f, centerY, width.toFloat(), centerY, Paint().apply {
            color = ContextCompat.getColor(context, R.color.graph_grid_center)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        })
    }

    private fun drawChannel(canvas: Canvas, samples: LinkedList<Float>, paint: Paint) {
        if (samples.size < 2) return

        val path = Path()
        val widthStep = width.toFloat() / (maxSamples - 1)

        val centerY = height / 2f
        val amplitudeScale = (height / 2f) / scale

        var x = (maxSamples - samples.size) * widthStep

        val firstY = centerY - (samples[0] * amplitudeScale)
        path.moveTo(x, firstY)

        for (i in 1 until samples.size) {
            x += widthStep
            val y = centerY - (samples[i] * amplitudeScale)
            path.lineTo(x, y)
        }

        canvas.drawPath(path, paint)
    }

    private fun drawLegend(canvas: Canvas) {
        val legendY = 40f
        val legendX = 20f
        val spacing = 120f

        canvas.drawText("O1", legendX, legendY, paintO1)
        canvas.drawText("O2", legendX + spacing, legendY, paintO2)
        canvas.drawText("T3", legendX + spacing * 2, legendY, paintT3)
        canvas.drawText("T4", legendX + spacing * 3, legendY, paintT4)
    }
}
