package com.smellouk.kamper.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smellouk.kamper.ui.KamperUiSettings
import com.smellouk.kamper.ui.KamperUiState

@Composable
internal fun SettingsTab(
    s: KamperUiState,
    cfg: KamperUiSettings,
    onSettingsChange: (KamperUiSettings) -> Unit,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onRestartEngine: () -> Unit,
    isTv: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isTv) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                ThemeToggle(
                    isDark = cfg.isDarkTheme,
                    onToggle = { onSettingsChange(cfg.copy(isDarkTheme = !cfg.isDarkTheme)) }
                )
            }
        }

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

@Composable
private fun IssuesSubConfig(cfg: KamperUiSettings, onChange: (KamperUiSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("DETECTORS")

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

        DetectorCard(
            name = "Crash",
            enabled = cfg.crashEnabled,
            onEnabledChange = { onChange(cfg.copy(crashEnabled = it)) }
        )

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
