package com.smellouk.kamper.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 24 D-13/D-14 verification.
 */
class UserEventInfoTest {

    @Test
    fun userEventInfo_INVALID_equals_constructor_with_empty_name_and_null_duration() {
        // covers D-14
        assertEquals(UserEventInfo("", null), UserEventInfo.INVALID)
        assertEquals("", UserEventInfo.INVALID.name)
        assertNull(UserEventInfo.INVALID.durationMs)
    }

    @Test
    fun userEventInfo_implements_Info_marker_interface() {
        // covers D-13
        val instant: Info = UserEventInfo("checkout", null)
        val duration: Info = UserEventInfo("video_decode", 1024L)
        assertTrue(instant is UserEventInfo)
        assertTrue(duration is UserEventInfo)
        assertEquals(1024L, (duration as UserEventInfo).durationMs)
    }
}
