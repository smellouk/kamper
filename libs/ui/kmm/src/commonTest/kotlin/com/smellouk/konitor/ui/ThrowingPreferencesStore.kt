package com.smellouk.konitor.ui

/**
 * Test fake for TEST-04b (phase 10). Throws on every PreferencesStore operation to verify
 * that SettingsRepository degrades gracefully when the underlying storage layer fails.
 *
 * D-08 OVERRIDE: CONTEXT.md D-08 specifies `IOException` as the throw type. This fake uses
 * `RuntimeException` instead because commonTest compiles to JVM + JS + Apple + WASM targets,
 * and IOException (java.io) is JVM-only. Importing it in commonTest is a hard compile error
 * on non-JVM targets. The override preserves D-08's intent (an unchecked simulated storage
 * failure SettingsRepository must catch) and changes only the exception class. See plan
 * .planning/phases/10-test-coverage/10-03-PLAN.md <objective> for the formal override record.
 */
internal class ThrowingPreferencesStore : PreferencesStore {
    private val error = RuntimeException("simulated storage failure")

    override fun getBoolean(key: String, default: Boolean): Boolean = throw error
    override fun putBoolean(key: String, value: Boolean): Unit = throw error
    override fun getLong(key: String, default: Long): Long = throw error
    override fun putLong(key: String, value: Long): Unit = throw error
    override fun getFloat(key: String, default: Float): Float = throw error
    override fun putFloat(key: String, value: Float): Unit = throw error
    override fun getInt(key: String, default: Int): Int = throw error
    override fun putInt(key: String, value: Int): Unit = throw error
    override fun getString(key: String, default: String): String = throw error
    override fun putString(key: String, value: String): Unit = throw error
}
