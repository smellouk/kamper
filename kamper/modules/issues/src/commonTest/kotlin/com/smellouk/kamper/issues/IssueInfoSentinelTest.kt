package com.smellouk.kamper.issues

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class IssueInfoSentinelTest {

    @Test
    fun `INVALID should wrap Issue INVALID`() {
        assertEquals(Issue.INVALID, IssueInfo.INVALID.issue)
    }

    @Test
    fun `Issue INVALID should have empty id, SLOW_SPAN type, INFO severity, empty message, -1L timestamp`() {
        with(Issue.INVALID) {
            assertEquals("", id)
            assertEquals(IssueType.SLOW_SPAN, type)
            assertEquals(Severity.INFO, severity)
            assertEquals("", message)
            assertEquals(-1L, timestampMs)
        }
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(IssueInfo.INVALID, IssueInfo.UNSUPPORTED)
    }
}
