package com.smellouk.konitor.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.smellouk.konitor.ui.KonitorUiSettings
import com.smellouk.konitor.ui.KonitorUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun ActivityTab(
    state: StateFlow<KonitorUiState>,
    settings: StateFlow<KonitorUiSettings>
) {
    val s by state.collectAsState()
    val cfg by settings.collectAsState()

    // Per-metric derived state — each MetricCard only recomposes when ITS value changes.
    // PERF-03: derivedStateOf scopes recomposition to the specific composable that reads
    // each derived value. The `remember { derivedStateOf { ... } }` wrapper is mandatory —
    // without `remember`, derivedStateOf would be recreated each recomposition (Pitfall 2).
    val cpuPercent      by remember { derivedStateOf { s.cpuPercent } }
    val cpuHistory      by remember { derivedStateOf { s.cpuHistory } }
    val fps             by remember { derivedStateOf { s.fps } }
    val fpsPeak         by remember { derivedStateOf { s.fpsPeak } }
    val fpsLow          by remember { derivedStateOf { s.fpsLow } }
    val fpsHistory      by remember { derivedStateOf { s.fpsHistory } }
    val memoryUsedMb    by remember { derivedStateOf { s.memoryUsedMb } }
    val memoryHistory   by remember { derivedStateOf { s.memoryHistory } }
    val downloadMbps    by remember { derivedStateOf { s.downloadMbps } }
    val downloadHistory by remember { derivedStateOf { s.downloadHistory } }
    val jankDropped     by remember { derivedStateOf { s.jankDroppedFrames } }
    val jankRatio       by remember { derivedStateOf { s.jankRatio } }
    val gcCountDelta    by remember { derivedStateOf { s.gcCountDelta } }
    val gcPauseDelta    by remember { derivedStateOf { s.gcPauseMsDelta } }
    val thermalState        by remember { derivedStateOf { s.thermalState } }
    val isThrottling        by remember { derivedStateOf { s.isThrottling } }
    val cpuUnsupported      by remember { derivedStateOf { s.cpuUnsupported } }
    val thermalUnsupported  by remember { derivedStateOf { s.thermalUnsupported } }
    val jankUnsupported     by remember { derivedStateOf { s.jankUnsupported } }
    val gcUnsupported       by remember { derivedStateOf { s.gcUnsupported } }
    val gpuUtilization      by remember { derivedStateOf { s.gpuUtilization } }
    val gpuUsedMemoryMb     by remember { derivedStateOf { s.gpuUsedMemoryMb } }
    val gpuTotalMemoryMb    by remember { derivedStateOf { s.gpuTotalMemoryMb } }
    val gpuHistory          by remember { derivedStateOf { s.gpuHistory } }
    val gpuUnsupported      by remember { derivedStateOf { s.gpuUnsupported } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (cfg.cpuEnabled) {
            val isCpuUnsupported = cpuUnsupported
            MetricCard(
                title       = "CPU",
                current     = if (isCpuUnsupported) "Unsupported" else "${cpuPercent.formatDp(1)}%",
                fraction    = if (isCpuUnsupported) 0f else (cpuPercent / 100f).coerceIn(0f, 1f),
                color       = KonitorTheme.BLUE,
                history     = if (isCpuUnsupported) emptyList() else cpuHistory,
                extra       = null,
                dimmed      = !cfg.showCpu,
                unsupported = isCpuUnsupported
            )
        }
        if (cfg.gpuEnabled) {
            val isGpuUnsupported = gpuUnsupported
            val memText = when {
                isGpuUnsupported -> null
                gpuUsedMemoryMb >= 0f && gpuTotalMemoryMb >= 0f ->
                    "${gpuUsedMemoryMb.formatDp(0)} MB / ${gpuTotalMemoryMb.formatDp(0)} MB"
                gpuUsedMemoryMb >= 0f -> "${gpuUsedMemoryMb.formatDp(0)} MB"
                gpuTotalMemoryMb >= 0f -> "— / ${gpuTotalMemoryMb.formatDp(0)} MB"
                else -> null
            }
            MetricCard(
                title       = "GPU",
                current     = when {
                    isGpuUnsupported    -> "Unsupported"
                    gpuUtilization < 0f -> "—%"
                    else                -> "${gpuUtilization.formatDp(1)}%"
                },
                fraction    = if (isGpuUnsupported || gpuUtilization < 0f) {
                    0f
                } else {
                    (gpuUtilization / 100f).coerceIn(0f, 1f)
                },
                color       = KonitorTheme.MAUVE,
                history     = if (isGpuUnsupported) emptyList() else gpuHistory,
                extra       = memText,
                dimmed      = !cfg.showGpu,
                unsupported = isGpuUnsupported
            )
        }
        if (cfg.fpsEnabled) {
            MetricCard(
                title    = "FPS",
                current  = "$fps",
                fraction = (fps / 60f).coerceIn(0f, 1f),
                color    = KonitorTheme.GREEN,
                history  = fpsHistory,
                extra    = if (fpsPeak > 0) {
                    "Peak $fpsPeak  Low ${if (fpsLow == Int.MAX_VALUE) "—" else "$fpsLow"}"
                } else null,
                dimmed   = !cfg.showFps
            )
        }
        if (cfg.memoryEnabled) {
            MetricCard(
                title    = "Memory",
                current  = "${memoryUsedMb.formatDp(0)} MB",
                fraction = (memoryUsedMb / 512f).coerceIn(0f, 1f),
                color    = KonitorTheme.PEACH,
                history  = memoryHistory,
                extra    = null,
                dimmed   = !cfg.showMemory
            )
        }
        if (cfg.networkEnabled) {
            val netDisplay = if (downloadMbps < 0.1f) {
                "${(downloadMbps * 1024f).formatDp(1)} KB/s"
            } else {
                "${downloadMbps.formatDp(2)} MB/s"
            }
            MetricCard(
                title    = "Network ↓",
                current  = netDisplay,
                fraction = (downloadMbps / 10f).coerceIn(0f, 1f),
                color    = KonitorTheme.TEAL,
                history  = downloadHistory,
                extra    = null,
                dimmed   = !cfg.showNetwork
            )
        }
        if (cfg.jankEnabled) {
            val isJankUnsupported = jankUnsupported
            MetricCard(
                title       = "Jank",
                current     = if (isJankUnsupported) "Unsupported" else "$jankDropped dropped",
                fraction    = if (isJankUnsupported) 0f else jankRatio.coerceIn(0f, 1f),
                color       = KonitorTheme.MAUVE,
                history     = emptyList(),
                extra       = if (isJankUnsupported) null else "ratio ${(jankRatio * 100).formatDp(1)}%",
                dimmed      = !cfg.showJank,
                unsupported = isJankUnsupported
            )
        }
        if (cfg.gcEnabled) {
            val isGcUnsupported = gcUnsupported
            MetricCard(
                title       = "GC",
                current     = if (isGcUnsupported) "Unsupported" else "+$gcCountDelta runs",
                fraction    = if (isGcUnsupported) 0f else (gcCountDelta / 10f).coerceIn(0f, 1f),
                color       = KonitorTheme.YELLOW,
                history     = emptyList(),
                extra       = if (isGcUnsupported) null else "+$gcPauseDelta ms pause",
                dimmed      = !cfg.showGc,
                unsupported = isGcUnsupported
            )
        }
        if (cfg.thermalEnabled) {
            val isThermalUnsupported = thermalUnsupported
            val thermalFraction = when (thermalState) {
                com.smellouk.konitor.thermal.ThermalState.NONE        -> 0f
                com.smellouk.konitor.thermal.ThermalState.LIGHT       -> 0.2f
                com.smellouk.konitor.thermal.ThermalState.MODERATE    -> 0.4f
                com.smellouk.konitor.thermal.ThermalState.SEVERE      -> 0.6f
                com.smellouk.konitor.thermal.ThermalState.CRITICAL    -> 0.8f
                com.smellouk.konitor.thermal.ThermalState.EMERGENCY,
                com.smellouk.konitor.thermal.ThermalState.SHUTDOWN    -> 1.0f
                com.smellouk.konitor.thermal.ThermalState.UNKNOWN     -> 0f
                com.smellouk.konitor.thermal.ThermalState.UNSUPPORTED -> 0f
            }
            val thermalColor = if (isThrottling) KonitorTheme.PEACH else KonitorTheme.GREEN
            MetricCard(
                title       = "Thermal",
                current     = if (isThermalUnsupported) "Unsupported" else thermalState.name,
                fraction    = if (isThermalUnsupported) 0f else thermalFraction,
                color       = thermalColor,
                history     = emptyList(),
                extra       = if (isThermalUnsupported || !isThrottling) null else "THROTTLING",
                dimmed      = !cfg.showThermal,
                unsupported = isThermalUnsupported
            )
        }
    }
}
