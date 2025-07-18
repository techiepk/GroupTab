package com.pennywiseai.tracker.ui.charts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.pennywiseai.tracker.R

class SimpleLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pennywise_primary)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val fillPaint = Paint().apply {
        // Get the primary color and set it with alpha
        val primaryColor = ContextCompat.getColor(context, R.color.pennywise_primary)
        // Set color with alpha (50/255 ≈ 0.2 opacity)
        color = (primaryColor and 0x00FFFFFF) or 0x32000000  // 0x32 = 50 in hex
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val pointPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pennywise_primary)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pennywise_text_primary)
        textSize = 20f
        isAntiAlias = true
    }
    
    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pennywise_text_secondary)
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        isAntiAlias = true
    }.also {
        it.alpha = 64  // Make grid lines subtle (0-255 range)
    }
    
    private var data: List<LineData> = emptyList()
    private val path = Path()
    private val fillPath = Path()
    private val padding = 48f
    
    data class LineData(
        val label: String,
        val value: Float
    )
    
    fun setData(newData: List<LineData>) {
        data = newData
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (data.isEmpty()) return
        
        val availableWidth = width - (padding * 2)
        val availableHeight = height - (padding * 2) - textPaint.textSize
        val pointSpacing = if (data.size > 1) availableWidth / (data.size - 1) else 0f
        val maxValue = data.maxOfOrNull { it.value } ?: 1f
        
        // Draw grid
        for (i in 0..4) {
            val y = padding + (availableHeight * (1 - i / 4f))
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }
        
        // Build line path
        path.reset()
        fillPath.reset()
        
        data.forEachIndexed { index, lineData ->
            val x = padding + (index * pointSpacing)
            val y = padding + availableHeight * (1 - lineData.value / maxValue)
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - padding - textPaint.textSize)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            // Draw point
            canvas.drawCircle(x, y, 8f, pointPaint)
            
            // Draw label
            val labelWidth = textPaint.measureText(lineData.label)
            val labelX = x - labelWidth / 2
            val labelY = height - padding / 2
            canvas.save()
            canvas.rotate(-45f, x, labelY)
            canvas.drawText(lineData.label, x - labelWidth / 2, labelY, textPaint)
            canvas.restore()
        }
        
        // Complete fill path
        if (data.isNotEmpty()) {
            val lastX = padding + ((data.size - 1) * pointSpacing)
            fillPath.lineTo(lastX, height - padding - textPaint.textSize)
            fillPath.close()
        }
        
        // Draw fill
        canvas.drawPath(fillPath, fillPaint)
        
        // Draw line
        canvas.drawPath(path, linePaint)
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