package com.smellouk.konitor.ui

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies that KonitorUi exposes public `show(context)` and `hide()` facades on the
 * Android actual. These functions are needed by external Gradle modules (e.g. the
 * react-native-konitor TurboModule) which cannot access `internal fun attach(context)`.
 *
 * Tests are structural (reflection-based) because KonitorUi.show(context) calls
 * attach(context) which requires a real Android Application — not available in JVM
 * unit tests. Compilation + reflection presence is sufficient to validate the API surface.
 */
@Suppress("IllegalIdentifier")
class KonitorUiPublicFacadeTest {

    @Test
    fun `KonitorUi should have a public show(context) method on Android actual`() {
        val method = KonitorUi::class.java.methods.find {
            it.name == "show" &&
                it.parameterCount == 1 &&
                it.parameterTypes[0].name.contains("Context")
        }
        assertNotNull(method, "KonitorUi.show(Context) must exist as a public method")
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.modifiers),
            "KonitorUi.show(Context) must be public"
        )
    }

    @Test
    fun `KonitorUi should have a public hide() method on Android actual`() {
        val method = KonitorUi::class.java.methods.find {
            it.name == "hide" && it.parameterCount == 0
        }
        assertNotNull(method, "KonitorUi.hide() must exist as a public method")
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.modifiers),
            "KonitorUi.hide() must be public"
        )
    }

    @Test
    fun `KonitorUi should still have internal attach(Context) preserved`() {
        // The internal attach(Context) is used by KonitorUiInitProvider — it must not be removed.
        // Kotlin compiles `internal` visibility to a JVM-mangled name: attach$<module_name> to
        // prevent cross-module access at the bytecode level. We find it by checking for any
        // declared method whose name starts with "attach" and takes a Context parameter.
        val method = KonitorUi::class.java.declaredMethods.find {
            it.name.startsWith("attach") &&
                it.parameterCount == 1 &&
                it.parameterTypes[0].name.contains("Context")
        }
        assertNotNull(method, "internal fun attach(Context) must still be present on KonitorUi (may have mangled name)")
    }

    @Test
    fun `KonitorUi should still have no-arg attach() preserved`() {
        // The no-arg actual fun attach() must remain (ContentProvider / expect contract)
        val method = KonitorUi::class.java.methods.find {
            it.name == "attach" && it.parameterCount == 0
        }
        assertNotNull(method, "actual fun attach() (no-arg) must still be present on KonitorUi")
    }

    @Test
    fun `KonitorUi should still have detach() preserved`() {
        val method = KonitorUi::class.java.methods.find {
            it.name == "detach" && it.parameterCount == 0
        }
        assertNotNull(method, "actual fun detach() must still be present on KonitorUi")
    }
}
