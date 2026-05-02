package com.smellouk.kamper.web.ui

import com.smellouk.kamper.gpu.GpuInfo
import org.w3c.dom.HTMLElement

/**
 * DOM section that renders GPU performance data.
 *
 * On the web (jsMain), [GpuInfo.UNSUPPORTED] is always emitted by the GPU module (D-08)
 * because browser GPU APIs are blocked by Spectre mitigations. The section therefore
 * always displays "Unsupported" and "N/A" for memory — which is the correct behavior.
 *
 * Accent color: #cba6f7 (Catppuccin mauve) — matches the KamperPanel GPU tile (D-10).
 *
 * Display states:
 * - [GpuInfo.INVALID]: transient read failure — update is silently skipped (D-13).
 * - [GpuInfo.UNSUPPORTED]: platform capability gap — shows "Unsupported" in overlay1 gray (D-04).
 * - Valid (defensive): shows utilization % and VRAM usage in mauve (unreachable on web, D-08).
 */
internal object GpuSection {
    private lateinit var utilSpan: HTMLElement
    private lateinit var memorySpan: HTMLElement

    fun build(container: HTMLElement) {
        container.div("card") {
            p("card-title") { textContent = "GPU" }

            div("fps-hero") {
                utilSpan = span("fps-number") {
                    style.color = "#7f849c"
                    textContent = "—"
                }
                p("fps-unit") { textContent = "GPU usage %" }
            }

            div("stat-rows") {
                div("stat-row") {
                    span("stat-label") { textContent = "Memory" }
                    memorySpan = span("stat-value") { textContent = "—" }
                }
            }
        }
    }

    fun update(info: GpuInfo) {
        if (info == GpuInfo.INVALID) return
        if (info == GpuInfo.UNSUPPORTED) {
            utilSpan.textContent = "N/A"
            utilSpan.style.color = "#7f849c"
            memorySpan.textContent = "N/A"
            return
        }
        // Defensive: web jsMain unconditionally returns UNSUPPORTED per D-08, but handle
        // valid readings in case a future platform supports it.
        utilSpan.textContent = if (info.utilization >= 0.0) {
            "${info.utilization.asDynamic().toFixed(1)}%"
        } else {
            "—%"
        }
        utilSpan.style.color = "#cba6f7"
        memorySpan.textContent = buildMemoryText(info.usedMemoryMb, info.totalMemoryMb)
    }

    private fun buildMemoryText(usedMb: Double, totalMb: Double): String = when {
        usedMb >= 0.0 && totalMb >= 0.0 -> "${usedMb.asDynamic().toFixed(0)} MB / ${totalMb.asDynamic().toFixed(0)} MB"
        totalMb >= 0.0                   -> "— / ${totalMb.asDynamic().toFixed(0)} MB"
        else                             -> "N/A"
    }
}
