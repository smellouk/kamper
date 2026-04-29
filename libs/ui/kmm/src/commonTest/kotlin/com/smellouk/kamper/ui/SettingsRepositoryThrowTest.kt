package com.smellouk.kamper.ui

import kotlin.test.Test
import kotlin.test.assertNotNull

@Suppress("IllegalIdentifier")
class SettingsRepositoryThrowTest {

    private val classToTest = SettingsRepository(ThrowingPreferencesStore())

    @Test
    fun `should not throw when PreferencesStore throws on load`() {
        // D-08 contract: SettingsRepository must catch PreferencesStore exceptions during
        // initialization and return a safe default rather than propagating the exception.
        // If we reach this assertion, no exception propagated from construction or read.
        val settings = classToTest.settings.value
        assertNotNull(settings)
    }

    @Test
    fun `should not throw when PreferencesStore throws on updateSettings`() {
        // D-08 contract: SettingsRepository must catch PreferencesStore exceptions during
        // saveSettingsSync() and not propagate them to callers of updateSettings().
        val defaultSettings = classToTest.settings.value

        // Act + Assert — no exception expected
        classToTest.updateSettings(defaultSettings)
    }
}
