package com.smellouk.konitor.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun Sparkline(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        drawSparkline(data, color)
    }
}

private fun DrawScope.drawSparkline(data: List<Float>, color: Color) {
    val maxVal = data.max().coerceAtLeast(0.001f)
    val w = size.width
    val h = size.height
    val stepX = w / (data.size - 1)

    val linePath = Path()
    val fillPath = Path()

    data.forEachIndexed { i, v ->
        val x = i * stepX
        val y = h - (v / maxVal) * h
        if (i == 0) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, h)
            fillPath.lineTo(x, y)
        } else {
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
    }

    fillPath.lineTo((data.size - 1) * stepX, h)
    fillPath.close()

    drawPath(fillPath, color.copy(alpha = 0.25f))
    drawPath(linePath, color, style = Stroke(width = 1.5f))

    val lastX = (data.size - 1) * stepX
    val lastY = h - (data.last() / maxVal) * h
    drawCircle(color, radius = 2.5f, center = Offset(lastX, lastY))
}
