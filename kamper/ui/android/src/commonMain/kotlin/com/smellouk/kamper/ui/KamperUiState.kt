package com.smellouk.kamper.ui

import com.smellouk.kamper.issues.Issue

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
    val unreadIssueCount: Int = 0
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
            downloadHistory = emptyList()
        )
    }
}

enum class ChipState { PEEK, EXPANDED }
