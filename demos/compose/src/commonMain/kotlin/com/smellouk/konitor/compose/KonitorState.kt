package com.smellouk.konitor.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.api.UserEventInfo
import com.smellouk.konitor.compose.ui.tabs.EventEntry
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.thermal.ThermalInfo
import kotlinx.coroutines.CoroutineScope

class KonitorState {
    var cpuInfo by mutableStateOf(CpuInfo.INVALID)
    var gpuInfo by mutableStateOf(GpuInfo.INVALID)
    var fpsInfo by mutableStateOf(FpsInfo.INVALID)
    var memoryInfo by mutableStateOf(MemoryInfo.INVALID)
    var networkInfo by mutableStateOf(NetworkInfo.INVALID)
    var isRunning by mutableStateOf(false)
    val issues = mutableStateListOf<Issue>()
    val userEvents = mutableStateListOf<EventEntry>()
    var jankInfo by mutableStateOf(JankInfo.INVALID)
    var gcInfo by mutableStateOf(GcInfo.INVALID)
    var thermalInfo by mutableStateOf(ThermalInfo.INVALID)

    fun addIssue(issue: Issue) {
        issues.add(0, issue)
        if (issues.size > 100) issues.removeAt(issues.size - 1)
    }

    fun clearIssues() = issues.clear()

    fun addUserEvent(info: UserEventInfo) {
        userEvents.add(0, EventEntry(info, currentTimeMs()))
        if (userEvents.size > 200) userEvents.removeAt(userEvents.size - 1)
    }

    fun clearUserEvents() = userEvents.clear()
}

expect val appTitle: String

expect fun currentTimeMs(): Long
expect fun KonitorState.initialize(scope: CoroutineScope)
expect fun startKonitor()
expect fun stopKonitor()
expect fun disposeKonitor()

/**
 * Returns true on platforms where per-app network traffic tracking is available.
 * On JVM/desktop and iOS, app-level traffic stats are not provided, so this returns false.
 */
expect fun platformSupportsAppTraffic(): Boolean
