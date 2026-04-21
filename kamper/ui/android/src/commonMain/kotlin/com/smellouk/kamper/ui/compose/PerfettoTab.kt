package com.smellouk.kamper.ui.compose

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.ui.KamperUiState
import kotlinx.coroutines.delay

@Composable
internal fun PerfettoTab(
    state: KamperUiState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onShareTrace: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Controls row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isRecordingTrace) {
                RecordingBadge()
                Spacer(Modifier.weight(1f))
                PerfettoButton(
                    label = "Stop",
                    color = KamperTheme.RED,
                    onClick = onStopCapture
                )
            } else {
                PerfettoButton(
                    label = "Start",
                    color = KamperTheme.RED,
                    dotShape = false,
                    onClick = onStartCapture
                )
                Spacer(Modifier.weight(1f))
                if (onShareTrace != null && state.traceFilePath != null) {
                    PerfettoButton(
                        label = "Share",
                        color = KamperTheme.TEAL,
                        dotShape = false,
                        onClick = onShareTrace
                    )
                }
            }
        }

        when {
            state.traceSpans.isNotEmpty() -> {
                FlameChart(spans = state.traceSpans)
            }
            state.isRecordingTrace -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Recording…", color = KamperTheme.SUBTEXT, fontSize = 13.sp)
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KamperTheme.BASE)
                        .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Press Start to record an ATrace capture",
                        color = KamperTheme.SUBTEXT,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingBadge() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            visible = !visible
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (visible) KamperTheme.RED else KamperTheme.SURFACE1)
        )
        Text(
            "Recording…",
            color = KamperTheme.RED,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PerfettoButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    dotShape: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dotShape) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
