package com.smellouk.kamper.firebase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class FirebaseConfigBuilderTest {

    @Test
    fun `Builder defaults are forwardIssues false per D-10`() {
        val cfg = FirebaseConfig.Builder().build()
        assertFalse(cfg.forwardIssues)
    }

    @Test
    fun `Builder forwardIssues toggle propagates to FirebaseConfig`() {
        val cfg = FirebaseConfig.Builder().apply { forwardIssues = true }.build()
        assertTrue(cfg.forwardIssues)
    }

    @Test
    fun `FirebaseModule factory invokes Builder DSL and returns a FirebaseIntegrationModule`() {
        val module = FirebaseModule { forwardIssues = true }
        assertTrue(module is FirebaseIntegrationModule)
    }

    @Test
    fun `FirebaseModule factory with empty DSL still produces a module with default config`() {
        val module = FirebaseModule()
        assertEquals(FirebaseIntegrationModule::class, module::class)
    }
}
