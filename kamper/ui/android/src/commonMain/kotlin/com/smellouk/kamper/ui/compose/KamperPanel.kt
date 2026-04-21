package com.smellouk.kamper.ui.compose

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    onSettingsChange: (KamperUiSettings) -> Unit,
    onClearIssues: () -> Unit,
    onDismiss: () -> Unit
) {
    val s by state.collectAsState()
    val cfg by settings.collectAsState()
    var visible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { visible = true }

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
                    .fillMaxHeight(0.75f)
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
                    Text(
                        "✕",
                        color = KamperTheme.SUBTEXT,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(4.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    PanelTab("Activity", selectedTab == 0) { selectedTab = 0 }
                    Spacer(Modifier.width(14.dp))
                    PanelTab("Issues", selectedTab == 1) { selectedTab = 1 }
                    Spacer(Modifier.width(14.dp))
                    PanelTab("Perfetto", selectedTab == 2) { selectedTab = 2 }
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
                        0    -> ActivityContent(s = s)
                        1    -> IssuesTab(issues = s.issues, onClear = onClearIssues)
                        2    -> PerfettoTab()
                        else -> SettingsContent(cfg = cfg, onSettingsChange = onSettingsChange)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
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

@Composable
private fun ActivityContent(s: KamperUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(
            title   = "CPU",
            current = "${s.cpuPercent.formatDp(1)}%",
            fraction = (s.cpuPercent / 100f).coerceIn(0f, 1f),
            color   = KamperTheme.BLUE,
            history = s.cpuHistory,
            extra   = null
        )
        MetricCard(
            title   = "FPS",
            current = "${s.fps}",
            fraction = (s.fps / 60f).coerceIn(0f, 1f),
            color   = KamperTheme.GREEN,
            history = s.fpsHistory,
            extra   = if (s.fpsPeak > 0) "Peak ${s.fpsPeak}  Low ${if (s.fpsLow == Int.MAX_VALUE) "—" else "${s.fpsLow}"}" else null
        )
        MetricCard(
            title   = "Memory",
            current = "${s.memoryUsedMb.formatDp(0)} MB",
            fraction = (s.memoryUsedMb / 512f).coerceIn(0f, 1f),
            color   = KamperTheme.PEACH,
            history = s.memoryHistory,
            extra   = null
        )
        val netDisplay = if (s.downloadMbps < 0.1f)
            "${(s.downloadMbps * 1024f).formatDp(1)} KB/s"
        else
            "${s.downloadMbps.formatDp(2)} MB/s"
        MetricCard(
            title   = "Network ↓",
            current = netDisplay,
            fraction = (s.downloadMbps / 10f).coerceIn(0f, 1f),
            color   = KamperTheme.TEAL,
            history = s.downloadHistory,
            extra   = null
        )
    }
}

@Composable
private fun SettingsContent(cfg: KamperUiSettings, onSettingsChange: (KamperUiSettings) -> Unit) {
    val enabledCount = listOf(cfg.showCpu, cfg.showFps, cfg.showMemory, cfg.showNetwork, cfg.showIssues).count { it }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "CHIP METRICS",
            color = KamperTheme.SUBTEXT,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        SettingToggle("⚙  CPU",    KamperTheme.BLUE,   cfg.showCpu,     cfg.showCpu     && enabledCount == 1) { onSettingsChange(cfg.copy(showCpu     = it)) }
        SettingToggle("◎  FPS",    KamperTheme.GREEN,  cfg.showFps,     cfg.showFps     && enabledCount == 1) { onSettingsChange(cfg.copy(showFps     = it)) }
        SettingToggle("▦  Memory", KamperTheme.PEACH,  cfg.showMemory,  cfg.showMemory  && enabledCount == 1) { onSettingsChange(cfg.copy(showMemory  = it)) }
        SettingToggle("↓  Network",KamperTheme.TEAL,   cfg.showNetwork, cfg.showNetwork && enabledCount == 1) { onSettingsChange(cfg.copy(showNetwork = it)) }
        SettingToggle("⚠  Issues", KamperTheme.RED,    cfg.showIssues,  cfg.showIssues  && enabledCount == 1) { onSettingsChange(cfg.copy(showIssues  = it)) }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    color: Color,
    checked: Boolean,
    isLastEnabled: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KamperTheme.BASE)
            .border(0.5.dp, KamperTheme.BORDER, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (isLastEnabled) KamperTheme.SUBTEXT else color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = !isLastEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor          = KamperTheme.BASE,
                checkedTrackColor          = KamperTheme.BLUE,
                uncheckedThumbColor        = KamperTheme.SUBTEXT,
                uncheckedTrackColor        = KamperTheme.SURFACE,
                uncheckedBorderColor       = KamperTheme.BORDER,
                disabledCheckedThumbColor  = KamperTheme.SURFACE,
                disabledCheckedTrackColor  = KamperTheme.SUBTEXT
            )
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    current: String,
    fraction: Float,
    color: Color,
    history: List<Float>,
    extra: String?
) {
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
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                current,
                color = KamperTheme.TEXT,
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
                    .background(color)
            )
        }

        if (history.size >= 2) {
            Spacer(Modifier.height(8.dp))
            Sparkline(
                data = history,
                color = color,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}
