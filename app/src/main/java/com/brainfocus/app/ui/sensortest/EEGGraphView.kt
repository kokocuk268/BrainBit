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
import kotlin.math.abs

class EEGGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maxSamples = 500

    private val o1Samples = RingBuffer(maxSamples)
    private val o2Samples = RingBuffer(maxSamples)
    private val t3Samples = RingBuffer(maxSamples)
    private val t4Samples = RingBuffer(maxSamples)

    private val paintO1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintO2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintT3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintT4 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintGrid = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val paintGridCenter = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val paintBackground = Paint().apply {
        style = Paint.Style.FILL
    }

    private var scale = 1e-4f
    private var currentMaxAbs = 0f
    private var needScaleRecalc = false
    private var recalcCounter = 0

    private val pathO1 = Path()
    private val pathO2 = Path()
    private val pathT3 = Path()
    private val pathT4 = Path()

    init {
        updateColors()
    }

    private fun updateColors() {
        paintO1.color = ContextCompat.getColor(context, R.color.graph_o1)
        paintO2.color = ContextCompat.getColor(context, R.color.graph_o2)
        paintT3.color = ContextCompat.getColor(context, R.color.graph_t3)
        paintT4.color = ContextCompat.getColor(context, R.color.graph_t4)
        paintGrid.color = ContextCompat.getColor(context, R.color.graph_grid)
        paintGridCenter.color = ContextCompat.getColor(context, R.color.graph_grid_center)
        paintBackground.color = ContextCompat.getColor(context, R.color.surface)
    }

    fun addSample(sample: EEGSample) {
        o1Samples.add(sample.o1)
        o2Samples.add(sample.o2)
        t3Samples.add(sample.t3)
        t4Samples.add(sample.t4)

        val newMax = abs(sample.o1)
            .coerceAtLeast(abs(sample.o2))
            .coerceAtLeast(abs(sample.t3))
            .coerceAtLeast(abs(sample.t4))

        if (newMax > currentMaxAbs) {
            currentMaxAbs = newMax
            if (newMax > 0) scale = newMax * 1.2f
        }

        needScaleRecalc = true

        postInvalidateOnAnimation()
    }

    fun clear() {
        o1Samples.clear()
        o2Samples.clear()
        t3Samples.clear()
        t4Samples.clear()
        currentMaxAbs = 0f
        scale = 1e-4f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBackground)

        drawGrid(canvas)

        if (o1Samples.isEmpty()) return

        recalcScaleIfNeeded()

        drawChannel(canvas, o1Samples, paintO1, pathO1)
        drawChannel(canvas, o2Samples, paintO2, pathO2)
        drawChannel(canvas, t3Samples, paintT3, pathT3)
        drawChannel(canvas, t4Samples, paintT4, pathT4)

        drawLegend(canvas)
    }

    private fun recalcScaleIfNeeded() {
        if (!needScaleRecalc) return
        recalcCounter++
        if (recalcCounter < 60) return
        recalcCounter = 0
        needScaleRecalc = false

        var maxAbs = 0f
        o1Samples.forEach { maxAbs = maxOf(maxAbs, abs(it)) }
        o2Samples.forEach { maxAbs = maxOf(maxAbs, abs(it)) }
        t3Samples.forEach { maxAbs = maxOf(maxAbs, abs(it)) }
        t4Samples.forEach { maxAbs = maxOf(maxAbs, abs(it)) }

        if (maxAbs > 0) {
            currentMaxAbs = maxAbs
            scale = maxAbs * 1.2f
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val centerY = height / 2f
        val gridSpacing = height / 8f

        for (i in 0..8) {
            val y = i * gridSpacing
            canvas.drawLine(0f, y, width.toFloat(), y, paintGrid)
        }

        canvas.drawLine(0f, centerY, width.toFloat(), centerY, paintGridCenter)
    }

    private fun drawChannel(canvas: Canvas, buffer: RingBuffer, paint: Paint, path: Path) {
        if (buffer.size < 2) return

        path.rewind()
        val widthStep = width.toFloat() / (maxSamples - 1)
        val centerY = height / 2f
        val amplitudeScale = (height / 2f) / scale

        var x = (maxSamples - buffer.size) * widthStep
        var first = true

        buffer.forEach { value ->
            val y = centerY - (value * amplitudeScale)
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            x += widthStep
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

    private class RingBuffer(private val capacity: Int) {
        private val buffer = FloatArray(capacity)
        private var writeIndex = 0
        private var _size = 0

        val size: Int get() = _size

        fun isEmpty(): Boolean = _size == 0

        fun add(value: Float) {
            buffer[writeIndex] = value
            writeIndex = if (writeIndex + 1 < capacity) writeIndex + 1 else 0
            if (_size < capacity) _size++
        }

        inline fun forEach(action: (Float) -> Unit) {
            if (_size == 0) return
            val start = if (_size < capacity) 0 else writeIndex
            for (i in 0 until _size) {
                val idx = start + i
                action(buffer[if (idx < capacity) idx else idx - capacity])
            }
        }

        fun clear() {
            writeIndex = 0
            _size = 0
        }
    }
}
