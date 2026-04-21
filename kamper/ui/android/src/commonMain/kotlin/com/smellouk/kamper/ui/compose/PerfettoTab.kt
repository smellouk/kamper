package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val CMD_RECORD = "adb shell perfetto -o /data/misc/perfetto-traces/trace.perfetto-trace -t 10s sched freq am wm view app"
private const val CMD_PULL   = "adb pull /data/misc/perfetto-traces/trace.perfetto-trace"
private const val URL_UI     = "https://ui.perfetto.dev"

@Composable
internal fun PerfettoTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Perfetto is a system-level profiler. Run these commands from your machine while the app is running.",
            color = KamperTheme.SUBTEXT,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        GuideStep(
            number = "1",
            label  = "Start a 10-second trace",
            cmd    = CMD_RECORD
        )
        GuideStep(
            number = "2",
            label  = "Pull the trace file",
            cmd    = CMD_PULL
        )

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
                Text(
                    "3",
                    color = KamperTheme.BLUE,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(KamperTheme.BLUE.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.padding(4.dp))
                Text("Open in Perfetto UI", color = KamperTheme.TEXT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(URL_UI, color = KamperTheme.TEAL, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

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
            Spacer(Modifier.padding(4.dp))
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
            Spacer(Modifier.padding(6.dp))
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
        Spacer(Modifier.height(0.dp))
    }
}
