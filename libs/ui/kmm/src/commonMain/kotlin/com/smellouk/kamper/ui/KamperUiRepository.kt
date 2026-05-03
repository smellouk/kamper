package com.smellouk.kamper.ui

import kotlinx.coroutines.flow.StateFlow

internal expect class KamperUiRepository {
    val state: StateFlow<KamperUiState>
    val settings: StateFlow<KamperUiSettings>
    val isRecording: StateFlow<Boolean>
    val recordingSampleCount: StateFlow<Int>
    val maxRecordingSamples: Int
    fun updateSettings(s: KamperUiSettings)
    fun clearIssues()
    fun clearEvents()
    fun startRecording()
    fun stopRecording()
    fun exportTrace(): ByteArray
    fun clearRecording()
    fun startEngine()
    fun stopEngine()
    fun restartEngine()
    fun clear()
}
