package com.smellouk.kamper.ui

/**
 * KamperUi — overlay control surface.
 *
 * Asymmetry note (LOCKED IN PHASE 14 — DO NOT add `expect fun show()`):
 * - `show()` is **platform-specific only** because the two platforms accept
 *   different parameter shapes:
 *     - Android: `fun show(context: Context)` — UI overlay attaches into
 *       the supplied Context's window.
 *     - iOS:     `fun show()` — no parameter; UIApplication.sharedApplication
 *       provides the root view controller.
 *   `commonMain` cannot reference `android.content.Context`, so `show()` is
 *   defined as a non-`actual` member on each platform's actual object.
 *   Callers always know which platform they target (the TurboModule layer
 *   in `kamper/react-native/{android,ios}/`) — common code does NOT need to
 *   call `show()`.
 * - `hide()` IS symmetric: no parameter on either platform. Declared on the
 *   `expect object` so that any commonMain caller that has access to the
 *   shared kamper-ui artifact can hide the overlay.
 *
 * Future contributors: do NOT add `expect fun show()` here — it would
 * force a compromise type (`Any`) that defeats the platform-specific
 * parameter contract. The TurboModule layer is the seam where platforms
 * diverge, and that's by design.
 */
expect object KamperUi {
    fun configure(block: KamperUiConfig.() -> Unit)
    fun attach()
    fun detach()

    /**
     * Hide the overlay. Public facade for external consumers
     * (e.g. react-native-kamper TurboModule). Equivalent to detach().
     */
    fun hide()
}
