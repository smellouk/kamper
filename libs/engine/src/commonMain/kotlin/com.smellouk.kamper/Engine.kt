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
import com.smellouk.kamper.api.UserEventInfo
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

    // Phase 24 D-08/D-11: thread-safe circular event buffer.
    @PublishedApi
    internal val eventBufferLock: EngineEventLock = EngineEventLock()

    @PublishedApi
    internal val eventBuffer: ArrayDeque<EventRecord> = ArrayDeque()

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
        // UserEventInfo likewise has no PerformanceModule; seed it so
        // addInfoListener<UserEventInfo> works without install().
        mapListeners[UserEventInfo::class] = mutableListOf()
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
        // Re-seed non-module listener slots so addInfoListener continues to work
        // across full clear+install cycles.
        mapListeners[ValidationInfo::class] = mutableListOf()
        mapListeners[UserEventInfo::class] = mutableListOf()
        eventBufferLock.withLock { eventBuffer.clear() }
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

    /**
     * Phase 24 D-01. Buffer a named instant event and dispatch a
     * `KamperEvent(moduleName="event")` to integrations.
     *
     * No-op when [KamperConfig.eventsEnabled] is `false` (D-06).
     */
    fun logEvent(name: String) {
        if (!config.eventsEnabled) return
        val tsNs = engineCurrentTimeNs()
        bufferEvent(EventRecord(tsNs, name, durationNs = null))
        dispatchEvent(name = name, timestampNs = tsNs, durationMs = null)
    }

    /**
     * Phase 24 D-02. Begin a duration event. Returns an [EventToken] consumed by
     * [endEvent]. Returns a sentinel token (with startNs=0) when events are disabled —
     * endEvent on that sentinel is a safe no-op.
     */
    fun startEvent(name: String): EventToken {
        if (!config.eventsEnabled) return EventToken(name, startNs = 0L)
        return EventToken(name = name, startNs = engineCurrentTimeNs())
    }

    /**
     * Phase 24 D-03. Complete a duration event started by [startEvent]. Computes
     * `durationNs = engineCurrentTimeNs() - token.startNs`, buffers an [EventRecord],
     * and dispatches a `KamperEvent` carrying the duration in milliseconds.
     *
     * No-op when events are disabled or when token.startNs is 0 (the sentinel).
     */
    fun endEvent(token: EventToken) {
        if (!config.eventsEnabled || token.startNs == 0L) return
        val nowNs = engineCurrentTimeNs()
        val durationNs = nowNs - token.startNs
        bufferEvent(EventRecord(token.startNs, token.name, durationNs))
        dispatchEvent(
            name = token.name,
            timestampNs = token.startNs,
            durationMs = durationNs / NS_PER_MS
        )
    }

    /**
     * Phase 24 D-04. Run [block], surrounding it with [startEvent] / [endEvent].
     * The token is closed in a `finally` so durations are recorded even when [block]
     * throws — the exception is then re-raised to the caller.
     *
     * Inline so `block` is captured zero-allocation; calls only public Engine APIs
     * (no @PublishedApi member access required from the call site).
     */
    inline fun <T> measureEvent(name: String, block: () -> T): T {
        val token = startEvent(name)
        return try {
            block()
        } finally {
            endEvent(token)
        }
    }

    /**
     * Phase 24 D-07. Emit a Journey-style summary of the event buffer to [logger].
     * Format:
     * ```
     * ===> Kamper Events: begin
     *     [timestampNs=12345] user_login
     *     [timestampNs=12345] purchase
     *     [timestampNs=12345] video_playback (1024 ms)
     * worst duration: video_playback, 1024 ms
     * total events: 3
     * <=== Kamper Events: end
     * ```
     *
     * Empty buffer renders the header/footer with `total events: 0` and no body.
     */
    fun dumpEvents() {
        val snapshot = eventBufferLock.withLock { eventBuffer.toList() }
        val sb = StringBuilder()
        sb.append("===> Kamper Events: begin\n")
        snapshot.forEach { e ->
            val durMs = e.durationNs?.let { it / NS_PER_MS }
            if (durMs != null) {
                sb.append("    [timestampNs=").append(e.timestampNs)
                    .append("] ").append(e.name).append(" (").append(durMs).append(" ms)\n")
            } else {
                sb.append("    [timestampNs=").append(e.timestampNs)
                    .append("] ").append(e.name).append('\n')
            }
        }
        val worst = snapshot.filter { it.durationNs != null }
            .maxByOrNull { it.durationNs!! }
        if (worst != null) {
            val worstMs = worst.durationNs!! / NS_PER_MS
            sb.append("worst duration: ").append(worst.name)
                .append(", ").append(worstMs).append(" ms\n")
        }
        sb.append("total events: ").append(snapshot.size).append('\n')
        sb.append("<=== Kamper Events: end")
        logger.log(sb.toString())
    }

    /**
     * Phase 24 D-10. Snapshot of the current event buffer; does NOT clear the buffer.
     * Called by `RecordingManager.exportTrace()` to fold custom events into the
     * Perfetto trace.
     */
    /**
     * Phase 24 D-10. Snapshot of the current event buffer. Called by
     * [com.smellouk.kamper.ui.RecordingManager] to fold custom events into the
     * Perfetto trace at export time.
     */
    fun drainEvents(): List<EventRecord> = eventBufferLock.withLock { eventBuffer.toList() }

    private fun bufferEvent(record: EventRecord) {
        eventBufferLock.withLock {
            if (eventBuffer.size >= EVENT_BUFFER_CAPACITY) eventBuffer.removeFirst()
            eventBuffer.addLast(record)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatchEvent(name: String, timestampNs: Long, durationMs: Long?) {
        val info = UserEventInfo(name, durationMs)
        // Push to direct listeners (addInfoListener<UserEventInfo> subscribers).
        (mapListeners[UserEventInfo::class] as? MutableList<InfoListener<UserEventInfo>>)
            ?.forEach { it.invoke(info) }
        if (integrationList.isEmpty()) return
        val event = KamperEvent(
            moduleName = "event",
            timestampMs = timestampNs / NS_PER_MS,
            platform = currentPlatform,
            info = info
        )
        dispatchToIntegrations(event)
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
        private const val EVENT_BUFFER_CAPACITY: Int = 1000
        private const val NS_PER_MS: Long = 1_000_000L
    }
}
