package com.smellouk.konitor.ui

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("IllegalIdentifier")
class SettingsRepositoryTest {
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val fakeStore = FakePreferencesStore()
    private val classToTest = createSettingsRepository(fakeStore, testDispatcher)

    @BeforeTest
    fun setUp() {
        // classToTest uses its own scope; fakeStore is fresh per test class instance
    }

    // ── Default state ─────────────────────────────────────────────────────────

    @Test
    fun `settings value should return defaults when store is empty`() {
        val defaults = KonitorUiSettings()
        val loaded = classToTest.settings.value
        assertEquals(defaults.showCpu, loaded.showCpu)
        assertEquals(defaults.isDarkTheme, loaded.isDarkTheme)
        assertEquals(defaults.cpuIntervalMs, loaded.cpuIntervalMs)
    }

    // ── Boolean CRUD ──────────────────────────────────────────────────────────

    @Test
    fun `updateSettings should persist showCpu change`() = runTest(testScheduler) {
        classToTest.updateSettings(classToTest.settings.value.copy(showCpu = false))
        advanceUntilIdle()
        assertFalse(fakeStore.getBoolean("show_cpu", true))
    }

    @Test
    fun `updateSettings should persist isDarkTheme change`() = runTest(testScheduler) {
        classToTest.updateSettings(classToTest.settings.value.copy(isDarkTheme = false))
        advanceUntilIdle()
        assertFalse(fakeStore.getBoolean("is_dark_theme", true))
    }

    @Test
    fun `updateSettings should persist showJank change`() = runTest(testScheduler) {
        classToTest.updateSettings(classToTest.settings.value.copy(showJank = true))
        advanceUntilIdle()
        assertEquals(true, fakeStore.getBoolean("show_jank", false))
    }

    // ── Long CRUD ─────────────────────────────────────────────────────────────

    @Test
    fun `updateSettings should persist cpuIntervalMs change`() = runTest(testScheduler) {
        classToTest.updateSettings(classToTest.settings.value.copy(cpuIntervalMs = 2_000L))
        advanceUntilIdle()
        assertEquals(2_000L, fakeStore.getLong("cpu_interval_ms", 1_000L))
    }

    @Test
    fun `updateSettings should persist anrThresholdMs change`() = runTest(testScheduler) {
        classToTest.updateSettings(classToTest.settings.value.copy(anrThresholdMs = 10_000L))
        advanceUntilIdle()
        assertEquals(10_000L, fakeStore.getLong("anr_threshold_ms", 5_000L))
    }

    // ── Float CRUD ────────────────────────────────────────────────────────────

    @Test
    fun `updateSettings should persist memPressureWarningPct change`() = runTest(testScheduler) {
        classToTest.updateSettings(classToTest.settings.value.copy(memPressureWarningPct = 0.70f))
        advanceUntilIdle()
        assertEquals(0.70f, fakeStore.getFloat("mem_pressure_warning_pct", 0.80f))
    }

    // ── Int CRUD ──────────────────────────────────────────────────────────────

    @Test
    fun `updateSettings should persist droppedFrameConsecutiveThreshold change`() =
        runTest(testScheduler) {
            classToTest.updateSettings(
                classToTest.settings.value.copy(droppedFrameConsecutiveThreshold = 5)
            )
            advanceUntilIdle()
            assertEquals(5, fakeStore.getInt("dropped_frame_consecutive", 3))
        }

    // ── In-memory StateFlow update ────────────────────────────────────────────

    @Test
    fun `updateSettings should update settings StateFlow synchronously`() =
        runTest(testScheduler) {
            classToTest.updateSettings(classToTest.settings.value.copy(showCpu = false))
            // StateFlow update is synchronous — no advanceUntilIdle needed
            assertFalse(classToTest.settings.value.showCpu)
        }

    // ── Scope cancellation ────────────────────────────────────────────────────

    @Test
    fun `clear cancels scope so subsequent saves do not reach store`() = runTest(testScheduler) {
        classToTest.clear()
        classToTest.updateSettings(classToTest.settings.value.copy(showCpu = false))
        advanceUntilIdle()
        // scope was cancelled before launch executes — store must not be written
        assertEquals(true, fakeStore.getBoolean("show_cpu", true))
    }

    // ── Round-trip persistence (TEST-01) ──────────────────────────────────────

    @Test
    fun `second instance reads value written by first instance`() = runTest(testScheduler) {
        val sharedMap = mutableMapOf<String, Any>()
        val sharedStore = FakePreferencesStore(sharedMap)
        val repo1 = createSettingsRepository(sharedStore, testDispatcher)
        val repo2 = createSettingsRepository(sharedStore, testDispatcher)

        repo1.updateSettings(repo1.settings.value.copy(isDarkTheme = false))
        advanceUntilIdle()

        val restored = repo2.loadSettings()
        assertFalse(restored.isDarkTheme)
    }

    @Test
    fun `second instance reads cpuIntervalMs written by first instance`() =
        runTest(testScheduler) {
            val sharedMap = mutableMapOf<String, Any>()
            val sharedStore = FakePreferencesStore(sharedMap)
            val repo1 = createSettingsRepository(sharedStore, testDispatcher)
            val repo2 = createSettingsRepository(sharedStore, testDispatcher)

            repo1.updateSettings(repo1.settings.value.copy(cpuIntervalMs = 3_000L))
            advanceUntilIdle()

            val restored = repo2.loadSettings()
            assertEquals(3_000L, restored.cpuIntervalMs)
        }
}

// ── Factory helpers ───────────────────────────────────────────────────────────

private fun createSettingsRepository(
    store: PreferencesStore = FakePreferencesStore(),
    dispatcher: CoroutineDispatcher = StandardTestDispatcher()
): SettingsRepository = SettingsRepository(store, dispatcher)
