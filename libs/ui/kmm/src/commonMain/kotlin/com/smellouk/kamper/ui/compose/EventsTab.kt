package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.ui.EventEntry

private enum class EventWindow(val label: String, val windowMs: Long) {
    LIVE("LIVE", -1L),
    SEC5("5s", 5_000L),
    SEC10("10s", 10_000L),
    SEC30("30s", 30_000L),
    ALL("All", Long.MAX_VALUE)
}

@Composable
internal fun EventsTab(
    events: List<EventEntry>,
    onClear: () -> Unit
) {
    var selected by remember { mutableStateOf(EventWindow.LIVE) }
    val newestMs = events.firstOrNull()?.receivedAtMs ?: 0L

    val filtered = remember(events, selected, newestMs) {
        when (selected) {
            EventWindow.LIVE  -> events.take(50)
            EventWindow.ALL   -> events
            else              -> events.filter { newestMs - it.receivedAtMs <= selected.windowMs }
        }
    }
    val maxDur = remember(filtered) {
        filtered.mapNotNull { it.durationMs }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Mini overview
        EventOverview(events = events, newestMs = newestMs)

        // Chip row
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EventWindow.entries.forEach { w ->
                val on = w == selected
                val isLive = w == EventWindow.LIVE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (on) KamperTheme.TEAL.copy(alpha = 0.15f) else KamperTheme.BASE)
                        .border(0.5.dp, if (on) KamperTheme.TEAL else KamperTheme.BORDER, RoundedCornerShape(100.dp))
                        .clickable { selected = w }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (isLive) {
                        Box(
                            Modifier
                                .width(6.dp).height(6.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (on) KamperTheme.GREEN else KamperTheme.SUBTEXT)
                        )
                    }
                    Text(
                        w.label,
                        color = if (on) KamperTheme.TEAL else KamperTheme.SUBTEXT,
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (events.isNotEmpty()) {
                Text(
                    "Clear",
                    color = KamperTheme.RED,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(KamperTheme.RED.copy(alpha = 0.1f))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Count
        Text(
            "${filtered.size} event${if (filtered.size != 1) "s" else ""}",
            color = KamperTheme.SUBTEXT,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        // List
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No events in window", color = KamperTheme.SUBTEXT, fontSize = 12.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filtered.forEach { entry ->
                    EventRow(entry = entry, newestMs = newestMs, maxDur = maxDur)
                }
            }
        }
    }
}

@Composable
private fun EventOverview(events: List<EventEntry>, newestMs: Long) {
    val teal   = KamperTheme.TEAL
    val blue   = KamperTheme.BLUE
    val yellow = KamperTheme.YELLOW
    val red    = KamperTheme.RED
    val border = KamperTheme.BORDER

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, border, RoundedCornerShape(6.dp))
    ) {
        if (events.isEmpty() || newestMs == 0L) {
            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                Text("no data", color = KamperTheme.SUBTEXT, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                val w = size.width
                val h = size.height
                val windowMs = 30_000L
                val winStart = newestMs - windowMs

                // Grid lines every 10s
                for (t in 0L..windowMs step 10_000L) {
                    val x = (t.toFloat() / windowMs) * w
                    drawLine(border.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }

                events.forEach { ev ->
                    if (ev.receivedAtMs < winStart || ev.receivedAtMs > newestMs) return@forEach
                    val xS = ((ev.receivedAtMs - winStart).toFloat() / windowMs) * w
                    val color = eventColor(ev, teal, blue, yellow, red)
                    if (ev.durationMs == null) {
                        drawLine(color.copy(alpha = 0.7f), Offset(xS, 4f), Offset(xS, h - 4f), strokeWidth = 1.5f)
                        drawCircle(color, radius = 3f, center = Offset(xS, h / 2))
                    } else {
                        val durPx = (ev.durationMs.toFloat() / windowMs) * w
                        val barW = durPx.coerceAtLeast(3f)
                        drawRect(
                            color = color.copy(alpha = 0.35f),
                            topLeft = Offset(xS, h / 2 - 5f),
                            size = Size(barW, 10f)
                        )
                        drawLine(color, Offset(xS, h / 2 - 5f), Offset(xS, h / 2 + 5f), strokeWidth = 1.5f)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(entry: EventEntry, newestMs: Long, maxDur: Long) {
    val teal   = KamperTheme.TEAL
    val blue   = KamperTheme.BLUE
    val yellow = KamperTheme.YELLOW
    val red    = KamperTheme.RED
    val color  = eventColor(entry, teal, blue, yellow, red)
    val barFraction = entry.durationMs?.let { (it.toFloat() / maxDur).coerceIn(0f, 1f) } ?: 0f
    val ageMs = newestMs - entry.receivedAtMs
    val ageLabel = when {
        ageMs < 1_000  -> "just now"
        ageMs < 60_000 -> "${ageMs / 1_000}s ago"
        else           -> "${ageMs / 60_000}m ago"
    }
    val durLabel = entry.durationMs?.let { "${it}ms" } ?: "instant"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(7.dp).height(7.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, color = KamperTheme.TEXT, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ageLabel, color = KamperTheme.SUBTEXT, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(durLabel, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        if (entry.durationMs != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(48.dp).height(3.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(KamperTheme.SURFACE)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction).height(3.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(color)
                )
            }
        }
    }
}

private fun eventColor(entry: EventEntry, teal: Color, blue: Color, yellow: Color, red: Color): Color =
    when {
        entry.durationMs == null   -> blue
        entry.durationMs >= 2_000  -> red
        entry.durationMs >= 500    -> yellow
        else                       -> teal
    }
