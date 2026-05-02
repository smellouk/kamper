package com.smellouk.kamper.web.ui

import com.smellouk.kamper.jank.JankInfo
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

internal object JankSection {
    private lateinit var droppedSpan:  HTMLElement
    private lateinit var ratioSpan:    HTMLElement
    private lateinit var worstSpan:    HTMLElement
    private var simulateBtn: HTMLButtonElement? = null

    fun build(container: HTMLElement) {
        container.div("card") {
            p("card-title") { textContent = "Jank" }

            div("fps-hero") {
                droppedSpan = span("fps-number") {
                    style.color = "#cba6f7"
                    textContent = "—"
                }
                p("fps-unit") { textContent = "dropped frames / window" }
            }

            div("stat-rows") {
                div("stat-row") {
                    span("stat-label") { textContent = "Janky ratio" }
                    ratioSpan = span("stat-value") { textContent = "—" }
                }
                div("stat-row") {
                    span("stat-label") { textContent = "Worst frame" }
                    worstSpan = span("stat-value") { textContent = "—" }
                }
            }

            div("card-footer") {
                simulateBtn = button("btn btn-action") {
                    textContent = "Simulate Jank"
                    onclick = {
                        val end = kotlin.js.Date().getTime() + 200
                        while (kotlin.js.Date().getTime() < end) { /* busy wait */ }
                        null
                    }
                }
            }
        }
    }

    fun update(info: JankInfo) {
        if (info == JankInfo.INVALID) return
        if (info == JankInfo.UNSUPPORTED) {
            droppedSpan.textContent = "N/A"
            droppedSpan.style.color = "#7f849c"
            ratioSpan.textContent   = "N/A"
            worstSpan.textContent   = "N/A"
            simulateBtn?.let { it.disabled = true; it.textContent = "Not supported in browser" }
            return
        }
        droppedSpan.textContent = info.droppedFrames.toString()
        ratioSpan.textContent   = "${(info.jankyFrameRatio * 100f * 10).toInt() / 10.0}%"
        worstSpan.textContent   = "${info.worstFrameMs} ms"
    }
}
