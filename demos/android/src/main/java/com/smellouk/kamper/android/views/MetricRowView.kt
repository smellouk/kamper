package com.smellouk.kamper.android.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

class MetricRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var label: String = ""
        set(value) { field = value; invalidate() }

    var fraction: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); cachedGradient = null; invalidate() }

    var valueText: String = "—"
        set(value) { field = value; invalidate() }

    var barColor: Int = 0xFF89B4FA.toInt()
        set(value) { field = value; cachedGradient = null; invalidate() }

    private val density = resources.displayMetrics.density
    private val sp = density * resources.configuration.fontScale
    private val labelWidth = 72 * density
    private val valueWidth = 60 * density
    private val barHeight = 12 * density
    private val cornerRadius = 6 * density

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12 * sp
        color = 0xFFA6ADC8.toInt()
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12 * sp
        color = 0xFFCDD6F4.toInt()
        textAlign = Paint.Align.RIGHT
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF313244.toInt()
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val barRect = RectF()
    private val trackRect = RectF()
    private var cachedGradient: LinearGradient? = null

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cy = h / 2f
        val baseline = cy - (labelPaint.descent() + labelPaint.ascent()) / 2f

        canvas.drawText(label, 0f, baseline, labelPaint)
        canvas.drawText(valueText, w, baseline, valuePaint)

        val barLeft = labelWidth
        val barRight = w - valueWidth
        val barTop = cy - barHeight / 2f
        val barBottom = cy + barHeight / 2f

        trackRect.set(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

        val fillRight = barLeft + (barRight - barLeft) * fraction
        if (fillRight > barLeft + 1f) {
            if (cachedGradient == null) {
                cachedGradient = LinearGradient(
                    barLeft, 0f, fillRight.coerceAtLeast(barLeft + 2f), 0f,
                    ColorUtils.blendARGB(barColor, Color.WHITE, 0.25f),
                    barColor,
                    Shader.TileMode.CLAMP
                )
                barPaint.shader = cachedGradient
            }
            barRect.set(barLeft, barTop, fillRight, barBottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
        }
    }
}
