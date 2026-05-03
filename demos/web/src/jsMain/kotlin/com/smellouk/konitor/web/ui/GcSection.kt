package com.smellouk.konitor.web.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.gc.GcInfo
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

internal object GcSection {
    private lateinit var gcCountSpan:  HTMLElement
    private lateinit var pauseSpan:    HTMLElement
    private lateinit var totalSpan:    HTMLElement
    private var simulateBtn: HTMLButtonElement? = null

    fun build(container: HTMLElement) {
        container.div("card") {
            p("card-title") { textContent = "GC" }

            div("fps-hero") {
                gcCountSpan = span("fps-number") {
                    style.color = "#f9e2af"
                    textContent = "—"
                }
                p("fps-unit") { textContent = "GC events / interval" }
            }

            div("stat-rows") {
                div("stat-row") {
                    span("stat-label") { textContent = "GC pause delta" }
                    pauseSpan = span("stat-value") { textContent = "—" }
                }
                div("stat-row") {
                    span("stat-label") { textContent = "Total GC count" }
                    totalSpan = span("stat-value") { textContent = "—" }
                }
            }

            div("card-footer") {
                simulateBtn = button("btn btn-action") {
                    textContent = "Simulate GC"
                    onclick = {
                        Konitor.logEvent("gc_simulate")
                        repeat(200_000) { js("new Array(100)") }
                        null
                    }
                }
            }
        }
    }

    fun update(info: GcInfo) {
        if (info == GcInfo.INVALID) return
        if (info == GcInfo.UNSUPPORTED) {
            gcCountSpan.textContent = "N/A"
            gcCountSpan.style.color = "#7f849c"
            pauseSpan.textContent   = "N/A"
            totalSpan.textContent   = "N/A"
            simulateBtn?.let { it.disabled = true; it.textContent = "Not supported in browser" }
            return
        }
        gcCountSpan.textContent = info.gcCountDelta.toString()
        pauseSpan.textContent   = "${info.gcPauseMsDelta} ms"
        totalSpan.textContent   = info.gcCount.toString()
    }
}
