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
            textContent = "Using navigator.connection API (Chromium-based browsers). Values reflect current bandwidth estimate."
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
            statusEl.textContent = "navigator.connection not available — use a Chromium-based browser (Chrome, Edge, Brave, Opera)."
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
        val statusElem = statusEl
        val dlElem = dlValue
        statusElem.textContent = "Measuring… (downloading 1 MB test file)"
        val t0 = js("performance.now()").unsafeCast<Double>()
        js("""
            fetch('https://speed.cloudflare.com/__down?bytes=1000000', {cache: 'no-store'})
              .then(function(r) { return r.blob(); })
              .then(function(b) {
                var secs = (performance.now() - t0) / 1000.0;
                var mbps = (b.size * 8.0 / 1000000.0) / secs;
                dlElem.textContent = mbps.toFixed(1) + ' Mbps';
                statusElem.textContent = 'Active measurement: ' + mbps.toFixed(1) + ' Mbps (' + b.size + ' bytes in ' + secs.toFixed(2) + 's)';
              }).catch(function(e) {
                statusElem.textContent = 'Measurement failed — CORS or network error: ' + e.message;
              });
        """)
    }
}
