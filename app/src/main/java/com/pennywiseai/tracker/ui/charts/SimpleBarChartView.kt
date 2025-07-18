package com.pennywiseai.tracker.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.pennywiseai.tracker.R
import kotlin.math.max

class SimpleBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val barPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pennywise_text_primary)
        textSize = 24f
        isAntiAlias = true
    }
    
    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pennywise_text_secondary)
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }.also {
        it.alpha = 64  // Make grid lines subtle (0-255 range)
    }
    
    private var data: List<BarData> = emptyList()
    private val barRect = RectF()
    private val padding = 32f
    private val barSpacing = 16f
    
    data class BarData(
        val label: String,
        val value: Float,
        val color: Int
    )
    
    fun setData(newData: List<BarData>) {
        data = newData
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (data.isEmpty()) return
        
        val availableWidth = width - (padding * 2)
        val availableHeight = height - (padding * 3) - textPaint.textSize
        val barWidth = (availableWidth - (barSpacing * (data.size - 1))) / data.size
        val maxValue = data.maxOfOrNull { it.value } ?: 1f
        
        // Draw grid lines
        for (i in 0..4) {
            val y = padding + (availableHeight * (1 - i / 4f))
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }
        
        // Draw bars
        data.forEachIndexed { index, barData ->
            val barHeight = (barData.value / maxValue) * availableHeight
            val left = padding + (index * (barWidth + barSpacing))
            val top = height - padding - textPaint.textSize - barHeight
            val right = left + barWidth
            val bottom = height - padding - textPaint.textSize
            
            barRect.set(left, top, right, bottom)
            barPaint.color = barData.color
            canvas.drawRoundRect(barRect, 8f, 8f, barPaint)
            
            // Draw labels
            val labelWidth = textPaint.measureText(barData.label)
            val labelX = left + (barWidth - labelWidth) / 2
            val labelY = height - padding / 2
            canvas.drawText(barData.label, labelX, labelY, textPaint)
            
            // Draw value on top of bar
            val valueText = String.format("%.0f", barData.value)
            val valueWidth = textPaint.measureText(valueText)
            val valueX = left + (barWidth - valueWidth) / 2
            val valueY = top - 8f
            canvas.drawText(valueText, valueX, valueY, textPaint)
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 300
        val desiredHeight = 200
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> widthSize.coerceAtMost(desiredWidth)
            else -> desiredWidth
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> heightSize.coerceAtMost(desiredHeight)
            else -> desiredHeight
        }
        
        setMeasuredDimension(width, height)
    }
}