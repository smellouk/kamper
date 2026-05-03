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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.compose.triggerCrash
import com.smellouk.kamper.compose.triggerSlowSpan
import com.smellouk.kamper.compose.ui.KamperColors

@Composable
fun IssuesTab(
    issues: List<Issue>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (issues.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No issues detected",
                    color = KamperColors.overlay1,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(issues, key = { it.id }) { issue ->
                    IssueRow(issue)
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
            TriggerButton("Slow Span", KamperColors.blue)  {
                Kamper.logEvent("issue_slow_span")
                triggerSlowSpan()
            }
            Spacer(Modifier.width(8.dp))
            TriggerButton("Crash", KamperColors.red)       {
                Kamper.logEvent("issue_crash_trigger")
                triggerCrash()
            }
            Spacer(Modifier.width(8.dp))
            TriggerButton("Clear", KamperColors.overlay1)  {
                Kamper.logEvent("issues_clear")
                onClear()
            }
        }
    }
}

@Composable
private fun TriggerButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = KamperColors.surface0),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IssueRow(issue: Issue) {
    val severityColor = severityColor(issue.severity)
    val typeColor = typeColor(issue.type)

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
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(severityColor)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeChip(typeShortName(issue.type), typeColor)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = issue.severity.name,
                    color = severityColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = fmtTime(issue.timestampMs),
                    color = KamperColors.overlay1,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(text = issue.message, color = KamperColors.text, fontSize = 12.sp, maxLines = 2)
            val details = buildDetails(issue)
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    color = KamperColors.overlay1,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun severityColor(s: Severity): Color = when (s) {
    Severity.CRITICAL -> Color(0xFFF38BA8)
    Severity.ERROR    -> Color(0xFFFAB387)
    Severity.WARNING  -> Color(0xFFF9E2AF)
    Severity.INFO     -> Color(0xFFA6E3A1)
}

private fun typeColor(t: IssueType): Color = when (t) {
    IssueType.ANR, IssueType.CRASH             -> Color(0xFFF38BA8)
    IssueType.SLOW_COLD_START,
    IssueType.SLOW_WARM_START,
    IssueType.SLOW_HOT_START                   -> Color(0xFFFAB387)
    IssueType.DROPPED_FRAME                    -> Color(0xFFF9E2AF)
    IssueType.SLOW_SPAN                        -> Color(0xFF89B4FA)
    IssueType.MEMORY_PRESSURE, IssueType.NEAR_OOM -> Color(0xFFCBA6F7)
    IssueType.STRICT_VIOLATION                 -> Color(0xFF94E2D5)
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

private fun buildDetails(issue: Issue): String {
    val parts = mutableListOf<String>()
    issue.durationMs?.let { parts.add("${it}ms") }
    issue.threadName?.let { parts.add("thread=$it") }
    issue.details.entries.take(2).forEach { parts.add("${it.key}=${it.value}") }
    return parts.joinToString("  ·  ")
}

private fun fmtTime(ms: Long): String {
    val sec = (ms / 1000) % 86400
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
