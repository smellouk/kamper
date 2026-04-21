package com.smellouk.kamper.samples.web.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
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
        setupKamper()
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

        val tabNames = listOf("CPU", "FPS", "Memory", "Network", "Issues")
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
        FpsSection.build(panels[1])
        MemorySection.build(panels[2])
        NetworkSection.build(panels[3])
        IssuesSection.build(panels[4])
    }

    private fun setupKamper() {
        Kamper.install(CpuModule)
        Kamper.install(FpsModule)
        Kamper.install(MemoryModule())
        Kamper.install(NetworkModule)
        Kamper.install(IssuesModule())

        Kamper.addInfoListener<CpuInfo>    { CpuSection.update(it) }
        Kamper.addInfoListener<FpsInfo>    { FpsSection.update(it) }
        Kamper.addInfoListener<MemoryInfo> { MemorySection.update(it) }
        Kamper.addInfoListener<NetworkInfo>{ NetworkSection.update(it) }
        Kamper.addInfoListener<IssueInfo>  { IssuesSection.addIssue(it.issue) }
    }

    private fun start() {
        running = true
        Kamper.start()
        statusDot.className = "status-indicator running"
        toggleBtn.textContent = "Stop"
        toggleBtn.className = "btn btn-stop"
    }

    private fun stop() {
        running = false
        Kamper.stop()
        statusDot.className = "status-indicator"
        toggleBtn.textContent = "Start"
        toggleBtn.className = "btn btn-start"
    }
}
