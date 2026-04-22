package com.smellouk.kamper.web.ui

import com.smellouk.kamper.network.NetworkInfo
import org.w3c.dom.HTMLElement

private fun Float.fmt1(unit: String): String {
    val scaled = (this * 10.0).toInt()
    return "${scaled / 10}.${scaled % 10} $unit"
}

internal object NetworkSection {
    private lateinit var dlFill: HTMLElement
    private lateinit var dlValue: HTMLElement
    private lateinit var ulFill: HTMLElement
    private lateinit var ulValue: HTMLElement
    private lateinit var statusEl: HTMLElement

    fun build(container: HTMLElement) {
        val card = container.div("card") {
            p("card-title") { textContent = "Network" }
            div("metric-row") {
                span("metric-label") { textContent = "Download" }
                div("bar-track") {
                    dlFill = div("bar-fill") { style.background = "#94e2d5" }
                }
                dlValue = span("metric-value") { textContent = "—" }
            }
            div("metric-row") {
                span("metric-label") { textContent = "Upload" }
                div("bar-track") {
                    ulFill = div("bar-fill") { style.background = "#cba6f7" }
                }
                ulValue = span("metric-value") { textContent = "—" }
            }
        }

        statusEl = container.div("card") {
            style.fontSize = "13px"
            style.color = "#7f849c"
            textContent = "Using navigator.connection API (Chrome/Edge). Values reflect current bandwidth estimate."
        }

        val controls = container.div("controls")
        controls.button("btn btn-action") {
            textContent = "Measure Bandwidth"
            onclick = { measureBandwidth() }
        }
    }

    fun update(info: NetworkInfo) {
        if (info == NetworkInfo.NOT_SUPPORTED) {
            dlValue.textContent = "N/A"
            ulValue.textContent = "N/A"
            statusEl.textContent = "Network API not available in this browser (Chrome/Edge required)."
            return
        }
        if (info == NetworkInfo.INVALID) return

        val maxMbps = 1000f
        val dlMbps = info.rxSystemTotalInMb * 8f
        val ulMbps = info.txSystemTotalInMb * 8f

        dlFill.style.width = "${(dlMbps / maxMbps * 100).coerceIn(0f, 100f).toInt()}%"
        dlValue.textContent = dlMbps.fmt1("Mbps")

        ulFill.style.width = "${(ulMbps / maxMbps * 100).coerceIn(0f, 100f).toInt()}%"
        ulValue.textContent = if (ulMbps > 0f) ulMbps.fmt1("Mbps") else "—"
    }

    private fun measureBandwidth() {
        val start = js("performance.now()").unsafeCast<Double>()
        js("""
            fetch('https://www.google.com/generate_204', { method: 'HEAD', mode: 'no-cors' })
              .then(function() {
                var elapsed = performance.now() - start;
                console.log('RTT: ' + elapsed.toFixed(0) + 'ms');
              }).catch(function(){});
        """)
    }
}
