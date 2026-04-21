package com.smellouk.kamper.samples.compose.ui.tabs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.samples.compose.ui.KamperColors

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = KamperColors.text,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
