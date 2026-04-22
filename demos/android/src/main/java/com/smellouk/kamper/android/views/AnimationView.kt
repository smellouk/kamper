package com.smellouk.kamper.android.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var angle = 0.0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val palette = intArrayOf(
        0xFF89B4FA.toInt(),
        0xFFA6E3A1.toInt(),
        0xFFF9E2AF.toInt(),
        0xFFFAB387.toInt(),
        0xFFCBA6F7.toInt(),
        0xFF94E2D5.toInt(),
    )

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            angle += 0.025
            invalidate()
            if (isAttachedToWindow) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFF181825.toInt())
        val cx = width / 2f
        val cy = height / 2f
        val r1 = min(width, height) * 0.30f
        val r2 = min(width, height) * 0.15f

        for (i in palette.indices) {
            val a = angle + i * Math.PI * 2 / palette.size
            val x = (cx + r1 * cos(a)).toFloat()
            val y = (cy + r1 * sin(a)).toFloat()
            paint.color = palette[i]
            canvas.drawCircle(x, y, 14f, paint)
        }

        for (i in palette.indices) {
            val a = -angle * 1.5 + i * Math.PI * 2 / palette.size
            val x = (cx + r2 * cos(a)).toFloat()
            val y = (cy + r2 * sin(a)).toFloat()
            val base = palette[(i + 3) % palette.size]
            paint.color = Color.argb(180, Color.red(base), Color.green(base), Color.blue(base))
            canvas.drawCircle(x, y, 7f, paint)
        }

        paint.color = 0xFF45475A.toInt()
        canvas.drawCircle(cx, cy, 5f, paint)
    }
}
