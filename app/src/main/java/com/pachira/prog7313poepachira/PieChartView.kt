package com.pachira.prog7313poepachira

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class PieSlice(val percentage: Float, val color: Int)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val data = mutableListOf<PieSlice>()

    fun setData(newData: List<PieSlice>) {
        data.clear()
        data.addAll(newData)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            // Draw empty state
            paint.color = Color.LTGRAY
            canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 2f - 10f, paint)
            return
        }

        // Calculate the bounds of the pie chart
        val padding = 10f
        rect.set(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding
        )

        var startAngle = 0f

        // Draw each slice
        for (slice in data) {
            paint.color = slice.color
            val sweepAngle = 360f * slice.percentage / 100f
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        // Draw a white circle in the center for a donut chart effect
        paint.color = Color.WHITE
        val centerRadius = minOf(width, height) / 4f
        canvas.drawCircle(width / 2f, height / 2f, centerRadius, paint)
    }
}
