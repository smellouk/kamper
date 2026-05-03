package com.smellouk.konitor

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Info
import com.smellouk.konitor.api.Performance
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class EngineValidateTest {

    private val classToTest = Engine()

    @BeforeTest
    fun setup() {
        classToTest.performanceList.clear()
        classToTest.mapListeners.clear()
        // Mirror Engine.init {} re-seeding so each test starts in the state Engine guarantees.
        classToTest.mapListeners[ValidationInfo::class] = mutableListOf()
    }

    @AfterTest
    fun teardown() {
        classToTest.performanceList.clear()
        classToTest.mapListeners.clear()
    }

    @Test
    fun `validate should return empty when no modules installed`() {
        val result = classToTest.validate()

        assertTrue(result.isEmpty(), "Expected empty list when no modules installed, got $result")
    }

    @Test
    fun `validate should report a problem when lastValidSampleAt is 0L AND installedAt is older than 10s`() {
        val performanceMock = mock<Performance<Config, IWatcher<Info>, Info>>(MockMode.autofill)
        every { performanceMock.lastValidSampleAt } returns 0L
        every { performanceMock.installedAt } returns engineCurrentTimeMs() - ELEVEN_SECONDS_MS
        classToTest.performanceList.add(performanceMock)

        val result = classToTest.validate()

        assertEquals(1, result.size, "Expected exactly 1 problem string, got $result")
        assertContains(result.first(), "no valid samples for")
        assertContains(result.first(), "(threshold: 10s)")
    }

    @Test
    fun `validate should NOT report a problem when lastValidSampleAt is 0L AND installedAt is recent`() {
        // Regression guard for the revision-1 bug: a 1-second-old module
        // must not be falsely reported as a 10-second problem.
        val performanceMock = mock<Performance<Config, IWatcher<Info>, Info>>(MockMode.autofill)
        every { performanceMock.lastValidSampleAt } returns 0L
        every { performanceMock.installedAt } returns engineCurrentTimeMs() - ONE_SECOND_MS
        classToTest.performanceList.add(performanceMock)

        val result = classToTest.validate()

        assertTrue(result.isEmpty(), "Recently-installed module must not be flagged; got $result")
    }

    @Test
    fun `addInfoListener of ValidationInfo should work without installing any module`() {
        // Pitfall 1: ValidationInfo slot must be seeded by Engine.init {}.
        classToTest.addInfoListener<ValidationInfo> { }

        val slot = classToTest.mapListeners[ValidationInfo::class]
        assertNotNull(slot, "ValidationInfo listener slot must be seeded by Engine.init")
        assertEquals(1, slot.size, "Listener must be registered, not silently dropped")
    }

    @Test
    fun `validate should emit ValidationInfo to registered listeners`() {
        val performanceMock = mock<Performance<Config, IWatcher<Info>, Info>>(MockMode.autofill)
        every { performanceMock.lastValidSampleAt } returns 0L
        every { performanceMock.installedAt } returns engineCurrentTimeMs() - ELEVEN_SECONDS_MS
        classToTest.performanceList.add(performanceMock)
        var captured: ValidationInfo? = null
        classToTest.addInfoListener<ValidationInfo> { info -> captured = info }

        val returned = classToTest.validate()

        assertNotNull(captured, "ValidationInfo listener must be invoked")
        assertEquals(returned, captured.problems, "Listener payload must equal returned list")
    }

    @Test
    fun `clear should re-seed ValidationInfo listener slot so addInfoListener works after clear`() {
        // Assumption A5: clear() must re-seed the slot.
        classToTest.addInfoListener<ValidationInfo> { }
        classToTest.clear()

        classToTest.addInfoListener<ValidationInfo> { }

        val slot = classToTest.mapListeners[ValidationInfo::class]
        assertNotNull(slot, "ValidationInfo slot must be re-seeded after clear()")
        assertEquals(1, slot.size, "New listener must be registered post-clear")
    }
}

private const val ONE_SECOND_MS: Long = 1_000L
private const val ELEVEN_SECONDS_MS: Long = 11_000L
