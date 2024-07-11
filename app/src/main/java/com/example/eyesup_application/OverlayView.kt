package com.example.eyesup_application

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color  // Import Color class
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()  // List of bounding boxes to draw
    private var boxPaint = Paint()  // Paint for bounding boxes
    private var textBackgroundPaint = Paint()  // Paint for text background
    private var textPaint = Paint()  // Paint for text

    private var bounds = Rect()  // Rectangle to measure text bounds

    init {
        initPaints()  // Initialize paints
    }

    fun clear() {
        textPaint.reset()  // Reset text paint
        textBackgroundPaint.reset()  // Reset text background paint
        boxPaint.reset()  // Reset box paint
        invalidate()  // Invalidate view to trigger redraw
        initPaints()  // Reinitialize paints
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK  // Set text background color
        textBackgroundPaint.style = Paint.Style.FILL  // Set text background style
        textBackgroundPaint.textSize = 50f  // Set text background size

        textPaint.color = Color.WHITE  // Set text color
        textPaint.style = Paint.Style.FILL  // Set text style
        textPaint.textSize = 50f  // Set text size

        boxPaint.strokeWidth = 8F  // Set bounding box stroke width
        boxPaint.style = Paint.Style.STROKE  // Set bounding box style
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1 * width  // Calculate left position
            val top = it.y1 * height  // Calculate top position
            val right = it.x2 * width  // Calculate right position
            val bottom = it.y2 * height  // Calculate bottom position

            boxPaint.color = it.color  // Set the color for the bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint)  // Draw bounding box

            val drawableText = it.clsName  // Get class name to draw
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)  // Measure text bounds
            val textWidth = bounds.width()  // Get text width
            val textHeight = bounds.height()  // Get text height
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint  // Draw text background
            )
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)  // Draw text
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes  // Update results
        invalidate()  // Invalidate view to trigger redraw
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8  // Padding for text background
    }
}
