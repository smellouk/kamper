package com.smellouk.kamper.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.kamper.ui.KamperUiSettings
import com.smellouk.kamper.ui.KamperUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun KamperPanel(
    state: StateFlow<KamperUiState>,
    settings: StateFlow<KamperUiSettings>,
    isRecording: StateFlow<Boolean>,
    recordingSampleCount: StateFlow<Int>,
    maxRecordingSamples: Int,
    onSettingsChange: (KamperUiSettings) -> Unit,
    onClearIssues: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onExportTrace: () -> Unit,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onRestartEngine: () -> Unit,
    onDismiss: () -> Unit
) {
    val s by state.collectAsState()
    val cfg by settings.collectAsState()
    val recording by isRecording.collectAsState()
    val sampleCount by recordingSampleCount.collectAsState()
    var visible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { visible = true }

    KamperThemeProvider(isDark = cfg.isDarkTheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KamperTheme.SCRIM)
            .clickable(onClick = onDismiss)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.80f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(KamperTheme.SURFACE1)
                    .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Kamper",
                        color = KamperTheme.TEXT,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    ThemeToggle(
                        isDark = cfg.isDarkTheme,
                        onToggle = { onSettingsChange(cfg.copy(isDarkTheme = !cfg.isDarkTheme)) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(KamperTheme.BASE)
                            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(6.dp))
                            .clickable(onClick = onDismiss)
                    ) {
                        Text("✕", color = KamperTheme.SUBTEXT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    PanelTab("Activity", selectedTab == 0) { selectedTab = 0 }
                    Spacer(Modifier.width(14.dp))
                    PanelTab("Perfetto", selectedTab == 1) { selectedTab = 1 }
                    Spacer(Modifier.width(14.dp))
                    PanelTab("Issues", selectedTab == 2) { selectedTab = 2 }
                    Spacer(Modifier.width(14.dp))
                    PanelTab("Settings", selectedTab == 3) { selectedTab = 3 }
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0    -> ActivityContent(s = s, cfg = cfg)
                        1    -> PerfettoTab(
                            isRecording = recording,
                            sampleCount = sampleCount,
                            maxRecordingSamples = maxRecordingSamples,
                            onStartRecording = onStartRecording,
                            onStopRecording = onStopRecording,
                            onExportTrace = onExportTrace
                        )
                        2    -> IssuesTab(issues = s.issues, onClear = onClearIssues)
                        else -> SettingsContent(
                            s = s,
                            cfg = cfg,
                            onSettingsChange = onSettingsChange,
                            onStartEngine = onStartEngine,
                            onStopEngine = onStopEngine,
                            onRestartEngine = onRestartEngine
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    } // KamperThemeProvider
}

@Composable
private fun ThemeToggle(isDark: Boolean, onToggle: () -> Unit) {
    val outerShape = RoundedCornerShape(8.dp)
    val innerShape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .clip(outerShape)
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, outerShape)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeSegment(icon = "🌙", label = "Dark",  active = isDark,  shape = innerShape, onClick = { if (!isDark) onToggle() })
        ThemeSegment(icon = "☀",  label = "Light", active = !isDark, shape = innerShape, onClick = { if (isDark)  onToggle() })
    }
}

@Composable
private fun ThemeSegment(icon: String, label: String, active: Boolean, shape: RoundedCornerShape, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(shape)
            .background(if (active) KamperTheme.SURFACE1 else Color.Transparent)
            .clickable(onClick = onClick)
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

@Composable
private fun PanelTab(label: String, selected: Boolean, onClick: () -> Unit) {
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

// ── Activity Tab ─────────────────────────────────────────────────────────────

@Composable
private fun ActivityContent(s: KamperUiState, cfg: KamperUiSettings) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (cfg.cpuEnabled) {
            MetricCard(
                title    = "CPU",
                current  = "${s.cpuPercent.formatDp(1)}%",
                fraction = (s.cpuPercent / 100f).coerceIn(0f, 1f),
                color    = KamperTheme.BLUE,
                history  = s.cpuHistory,
                extra    = null,
                dimmed   = !cfg.showCpu
            )
        }
        if (cfg.fpsEnabled) {
            MetricCard(
                title    = "FPS",
                current  = "${s.fps}",
                fraction = (s.fps / 60f).coerceIn(0f, 1f),
                color    = KamperTheme.GREEN,
                history  = s.fpsHistory,
                extra    = if (s.fpsPeak > 0) "Peak ${s.fpsPeak}  Low ${if (s.fpsLow == Int.MAX_VALUE) "—" else "${s.fpsLow}"}" else null,
                dimmed   = !cfg.showFps
            )
        }
        if (cfg.memoryEnabled) {
            MetricCard(
                title    = "Memory",
                current  = "${s.memoryUsedMb.formatDp(0)} MB",
                fraction = (s.memoryUsedMb / 512f).coerceIn(0f, 1f),
                color    = KamperTheme.PEACH,
                history  = s.memoryHistory,
                extra    = null,
                dimmed   = !cfg.showMemory
            )
        }
        if (cfg.networkEnabled) {
            val netDisplay = if (s.downloadMbps < 0.1f)
                "${(s.downloadMbps * 1024f).formatDp(1)} KB/s"
            else
                "${s.downloadMbps.formatDp(2)} MB/s"
            MetricCard(
                title    = "Network ↓",
                current  = netDisplay,
                fraction = (s.downloadMbps / 10f).coerceIn(0f, 1f),
                color    = KamperTheme.TEAL,
                history  = s.downloadHistory,
                extra    = null,
                dimmed   = !cfg.showNetwork
            )
        }
        if (cfg.jankEnabled) {
            MetricCard(
                title    = "Jank",
                current  = "${s.jankDroppedFrames} dropped",
                fraction = (s.jankRatio).coerceIn(0f, 1f),
                color    = KamperTheme.MAUVE,
                history  = emptyList(),
                extra    = "ratio ${(s.jankRatio * 100).formatDp(1)}%",
                dimmed   = !cfg.showJank
            )
        }
        if (cfg.gcEnabled) {
            MetricCard(
                title    = "GC",
                current  = "+${s.gcCountDelta} runs",
                fraction = (s.gcCountDelta / 10f).coerceIn(0f, 1f),
                color    = KamperTheme.YELLOW,
                history  = emptyList(),
                extra    = "+${s.gcPauseMsDelta} ms pause",
                dimmed   = !cfg.showGc
            )
        }
        if (cfg.thermalEnabled) {
            val thermalFraction = when (s.thermalState) {
                com.smellouk.kamper.thermal.ThermalState.NONE      -> 0f
                com.smellouk.kamper.thermal.ThermalState.LIGHT     -> 0.2f
                com.smellouk.kamper.thermal.ThermalState.MODERATE  -> 0.4f
                com.smellouk.kamper.thermal.ThermalState.SEVERE    -> 0.6f
                com.smellouk.kamper.thermal.ThermalState.CRITICAL  -> 0.8f
                com.smellouk.kamper.thermal.ThermalState.EMERGENCY,
                com.smellouk.kamper.thermal.ThermalState.SHUTDOWN  -> 1.0f
                com.smellouk.kamper.thermal.ThermalState.UNKNOWN   -> 0f
            }
            val thermalColor = if (s.isThrottling) KamperTheme.PEACH else KamperTheme.GREEN
            MetricCard(
                title    = "Thermal",
                current  = s.thermalState.name,
                fraction = thermalFraction,
                color    = thermalColor,
                history  = emptyList(),
                extra    = if (s.isThrottling) "THROTTLING" else null,
                dimmed   = !cfg.showThermal
            )
        }
    }
}

// ── Settings Tab ──────────────────────────────────────────────────────────────

@Composable
private fun SettingsContent(
    s: KamperUiState,
    cfg: KamperUiSettings,
    onSettingsChange: (KamperUiSettings) -> Unit,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onRestartEngine: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Engine controls
        EngineSection(
            running = s.engineRunning,
            onStart = onStartEngine,
            onStop = onStopEngine,
            onRestart = onRestartEngine
        )

        SectionLabel("MODULES")

        ModuleCard(
            icon = "⚙",
            name = "CPU",
            color = KamperTheme.BLUE,
            enabled = cfg.cpuEnabled,
            showInChip = cfg.showCpu,
            intervalMs = cfg.cpuIntervalMs,
            intervalOptions = INTERVAL_OPTIONS,
            onEnabledChange = { onSettingsChange(cfg.copy(cpuEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showCpu = it)) },
            onIntervalChange = { onSettingsChange(cfg.copy(cpuIntervalMs = it)) }
        )

        ModuleCard(
            icon = "◎",
            name = "FPS",
            color = KamperTheme.GREEN,
            enabled = cfg.fpsEnabled,
            showInChip = cfg.showFps,
            intervalMs = null,
            intervalOptions = emptyList(),
            onEnabledChange = { onSettingsChange(cfg.copy(fpsEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showFps = it)) },
            onIntervalChange = {}
        )

        ModuleCard(
            icon = "▦",
            name = "Memory",
            color = KamperTheme.PEACH,
            enabled = cfg.memoryEnabled,
            showInChip = cfg.showMemory,
            intervalMs = cfg.memoryIntervalMs,
            intervalOptions = INTERVAL_OPTIONS,
            onEnabledChange = { onSettingsChange(cfg.copy(memoryEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showMemory = it)) },
            onIntervalChange = { onSettingsChange(cfg.copy(memoryIntervalMs = it)) }
        )

        ModuleCard(
            icon = "↓",
            name = "Network",
            color = KamperTheme.TEAL,
            enabled = cfg.networkEnabled,
            showInChip = cfg.showNetwork,
            intervalMs = cfg.networkIntervalMs,
            intervalOptions = INTERVAL_OPTIONS,
            onEnabledChange = { onSettingsChange(cfg.copy(networkEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showNetwork = it)) },
            onIntervalChange = { onSettingsChange(cfg.copy(networkIntervalMs = it)) }
        )

        ModuleCard(
            icon = "⚠",
            name = "Issues",
            color = KamperTheme.RED,
            enabled = cfg.issuesEnabled,
            showInChip = cfg.showIssues,
            intervalMs = cfg.issuesIntervalMs,
            intervalOptions = INTERVAL_OPTIONS,
            onEnabledChange = { onSettingsChange(cfg.copy(issuesEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showIssues = it)) },
            onIntervalChange = { onSettingsChange(cfg.copy(issuesIntervalMs = it)) },
            extraContent = { IssuesSubConfig(cfg = cfg, onChange = onSettingsChange) }
        )

        ModuleCard(
            icon = "⚡",
            name = "Jank",
            color = KamperTheme.MAUVE,
            enabled = cfg.jankEnabled,
            showInChip = cfg.showJank,
            intervalMs = null,
            intervalOptions = emptyList(),
            onEnabledChange = { onSettingsChange(cfg.copy(jankEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showJank = it)) },
            onIntervalChange = {}
        )

        ModuleCard(
            icon = "♻",
            name = "GC",
            color = KamperTheme.YELLOW,
            enabled = cfg.gcEnabled,
            showInChip = cfg.showGc,
            intervalMs = null,
            intervalOptions = emptyList(),
            onEnabledChange = { onSettingsChange(cfg.copy(gcEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showGc = it)) },
            onIntervalChange = {}
        )

        ModuleCard(
            icon = "🌡",
            name = "Thermal",
            color = KamperTheme.PEACH,
            enabled = cfg.thermalEnabled,
            showInChip = cfg.showThermal,
            intervalMs = null,
            intervalOptions = emptyList(),
            onEnabledChange = { onSettingsChange(cfg.copy(thermalEnabled = it)) },
            onShowInChipChange = { onSettingsChange(cfg.copy(showThermal = it)) },
            onIntervalChange = {}
        )
    }
}

// ── Issues Sub-Config ─────────────────────────────────────────────────────────

@Composable
private fun IssuesSubConfig(cfg: KamperUiSettings, onChange: (KamperUiSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("DETECTORS")

        // Slow Span
        DetectorCard(
            name = "Slow Span",
            enabled = cfg.slowSpanEnabled,
            onEnabledChange = { onChange(cfg.copy(slowSpanEnabled = it)) }
        ) {
            OptionRow(
                label = "Threshold",
                options = listOf(500L to "500 ms", 1_000L to "1 s", 2_000L to "2 s", 5_000L to "5 s"),
                selected = cfg.slowSpanThresholdMs,
                onSelect = { onChange(cfg.copy(slowSpanThresholdMs = it)) }
            )
        }

        // Dropped Frames
        DetectorCard(
            name = "Dropped Frames",
            enabled = cfg.droppedFramesEnabled,
            onEnabledChange = { onChange(cfg.copy(droppedFramesEnabled = it)) }
        ) {
            OptionRow(
                label = "Frame threshold",
                options = listOf(16L to "16 ms", 32L to "32 ms", 64L to "64 ms"),
                selected = cfg.droppedFrameThresholdMs,
                onSelect = { onChange(cfg.copy(droppedFrameThresholdMs = it)) }
            )
            Spacer(Modifier.height(6.dp))
            OptionRow(
                label = "Consecutive frames",
                options = listOf(2 to "2", 3 to "3", 5 to "5"),
                selected = cfg.droppedFrameConsecutiveThreshold,
                onSelect = { onChange(cfg.copy(droppedFrameConsecutiveThreshold = it)) }
            )
        }

        // Crash
        DetectorCard(
            name = "Crash",
            enabled = cfg.crashEnabled,
            onEnabledChange = { onChange(cfg.copy(crashEnabled = it)) }
        )

        // Memory Pressure
        DetectorCard(
            name = "Memory Pressure",
            enabled = cfg.memoryPressureEnabled,
            onEnabledChange = { onChange(cfg.copy(memoryPressureEnabled = it)) }
        ) {
            OptionRow(
                label = "Warning at",
                options = listOf(0.70f to "70%", 0.80f to "80%", 0.90f to "90%"),
                selected = cfg.memPressureWarningPct,
                onSelect = { onChange(cfg.copy(memPressureWarningPct = it)) }
            )
            Spacer(Modifier.height(6.dp))
            OptionRow(
                label = "Critical at",
                options = listOf(0.90f to "90%", 0.95f to "95%", 0.99f to "99%"),
                selected = cfg.memPressureCriticalPct,
                onSelect = { onChange(cfg.copy(memPressureCriticalPct = it)) }
            )
        }

        // ANR
        DetectorCard(
            name = "ANR",
            enabled = cfg.anrEnabled,
            onEnabledChange = { onChange(cfg.copy(anrEnabled = it)) }
        ) {
            OptionRow(
                label = "Threshold",
                options = listOf(2_000L to "2 s", 5_000L to "5 s", 10_000L to "10 s"),
                selected = cfg.anrThresholdMs,
                onSelect = { onChange(cfg.copy(anrThresholdMs = it)) }
            )
        }

        // Slow Start
        DetectorCard(
            name = "Slow Start",
            enabled = cfg.slowStartEnabled,
            onEnabledChange = { onChange(cfg.copy(slowStartEnabled = it)) }
        ) {
            OptionRow(
                label = "Cold start threshold",
                options = listOf(1_000L to "1 s", 2_000L to "2 s", 3_000L to "3 s"),
                selected = cfg.slowStartColdThresholdMs,
                onSelect = { onChange(cfg.copy(slowStartColdThresholdMs = it)) }
            )
            Spacer(Modifier.height(6.dp))
            OptionRow(
                label = "Warm start threshold",
                options = listOf(500L to "500 ms", 800L to "800 ms", 1_000L to "1 s"),
                selected = cfg.slowStartWarmThresholdMs,
                onSelect = { onChange(cfg.copy(slowStartWarmThresholdMs = it)) }
            )
        }
    }
}

@Composable
private fun DetectorCard(
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

// ── Engine Section ────────────────────────────────────────────────────────────

@Composable
private fun EngineSection(
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

@Composable
private fun EngineButton(
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

// ── Module Card ───────────────────────────────────────────────────────────────

@Composable
private fun ModuleCard(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KamperTheme.BASE)
            .border(
                0.5.dp,
                if (enabled) color.copy(alpha = 0.3f) else KamperTheme.BORDER,
                RoundedCornerShape(10.dp)
            )
            .animateContentSize()
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (enabled) expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
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

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = KamperTheme.SUBTEXT,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun KamperSwitch(checked: Boolean, color: Color, onCheckedChange: (Boolean) -> Unit) {
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

@Composable
private fun <T> OptionRow(
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

// ── MetricCard ────────────────────────────────────────────────────────────────

@Composable
private fun MetricCard(
    title: String,
    current: String,
    fraction: Float,
    color: Color,
    history: List<Float>,
    extra: String?,
    dimmed: Boolean = false
) {
    val tint = if (dimmed) color.copy(alpha = 0.4f) else color
    val textColor = if (dimmed) KamperTheme.SUBTEXT else KamperTheme.TEXT
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(8.dp))
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

// ── Constants ─────────────────────────────────────────────────────────────────

private val INTERVAL_OPTIONS = listOf(
    500L to "500 ms",
    1_000L to "1 s",
    2_000L to "2 s",
    5_000L to "5 s"
)
