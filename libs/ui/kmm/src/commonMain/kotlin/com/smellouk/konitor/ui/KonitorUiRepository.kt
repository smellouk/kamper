package com.smellouk.konitor.ui

import kotlinx.coroutines.flow.StateFlow

internal expect class KonitorUiRepository {
    val state: StateFlow<KonitorUiState>
    val settings: StateFlow<KonitorUiSettings>
    val isRecording: StateFlow<Boolean>
    val recordingSampleCount: StateFlow<Int>
    val maxRecordingSamples: Int
    fun updateSettings(s: KonitorUiSettings)
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
