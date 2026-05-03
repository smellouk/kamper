package com.smellouk.konitor.flutter

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.smellouk.konitor.EventToken
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.api.UserEventInfo
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.gc.GcModule
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.GpuModule
import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssueSpans
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.JankModule
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalModule
import com.smellouk.konitor.ui.KonitorUi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class KonitorFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var applicationContext: Context
    private var activity: Activity? = null
    private val eventSinks = ConcurrentHashMap<String, EventChannel.EventSink?>()
    private val channels = mutableListOf<EventChannel>()
    private val tokenMap = ConcurrentHashMap<Int, EventToken>()
    private val tokenIdCounter = AtomicInteger(0)
    private val handler = Handler(Looper.getMainLooper())

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        val messenger = binding.binaryMessenger

        val metrics = listOf("cpu", "fps", "memory", "network", "issues",
                             "jank", "gc", "thermal", "gpu", "user_event")
        for (name in metrics) {
            val ch = EventChannel(messenger, "com.smellouk.konitor/$name")
            ch.setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSinks[name] = events
                }
                override fun onCancel(arguments: Any?) {
                    eventSinks[name] = null
                }
            })
            channels.add(ch)
        }

        MethodChannel(messenger, "com.smellouk.konitor/control")
            .setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channels.forEach { it.setStreamHandler(null) }
        channels.clear()
        eventSinks.clear()
        Konitor.stop()
        Konitor.clear()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                Konitor.stop()
                Konitor.clear()
                val config = call.arguments as? Map<*, *>
                installModulesAndListeners(config)
                // Re-register the chip's event listener that Konitor.clear() wiped.
                KonitorUi.reattachEventListener()
                Konitor.start()
                result.success(null)
            }
            "stop" -> {
                Konitor.stop()
                result.success(null)
            }
            "showOverlay" -> {
                if (BuildConfig.DEBUG) {
                    activity?.runOnUiThread { KonitorUi.show(applicationContext) }
                }
                result.success(null)
            }
            "hideOverlay" -> {
                if (BuildConfig.DEBUG) {
                    activity?.runOnUiThread { KonitorUi.hide() }
                }
                result.success(null)
            }
            "logEvent" -> {
                val name = call.argument<String>("name") ?: return result.error("INVALID_ARG", "name required", null)
                Konitor.logEvent(name)
                result.success(null)
            }
            "startEvent" -> {
                val name = call.argument<String>("name") ?: return result.error("INVALID_ARG", "name required", null)
                val id = tokenIdCounter.incrementAndGet()
                tokenMap[id] = Konitor.startEvent(name)
                result.success(id)
            }
            "endEvent" -> {
                val tokenId = call.argument<Int>("tokenId") ?: return result.error("INVALID_ARG", "tokenId required", null)
                tokenMap.remove(tokenId)?.let { Konitor.endEvent(it) }
                result.success(null)
            }
            "simulateCrash" -> {
                result.success(null)
                // Deliver crash to the Flutter Issues tab directly via EventChannel.
                // The chip's CrashDetector reinstalls on activity-resume and ends up
                // outermost (chain=false), so the plugin's UncaughtExceptionHandler
                // never fires. Direct delivery is the only reliable path for the demo tab.
                val ts = System.currentTimeMillis()
                val map = mutableMapOf<String, Any>(
                    "id"        to "CRASH_${ts}_${java.util.UUID.randomUUID()}",
                    "type"      to "CRASH",
                    "severity"  to "CRITICAL",
                    "message"   to "RuntimeException: Demo crash: triggered by user",
                    "timestamp" to ts.toDouble(),
                    "threadName" to "background"
                )
                handler.post { eventSinks["issues"]?.success(map) }
                // Also throw so the chip's CrashDetector can record it.
                Thread { throw RuntimeException("Demo crash: triggered by user") }.start()
            }
            "simulateSlowSpan" -> {
                // Use IssueSpans so SlowSpanDetector fires reliably (deterministic)
                // regardless of AnrDetector watchdog phase timing.
                result.success(null)
                activity?.runOnUiThread {
                    val span = IssueSpans.begin("demo_slow_span")
                    val end = System.currentTimeMillis() + 1_200L
                    while (System.currentTimeMillis() < end) { /* busy-wait */ }
                    span.end()
                }
            }
            "simulateJank" -> {
                // Block the Android main thread for 300 ms so FrameMetricsAggregator
                // records many dropped frames that JankModule picks up.
                result.success(null)
                activity?.runOnUiThread {
                    val end = System.currentTimeMillis() + 300L
                    while (System.currentTimeMillis() < end) { /* busy-wait */ }
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) { activity = binding.activity }
    override fun onDetachedFromActivity() { activity = null }
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) { activity = binding.activity }
    override fun onDetachedFromActivityForConfigChanges() { activity = null }

    private fun installModulesAndListeners(config: Map<*, *>?) {
        if (config.flag("cpu")) {
            Konitor.install(CpuModule)
            Konitor.addInfoListener<CpuInfo> { info ->
                if (info == CpuInfo.INVALID) return@addInfoListener
                handler.post {
                    eventSinks["cpu"]?.success(mapOf(
                        "totalUseRatio" to info.totalUseRatio.toDouble(),
                        "appRatio"      to info.appRatio.toDouble(),
                        "userRatio"     to info.userRatio.toDouble(),
                        "systemRatio"   to info.systemRatio.toDouble(),
                        "ioWaitRatio"   to info.ioWaitRatio.toDouble()
                    ))
                }
            }
        }

        if (config.flag("fps")) {
            Konitor.install(FpsModule)
            Konitor.addInfoListener<FpsInfo> { info ->
                if (info == FpsInfo.INVALID) return@addInfoListener
                handler.post { eventSinks["fps"]?.success(mapOf("fps" to info.fps)) }
            }
        }

        if (config.flag("memory")) {
            Konitor.install(MemoryModule(applicationContext))
            Konitor.addInfoListener<MemoryInfo> { info ->
                if (info == MemoryInfo.INVALID) return@addInfoListener
                handler.post {
                    eventSinks["memory"]?.success(mapOf(
                        "heapAllocatedMb" to info.heapMemoryInfo.allocatedInMb.toDouble(),
                        "heapMaxMb"       to info.heapMemoryInfo.maxMemoryInMb.toDouble(),
                        "ramUsedMb"       to (info.ramInfo.totalRamInMb - info.ramInfo.availableRamInMb).toDouble(),
                        "ramTotalMb"      to info.ramInfo.totalRamInMb.toDouble(),
                        "isLowMemory"     to info.ramInfo.isLowMemory
                    ))
                }
            }
        }

        if (config.flag("network")) {
            Konitor.install(NetworkModule)
            Konitor.addInfoListener<NetworkInfo> { info ->
                if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@addInfoListener
                handler.post {
                    eventSinks["network"]?.success(mapOf(
                        "rxMb" to info.rxSystemTotalInMb.toDouble(),
                        "txMb" to info.txSystemTotalInMb.toDouble()
                    ))
                }
            }
        }

        if (config.flag("issues")) {
            Konitor.install(IssuesModule(
                applicationContext,
                anr = AnrConfig(isEnabled = true, thresholdMs = 1_000L, ignoreWhenDebuggerAttached = false)
            ) {
                crash { chainToPreviousHandler = false }
            })
            Konitor.addInfoListener<IssueInfo> { info ->
                if (info == IssueInfo.INVALID) return@addInfoListener
                val map = mutableMapOf<String, Any>(
                    "id"        to info.issue.id,
                    "type"      to info.issue.type.name,
                    "severity"  to info.issue.severity.name,
                    "message"   to info.issue.message,
                    "timestamp" to info.issue.timestampMs.toDouble()
                )
                info.issue.durationMs?.let { map["durationMs"] = it.toDouble() }
                info.issue.threadName?.let { map["threadName"] = it }
                handler.post { eventSinks["issues"]?.success(map) }
            }
        }

        if (config.flag("jank")) {
            activity?.application?.let { app ->
                Konitor.install(JankModule(app, activity))
            }
            Konitor.addInfoListener<JankInfo> { info ->
                if (info == JankInfo.INVALID) return@addInfoListener
                handler.post {
                    eventSinks["jank"]?.success(mapOf(
                        "droppedFrames" to info.droppedFrames,
                        "jankyRatio"    to info.jankyFrameRatio.toDouble(),
                        "worstFrameMs"  to info.worstFrameMs.toDouble()
                    ))
                }
            }
        }

        if (config.flag("gc")) {
            Konitor.install(GcModule)
            Konitor.addInfoListener<GcInfo> { info ->
                if (info == GcInfo.INVALID) return@addInfoListener
                handler.post {
                    eventSinks["gc"]?.success(mapOf(
                        "gcCountDelta"   to info.gcCountDelta.toDouble(),
                        "gcPauseMsDelta" to info.gcPauseMsDelta.toDouble(),
                        "gcCount"        to info.gcCount.toDouble()
                    ))
                }
            }
        }

        if (config.flag("thermal")) {
            Konitor.install(ThermalModule(applicationContext))
            Konitor.addInfoListener<ThermalInfo> { info ->
                if (info == ThermalInfo.INVALID) return@addInfoListener
                handler.post {
                    eventSinks["thermal"]?.success(mapOf(
                        "state"        to info.state.name,
                        "isThrottling" to info.isThrottling,
                        "temperatureC" to info.temperatureC
                    ))
                }
            }
        }

        if (config.flag("gpu")) {
            Konitor.install(GpuModule)
            Konitor.addInfoListener<GpuInfo> { info ->
                if (info == GpuInfo.INVALID || info == GpuInfo.UNSUPPORTED) return@addInfoListener
                handler.post {
                    eventSinks["gpu"]?.success(mapOf(
                        "utilization"         to info.utilization,
                        "usedMemoryMb"        to info.usedMemoryMb,
                        "totalMemoryMb"       to info.totalMemoryMb,
                        "curFreqKhz"          to info.curFreqKhz.toDouble(),
                        "maxFreqKhz"          to info.maxFreqKhz.toDouble(),
                        "appUtilization"      to info.appUtilization,
                        "rendererUtilization" to info.rendererUtilization,
                        "tilerUtilization"    to info.tilerUtilization,
                        "computeUtilization"  to info.computeUtilization
                    ))
                }
            }
        }

        // UserEventInfo — always wired (not config-gated, mirrors event API availability)
        Konitor.addInfoListener<UserEventInfo> { info ->
            if (info == UserEventInfo.INVALID) return@addInfoListener
            val map = mutableMapOf<String, Any>("name" to info.name)
            info.durationMs?.let { map["durationMs"] = it.toDouble() }
            handler.post { eventSinks["user_event"]?.success(map) }
        }
    }
}

private fun Map<*, *>?.flag(key: String): Boolean {
    if (this == null || !containsKey(key)) return true
    return when (val v = get(key)) {
        is Boolean -> v
        else -> true
    }
}
