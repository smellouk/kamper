package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.ui.KamperUiSettings
import com.smellouk.kamper.ui.KamperUiState

private val CHIP_SHAPE = RoundedCornerShape(10.dp)

@Composable
internal fun KamperChip(
    state: KamperUiState,
    settings: KamperUiSettings = KamperUiSettings(),
    onClick: () -> Unit,
    mirrorLayout: Boolean = false,
    onDrag: ((dx: Float, dy: Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    val netDisplay = if (state.downloadMbps < 0.1f) {
        "${(state.downloadMbps * 1024f).formatDp(1)}K/s"
    } else {
        "${state.downloadMbps.formatDp(2)}M/s"
    }

    val dragModifier = if (onDrag != null) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                },
                onDragEnd = { onDragEnd?.invoke() },
                onDragCancel = { onDragEnd?.invoke() }
            )
        }
    } else Modifier

    Box(
        modifier = dragModifier
            .shadow(8.dp, CHIP_SHAPE)
            .clip(CHIP_SHAPE)
            .background(KamperTheme.SURFACE1)
            .border(0.5.dp, KamperTheme.BORDER, CHIP_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (settings.showCpu) MetricRow("⚙", KamperTheme.BLUE, "CPU", "${state.cpuPercent.formatDp(1)}%", mirrorLayout)
            if (settings.showFps) MetricRow("◎", KamperTheme.GREEN, "FPS", "${state.fps} fps", mirrorLayout)
            if (settings.showMemory) MetricRow("▦", KamperTheme.PEACH, "MEM", "${state.memoryUsedMb.formatDp(0)} MB", mirrorLayout)
            if (settings.showNetwork) MetricRow("↓", KamperTheme.TEAL, "NET", netDisplay, mirrorLayout)
        }
    }
}

@Composable
private fun MetricRow(icon: String, color: Color, label: String, value: String, mirror: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(120.dp)
    ) {
        if (!mirror) {
            Text(icon, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(30.dp))
            Spacer(Modifier.weight(1f))
            Text(value, color = KamperTheme.TEXT, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        } else {
            Text(value, color = KamperTheme.TEXT, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(30.dp))
            Text(icon, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
        }
    }
}
