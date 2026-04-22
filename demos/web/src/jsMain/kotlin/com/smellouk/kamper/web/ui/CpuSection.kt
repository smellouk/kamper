package com.smellouk.kamper.web.ui

import com.smellouk.kamper.cpu.CpuInfo
import org.w3c.dom.HTMLElement

internal object CpuSection {
    private val rows = listOf(
        RowConfig("Total",  "#89b4fa", "totalUseRatio"),
        RowConfig("App",    "#a6e3a1", "appRatio"),
        RowConfig("User",   "#f9e2af", "userRatio"),
        RowConfig("System", "#fab387", "systemRatio"),
        RowConfig("IOWait", "#cba6f7", "ioWaitRatio")
    )

    private lateinit var fills: List<HTMLElement>
    private lateinit var values: List<HTMLElement>
    private lateinit var loadBtn: HTMLElement

    fun build(container: HTMLElement) {
        val card = container.div("card") {
            p("card-title") { textContent = "CPU Usage" }
        }

        val fillsList = mutableListOf<HTMLElement>()
        val valuesList = mutableListOf<HTMLElement>()
        rows.forEach { row ->
            var fill: HTMLElement? = null
            var value: HTMLElement? = null
            card.div("metric-row") {
                span("metric-label") { textContent = row.label }
                div("bar-track") {
                    fill = div("bar-fill") { style.background = row.color }
                }
                value = span("metric-value") { textContent = "—" }
            }
            fillsList.add(fill!!)
            valuesList.add(value!!)
        }
        fills = fillsList
        values = valuesList

        // CPU stress button
        val controls = container.div("controls")
        loadBtn = controls.button("btn btn-action") {
            textContent = "Start CPU Load"
        }
        var stressJob: Any? = null
        loadBtn.onclick = {
            if (stressJob == null) {
                stressJob = startCpuStress()
                loadBtn.textContent = "Stop CPU Load"
            } else {
                stopCpuStress(stressJob!!)
                stressJob = null
                loadBtn.textContent = "Start CPU Load"
            }
        }
    }

    fun update(info: CpuInfo) {
        if (info == CpuInfo.INVALID) return
        val ratios = listOf(
            info.totalUseRatio, info.appRatio, info.userRatio, info.systemRatio, info.ioWaitRatio
        )
        ratios.forEachIndexed { i, ratio ->
            fills[i].style.width = "${(ratio * 100).coerceIn(0.0, 100.0).toInt()}%"
            values[i].textContent = "${(ratio * 100).toInt()}%"
        }
    }

    private fun startCpuStress(): Any {
        var running = true
        val handle = js("setInterval(function() { var s=0; for(var i=0;i<5000000;i++) s+=Math.sqrt(i); }, 0)")
        return handle
    }

    private fun stopCpuStress(handle: Any) {
        js("clearInterval(handle)")
    }

    private data class RowConfig(val label: String, val color: String, val field: String)
}
