package com.smellouk.kamper.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.UserEventInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSinceReferenceDate

internal actual class KamperUiRepository(
    actual val maxRecordingSamples: Int = 4_200
) {
    private val preferencesStore = ApplePreferencesStore()
    private val _state = MutableStateFlow(KamperUiState.EMPTY)
    private val settingsRepository = SettingsRepository(preferencesStore)
    private val recordingManager = RecordingManager(maxSamples = maxRecordingSamples)
    private val lifecycleManager = ModuleLifecycleManager(
        state            = _state,
        preferencesStore = preferencesStore
    )

    actual val state: StateFlow<KamperUiState> = _state.asStateFlow()
    actual val settings: StateFlow<KamperUiSettings> = settingsRepository.settings
    actual val isRecording: StateFlow<Boolean> = recordingManager.isRecording
    actual val recordingSampleCount: StateFlow<Int> = recordingManager.recordingSampleCount

    private val eventListener: InfoListener<UserEventInfo> = { info ->
        if (info != UserEventInfo.INVALID) {
            // NSDate.timeIntervalSinceReferenceDate = seconds since 2001-01-01; add offset to get Unix epoch ms
            val wallMs = ((NSDate.timeIntervalSinceReferenceDate + 978_307_200.0) * 1000).toLong()
            val entry = EventEntry(info.name, info.durationMs, wallMs)
            _state.update { s ->
                val updated = (listOf(entry) + s.events).take(MAX_EVENTS)
                s.copy(events = updated)
            }
        }
    }

    init {
        lifecycleManager.initialise(settingsRepository.settings.value)
        Kamper.addInfoListener(eventListener)
    }

    private companion object {
        const val MAX_EVENTS = 200
    }

    actual fun updateSettings(s: KamperUiSettings) {
        val old = settingsRepository.settings.value
        val normalized = s.copy(
            showJank    = if (!old.jankEnabled    && s.jankEnabled)    true else s.showJank,
            showGc      = if (!old.gcEnabled      && s.gcEnabled)      true else s.showGc,
            showThermal = if (!old.thermalEnabled && s.thermalEnabled) true else s.showThermal,
            showGpu     = if (!old.gpuEnabled     && s.gpuEnabled)     true else s.showGpu
        )
        settingsRepository.updateSettings(normalized)
        lifecycleManager.applySettings(old, normalized)
    }

    actual fun clearIssues() = lifecycleManager.clearIssues()
    actual fun clearEvents() = _state.update { it.copy(events = emptyList()) }
    actual fun startRecording() = recordingManager.startRecording()
    actual fun stopRecording() = recordingManager.stopRecording()
    actual fun exportTrace(): ByteArray = recordingManager.exportTrace()
    actual fun clearRecording() = recordingManager.clearRecording()
    actual fun startEngine() = lifecycleManager.startEngine()
    actual fun stopEngine() = lifecycleManager.stopEngine()
    actual fun restartEngine() = lifecycleManager.restartEngine()

    actual fun clear() {
        Kamper.removeInfoListener(eventListener)
        lifecycleManager.clear()
        settingsRepository.clear()
    }
}
