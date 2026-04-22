package com.smellouk.kamper.compose.ui.tabs

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.compose.ui.KamperColors
import com.smellouk.kamper.compose.ui.MetricRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun CpuTab(info: CpuInfo, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var isStressRunning by remember { mutableStateOf(false) }
    var stressJob by remember { mutableStateOf<Job?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        SectionTitle("CPU Usage")

        MetricRow("Total", info.totalUseRatio.toFloat(), "${(info.totalUseRatio * 100).toInt()}%", KamperColors.blue)
        Spacer(Modifier.height(12.dp))
        MetricRow("App", info.appRatio.toFloat(), "${(info.appRatio * 100).toInt()}%", KamperColors.green)
        Spacer(Modifier.height(12.dp))
        MetricRow("User", info.userRatio.toFloat(), "${(info.userRatio * 100).toInt()}%", KamperColors.yellow)
        Spacer(Modifier.height(12.dp))
        MetricRow("System", info.systemRatio.toFloat(), "${(info.systemRatio * 100).toInt()}%", KamperColors.peach)
        Spacer(Modifier.height(12.dp))
        MetricRow("IO Wait", info.ioWaitRatio.toFloat(), "${(info.ioWaitRatio * 100).toInt()}%", KamperColors.mauve)

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (!isStressRunning) {
                        isStressRunning = true
                        stressJob = scope.launch(Dispatchers.Default) {
                            var s = 0.0
                            while (isActive) {
                                repeat(100_000) { i -> s += sqrt(i.toDouble()) }
                            }
                        }
                    } else {
                        stressJob?.cancel()
                        stressJob = null
                        isStressRunning = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KamperColors.surface1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = if (isStressRunning) "STOP CPU LOAD" else "START CPU LOAD",
                    color = KamperColors.text
                )
            }
        }
    }
}
