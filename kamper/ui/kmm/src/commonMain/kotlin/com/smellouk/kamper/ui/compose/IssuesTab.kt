package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity

@Composable
internal fun IssuesTab(issues: List<Issue>, onClear: () -> Unit) {
    var selected by remember { mutableStateOf<Issue?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${issues.size} issue${if (issues.size == 1) "" else "s"}",
            color = KamperTheme.SUBTEXT,
            fontSize = 11.sp
        )
        Spacer(Modifier.weight(1f))
        if (issues.isNotEmpty()) {
            var clearFocused by remember { mutableStateOf(false) }
            Text(
                "Clear",
                color = KamperTheme.RED,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (clearFocused) KamperTheme.RED.copy(alpha = 0.15f) else Color.Transparent)
                    .onFocusChanged { clearFocused = it.isFocused }
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    if (issues.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No issues detected", color = KamperTheme.SUBTEXT, fontSize = 13.sp)
        }
    } else {
        val grouped = IssueType.entries.mapNotNull { type ->
            val group = issues.filter { it.type == type }
            if (group.isEmpty()) null else type to group
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            grouped.forEach { (type, group) ->
                IssueGroupHeader(type)
                Spacer(Modifier.height(4.dp))
                group.forEach { issue ->
                    IssueRow(issue, onClick = { selected = issue })
                    Spacer(Modifier.height(3.dp))
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    selected?.let { issue ->
        IssueDetailDialog(issue = issue, onDismiss = { selected = null })
    }
}

@Composable
private fun IssueGroupHeader(type: IssueType) {
    val color = typeColor(type)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            typeDisplayName(type),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IssueRow(issue: Issue, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(52.dp)
                .background(severityColor(issue.severity))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeChip(issue.type)
                Spacer(Modifier.width(6.dp))
                Text(
                    issue.severity.name,
                    color = severityColor(issue.severity),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    fmtTime(issue.timestampMs),
                    color = KamperTheme.SUBTEXT,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                issue.message,
                color = KamperTheme.TEXT,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TypeChip(type: IssueType) {
    val color = typeColor(type)
    Text(
        typeShortName(type),
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(0.5.dp, color, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

// IssueDetailDialog and DetailField moved to PanelComponents.kt as internal fun

@Composable
private fun severityColor(s: Severity): Color = when (s) {
    Severity.CRITICAL -> KamperTheme.RED
    Severity.ERROR    -> KamperTheme.PEACH
    Severity.WARNING  -> KamperTheme.YELLOW
    Severity.INFO     -> KamperTheme.GREEN
}

@Composable
private fun typeColor(t: IssueType): Color = when (t) {
    IssueType.ANR, IssueType.CRASH                                        -> KamperTheme.RED
    IssueType.SLOW_COLD_START, IssueType.SLOW_WARM_START,
    IssueType.SLOW_HOT_START                                              -> KamperTheme.PEACH
    IssueType.DROPPED_FRAME                                               -> KamperTheme.YELLOW
    IssueType.SLOW_SPAN                                                   -> KamperTheme.BLUE
    IssueType.MEMORY_PRESSURE, IssueType.NEAR_OOM                        -> KamperTheme.MAUVE
    IssueType.STRICT_VIOLATION                                            -> KamperTheme.TEAL
}

private fun typeShortName(t: IssueType): String = when (t) {
    IssueType.ANR              -> "ANR"
    IssueType.SLOW_COLD_START  -> "COLD"
    IssueType.SLOW_WARM_START  -> "WARM"
    IssueType.SLOW_HOT_START   -> "HOT"
    IssueType.DROPPED_FRAME    -> "JANK"
    IssueType.SLOW_SPAN        -> "SPAN"
    IssueType.MEMORY_PRESSURE  -> "MEM"
    IssueType.NEAR_OOM         -> "OOM"
    IssueType.CRASH            -> "CRASH"
    IssueType.STRICT_VIOLATION -> "STRICT"
}

private fun typeDisplayName(t: IssueType): String = when (t) {
    IssueType.ANR              -> "ANR — Application Not Responding"
    IssueType.SLOW_COLD_START  -> "Slow Cold Start"
    IssueType.SLOW_WARM_START  -> "Slow Warm Start"
    IssueType.SLOW_HOT_START   -> "Slow Hot Start"
    IssueType.DROPPED_FRAME    -> "Dropped Frames (Jank)"
    IssueType.SLOW_SPAN        -> "Slow Spans"
    IssueType.MEMORY_PRESSURE  -> "Memory Pressure"
    IssueType.NEAR_OOM         -> "Near Out of Memory"
    IssueType.CRASH            -> "Crashes"
    IssueType.STRICT_VIOLATION -> "Strict Mode Violations"
}

private fun fmtTime(ms: Long): String {
    val sec = (ms / 1000) % 86400
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
}
