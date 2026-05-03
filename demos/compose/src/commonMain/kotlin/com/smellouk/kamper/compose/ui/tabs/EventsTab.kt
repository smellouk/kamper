package com.smellouk.kamper.compose.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.api.UserEventInfo
import com.smellouk.kamper.compose.ui.KamperColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EventEntry(val info: UserEventInfo, val wallClockMs: Long)

@Composable
fun EventsTab(
    userEvents: List<EventEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customName by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        if (userEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events logged",
                    color = KamperColors.overlay1,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(userEvents) { entry ->
                    EventRow(entry)
                    HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)
                }
            }
        }

        HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KamperColors.mantle)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EventButton("user_login", KamperColors.blue)    { Kamper.logEvent("user_login") }
            Spacer(Modifier.width(8.dp))
            EventButton("purchase", KamperColors.blue)      { Kamper.logEvent("purchase") }
            Spacer(Modifier.width(8.dp))
            EventButton("screen_view", KamperColors.blue)   { Kamper.logEvent("screen_view") }
            Spacer(Modifier.width(8.dp))
            EventButton(if (recording) "Recording…" else "video_playback", KamperColors.blue, enabled = !recording) {
                recording = true
                scope.launch {
                    val token = Kamper.startEvent("video_playback")
                    delay(2000)
                    Kamper.endEvent(token)
                    recording = false
                }
            }
            Spacer(Modifier.width(8.dp))
            EventButton("Clear", KamperColors.overlay1)     { onClear() }
        }

        HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KamperColors.mantle)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(KamperColors.surface0, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (customName.isEmpty()) {
                    Text(
                        text = "custom event name…",
                        color = KamperColors.overlay1,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                BasicTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    textStyle = TextStyle(
                        color = KamperColors.text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(KamperColors.text),
                    singleLine = true
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val trimmed = customName.trim()
                    if (trimmed.isNotEmpty()) {
                        Kamper.logEvent(trimmed)
                        customName = ""
                    }
                },
                enabled = customName.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = KamperColors.surface0),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "LOG", color = KamperColors.blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EventButton(label: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = KamperColors.surface0),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun fmtWallClock(ms: Long): String {
    val sec = (ms / 1000) % 86400
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

@Composable
private fun EventRow(entry: EventEntry) {
    val event = entry.info
    val barColor = if (event.durationMs != null) KamperColors.blue else KamperColors.green

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KamperColors.bg)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.name,
                color = KamperColors.text,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            event.durationMs?.let { dur ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${dur}ms",
                    color = KamperColors.blue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Text(
            text = fmtWallClock(entry.wallClockMs),
            color = KamperColors.overlay1,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
