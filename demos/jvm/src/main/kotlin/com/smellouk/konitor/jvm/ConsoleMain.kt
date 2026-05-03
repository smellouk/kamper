package com.smellouk.konitor.jvm

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule

fun main() {
    println("Konitor – JVM Console Monitor (Ctrl+C to exit)")
    println("=".repeat(50))

    Konitor.apply {
        install(CpuModule)
        install(MemoryModule())
        install(NetworkModule)

        addInfoListener<CpuInfo> { info ->
            if (info == CpuInfo.INVALID) return@addInfoListener
            println(buildString {
                append("[CPU]  ")
                append("total=%-6s".format("%.1f%%".format(info.totalUseRatio * 100)))
                append("  app=%-6s".format("%.1f%%".format(info.appRatio * 100)))
                append("  user=%-6s".format("%.1f%%".format(info.userRatio * 100)))
                append("  sys=%-6s".format("%.1f%%".format(info.systemRatio * 100)))
            })
        }

        addInfoListener<MemoryInfo> { info ->
            if (info == MemoryInfo.INVALID) return@addInfoListener
            println(buildString {
                append("[MEM]  ")
                append("heap=%-10s".format("%.1f/%.1f MB".format(
                    info.heapMemoryInfo.allocatedInMb,
                    info.heapMemoryInfo.maxMemoryInMb
                )))
                append("  ram_avail=%-8s".format("%.0f MB".format(info.ramInfo.availableRamInMb)))
                append("  ram_total=%.0f MB".format(info.ramInfo.totalRamInMb))
                if (info.ramInfo.isLowMemory) append("  ⚠ LOW MEM")
            })
        }

        addInfoListener<NetworkInfo> { info ->
            if (info == NetworkInfo.INVALID) return@addInfoListener
            if (info == NetworkInfo.NOT_SUPPORTED) {
                println("[NET]  not supported on this platform")
                return@addInfoListener
            }
            println(buildString {
                append("[NET]  ")
                append("rx=%-10s".format("%.3f MB/s".format(info.rxSystemTotalInMb)))
                append("  tx=%.3f MB/s".format(info.txSystemTotalInMb))
            })
        }
    }

    Konitor.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        Konitor.stop()
        Konitor.clear()
        println("\nStopped.")
    })

    Thread.currentThread().join()
}
