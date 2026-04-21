package com.kampersample

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule

class KamperModule(private val ctx: ReactApplicationContext) :
    ReactContextBaseJavaModule(ctx) {

    override fun getName() = "KamperModule"

    @ReactMethod
    fun start() {
        Kamper.apply {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule(ctx))
            install(NetworkModule)
            install(IssuesModule(ctx, anr = AnrConfig(thresholdMs = 5_000L)) {
                crash { chainToPreviousHandler = false }
            })

            addInfoListener<CpuInfo> { info ->
                if (info == CpuInfo.INVALID) return@addInfoListener
                emit("kamper_cpu", Arguments.createMap().apply {
                    putDouble("totalUseRatio", info.totalUseRatio.toDouble())
                    putDouble("appRatio",      info.appRatio.toDouble())
                    putDouble("userRatio",     info.userRatio.toDouble())
                    putDouble("systemRatio",   info.systemRatio.toDouble())
                    putDouble("ioWaitRatio",   info.ioWaitRatio.toDouble())
                })
            }
            addInfoListener<FpsInfo> { info ->
                if (info == FpsInfo.INVALID) return@addInfoListener
                emit("kamper_fps", Arguments.createMap().apply {
                    putInt("fps", info.fps)
                })
            }
            addInfoListener<MemoryInfo> { info ->
                if (info == MemoryInfo.INVALID) return@addInfoListener
                emit("kamper_memory", Arguments.createMap().apply {
                    putDouble("heapAllocatedMb", info.heapMemoryInfo.allocatedInMb.toDouble())
                    putDouble("heapMaxMb",       info.heapMemoryInfo.maxMemoryInMb.toDouble())
                    putDouble("ramUsedMb",       (info.ramInfo.totalRamInMb - info.ramInfo.availableRamInMb).toDouble())
                    putDouble("ramTotalMb",      info.ramInfo.totalRamInMb.toDouble())
                    putBoolean("isLowMemory",    info.ramInfo.isLowMemory)
                })
            }
            addInfoListener<NetworkInfo> { info ->
                if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@addInfoListener
                emit("kamper_network", Arguments.createMap().apply {
                    putDouble("rxMb", info.rxSystemTotalInMb.toDouble())
                    putDouble("txMb", info.txSystemTotalInMb.toDouble())
                })
            }
            addInfoListener<IssueInfo> { info ->
                emit("kamper_issues", Arguments.createMap().apply {
                    putString("id",        info.issue.id)
                    putString("type",      info.issue.type.name)
                    putString("severity",  info.issue.severity.name)
                    putString("message",   info.issue.message)
                    putDouble("timestamp", info.issue.timestampMs.toDouble())
                    info.issue.durationMs?.let { putDouble("durationMs", it.toDouble()) }
                    info.issue.threadName?.let { putString("threadName", it) }
                })
            }
        }
        Kamper.start()
    }

    @ReactMethod
    fun stop() = Kamper.stop()

    @ReactMethod fun addListener(eventName: String?) {}
    @ReactMethod fun removeListeners(count: Int) {}

    private fun emit(event: String, params: WritableMap) {
        ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(event, params)
    }
}
