package com.smellouk.konitor.compose.ui.tabs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.konitor.compose.ui.KonitorColors

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = KonitorColors.text,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
