package com.smellouk.kamper.ui.compose

// Imports — union of imports needed by every composable copied from KamperPanel.kt,
// IssuesTab.kt, and PerfettoTab.kt. See <interfaces> section of PLAN for source mapping.
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.focusable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Shared internal composables for the Kamper UI panel.
// All composables here are `internal` (NOT `private`) because they are called
// cross-file by ActivityTab.kt, SettingsTab.kt, KamperPanel.kt, IssuesTab.kt,
// and PerfettoTab.kt within the same module.
// ─────────────────────────────────────────────────────────────────────────────

internal val INTERVAL_OPTIONS = listOf(
    500L to "500 ms",
    1_000L to "1 s",
    2_000L to "2 s",
    5_000L to "5 s"
)

// ── 1. ThemeToggle ─────────────────────────────────────────────────────────────

@Composable
internal fun ThemeToggle(isDark: Boolean, onToggle: () -> Unit) {
    val outerShape = RoundedCornerShape(8.dp)
    val innerShape = RoundedCornerShape(6.dp)
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(outerShape)
            .background(KamperTheme.BASE)
            .border(
                if (isFocused) 1.5.dp else 0.5.dp,
                if (isFocused) KamperTheme.BLUE else KamperTheme.BORDER,
                outerShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onToggle)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeSegment(icon = "🌙", label = "Dark",  active = isDark,  shape = innerShape)
        ThemeSegment(icon = "☀",  label = "Light", active = !isDark, shape = innerShape)
    }
}

// ── 2. ThemeSegment ────────────────────────────────────────────────────────────

@Composable
internal fun ThemeSegment(icon: String, label: String, active: Boolean, shape: RoundedCornerShape) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(shape)
            .background(if (active) KamperTheme.SURFACE1 else Color.Transparent)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(icon, fontSize = 10.sp)
        Text(
            label,
            color = if (active) KamperTheme.TEXT else KamperTheme.SUBTEXT,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── 3. PanelTab ────────────────────────────────────────────────────────────────

@Composable
internal fun PanelTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            color = if (selected) KamperTheme.BLUE else KamperTheme.SUBTEXT,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (selected) KamperTheme.BLUE else Color.Transparent)
        )
    }
}

// ── 4. DetectorCard ────────────────────────────────────────────────────────────

@Composable
internal fun DetectorCard(
    name: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KamperTheme.BASE)
            .border(
                0.5.dp,
                if (enabled) KamperTheme.RED.copy(alpha = 0.25f) else KamperTheme.BORDER,
                RoundedCornerShape(8.dp)
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                color = if (enabled) KamperTheme.TEXT else KamperTheme.SUBTEXT,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            KamperSwitch(checked = enabled, color = KamperTheme.RED, onCheckedChange = onEnabledChange)
        }
        if (enabled && content != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KamperTheme.SURFACE1)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

// ── 5. EngineSection ───────────────────────────────────────────────────────────

@Composable
internal fun EngineSection(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ENGINE",
                color = KamperTheme.SUBTEXT,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (running) KamperTheme.GREEN.copy(alpha = 0.15f) else KamperTheme.RED.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    if (running) "● RUNNING" else "● STOPPED",
                    color = if (running) KamperTheme.GREEN else KamperTheme.RED,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EngineButton(
                label = "▶ Start",
                color = KamperTheme.GREEN,
                enabled = !running,
                modifier = Modifier.weight(1f),
                onClick = onStart
            )
            EngineButton(
                label = "⏹ Stop",
                color = KamperTheme.RED,
                enabled = running,
                modifier = Modifier.weight(1f),
                onClick = onStop
            )
            EngineButton(
                label = "↺ Restart",
                color = KamperTheme.BLUE,
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onRestart
            )
        }
    }
}

// ── 6. EngineButton ────────────────────────────────────────────────────────────

@Composable
internal fun EngineButton(
    label: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color,
            disabledContainerColor = KamperTheme.SURFACE.copy(alpha = 0.3f),
            disabledContentColor = KamperTheme.SUBTEXT.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── 7. ModuleCard ──────────────────────────────────────────────────────────────

@Composable
internal fun ModuleCard(
    icon: String,
    name: String,
    color: Color,
    enabled: Boolean,
    showInChip: Boolean,
    intervalMs: Long?,
    intervalOptions: List<Pair<Long, String>>,
    onEnabledChange: (Boolean) -> Unit,
    onShowInChipChange: (Boolean) -> Unit,
    onIntervalChange: (Long) -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var cardHasFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KamperTheme.BASE)
            .border(
                if (cardHasFocus) 1.5.dp else 0.5.dp,
                when {
                    cardHasFocus -> KamperTheme.BLUE
                    enabled      -> color.copy(alpha = 0.3f)
                    else         -> KamperTheme.BORDER
                },
                RoundedCornerShape(10.dp)
            )
            .onFocusChanged { cardHasFocus = it.hasFocus }
            .animateContentSize()
    ) {
        // Header row — expand area and switch are peer focusable elements
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand-clickable area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { if (enabled) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    icon,
                    color = if (enabled) color else KamperTheme.SUBTEXT,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    name,
                    color = if (enabled) KamperTheme.TEXT else KamperTheme.SUBTEXT,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (enabled) {
                    Text(
                        if (expanded) "▲" else "▼",
                        color = KamperTheme.SUBTEXT,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            // Switch is a standalone peer — independently focusable via D-pad
            KamperSwitch(
                checked = enabled,
                color = color,
                onCheckedChange = {
                    onEnabledChange(it)
                    if (!it) expanded = false
                }
            )
        }

        // Expanded content
        if (enabled && expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KamperTheme.SURFACE1)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Show in chip toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Show in chip",
                        color = KamperTheme.SUBTEXT,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    KamperSwitch(
                        checked = showInChip,
                        color = color,
                        onCheckedChange = onShowInChipChange
                    )
                }

                // Interval picker (if applicable)
                if (intervalMs != null && intervalOptions.isNotEmpty()) {
                    OptionRow(
                        label = "Poll interval",
                        options = intervalOptions,
                        selected = intervalMs,
                        onSelect = onIntervalChange
                    )
                }

                extraContent?.invoke()
            }
        }
    }
}

// ── 8. SectionLabel ────────────────────────────────────────────────────────────

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        color = KamperTheme.SUBTEXT,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

// ── 9. KamperSwitch ────────────────────────────────────────────────────────────

@Composable
internal fun KamperSwitch(checked: Boolean, color: Color, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor     = KamperTheme.BASE,
            checkedTrackColor     = color,
            uncheckedThumbColor   = KamperTheme.SUBTEXT,
            uncheckedTrackColor   = KamperTheme.SURFACE,
            uncheckedBorderColor  = KamperTheme.BORDER
        )
    )
}

// ── 10. OptionRow ──────────────────────────────────────────────────────────────

@Composable
internal fun <T> OptionRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = KamperTheme.SUBTEXT, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (value, displayLabel) ->
                val isSelected = value == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isSelected) KamperTheme.BLUE.copy(alpha = 0.2f) else KamperTheme.SURFACE)
                        .border(
                            0.5.dp,
                            if (isSelected) KamperTheme.BLUE else KamperTheme.BORDER,
                            RoundedCornerShape(5.dp)
                        )
                        .clickable { onSelect(value) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        displayLabel,
                        color = if (isSelected) KamperTheme.BLUE else KamperTheme.SUBTEXT,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── From IssuesTab.kt ─────────────────────────────────────────────────────────
// IssueDetailDialog and DetailField copied here as `internal` with private helpers
// (TypeChip body, severityColor, fmtTime) inlined to avoid cross-file private calls.
// Plan 04 will remove the originals from IssuesTab.kt.

// ── 12. IssueDetailDialog (with inlined TypeChip/severityColor/fmtTime) ───────

@Composable
internal fun IssueDetailDialog(issue: Issue, onDismiss: () -> Unit) {
    // Inline severityColor logic (from IssuesTab.kt severityColor fun)
    val sevColor: Color = when (issue.severity) {
        Severity.CRITICAL -> KamperTheme.RED
        Severity.ERROR    -> KamperTheme.PEACH
        Severity.WARNING  -> KamperTheme.YELLOW
        Severity.INFO     -> KamperTheme.GREEN
    }

    // Inline typeColor/typeShortName logic (from IssuesTab.kt typeColor/typeShortName funs)
    val typeChipColor: Color = when (issue.type) {
        IssueType.ANR, IssueType.CRASH                                        -> KamperTheme.RED
        IssueType.SLOW_COLD_START, IssueType.SLOW_WARM_START,
        IssueType.SLOW_HOT_START                                              -> KamperTheme.PEACH
        IssueType.DROPPED_FRAME                                               -> KamperTheme.YELLOW
        IssueType.SLOW_SPAN                                                   -> KamperTheme.BLUE
        IssueType.MEMORY_PRESSURE, IssueType.NEAR_OOM                        -> KamperTheme.MAUVE
        IssueType.STRICT_VIOLATION                                            -> KamperTheme.TEAL
    }
    val typeShortName: String = when (issue.type) {
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

    // Inline fmtTime logic (from IssuesTab.kt fmtTime fun)
    val fmtTime: String = run {
        val sec = (issue.timestampMs / 1000) % 86400
        val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
        "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KamperTheme.SURFACE,
        titleContentColor = KamperTheme.TEXT,
        textContentColor = KamperTheme.SUBTEXT,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Inlined TypeChip body
                Text(
                    typeShortName,
                    color = typeChipColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, typeChipColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    issue.severity.name,
                    color = sevColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailField("Time", fmtTime)
                DetailField("Message", issue.message)
                issue.durationMs?.let { DetailField("Duration", "${it}ms") }
                issue.threadName?.let { DetailField("Thread", it) }
                if (issue.details.isNotEmpty()) {
                    issue.details.entries.forEach { (k, v) -> DetailField(k, v) }
                }
                val stackTrace = issue.stackTrace
                if (!stackTrace.isNullOrBlank()) {
                    Text(
                        "Stack Trace",
                        color = KamperTheme.SUBTEXT,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(KamperTheme.BASE)
                            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            stackTrace,
                            color = KamperTheme.TEXT,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = KamperTheme.BLUE)
            }
        }
    )
}

// ── 13. DetailField ────────────────────────────────────────────────────────────

@Composable
internal fun DetailField(label: String, value: String) {
    Column {
        Text(label, color = KamperTheme.SUBTEXT, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = KamperTheme.TEXT, fontSize = 12.sp)
    }
}

// ── From PerfettoTab.kt ───────────────────────────────────────────────────────
// RecordingBadge, GuideStep, StepBadge copied here as `internal`.
// Plan 04 will remove the originals from PerfettoTab.kt.

// ── 14. RecordingBadge ─────────────────────────────────────────────────────────

@Composable
internal fun RecordingBadge(
    isRecording: Boolean,
    sampleCount: Int,
    maxRecordingSamples: Int
) {
    val warningSampleCount = maxRecordingSamples * 9 / 10
    val isWarning = isRecording && sampleCount >= warningSampleCount

    var elapsed by remember { mutableStateOf(0) }
    LaunchedEffect(isRecording) {
        elapsed = 0
        if (isRecording) {
            while (true) { delay(1_000); elapsed++ }
        }
    }

    AnimatedContent(
        targetState = isRecording,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "recording_badge"
    ) { recording ->
        val badgeColor = when {
            recording && isWarning -> KamperTheme.WARNING
            recording              -> KamperTheme.RED
            else                   -> KamperTheme.SUBTEXT
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (recording) badgeColor.copy(alpha = 0.15f)
                    else KamperTheme.SURFACE.copy(alpha = 0.6f)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (recording) badgeColor else KamperTheme.SUBTEXT)
            )
            Spacer(Modifier.width(5.dp))
            if (recording) {
                val m = elapsed / 60
                val s = elapsed % 60
                val mm = m.toString().padStart(2, '0')
                val ss = s.toString().padStart(2, '0')
                Column {
                    Text(
                        "REC  $mm:$ss  $sampleCount pts",
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isWarning) {
                        val pct = sampleCount * 100 / maxRecordingSamples
                        Text(
                            "⚠ Buffer ${pct}% full",
                            color = KamperTheme.WARNING,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                Text(
                    if (sampleCount > 0) "STOPPED  $sampleCount pts" else "IDLE",
                    color = KamperTheme.SUBTEXT,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ── 15. GuideStep ──────────────────────────────────────────────────────────────

@Composable
internal fun GuideStep(number: String, label: String, cmd: String) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepBadge(number)
            Spacer(Modifier.width(8.dp))
            Text(label, color = KamperTheme.TEXT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(KamperTheme.SURFACE)
                .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                cmd,
                color = KamperTheme.GREEN,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Copy",
                color = KamperTheme.BLUE,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(KamperTheme.BLUE.copy(alpha = 0.12f))
                    .clickable { clipboard.setText(AnnotatedString(cmd)) }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

// ── 16. StepBadge ──────────────────────────────────────────────────────────────

@Composable
internal fun StepBadge(number: String) {
    Text(
        number,
        color = KamperTheme.BLUE,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(KamperTheme.BLUE.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ── 11. MetricCard ─────────────────────────────────────────────────────────────

@Composable
internal fun MetricCard(
    title: String,
    current: String,
    fraction: Float,
    color: Color,
    history: List<Float>,
    extra: String?,
    dimmed: Boolean = false,
    unsupported: Boolean = false
) {
    val tint = when {
        unsupported -> KamperTheme.SUBTEXT
        dimmed      -> color.copy(alpha = 0.4f)
        else        -> color
    }
    val textColor = when {
        unsupported -> KamperTheme.SUBTEXT
        dimmed      -> KamperTheme.SUBTEXT
        else        -> KamperTheme.TEXT
    }
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KamperTheme.BASE)
            .border(
                if (isFocused) 1.5.dp else 0.5.dp,
                if (isFocused) KamperTheme.BLUE else KamperTheme.BORDER,
                RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                title,
                color = tint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                current,
                color = textColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (extra != null) {
            Text(extra, color = KamperTheme.SUBTEXT, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KamperTheme.SURFACE)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tint)
            )
        }

        if (history.size >= 2) {
            Spacer(Modifier.height(8.dp))
            Sparkline(
                data = history,
                color = tint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}
