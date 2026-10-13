package com.ultracam.pro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class HistogramView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val redPaint = Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val greenPaint = Paint().apply { color = Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val bluePaint = Paint().apply { color = Color.BLUE; style = Paint.Style.STROKE; strokeWidth = 2f }
    private var redHist = IntArray(256)
    private var greenHist = IntArray(256)
    private var blueHist = IntArray(256)

    fun updateHistogram(r: IntArray, g: IntArray, b: IntArray) {
        this.redHist = r; this.greenHist = g; this.blueHist = b
        postInvalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat(); val height = height.toFloat()
        val maxVal = (redHist.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        for (i in 0 until 255) {
            val x1 = (i / 256f) * width; val x2 = ((i + 1) / 256f) * width
            canvas.drawLine(x1, height - (redHist[i] / maxVal) * height, x2, height - (redHist[i + 1] / maxVal) * height, redPaint)
            canvas.drawLine(x1, height - (greenHist[i] / maxVal) * height, x2, height - (greenHist[i + 1] / maxVal) * height, greenPaint)
            canvas.drawLine(x1, height - (blueHist[i] / maxVal) * height, x2, height - (blueHist[i + 1] / maxVal) * height, bluePaint)
        }
    }
}
