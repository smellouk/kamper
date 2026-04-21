package com.smellouk.kamper.samples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.thermal.ThermalInfo
import kotlinx.coroutines.CoroutineScope

class KamperState {
    var cpuInfo by mutableStateOf(CpuInfo.INVALID)
    var fpsInfo by mutableStateOf(FpsInfo.INVALID)
    var memoryInfo by mutableStateOf(MemoryInfo.INVALID)
    var networkInfo by mutableStateOf(NetworkInfo.INVALID)
    var isRunning by mutableStateOf(false)
    val issues = mutableStateListOf<Issue>()
    var jankInfo by mutableStateOf(JankInfo.INVALID)
    var gcInfo by mutableStateOf(GcInfo.INVALID)
    var thermalInfo by mutableStateOf(ThermalInfo.INVALID)

    fun addIssue(issue: Issue) {
        issues.add(0, issue)
        if (issues.size > 100) issues.removeAt(issues.size - 1)
    }

    fun clearIssues() = issues.clear()
}

expect fun KamperState.initialize(scope: CoroutineScope)
expect fun startKamper()
expect fun stopKamper()
expect fun disposeKamper()
