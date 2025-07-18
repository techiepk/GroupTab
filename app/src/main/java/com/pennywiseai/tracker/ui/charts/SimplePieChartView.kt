package com.pennywiseai.tracker.ui.charts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.pennywiseai.tracker.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SimplePieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val slicePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pennywise_text_primary)
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val labelPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private var data: List<PieData> = emptyList()
    private val pieRect = RectF()
    private val padding = 32f
    
    data class PieData(
        val label: String,
        val value: Float,
        val color: Int
    )
    
    fun setData(newData: List<PieData>) {
        data = newData
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (data.isEmpty()) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - padding
        
        pieRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        val total = data.sumOf { it.value.toDouble() }.toFloat()
        var startAngle = -90f // Start from top
        
        data.forEach { pieData ->
            val sweepAngle = (pieData.value / total) * 360f
            
            // Draw slice
            slicePaint.color = pieData.color
            canvas.drawArc(pieRect, startAngle, sweepAngle, true, slicePaint)
            
            // Draw percentage label if slice is big enough
            if (sweepAngle > 15f) {
                val percentage = (pieData.value / total * 100).toInt()
                val labelAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                val labelX = centerX + (radius * 0.7f * cos(labelAngle)).toFloat()
                val labelY = centerY + (radius * 0.7f * sin(labelAngle)).toFloat() + labelPaint.textSize / 3
                
                canvas.drawText("$percentage%", labelX, labelY, labelPaint)
            }
            
            startAngle += sweepAngle
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = 250
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> widthSize.coerceAtMost(desiredSize)
            else -> desiredSize
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> heightSize.coerceAtMost(desiredSize)
            else -> desiredSize
        }
        
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }
}