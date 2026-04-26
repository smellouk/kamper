package com.smellouk.kamper.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
                        0    -> ActivityTab(state = state, settings = settings)
                        1    -> PerfettoTab(
                            isRecording = recording,
                            sampleCount = sampleCount,
                            maxRecordingSamples = maxRecordingSamples,
                            onStartRecording = onStartRecording,
                            onStopRecording = onStopRecording,
                            onExportTrace = onExportTrace
                        )
                        2    -> IssuesTab(issues = s.issues, onClear = onClearIssues)
                        else -> SettingsTab(
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
