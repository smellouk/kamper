package com.smellouk.kamper.ui

import kotlinx.coroutines.flow.StateFlow

internal expect class KamperUiRepository {
    val state: StateFlow<KamperUiState>
    val settings: StateFlow<KamperUiSettings>
    fun updateSettings(s: KamperUiSettings)
    fun clearIssues()
    fun startCapture()
    fun stopCapture()
    fun clear()
}
