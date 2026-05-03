package com.smellouk.konitor.web.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.gc.GcModule
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.GpuModule
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.JankModule
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import com.smellouk.konitor.api.UserEventInfo
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalModule
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

internal object App {
    private var running = false
    private lateinit var statusDot: HTMLElement
    private lateinit var toggleBtn: HTMLButtonElement

    fun init() {
        val appRoot = document.getElementById("app") as HTMLElement

        buildHeader(appRoot)
        val (tabs, panels) = buildTabs(appRoot)
        buildSections(panels)
        setupTabSwitching(tabs, panels)
        setupKonitor()
        start()
    }

    private fun buildHeader(root: HTMLElement) {
        val header = document.createElement("header") as HTMLElement
        root.appendChild(header)

        val title = document.createElement("div") as HTMLElement
        title.className = "app-title"
        title.textContent = "K|Web"
        header.appendChild(title)

        val right = header.div("header-right")
        statusDot = right.div("status-indicator")
        toggleBtn = right.button("btn btn-start") {
            textContent = "Stop"
            onclick = { if (running) stop() else start() }
        }
    }

    private fun buildTabs(root: HTMLElement): Pair<List<HTMLElement>, List<HTMLElement>> {
        val nav = document.createElement("nav") as HTMLElement
        root.appendChild(nav)

        val main = document.createElement("main") as HTMLElement
        root.appendChild(main)

        val tabNames = listOf("CPU", "GPU", "FPS", "Memory", "Events", "Network", "Issues", "Jank", "GC", "Thermal")
        val tabs = tabNames.map { name ->
            (document.createElement("button") as HTMLElement).also { btn ->
                btn.className = "tab"
                btn.textContent = name
                nav.appendChild(btn)
            }
        }
        val panels = tabNames.map { name ->
            (document.createElement("div") as HTMLElement).also { panel ->
                panel.className = "panel"
                panel.id = "panel-$name"
                main.appendChild(panel)
            }
        }
        return tabs to panels
    }

    private fun setupTabSwitching(tabs: List<HTMLElement>, panels: List<HTMLElement>) {
        fun activate(index: Int) {
            tabs.forEachIndexed { i, t -> t.className = if (i == index) "tab active" else "tab" }
            panels.forEachIndexed { i, p -> p.className = if (i == index) "panel active" else "panel" }
        }
        tabs.forEachIndexed { i, btn -> btn.onclick = { activate(i) } }
        activate(0)
    }

    private fun buildSections(panels: List<HTMLElement>) {
        CpuSection.build(panels[0])
        GpuSection.build(panels[1])
        FpsSection.build(panels[2])
        MemorySection.build(panels[3])
        EventsSection.build(panels[4])
        NetworkSection.build(panels[5])
        IssuesSection.build(panels[6])
        JankSection.build(panels[7])
        GcSection.build(panels[8])
        ThermalSection.build(panels[9])
    }

    private fun setupKonitor() {
        Konitor.install(CpuModule)
        Konitor.install(GpuModule)
        Konitor.install(FpsModule)
        Konitor.install(MemoryModule())
        Konitor.install(NetworkModule)
        Konitor.install(IssuesModule())
        Konitor.install(JankModule)
        Konitor.install(GcModule)
        Konitor.install(ThermalModule)

        Konitor.addInfoListener<CpuInfo>     { CpuSection.update(it) }
        Konitor.addInfoListener<GpuInfo>     { GpuSection.update(it) }
        Konitor.addInfoListener<FpsInfo>     { FpsSection.update(it) }
        Konitor.addInfoListener<MemoryInfo>  { MemorySection.update(it) }
        Konitor.addInfoListener<NetworkInfo> { NetworkSection.update(it) }
        Konitor.addInfoListener<IssueInfo>   { IssuesSection.addIssue(it.issue) }
        Konitor.addInfoListener<JankInfo>    { JankSection.update(it) }
        Konitor.addInfoListener<GcInfo>      { GcSection.update(it) }
        Konitor.addInfoListener<ThermalInfo>    { ThermalSection.update(it) }
        Konitor.addInfoListener<UserEventInfo>  { EventsSection.addEvent(it) }
    }

    private fun start() {
        Konitor.logEvent("konitor_start")
        running = true
        Konitor.start()
        statusDot.className = "status-indicator running"
        toggleBtn.textContent = "Stop"
        toggleBtn.className = "btn btn-stop"
    }

    private fun stop() {
        Konitor.logEvent("konitor_stop")
        running = false
        Konitor.stop()
        statusDot.className = "status-indicator"
        toggleBtn.textContent = "Start"
        toggleBtn.className = "btn btn-start"
    }
}
