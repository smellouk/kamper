package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val CMD_RECORD =
    "adb shell perfetto -o /data/misc/perfetto-traces/trace.perfetto-trace -t 10s sched freq am wm view app"
private const val CMD_PULL = "adb pull /data/misc/perfetto-traces/trace.perfetto-trace"
private const val URL_UI = "https://ui.perfetto.dev"

@Composable
internal fun PerfettoTab(
    isRecording: Boolean,
    sampleCount: Int,
    maxRecordingSamples: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onExportTrace: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Automatic section ─────────────────────────────────────────────────

        Text(
            "AUTOMATIC",
            color = KamperTheme.SUBTEXT,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "Kamper records performance metrics directly from the app. " +
            "Hit Record, use your app, then export a .perfetto-trace file " +
            "you can open at ui.perfetto.dev — no ADB or Android Studio required.",
            color = KamperTheme.SUBTEXT,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        // ── Recorder card ─────────────────────────────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KamperTheme.BASE)
                .border(
                    0.5.dp,
                    if (isRecording) KamperTheme.RED.copy(alpha = 0.5f) else KamperTheme.BORDER,
                    RoundedCornerShape(10.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RECORDER",
                    color = KamperTheme.SUBTEXT,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                RecordingBadge(
                    isRecording = isRecording,
                    sampleCount = sampleCount,
                    maxRecordingSamples = maxRecordingSamples
                )
            }

            // Control buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartRecording,
                    enabled = !isRecording,
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KamperTheme.RED.copy(alpha = 0.2f),
                        contentColor = KamperTheme.RED,
                        disabledContainerColor = KamperTheme.SURFACE.copy(alpha = 0.3f),
                        disabledContentColor = KamperTheme.SUBTEXT.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("● Record", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onStopRecording,
                    enabled = isRecording,
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KamperTheme.SURFACE.copy(alpha = 0.6f),
                        contentColor = KamperTheme.TEXT,
                        disabledContainerColor = KamperTheme.SURFACE.copy(alpha = 0.3f),
                        disabledContentColor = KamperTheme.SUBTEXT.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("■ Stop", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Export button
            Button(
                onClick = onExportTrace,
                enabled = !isRecording && sampleCount > 0,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KamperTheme.BLUE.copy(alpha = 0.2f),
                    contentColor = KamperTheme.BLUE,
                    disabledContainerColor = KamperTheme.SURFACE.copy(alpha = 0.3f),
                    disabledContentColor = KamperTheme.SUBTEXT.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    if (sampleCount > 0) "Export .perfetto-trace  ($sampleCount samples)"
                    else "Export .perfetto-trace",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Manual (adb) section — Android only ──────────────────────────────

        if (showAdbGuide) Text(
            "MANUAL (ADB)",
            color = KamperTheme.SUBTEXT,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (showAdbGuide) Text(
            "Run a system-level trace directly from your machine while the app is running.",
            color = KamperTheme.SUBTEXT,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        if (showAdbGuide) GuideStep(number = "1", label = "Start a 10-second trace", cmd = CMD_RECORD)
        if (showAdbGuide) GuideStep(number = "2", label = "Pull the trace file", cmd = CMD_PULL)
        if (showAdbGuide) Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(KamperTheme.BASE)
                .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepBadge("3")
                Spacer(Modifier.width(8.dp))
                Text("Open in Perfetto UI", color = KamperTheme.TEXT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(URL_UI, color = KamperTheme.TEAL, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

internal expect val showAdbGuide: Boolean

// RecordingBadge, GuideStep, StepBadge moved to PanelComponents.kt as internal fun
