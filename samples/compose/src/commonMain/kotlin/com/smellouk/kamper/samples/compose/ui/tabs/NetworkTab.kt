package com.smellouk.kamper.samples.compose.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.samples.compose.performNetworkTest
import com.smellouk.kamper.samples.compose.ui.KamperColors
import com.smellouk.kamper.samples.compose.ui.MetricRow
import kotlinx.coroutines.launch

@Composable
fun NetworkTab(info: NetworkInfo, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStatus by remember { mutableStateOf("") }

    var peakRx by remember { mutableFloatStateOf(0f) }
    var peakTx by remember { mutableFloatStateOf(0f) }

    val isValid = info != NetworkInfo.INVALID && info != NetworkInfo.NOT_SUPPORTED
    if (isValid) {
        if (info.rxSystemTotalInMb > peakRx) peakRx = info.rxSystemTotalInMb
        if (info.txSystemTotalInMb > peakTx) peakTx = info.txSystemTotalInMb
    }
    val scale = maxOf(peakRx, peakTx, 1f)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        SectionTitle("System Traffic  (per interval)")

        val rxMb = if (isValid) info.rxSystemTotalInMb else 0f
        val txMb = if (isValid) info.txSystemTotalInMb else 0f
        MetricRow(
            label = "Download",
            fraction = (rxMb / scale).coerceIn(0f, 1f),
            valueText = if (isValid) rxMb.formatSpeed() else "—",
            barColor = KamperColors.teal
        )
        if (isValid) {
            Text(
                text = "${rxMb.fmt3()} MB/interval   peak ${peakRx.fmt2()} MB",
                fontSize = 11.sp,
                color = KamperColors.overlay1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        MetricRow(
            label = "Upload",
            fraction = (txMb / scale).coerceIn(0f, 1f),
            valueText = if (isValid) txMb.formatSpeed() else "—",
            barColor = KamperColors.mauve
        )
        if (isValid) {
            Text(
                text = "${txMb.fmt3()} MB/interval   peak ${peakTx.fmt2()} MB",
                fontSize = 11.sp,
                color = KamperColors.overlay1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("App Traffic  (Android)")

        val appRxMb = if (isValid) info.rxAppInMb.coerceAtLeast(0f) else 0f
        val appTxMb = if (isValid) info.txAppInMb.coerceAtLeast(0f) else 0f
        val appScale = maxOf(appRxMb, appTxMb, 0.1f)
        MetricRow(
            label = "Rx App",
            fraction = (appRxMb / appScale).coerceIn(0f, 1f),
            valueText = if (isValid) appRxMb.formatSpeed() else "—",
            barColor = KamperColors.teal
        )
        if (isValid) {
            Text(
                text = "${appRxMb.fmt3()} MB/interval",
                fontSize = 11.sp,
                color = KamperColors.overlay1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        MetricRow(
            label = "Tx App",
            fraction = (appTxMb / appScale).coerceIn(0f, 1f),
            valueText = if (isValid) appTxMb.formatSpeed() else "—",
            barColor = KamperColors.mauve
        )
        if (isValid) {
            Text(
                text = "${appTxMb.fmt3()} MB/interval",
                fontSize = 11.sp,
                color = KamperColors.overlay1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        if (downloadStatus.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(text = downloadStatus, fontSize = 12.sp, color = KamperColors.overlay1)
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (!isDownloading) {
                        scope.launch {
                            isDownloading = true
                            performNetworkTest { status -> downloadStatus = status }
                            isDownloading = false
                        }
                    }
                },
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = KamperColors.surface1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = if (isDownloading) "FETCHING 5 MB…" else "TEST DOWNLOAD",
                    color = KamperColors.text,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun Float.formatSpeed(): String = when {
    this >= 1f -> "${fmt2()} MB"
    this >= 0.01f -> "${(this * 1024).toInt()} KB"
    else -> "< 10 KB"
}

private fun Float.fmt2(): String {
    val v = (this * 100f).toInt().coerceAtLeast(0)
    val frac = v % 100
    return "${v / 100}.${(frac / 10)}${(frac % 10)}"
}

private fun Float.fmt3(): String {
    val v = (this * 1000f).toInt().coerceAtLeast(0)
    val frac = v % 1000
    return "${v / 1000}.${(frac / 100)}${(frac / 10) % 10}${(frac % 10)}"
}
