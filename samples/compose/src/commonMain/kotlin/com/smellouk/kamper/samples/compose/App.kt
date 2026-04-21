package com.smellouk.kamper.samples.compose

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
import androidx.compose.material3.ScrollableTabRow
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
import com.smellouk.kamper.samples.compose.ui.KamperColors
import com.smellouk.kamper.samples.compose.ui.KamperTheme
import com.smellouk.kamper.samples.compose.ui.tabs.CpuTab
import com.smellouk.kamper.samples.compose.ui.tabs.FpsTab
import com.smellouk.kamper.samples.compose.ui.tabs.IssuesTab
import com.smellouk.kamper.samples.compose.ui.tabs.MemoryTab
import com.smellouk.kamper.samples.compose.ui.tabs.NetworkTab

@Composable
fun App() {
    val state = remember { KamperState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        state.initialize(scope)
        startKamper()
        state.isRunning = true
        onDispose { disposeKamper() }
    }

    KamperTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KamperColors.bg)
        ) {
            Header(isRunning = state.isRunning)
            KamperContent(state = state)
        }
    }
}

@Composable
private fun Header(isRunning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KamperColors.mantle)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kamper Performance Monitor",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KamperColors.lavender
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) KamperColors.green else KamperColors.surface1)
            )
        }
        HorizontalDivider(color = KamperColors.surface0, thickness = 1.dp)
    }
}

@Composable
private fun KamperContent(state: KamperState) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("CPU", "FPS", "Memory", "Network", "Issues")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = KamperColors.mantle,
            contentColor = KamperColors.blue,
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
                            color = if (selectedTab == index) KamperColors.blue else KamperColors.overlay1,
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
                .background(KamperColors.bg)
                .navigationBarsPadding()
        ) {
            when (selectedTab) {
                0 -> CpuTab(info = state.cpuInfo)
                1 -> FpsTab(info = state.fpsInfo)
                2 -> MemoryTab(info = state.memoryInfo)
                3 -> NetworkTab(info = state.networkInfo)
                4 -> IssuesTab(issues = state.issues, onClear = { state.clearIssues() })
            }
        }
    }
}
