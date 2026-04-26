package com.smellouk.kamper.ui

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal actual class KamperUiRepository(
    context: Context,
    actual val maxRecordingSamples: Int = 4_200
) {
    private val appContext = context.applicationContext as Application
    private val preferencesStore = AndroidPreferencesStore(context)
    private val _state = MutableStateFlow(KamperUiState.EMPTY)
    private val settingsRepository = SettingsRepository(preferencesStore)
    private val recordingManager = RecordingManager(maxSamples = maxRecordingSamples)
    private val lifecycleManager = ModuleLifecycleManager(
        appContext        = appContext,
        state             = _state,
        recordingManager  = recordingManager,
        preferencesStore  = preferencesStore
    )

    actual val state: StateFlow<KamperUiState> = _state.asStateFlow()
    actual val settings: StateFlow<KamperUiSettings> = settingsRepository.settings
    actual val isRecording: StateFlow<Boolean> = recordingManager.isRecording
    actual val recordingSampleCount: StateFlow<Int> = recordingManager.recordingSampleCount

    init {
        lifecycleManager.initialise(settingsRepository.settings.value)
    }

    actual fun updateSettings(s: KamperUiSettings) {
        val old = settingsRepository.settings.value
        val normalized = s.copy(
            showJank    = if (!old.jankEnabled    && s.jankEnabled)    true else s.showJank,
            showGc      = if (!old.gcEnabled      && s.gcEnabled)      true else s.showGc,
            showThermal = if (!old.thermalEnabled && s.thermalEnabled) true else s.showThermal
        )
        settingsRepository.updateSettings(normalized)
        lifecycleManager.applySettings(old, normalized)
    }

    actual fun clearIssues() = lifecycleManager.clearIssues()
    actual fun startRecording() = recordingManager.startRecording()
    actual fun stopRecording() = recordingManager.stopRecording()
    actual fun exportTrace(): ByteArray = recordingManager.exportTrace()
    actual fun clearRecording() = recordingManager.clearRecording()
    actual fun startEngine() = lifecycleManager.startEngine()
    actual fun stopEngine() = lifecycleManager.stopEngine()
    actual fun restartEngine() = lifecycleManager.restartEngine()

    actual fun clear() {
        lifecycleManager.clear()
        settingsRepository.clear()
    }
}
