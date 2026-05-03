package com.smellouk.konitor.compose.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.compose.ui.KonitorColors
import com.smellouk.konitor.gpu.GpuInfo
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.random.Random

private const val CIRCLES_PER_FRAME = 800
private const val CIRCLE_MAX_RADIUS = 180f
private const val CIRCLE_MIN_RADIUS = 20f
private const val CIRCLE_ALPHA = 0.7f
private val STRESS_BOX_BG = Color(0xFF0D0D1A)

@Composable
fun GpuTab(info: GpuInfo, modifier: Modifier = Modifier) {
    var isStressing by remember { mutableStateOf(false) }
    var frameNanos by remember { mutableStateOf(0L) }

    LaunchedEffect(isStressing) {
        if (!isStressing) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { frameNanos = it }
        }
    }

    val heroText: String
    val heroColor: Color
    when {
        info == GpuInfo.UNSUPPORTED -> {
            heroText = "UNSUPPORTED"
            heroColor = KonitorColors.overlay1
        }
        info == GpuInfo.INVALID || info.utilization < 0.0 -> {
            heroText = "—%"
            heroColor = KonitorColors.overlay1
        }
        else -> {
            val tenths = (info.utilization * 10).roundToInt()
            heroText = "${tenths / 10}.${tenths % 10}%"
            heroColor = KonitorColors.mauve
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KonitorColors.mantle)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = heroText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = heroColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "GPU usage %",
                fontSize = 16.sp,
                color = KonitorColors.overlay1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left: stat rows
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                GpuStatRow(label = "Memory",        value = memoryRowText(info))
                HorizontalDivider(color = KonitorColors.surface0, thickness = 1.dp)
                GpuStatRow(label = "App Util",      value = utilRowText(info.appUtilization))
                HorizontalDivider(color = KonitorColors.surface0, thickness = 1.dp)
                GpuStatRow(label = "Renderer Util", value = utilRowText(info.rendererUtilization))
                HorizontalDivider(color = KonitorColors.surface0, thickness = 1.dp)
                GpuStatRow(label = "Tiler Util",    value = utilRowText(info.tilerUtilization))
                HorizontalDivider(color = KonitorColors.surface0, thickness = 1.dp)
                GpuStatRow(label = "Compute Util",  value = utilRowText(info.computeUtilization))
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(KonitorColors.surface0)
            )

            // Right: stress rendering panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(STRESS_BOX_BG),
                    contentAlignment = Alignment.Center
                ) {
                    if (isStressing) {
                        val capturedNanos = frameNanos
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val rng = Random(capturedNanos)
                            repeat(CIRCLES_PER_FRAME) {
                                drawCircle(
                                    color = Color(
                                        rng.nextFloat(), rng.nextFloat(), rng.nextFloat(), CIRCLE_ALPHA
                                    ),
                                    radius = rng.nextFloat() * CIRCLE_MAX_RADIUS + CIRCLE_MIN_RADIUS,
                                    center = Offset(
                                        rng.nextFloat() * size.width, rng.nextFloat() * size.height
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "press STRESS GPU\nto start rendering",
                            color = KonitorColors.overlay1,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (isStressing) Konitor.logEvent("gpu_stress_stop")
                        else Konitor.logEvent("gpu_stress_start")
                        isStressing = !isStressing
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KonitorColors.surface1),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isStressing) "STOP STRESS" else "STRESS GPU", color = KonitorColors.text)
                }
            }
        }
    }
}

private fun utilRowText(value: Double): String = when {
    value >= 0.0 -> {
        val tenths = (value * 10).roundToInt()
        "${tenths / 10}.${tenths % 10}%"
    }
    else -> "N/A"
}

private fun memoryRowText(info: GpuInfo): String = when {
    info == GpuInfo.UNSUPPORTED || info == GpuInfo.INVALID -> "N/A"
    info.usedMemoryMb >= 0.0 && info.totalMemoryMb >= 0.0 ->
        "${info.usedMemoryMb.roundToInt()} MB / ${info.totalMemoryMb.roundToInt()} MB"
    info.totalMemoryMb >= 0.0 -> "— / ${info.totalMemoryMb.roundToInt()} MB"
    else -> "N/A"
}

@Composable
private fun GpuStatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = KonitorColors.overlay1, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = KonitorColors.text,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
