// Branch chosen: reflection-based call to the private detachFromActivity(Activity) method.
// Rationale: Phase 2 (FRAG-01) kept detachFromActivity() private; neither an internal
// visibility change nor an ActivityLifecycleCallbacks slot-capture is available without
// additional production-code changes. Reflection is the same approach already established
// in the sibling AndroidOverlayManagerTest.kt (see callDetach() helper there).
package com.smellouk.konitor.ui

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.view.Window
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

@Suppress("IllegalIdentifier")
class AndroidOverlayManagerDetachTest {

    private val app = mockk<Application>(relaxed = true)
    private val state = MutableStateFlow(KonitorUiState.EMPTY)
    private val settings = MutableStateFlow(KonitorUiSettings())
    private val config = mockk<KonitorUiConfig>(relaxed = true)
    private val onClearIssues = mockk<() -> Unit>(relaxed = true)

    private lateinit var classToTest: AndroidOverlayManager
    private lateinit var activity: Activity
    private lateinit var window: Window
    private lateinit var root: ViewGroup

    @Before
    fun setup() {
        every { app.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)

        classToTest = AndroidOverlayManager(app, state, settings, config, onClearIssues)

        activity = mockk(relaxed = true)
        window = mockk(relaxed = true)
        root = mockk(relaxed = true)
        every { activity.window } returns window
        every { window.decorView } returns root
    }

    @Suppress("UNCHECKED_CAST")
    private fun overlayViews(): MutableSet<View> {
        val field = AndroidOverlayManager::class.java.getDeclaredField("overlayViews")
        field.isAccessible = true
        return field.get(classToTest) as MutableSet<View>
    }

    private fun callDetach() {
        val m = AndroidOverlayManager::class.java.getDeclaredMethod(
            "detachFromActivity",
            Activity::class.java
        )
        m.isAccessible = true
        m.invoke(classToTest, activity)
    }

    @Test
    fun detachFromActivity_shouldNotPropagateException_whenRemoveViewThrows() {
        // Arrange: put a view in the tracked set whose parent is the activity root.
        val v = mockk<View>(relaxed = true)
        every { v.parent } returns root
        overlayViews().add(v)

        // Configure removeView to throw — simulates the Activity-already-destroyed scenario
        // that Phase 2 FRAG-01 guards against (D-09).
        every { root.removeView(any()) } throws RuntimeException("view not attached to window manager")

        // Act: must NOT propagate the RuntimeException.
        callDetach()

        // If we reach this line the test passes — the try-catch in detachFromActivity()
        // swallowed the exception, preserving the graceful-degradation contract (D-09).
    }
}
