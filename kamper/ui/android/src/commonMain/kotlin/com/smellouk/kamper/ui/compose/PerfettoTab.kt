package com.smellouk.kamper.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val CMD_RECORD =
    "adb shell perfetto -o /data/misc/perfetto-traces/trace.perfetto-trace -t 10s sched freq am wm view app"
private const val CMD_PULL = "adb pull /data/misc/perfetto-traces/trace.perfetto-trace"
private const val URL_UI = "https://ui.perfetto.dev"

@Composable
internal fun PerfettoTab(
    isRecording: Boolean,
    sampleCount: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onExportTrace: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

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
                RecordingBadge(isRecording = isRecording, sampleCount = sampleCount)
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

        // ── Manual (adb) section ──────────────────────────────────────────────

        Text(
            "MANUAL (ADB)",
            color = KamperTheme.SUBTEXT,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "Run a system-level trace directly from your machine while the app is running.",
            color = KamperTheme.SUBTEXT,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        GuideStep(number = "1", label = "Start a 10-second trace", cmd = CMD_RECORD)
        GuideStep(number = "2", label = "Pull the trace file", cmd = CMD_PULL)
        Column(
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

// ── Recording badge with live elapsed timer ───────────────────────────────────

@Composable
private fun RecordingBadge(isRecording: Boolean, sampleCount: Int) {
    var elapsed by remember { mutableStateOf(0) }
    LaunchedEffect(isRecording) {
        elapsed = 0
        if (isRecording) {
            while (true) { delay(1_000); elapsed++ }
        }
    }

    AnimatedContent(
        targetState = isRecording,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "recording_badge"
    ) { recording ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (recording) KamperTheme.RED.copy(alpha = 0.15f)
                    else KamperTheme.SURFACE.copy(alpha = 0.6f)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (recording) KamperTheme.RED else KamperTheme.SUBTEXT)
            )
            Spacer(Modifier.width(5.dp))
            if (recording) {
                val m = elapsed / 60
                val s = elapsed % 60
                val mm = m.toString().padStart(2, '0')
                val ss = s.toString().padStart(2, '0')
                Text(
                    "REC  $mm:$ss  $sampleCount pts",
                    color = KamperTheme.RED,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    if (sampleCount > 0) "STOPPED  $sampleCount pts" else "IDLE",
                    color = KamperTheme.SUBTEXT,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ── Guide step with copy button ───────────────────────────────────────────────

@Composable
private fun GuideStep(number: String, label: String, cmd: String) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepBadge(number)
            Spacer(Modifier.width(8.dp))
            Text(label, color = KamperTheme.TEXT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(KamperTheme.SURFACE)
                .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                cmd,
                color = KamperTheme.GREEN,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Copy",
                color = KamperTheme.BLUE,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(KamperTheme.BLUE.copy(alpha = 0.12f))
                    .clickable { clipboard.setText(AnnotatedString(cmd)) }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun StepBadge(number: String) {
    Text(
        number,
        color = KamperTheme.BLUE,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(KamperTheme.BLUE.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
