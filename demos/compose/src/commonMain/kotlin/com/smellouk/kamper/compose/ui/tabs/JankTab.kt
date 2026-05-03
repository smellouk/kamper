package com.smellouk.kamper.compose.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.compose.ui.KamperColors
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Composable
fun JankTab(info: JankInfo, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val isUnsupported = info == JankInfo.UNSUPPORTED
    val isValid = info != JankInfo.INVALID && !isUnsupported

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KamperColors.mantle)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    isUnsupported -> "N/A"
                    isValid -> info.droppedFrames.toString()
                    else -> "—"
                },
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isUnsupported) KamperColors.overlay1 else KamperColors.mauve,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (isUnsupported) "not supported on this platform" else "dropped frames / window",
                fontSize = 16.sp,
                color = KamperColors.overlay1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            StatRow(
                label = "Janky ratio",
                value = when {
                    isUnsupported -> "N/A"
                    isValid -> "${(info.jankyFrameRatio * 100f * 10).toInt() / 10.0}%"
                    else -> "—"
                }
            )
            HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)
            StatRow(
                label = "Worst frame",
                value = when {
                    isUnsupported -> "N/A"
                    isValid -> "${info.worstFrameMs} ms"
                    else -> "—"
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KamperColors.mantle)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    Kamper.logEvent("jank_simulate")
                    scope.launch {
                        val end = TimeSource.Monotonic.markNow() + 200.milliseconds
                        while (end.hasNotPassedNow()) {}
                    }
                },
                enabled = !isUnsupported,
                colors = ButtonDefaults.buttonColors(containerColor = KamperColors.surface1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text("SIMULATE JANK", color = KamperColors.text)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = KamperColors.overlay1, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = KamperColors.text,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
