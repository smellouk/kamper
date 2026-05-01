package com.smellouk.kamper.ui

import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.thermal.ThermalState

data class KamperUiState(
    val cpuPercent: Float,
    val cpuHistory: List<Float>,
    val fps: Int,
    val fpsPeak: Int,
    val fpsLow: Int,
    val fpsHistory: List<Float>,
    val memoryUsedMb: Float,
    val memoryHistory: List<Float>,
    val downloadMbps: Float,
    val downloadHistory: List<Float>,
    val issues: List<Issue> = emptyList(),
    val unreadIssueCount: Int = 0,
    val engineRunning: Boolean = true,
    val jankDroppedFrames: Int = 0,
    val jankRatio: Float = 0f,
    val gcCountDelta: Long = 0L,
    val gcPauseMsDelta: Long = 0L,
    val thermalState: ThermalState = ThermalState.NONE,
    val isThrottling: Boolean = false,
    val cpuUnsupported: Boolean = false,
    val thermalUnsupported: Boolean = false,
    val jankUnsupported: Boolean = false,
    val networkUnsupported: Boolean = false,
    val gcUnsupported: Boolean = false
) {
    companion object {
        val EMPTY = KamperUiState(
            cpuPercent = 0f,
            cpuHistory = emptyList(),
            fps = 0,
            fpsPeak = 0,
            fpsLow = Int.MAX_VALUE,
            fpsHistory = emptyList(),
            memoryUsedMb = 0f,
            memoryHistory = emptyList(),
            downloadMbps = 0f,
            downloadHistory = emptyList(),
            engineRunning = false
        )
    }
}

enum class ChipState { PEEK, EXPANDED }
