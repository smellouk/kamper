package com.smellouk.kamper.compose.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.compose.ui.KamperColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val palette = listOf(
    KamperColors.blue,
    KamperColors.green,
    KamperColors.yellow,
    KamperColors.peach,
    KamperColors.mauve,
    KamperColors.teal,
)

@Composable
fun FpsTab(info: FpsInfo, modifier: Modifier = Modifier) {
    // Never wraps — matches Android `angle += 0.025` without reset
    var angle by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { angle += 0.025 }
        }
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
                text = if (info == FpsInfo.INVALID) "—" else info.fps.toString(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = KamperColors.blue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "fps",
                fontSize = 16.sp,
                color = KamperColors.overlay1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Same background as Android AnimationView
            drawRect(color = KamperColors.mantle)

            val cx = size.width / 2f
            val cy = size.height / 2f
            val minDim = size.minDimension
            val r1 = minDim * 0.30f
            val r2 = minDim * 0.15f
            val outerDotR = minDim * 0.040f
            val innerDotR = minDim * 0.020f
            val centerDotR = minDim * 0.013f

            // Outer ring — full opacity, rotates forward
            for (i in palette.indices) {
                val a = angle + i * (2.0 * PI / palette.size)
                drawCircle(
                    color = palette[i],
                    radius = outerDotR,
                    center = Offset((cx + r1 * cos(a)).toFloat(), (cy + r1 * sin(a)).toFloat())
                )
            }

            // Inner ring — 70% opacity, rotates backward at 1.5×
            for (i in palette.indices) {
                val a = -angle * 1.5 + i * (2.0 * PI / palette.size)
                val base = palette[(i + 3) % palette.size]
                drawCircle(
                    color = Color(base.red, base.green, base.blue, alpha = 180f / 255f),
                    radius = innerDotR,
                    center = Offset((cx + r2 * cos(a)).toFloat(), (cy + r2 * sin(a)).toFloat())
                )
            }

            // Center dot
            drawCircle(color = KamperColors.surface1, radius = centerDotR, center = Offset(cx, cy))
        }

        Text(
            text = "Choreographer-based frame measurement",
            fontSize = 11.sp,
            color = KamperColors.overlay1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(KamperColors.mantle)
                .padding(vertical = 12.dp)
        )
    }
}
