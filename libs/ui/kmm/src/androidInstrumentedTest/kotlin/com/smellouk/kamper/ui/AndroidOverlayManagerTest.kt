package com.smellouk.kamper.ui

import android.app.Application
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidOverlayManagerTest {
    private val app = mockk<Application>(relaxed = true)
    private val state = MutableStateFlow(KamperUiState.EMPTY)
    private val settings = MutableStateFlow(KamperUiSettings())
    private val config = mockk<KamperUiConfig>(relaxed = true)
    private val onClearIssues = mockk<() -> Unit>(relaxed = true)

    private lateinit var manager: AndroidOverlayManager
    private lateinit var activity: Activity
    private lateinit var window: Window
    private lateinit var root: ViewGroup

    @Before
    fun setup() {
        every { app.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)

        manager = AndroidOverlayManager(app, state, settings, config, onClearIssues)

        activity = mockk(relaxed = true)
        window = mockk(relaxed = true)
        root = mockk(relaxed = true)
        every { activity.window } returns window
        every { window.decorView } returns root
    }

    private fun overlayViews(): MutableSet<View> {
        val field = AndroidOverlayManager::class.java.getDeclaredField("overlayViews")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(manager) as MutableSet<View>
    }

    private fun setChipView(v: View?) {
        val field = AndroidOverlayManager::class.java.getDeclaredField("chipView")
        field.isAccessible = true
        field.set(manager, v)
    }

    private fun chipView(): View? {
        val field = AndroidOverlayManager::class.java.getDeclaredField("chipView")
        field.isAccessible = true
        return field.get(manager) as View?
    }

    private fun callDetach() {
        val m = AndroidOverlayManager::class.java.getDeclaredMethod(
            "detachFromActivity",
            Activity::class.java
        )
        m.isAccessible = true
        m.invoke(manager, activity)
    }

    @Test
    fun detachFromActivity_shouldRemoveView_evenWhenRemoveViewThrows() {
        val v = mockk<View>(relaxed = true)
        every { v.parent } returns root
        overlayViews().add(v)
        every { root.removeView(v) } throws RuntimeException("boom")

        callDetach()

        assertTrue(overlayViews().isEmpty(), "overlayViews must be empty after detach even when removeView throws")
    }

    @Test
    fun detachFromActivity_shouldNullChipView_whenChipViewIsTheRemovedView() {
        val v = mockk<View>(relaxed = true)
        every { v.parent } returns root
        overlayViews().add(v)
        setChipView(v)

        callDetach()

        assertNull(chipView(), "chipView must be null after the tracked view is detached")
    }

    @Test
    fun detachFromActivity_shouldLeaveOverlayViewsEmpty_afterSuccessfulRemoval() {
        val v = mockk<View>(relaxed = true)
        every { v.parent } returns root
        overlayViews().add(v)

        callDetach()

        verify { root.removeView(v) }
        assertTrue(overlayViews().isEmpty())
    }

    @Test
    fun detachFromActivity_shouldNotRemoveViews_whenParentIsDifferentRoot() {
        val otherRoot = mockk<ViewGroup>(relaxed = true)
        val v = mockk<View>(relaxed = true)
        every { v.parent } returns otherRoot
        overlayViews().add(v)

        callDetach()

        verify(exactly = 0) { root.removeView(any()) }
        assertEquals(1, overlayViews().size, "View whose parent is not the activity root must remain tracked")
    }

    @Test
    fun detachFromActivity_onEmptyOverlayViews_shouldBeNoOp() {
        assertTrue(overlayViews().isEmpty())

        callDetach()

        verify(exactly = 0) { root.removeView(any()) }
        assertTrue(overlayViews().isEmpty())
    }
}
