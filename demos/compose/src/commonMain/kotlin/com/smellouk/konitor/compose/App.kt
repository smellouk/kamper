package com.smellouk.konitor.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.konitor.compose.ui.KonitorColors
import com.smellouk.konitor.compose.ui.KonitorTheme
import com.smellouk.konitor.compose.ui.tabs.CpuTab
import com.smellouk.konitor.compose.ui.tabs.EventsTab
import com.smellouk.konitor.compose.ui.tabs.FpsTab
import com.smellouk.konitor.compose.ui.tabs.GpuTab
import com.smellouk.konitor.compose.ui.tabs.GcTab
import com.smellouk.konitor.compose.ui.tabs.IssuesTab
import com.smellouk.konitor.compose.ui.tabs.JankTab
import com.smellouk.konitor.compose.ui.tabs.MemoryTab
import com.smellouk.konitor.compose.ui.tabs.NetworkTab
import com.smellouk.konitor.compose.ui.tabs.ThermalTab

@Composable
fun App() {
    val state = remember { KonitorState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        state.initialize(scope)
        startKonitor()
        state.isRunning = true
        onDispose { disposeKonitor() }
    }

    KonitorTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KonitorColors.bg)
        ) {
            Header(isRunning = state.isRunning)
            KonitorContent(state = state)
        }
    }
}

@Composable
private fun Header(isRunning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KonitorColors.mantle)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KonitorColors.lavender
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) KonitorColors.green else KonitorColors.surface1)
            )
        }
        HorizontalDivider(color = KonitorColors.surface0, thickness = 1.dp)
    }
}

@Composable
private fun KonitorContent(state: KonitorState) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("CPU", "GPU", "FPS", "Memory", "Events", "Network", "Issues", "Jank", "GC", "Thermal")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = KonitorColors.mantle,
            contentColor = KonitorColors.blue,
            edgePadding = 0.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title.uppercase(),
                            color = if (selectedTab == index) KonitorColors.blue else KonitorColors.overlay1,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KonitorColors.bg)
                .navigationBarsPadding()
        ) {
            when (selectedTab) {
                0 -> CpuTab(info = state.cpuInfo)
                1 -> GpuTab(info = state.gpuInfo)
                2 -> FpsTab(info = state.fpsInfo)
                3 -> MemoryTab(info = state.memoryInfo)
                4 -> EventsTab(userEvents = state.userEvents, onClear = { state.clearUserEvents() })
                5 -> NetworkTab(info = state.networkInfo, showAppTrafficSection = platformSupportsAppTraffic())
                6 -> IssuesTab(issues = state.issues, onClear = { state.clearIssues() })
                7 -> JankTab(info = state.jankInfo)
                8 -> GcTab(info = state.gcInfo)
                9 -> ThermalTab(info = state.thermalInfo)
            }
        }
    }
}
