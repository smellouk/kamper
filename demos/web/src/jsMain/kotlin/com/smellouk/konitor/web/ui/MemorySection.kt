package com.smellouk.konitor.web.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.memory.MemoryInfo
import org.w3c.dom.HTMLElement

private fun Float.fmt1(unit: String): String {
    val scaled = (this * 10.0).toInt()
    return "${scaled / 10}.${scaled % 10} $unit"
}

internal object MemorySection {
    private lateinit var heapFill: HTMLElement
    private lateinit var heapValue: HTMLElement
    private lateinit var ramFill: HTMLElement
    private lateinit var ramValue: HTMLElement
    private lateinit var warningEl: HTMLElement

    private val allocations = mutableListOf<ByteArray>()

    fun build(container: HTMLElement) {
        val card = container.div("card") {
            p("card-title") { textContent = "Memory" }
            div("metric-row") {
                span("metric-label") { textContent = "JS Heap" }
                div("bar-track") {
                    heapFill = div("bar-fill") { style.background = "#a6e3a1" }
                }
                heapValue = span("metric-value") { textContent = "—" }
            }
            div("metric-row") {
                span("metric-label") { textContent = "Available" }
                div("bar-track") {
                    ramFill = div("bar-fill") { style.background = "#89b4fa" }
                }
                ramValue = span("metric-value") { textContent = "—" }
            }
        }

        warningEl = container.div("warning") { textContent = "Low memory detected!" }

        val controls = container.div("controls")
        controls.button("btn btn-action") {
            textContent = "Alloc 8 MB"
            onclick = {
                Konitor.logEvent("memory_alloc_32mb")
                allocations.add(ByteArray(8 * 1024 * 1024))
            }
        }
        controls.button("btn btn-action") {
            textContent = "Free"
            onclick = {
                Konitor.logEvent("memory_gc")
                allocations.clear()
            }
        }
    }

    fun update(info: MemoryInfo) {
        if (info == MemoryInfo.INVALID) {
            heapValue.textContent = "N/A (non-Chrome)"
            ramValue.textContent = "N/A"
            return
        }

        val heap = info.heapMemoryInfo
        val ram = info.ramInfo

        val heapPct = if (heap.maxMemoryInMb > 0)
            (heap.allocatedInMb / heap.maxMemoryInMb * 100).toInt().coerceIn(0, 100)
        else 0
        heapFill.style.width = "$heapPct%"
        heapValue.textContent = heap.allocatedInMb.fmt1("MB")

        val ramPct = if (ram.totalRamInMb > 0)
            ((ram.totalRamInMb - ram.availableRamInMb) / ram.totalRamInMb * 100).toInt().coerceIn(0, 100)
        else 0
        ramFill.style.width = "$ramPct%"
        ramValue.textContent = ram.availableRamInMb.fmt1("MB")

        if (ram.isLowMemory) warningEl.classList.add("show")
        else warningEl.classList.remove("show")
    }
}
