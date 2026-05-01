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
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.compose.ui.KamperColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GcTab(info: GcInfo, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

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
                    info == GcInfo.UNSUPPORTED -> "N/A"
                    info == GcInfo.INVALID     -> "—"
                    else                       -> info.gcCountDelta.toString()
                },
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = KamperColors.yellow,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "GC events / interval",
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
            GcStatRow(
                label = "GC pause delta",
                value = if (info == GcInfo.INVALID || info == GcInfo.UNSUPPORTED) "—" else "${info.gcPauseMsDelta} ms"
            )
            HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)
            GcStatRow(
                label = "Total GC count",
                value = if (info == GcInfo.INVALID || info == GcInfo.UNSUPPORTED) "—" else info.gcCount.toString()
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
                    scope.launch(Dispatchers.Default) {
                        repeat(200_000) { ByteArray(1024) }
                        gc()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KamperColors.surface1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text("SIMULATE GC", color = KamperColors.text)
            }
        }
    }
}

@Composable
private fun GcStatRow(label: String, value: String) {
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
