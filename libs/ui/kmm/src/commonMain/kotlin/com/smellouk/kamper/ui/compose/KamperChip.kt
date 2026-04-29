package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.ui.ChipState
import com.smellouk.kamper.ui.KamperUiSettings
import com.smellouk.kamper.ui.KamperUiState

private val CHIP_SHAPE_LEFT  = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 10.dp, bottomEnd = 10.dp)
private val CHIP_SHAPE_RIGHT = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 0.dp, bottomEnd = 0.dp)

// Row = icon(14dp) + label(natural, max 44dp) + spacer + value
// Total min width ~120dp so PEEK (56dp) always shows icon + label fully
private const val ROW_WIDTH_DP = 128

@Composable
internal fun KamperChip(
    state: KamperUiState,
    settings: KamperUiSettings = KamperUiSettings(),
    chipState: ChipState = ChipState.PEEK,
    onClick: () -> Unit,
    mirrorLayout: Boolean = false,
    onDrag: ((dx: Float, dy: Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    isTv: Boolean = false,
    hasTvFocus: Boolean = false
) {
    val fontSize = if (isTv) (11f * 0.75f).sp else 11.sp

    val netDisplay = if (state.downloadMbps < 0.1f) {
        "${(state.downloadMbps * 1024f).formatDp(1)}K/s"
    } else {
        "${state.downloadMbps.formatDp(2)}M/s"
    }

    val dragModifier = if (onDrag != null && !isTv) {
        Modifier.pointerInput(Unit) {
            var dragging = false
            detectDragGestures(
                onDragStart = { dragging = true },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                },
                onDragEnd   = { if (dragging) { dragging = false; onDragEnd?.invoke() } },
                onDragCancel = { if (dragging) { dragging = false; onDragEnd?.invoke() } }
            )
        }
    } else Modifier

    // D-08: Android TV chip is focusable via D-pad. NOTE: Do NOT apply on tvOS — UIKit
    // focus engine is separate from Compose focus (see Phase 15 RESEARCH.md Pitfall 2).
    // The tvosMain actual passes isTv = false; only AndroidOverlayManager passes true on leanback.
    val focusModifier = if (isTv) Modifier.focusable() else Modifier

    KamperThemeProvider(isDark = settings.isDarkTheme) {
    val chipShape = if (mirrorLayout) CHIP_SHAPE_LEFT else CHIP_SHAPE_RIGHT
    val bgColor = if (hasTvFocus) KamperTheme.SURFACE else KamperTheme.SURFACE1
    val blueColor = KamperTheme.BLUE
    val borderNormalColor = KamperTheme.BORDER
    // Focused: 3-sided path (top + rounded-open-side + bottom), same 1.5dp as panel cards.
    // Non-focused: standard full border at 0.5dp.
    val focusBorderMod: Modifier = if (hasTvFocus) {
        Modifier.drawBehind {
            val strokeW = 1.5.dp.toPx()
            val halfW   = strokeW / 2f
            val r       = 10.dp.toPx()
            val path    = Path()
            if (!mirrorLayout) {
                // Right-side chip: top → left arc → bottom, skip right
                path.moveTo(size.width, halfW)
                path.lineTo(r, halfW)
                path.quadraticTo(halfW, halfW, halfW, r)
                path.lineTo(halfW, size.height - r)
                path.quadraticTo(halfW, size.height - halfW, r, size.height - halfW)
                path.lineTo(size.width, size.height - halfW)
            } else {
                // Left-side chip: top → right arc → bottom, skip left
                path.moveTo(0f, halfW)
                path.lineTo(size.width - r, halfW)
                path.quadraticTo(size.width - halfW, halfW, size.width - halfW, r)
                path.lineTo(size.width - halfW, size.height - r)
                path.quadraticTo(size.width - halfW, size.height - halfW, size.width - r, size.height - halfW)
                path.lineTo(0f, size.height - halfW)
            }
            drawPath(path, color = blueColor, style = Stroke(width = strokeW, cap = StrokeCap.Square))
        }
    } else {
        Modifier.border(0.5.dp, borderNormalColor, chipShape)
    }
    Box(
        modifier = focusModifier
            .then(dragModifier)
            .shadow(8.dp, chipShape)
            .clip(chipShape)
            .background(bgColor)
            .then(focusBorderMod)
            .clickable(onClick = onClick)
            .padding(
                start = if (mirrorLayout && chipState == ChipState.PEEK) 6.dp else 6.dp,
                end   = if (mirrorLayout && chipState == ChipState.PEEK) 3.dp else 6.dp,
                top   = 6.dp,
                bottom = 6.dp
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (settings.cpuEnabled && settings.showCpu)       MetricRow("⚙", KamperTheme.BLUE,  "CPU", "${state.cpuPercent.formatDp(1)}%", mirrorLayout, fontSize)
            if (settings.fpsEnabled && settings.showFps)       MetricRow("◎", KamperTheme.GREEN, "FPS", "${state.fps} fps", mirrorLayout, fontSize)
            if (settings.memoryEnabled && settings.showMemory) MetricRow("▦", KamperTheme.PEACH, "MEM", "${state.memoryUsedMb.formatDp(0)} MB", mirrorLayout, fontSize)
            if (settings.networkEnabled && settings.showNetwork) MetricRow("↓", KamperTheme.TEAL, "NET", netDisplay, mirrorLayout, fontSize)

            if (settings.jankEnabled && settings.showJank && state.jankDroppedFrames >= 0)
                MetricRow("⚡", KamperTheme.MAUVE, "JANK", "${state.jankDroppedFrames} fr", mirrorLayout, fontSize)
            if (settings.gcEnabled && settings.showGc && state.gcCountDelta >= 0)
                MetricRow("♻", KamperTheme.YELLOW, "GC", "+${state.gcCountDelta}", mirrorLayout, fontSize)
            if (settings.thermalEnabled && settings.showThermal)
                MetricRow("🌡", KamperTheme.PEACH, "THRM", state.thermalState.name, mirrorLayout, fontSize)

            if (settings.issuesEnabled && settings.showIssues) {
                Box(
                    Modifier
                        .width(ROW_WIDTH_DP.dp)
                        .height(0.5.dp)
                        .background(KamperTheme.BORDER)
                )
                val count  = state.issues.size
                val unread = state.unreadIssueCount
                val badgeColor = if (count > 0) KamperTheme.RED else KamperTheme.SUBTEXT

                if (chipState == ChipState.PEEK) {
                    // PEEK: icon + count badge only, no label
                    IssueBadgeRow(count, badgeColor, mirrorLayout, fontSize)
                } else {
                    val value = when {
                        count == 0 -> "none"
                        unread > 0 -> "$count (+$unread)"
                        else       -> "$count"
                    }
                    MetricRow("⚠", badgeColor, "ISSUES", value, mirrorLayout, fontSize)
                }
            }
        }
    }
    } // KamperThemeProvider
}

@Composable
private fun IssueBadgeRow(count: Int, color: Color, mirror: Boolean, fontSize: androidx.compose.ui.unit.TextUnit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(ROW_WIDTH_DP.dp)
    ) {
        if (!mirror) {
            Text("⚠", color = color, fontSize = fontSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(14.dp))
            if (count > 0) {
                Text(
                    "$count",
                    color = color,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
            if (count > 0) {
                Text(
                    "$count",
                    color = color,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(2.dp))
            }
            Text("⚠", color = color, fontSize = fontSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(14.dp))
        }
    }
}

@Composable
private fun MetricRow(icon: String, color: Color, label: String, value: String, mirror: Boolean, fontSize: androidx.compose.ui.unit.TextUnit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(ROW_WIDTH_DP.dp)
    ) {
        if (!mirror) {
            Text(
                icon,
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(14.dp)
            )
            Text(
                label,
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.widthIn(max = 44.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                value,
                color = KamperTheme.TEXT,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        } else {
            Text(
                value,
                color = KamperTheme.TEXT,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            Text(
                label,
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.widthIn(max = 44.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                icon,
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(14.dp)
            )
        }
    }
}
