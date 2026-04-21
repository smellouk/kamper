package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.ui.TraceSpan

private const val RULER_HEIGHT_DP = 28f
private const val SPAN_HEIGHT_DP  = 24f
private const val SPAN_PADDING_DP = 2f
private const val MIN_LABEL_WIDTH_DP = 20f

private val SPAN_COLORS = listOf(
    Color(0xFF89B4FA),
    Color(0xFFA6E3A1),
    Color(0xFFFAB387),
    Color(0xFF94E2D5),
    Color(0xFFCBA6F7),
    Color(0xFFF38BA8),
    Color(0xFFF9E2AF),
    Color(0xFFB4BEFE),
)

private fun colorForName(name: String): Color =
    SPAN_COLORS[(name.hashCode() and Int.MAX_VALUE) % SPAN_COLORS.size]

private fun tickIntervalMs(pxPerMs: Float): Long {
    val targetPx = 80f
    val rawMs = targetPx / pxPerMs.coerceAtLeast(0.001f)
    return when {
        rawMs <= 10  -> 10L
        rawMs <= 50  -> 50L
        rawMs <= 100 -> 100L
        rawMs <= 500 -> 500L
        rawMs <= 1000 -> 1000L
        rawMs <= 5000 -> 5000L
        else -> 10000L
    }
}

@Composable
internal fun FlameChart(
    spans: List<TraceSpan>,
    modifier: Modifier = Modifier
) {
    if (spans.isEmpty()) return

    val totalMs   = spans.maxOf { it.startMs + it.durationMs }.coerceAtLeast(1L)
    val maxDepth  = spans.maxOf { it.depth }
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerDp = maxWidth.value.coerceAtLeast(1f)
        // auto-fit: scale the whole trace to fill container width
        val canvasWidthDp  = containerDp
        val canvasHeightDp = RULER_HEIGHT_DP + (maxDepth + 1) * SPAN_HEIGHT_DP

        val hScroll = rememberScrollState()
        val vScroll = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeightDp.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF11111B))
                .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(8.dp))
                .horizontalScroll(hScroll)
                .verticalScroll(vScroll)
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .width(canvasWidthDp.dp)
                    .height(canvasHeightDp.dp)
            ) {
                val pxPerMs    = size.width / totalMs.toFloat()
                val spanH      = SPAN_HEIGHT_DP.dp.toPx()
                val spanPad    = SPAN_PADDING_DP.dp.toPx()
                val rulerH     = RULER_HEIGHT_DP.dp.toPx()
                val minLabelPx = MIN_LABEL_WIDTH_DP.dp.toPx()

                // ── ruler background ──────────────────────────────────────────
                drawRect(
                    color = Color(0xFF1A1A2E),
                    topLeft = Offset.Zero,
                    size = Size(size.width, rulerH)
                )

                // ── tick marks + labels ───────────────────────────────────────
                val tickMs   = tickIntervalMs(pxPerMs)
                val rulerStyle = TextStyle(
                    fontSize   = 9.sp,
                    color      = KamperTheme.SUBTEXT,
                    fontFamily = FontFamily.Monospace
                )
                var t = 0L
                while (t <= totalMs) {
                    val x = t * pxPerMs
                    drawLine(
                        color = KamperTheme.BORDER,
                        start = Offset(x, rulerH * 0.5f),
                        end   = Offset(x, rulerH),
                        strokeWidth = 0.5.dp.toPx()
                    )
                    val label = if (t >= 1000) "${t / 1000}s" else "${t}ms"
                    val measured = textMeasurer.measure(label, style = rulerStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(x + 3f, 4f)
                    )
                    t += tickMs
                }

                // ── separator line under ruler ────────────────────────────────
                drawLine(
                    color = KamperTheme.BORDER,
                    start = Offset(0f, rulerH),
                    end   = Offset(size.width, rulerH),
                    strokeWidth = 0.5.dp.toPx()
                )

                // ── spans ─────────────────────────────────────────────────────
                spans.forEach { span ->
                    val x = span.startMs * pxPerMs
                    val w = (span.durationMs * pxPerMs).coerceAtLeast(1f)
                    val y = rulerH + span.depth * spanH + spanPad
                    val h = spanH - spanPad * 2f
                    val color = colorForName(span.name)

                    drawRoundRect(
                        color       = color.copy(alpha = 0.85f),
                        topLeft     = Offset(x, y),
                        size        = Size(w, h),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )

                    if (w >= minLabelPx) {
                        val textStyle = TextStyle(
                            fontSize   = 9.sp,
                            color      = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        val labelW = (w - 8f).coerceAtLeast(0f)
                        val measured = textMeasurer.measure(
                            text        = span.name,
                            style       = textStyle,
                            overflow    = TextOverflow.Ellipsis,
                            maxLines    = 1,
                            constraints = androidx.compose.ui.unit.Constraints(
                                maxWidth = labelW.toInt().coerceAtLeast(0)
                            )
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(x + 4f, y + (h - measured.size.height) / 2f)
                        )
                    }
                }
            }
        }
    }
}
