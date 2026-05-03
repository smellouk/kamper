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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import com.smellouk.kamper.compose.ui.KamperColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ThermalTab(info: ThermalInfo, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var isStressRunning by remember { mutableStateOf(false) }
    var stressJob by remember { mutableStateOf<Job?>(null) }

    val stateColor = when (info.state) {
        ThermalState.NONE, ThermalState.LIGHT -> KamperColors.green
        ThermalState.MODERATE                 -> KamperColors.yellow
        ThermalState.SEVERE,
        ThermalState.CRITICAL,
        ThermalState.EMERGENCY,
        ThermalState.SHUTDOWN                 -> KamperColors.peach
        ThermalState.UNKNOWN,
        ThermalState.UNSUPPORTED              -> KamperColors.overlay1
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KamperColors.mantle)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = info.state.name,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = stateColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "thermal state",
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
            ThermalStatRow(
                label = "Temperature",
                value = when {
                    info == ThermalInfo.INVALID -> "—"
                    info.temperatureC >= 0 ->
                        (info.temperatureC * 10).roundToInt().let { "${it / 10}.${it % 10} °C" }
                    else -> when (info.state) {
                        ThermalState.NONE        -> "< 60 °C"
                        ThermalState.LIGHT       -> "60 – 75 °C"
                        ThermalState.MODERATE    -> "75 – 85 °C"
                        ThermalState.SEVERE      -> "85 – 95 °C"
                        ThermalState.CRITICAL,
                        ThermalState.EMERGENCY,
                        ThermalState.SHUTDOWN    -> "> 95 °C"
                        else                     -> "—"
                    }
                }
            )
            HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)
            ThermalStatRow(
                label = "Throttling",
                value = if (info == ThermalInfo.INVALID) "—" else if (info.isThrottling) "YES" else "NO"
            )
            HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)
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
                    if (!isStressRunning) {
                        Kamper.logEvent("thermal_stress_start")
                        isStressRunning = true
                        stressJob = scope.launch(Dispatchers.Default) {
                            var s = 0.0
                            while (isActive) {
                                repeat(100_000) { i -> s += sqrt(i.toDouble()) }
                            }
                        }
                    } else {
                        Kamper.logEvent("thermal_stress_stop")
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
                    text = if (isStressRunning) "STOP CPU STRESS" else "START CPU STRESS",
                    color = KamperColors.text
                )
            }
        }
    }
}

@Composable
private fun ThermalStatRow(label: String, value: String) {
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
