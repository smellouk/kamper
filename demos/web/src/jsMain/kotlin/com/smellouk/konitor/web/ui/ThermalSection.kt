package com.smellouk.konitor.web.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalState
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

internal object ThermalSection {
    private lateinit var stateSpan:      HTMLElement
    private lateinit var throttlingSpan: HTMLElement
    private lateinit var stressBtn:      HTMLButtonElement
    private var stressInterval: Int = 0
    private var stressActive   = false

    fun build(container: HTMLElement) {
        container.div("card") {
            p("card-title") { textContent = "Thermal" }

            div("fps-hero") {
                stateSpan = span("fps-number") {
                    style.fontSize = "48px"
                    style.color    = "#7f849c"
                    textContent    = "UNKNOWN"
                }
                p("fps-unit") { textContent = "thermal state" }
            }

            div("stat-rows") {
                div("stat-row") {
                    span("stat-label") { textContent = "Throttling" }
                    throttlingSpan = span("stat-value") { textContent = "—" }
                }
            }

            div("card-footer") {
                stressBtn = button("btn btn-action") {
                    textContent = "Start CPU Stress"
                    onclick = { toggleStress(); null }
                }
            }
        }
    }

    fun update(info: ThermalInfo) {
        if (info == ThermalInfo.INVALID) return
        if (info == ThermalInfo.UNSUPPORTED) {
            stateSpan.textContent      = "N/A"
            stateSpan.style.color      = "#7f849c"
            stateSpan.style.fontSize   = "80px"
            throttlingSpan.textContent = "N/A"
            stressBtn.disabled         = true
            stressBtn.textContent      = "Not supported in browser"
            return
        }
        stateSpan.textContent      = info.state.name
        stateSpan.style.color      = stateColor(info.state)
        throttlingSpan.textContent = if (info.isThrottling) "YES" else "NO"
    }

    private fun stateColor(state: ThermalState): String = when (state) {
        ThermalState.NONE, ThermalState.LIGHT -> "#a6e3a1"
        ThermalState.MODERATE                 -> "#f9e2af"
        ThermalState.SEVERE,
        ThermalState.CRITICAL,
        ThermalState.EMERGENCY,
        ThermalState.SHUTDOWN                 -> "#fab387"
        ThermalState.UNKNOWN,
        ThermalState.UNSUPPORTED              -> "#7f849c"
    }

    private fun toggleStress() {
        if (!stressActive) {
            Konitor.logEvent("thermal_stress_start")
            stressActive = true
            stressBtn.textContent = "Stop CPU Stress"
            stressInterval = kotlinx.browser.window.setInterval({
                val end = kotlin.js.Date().getTime() + 40
                while (kotlin.js.Date().getTime() < end) {}
            }, 60)
        } else {
            Konitor.logEvent("thermal_stress_stop")
            stressActive = false
            stressBtn.textContent = "Start CPU Stress"
            kotlinx.browser.window.clearInterval(stressInterval)
            stressInterval = 0
        }
    }
}
