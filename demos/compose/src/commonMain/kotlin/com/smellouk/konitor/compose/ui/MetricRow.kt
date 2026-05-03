package com.smellouk.konitor.compose.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetricRow(
    label: String,
    fraction: Float,
    valueText: String,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val animFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = label
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = KonitorColors.subtext1,
            fontSize = 13.sp,
            modifier = Modifier.width(90.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KonitorColors.surface1)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animFraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Text(
            text = valueText,
            color = KonitorColors.text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(72.dp)
        )
    }
}
