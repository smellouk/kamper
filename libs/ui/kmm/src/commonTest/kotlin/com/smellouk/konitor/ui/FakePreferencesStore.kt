package com.smellouk.konitor.ui

internal class FakePreferencesStore(
    private val map: MutableMap<String, Any> = mutableMapOf()
) : PreferencesStore {

    override fun getBoolean(key: String, default: Boolean): Boolean =
        map[key] as? Boolean ?: default

    override fun putBoolean(key: String, value: Boolean) {
        map[key] = value
    }

    override fun getLong(key: String, default: Long): Long =
        map[key] as? Long ?: default

    override fun putLong(key: String, value: Long) {
        map[key] = value
    }

    override fun getFloat(key: String, default: Float): Float =
        map[key] as? Float ?: default

    override fun putFloat(key: String, value: Float) {
        map[key] = value
    }

    override fun getInt(key: String, default: Int): Int =
        map[key] as? Int ?: default

    override fun putInt(key: String, value: Int) {
        map[key] = value
    }

    override fun getString(key: String, default: String): String =
        map[key] as? String ?: default

    override fun putString(key: String, value: String) {
        map[key] = value
    }
}
