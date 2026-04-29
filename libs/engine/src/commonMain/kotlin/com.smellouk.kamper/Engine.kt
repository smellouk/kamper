package com.smellouk.kamper

import com.smellouk.kamper.api.Cleanable
import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.IntegrationModule
import com.smellouk.kamper.api.KamperEvent
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.currentPlatform
import kotlin.reflect.KClass

open class Engine {

    // Visible for testing
    @PublishedApi
    internal val performanceList: MutableList<Performance<*, *, *>> = mutableListOf()

    // Visible for testing
    @PublishedApi
    internal val integrationList: MutableList<IntegrationModule> = mutableListOf()

    // Visible for testing
    @PublishedApi
    internal val mapListeners: MutableMap<KClass<*>, MutableList<InfoListener<*>>> = mutableMapOf()

    // Visible for testing
    @PublishedApi
    internal var config: KamperConfig = KamperConfig.DEFAULT
        set(value) {
            logger = value.logger
            field = value
        }

    // Visible for testing
    @PublishedApi
    internal var logger: Logger = Logger.EMPTY

    init {
        // FEAT-03: ValidationInfo has no PerformanceModule that installs it,
        // so the listener-slot must be seeded here — otherwise
        // addInfoListener<ValidationInfo> would silently log "Can't add listener"
        // (RESEARCH Pitfall 1).
        mapListeners[ValidationInfo::class] = mutableListOf()
    }

    open fun start() {
        performanceList.forEach { performance ->
            performance.start()
        }
    }

    open fun stop() {
        performanceList.forEach { performance ->
            performance.stop()
        }
    }

    open fun clear() {
        // Clean integrations first so they can flush before metric modules stop.
        // Each clean() is wrapped — a buggy integration must never block teardown.
        integrationList.forEach { integration ->
            try {
                integration.clean()
            } catch (t: Throwable) {
                logger.log(
                    "IntegrationModule.clean() threw during Engine.clear(): " +
                        "${t::class.simpleName} ${t.message}"
                )
            }
        }
        integrationList.clear()

        performanceList.filterIsInstance<Cleanable>()
            .forEach { cleanable ->
                cleanable.clean()
            }
        performanceList.clear()
        mapListeners.clear()
        // Re-seed ValidationInfo slot after clear so addInfoListener<ValidationInfo>
        // continues to work across full clear+install cycles.
        mapListeners[ValidationInfo::class] = mutableListOf()
    }

    inline fun <reified I : Info> addInfoListener(noinline listener: InfoListener<I>): Engine {
        mapListeners[I::class]?.add(listener)
            ?: logger.log(
                "Can't add listener, maybe you should try to install a module before adding a listener"
            )
        return this
    }

    inline fun <reified I : Info> removeInfoListener(noinline listener: InfoListener<I>) {
        mapListeners[I::class]?.remove(listener)
    }

    /**
     * Register an [IntegrationModule] that receives a [KamperEvent] for every Info
     * emitted by an installed metric module. Per Phase 16 D-02.
     *
     * Safe to call before or after `install(...)`. Integrations registered before
     * any module is installed will start receiving events as soon as a module is
     * installed and produces an Info update.
     *
     * Returns `this` to support fluent chaining: `kamper.addIntegration(a).addIntegration(b)`.
     */
    fun addIntegration(integration: IntegrationModule): Engine {
        integrationList.add(integration)
        return this
    }

    /**
     * Remove a previously registered [IntegrationModule]. The integration's
     * [IntegrationModule.clean] is called BEFORE removal so it can shut down its
     * underlying SDK if needed. No-op if [integration] was never added.
     */
    fun removeIntegration(integration: IntegrationModule): Engine {
        if (integrationList.remove(integration)) {
            try {
                integration.clean()
            } catch (t: Throwable) {
                logger.log("IntegrationModule.clean() threw: ${t::class.simpleName} ${t.message}")
            }
        }
        return this
    }

    @PublishedApi
    internal fun dispatchToIntegrations(event: KamperEvent) {
        // Snapshot to avoid ConcurrentModificationException if an integration
        // re-enters and calls removeIntegration during onEvent. Per threat T-16-02,
        // a single integration's failure must never affect the others or Kamper core.
        val snapshot = integrationList.toList()
        for (integration in snapshot) {
            try {
                integration.onEvent(event)
            } catch (t: Throwable) {
                logger.log(
                    "IntegrationModule.onEvent threw: ${t::class.simpleName} ${t.message}"
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <C : Config, reified I : Info> uninstall(module: PerformanceModule<C, I>) {
        val target = performanceList.find { it::class == module.performance::class } ?: return
        target.stop()
        if (target is Cleanable) (target as Cleanable).clean()
        performanceList.remove(target)
        mapListeners.remove(I::class)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <C : Config, reified I : Info> install(module: PerformanceModule<C, I>) {
        with(module) {
            if (performanceList.any { it::class == performance::class }) {
                logger.log("${performance::class.simpleName} performance is already installed!")
                return
            }
            if (!mapListeners.containsKey(I::class)) {
                mapListeners[I::class] = mutableListOf()
            }

            // Internal integration fan-out listener. Added FIRST so that user listeners
            // registered later via addInfoListener still see Info updates after this one
            // has dispatched the KamperEvent. The lowercase moduleName mirrors the
            // existing Module class naming convention (e.g., CpuInfo -> "cpu").
            val moduleName: String = I::class.simpleName
                ?.removeSuffix("Info")
                ?.lowercase()
                ?: "unknown"
            val integrationFanOut: InfoListener<I> = { info ->
                if (integrationList.isNotEmpty()) {
                    val event = KamperEvent(
                        moduleName = moduleName,
                        timestampMs = engineCurrentTimeMs(),
                        platform = currentPlatform,
                        info = info
                    )
                    dispatchToIntegrations(event)
                }
            }
            (mapListeners[I::class] as MutableList<InfoListener<I>>).add(integrationFanOut)

            val isInitialized = config.isEnabled && performance.initialize(
                config,
                mapListeners[I::class] as List<InfoListener<I>>
            )
            if (isInitialized) {
                performanceList.add(performance)
            } else {
                logger.log(
                    "${performance::class.simpleName} performance is not initialized! " +
                            "could be disabled or failed to initialize."
                )
            }
        }
    }

    /**
     * Health check (FEAT-03 / D-09–D-13). Iterates installed modules and returns a
     * list of human-readable problem strings — one per module that has been
     * initialised for at least 10 seconds without delivering a sample. Also
     * emits the result to every registered [ValidationInfo] listener so callers
     * can subscribe instead of polling.
     *
     * Elapsed-time anchor (revision-1):
     * - If `lastValidSampleAt != 0L`: elapsed = nowMs - lastValidSampleAt.
     * - If `lastValidSampleAt == 0L` AND `installedAt != 0L`: elapsed = nowMs - installedAt.
     *   This is the "module installed for 10+ seconds but never delivered" SC #3 case.
     * - If both are 0L (never started): skip the module entirely.
     * Without the installedAt anchor, modules with `lastValidSampleAt == 0L` would be
     * compared against epoch (0L), so any wall-clock greater than 10_000ms would
     * trivially flag every freshly-installed module as a 10-second problem.
     *
     * This is a point-in-time call (D-13). No background scheduling. Callers
     * invoke it on demand — typically after their own setup-timeout window.
     */
    @Suppress("UNCHECKED_CAST")
    fun validate(): List<String> {
        val nowMs = engineCurrentTimeMs()
        val problems = performanceList.mapNotNull { performance ->
            val last = performance.lastValidSampleAt
            val installed = performance.installedAt
            val elapsed = when {
                last != 0L -> nowMs - last
                installed != 0L -> nowMs - installed
                else -> return@mapNotNull null  // never started
            }
            if (elapsed >= VALIDATION_THRESHOLD_MS) {
                val name = performance::class.simpleName ?: "Unknown"
                "$name: no valid samples for ${elapsed / MS_PER_SECOND}s (threshold: 10s)"
            } else {
                null
            }
        }
        // Emit the list as ValidationInfo to all registered listeners (D-12).
        (mapListeners[ValidationInfo::class] as? MutableList<InfoListener<ValidationInfo>>)
            ?.forEach { it.invoke(ValidationInfo(problems)) }
        return problems
    }

    private companion object {
        private const val VALIDATION_THRESHOLD_MS: Long = 10_000L
        private const val MS_PER_SECOND: Long = 1_000L
    }
}
