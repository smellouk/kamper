package com.smellouk.konitor

import com.smellouk.konitor.api.Info

/**
 * Health-check result emitted by [Engine.validate]. Each entry in [problems]
 * is a human-readable string describing one module that has been installed
 * for 10+ seconds without producing a valid sample (FEAT-03 / D-11, D-12).
 *
 * Register a listener via `engine.addInfoListener<ValidationInfo> { info -> ... }`.
 * The listener slot is seeded by Engine itself — no module install required.
 */
data class ValidationInfo(val problems: List<String>) : Info {
    companion object {
        val EMPTY = ValidationInfo(emptyList())
    }
}
